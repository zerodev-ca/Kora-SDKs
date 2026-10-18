import { KoraError } from "./error.js";
import { Resource, type Transport } from "./resource.js";
import { Signature } from "./signature.js";
import type { ClientOptions, Customer, CustomerInput, DeactivationResponse, License, LicenseInput, OfflineLicense, Product, ProductInput, ValidationResponse } from "./types.js";

type Reply = { status: number; text: string; signature: string | null; data: Record<string, unknown> };

export class Client implements Transport {
    readonly licenses: Resource<License, LicenseInput>;
    readonly products: Resource<Product, ProductInput>;
    readonly customers: Resource<Customer, CustomerInput>;
    private options: ClientOptions;
    private signature: Signature;

    constructor(options: ClientOptions) {
        this.options = options;
        this.signature = new Signature();
        this.licenses = new Resource(this, "licenses", "license");
        this.products = new Resource(this, "products", "product");
        this.customers = new Resource(this, "customers", "customer");
    }

    url(path: string): string {
        return `${this.options.url.replace(/\/+$/, "").replace(/\/api\/v1$/, "")}/api/v1${path}`;
    }

    private async send(method: string, path: string, body?: unknown): Promise<Reply> {
        const response = await fetch(this.url(path), {
            method,
            headers: {
                "content-type": "application/json",
                accept: "application/json",
                ...(this.options.apiKey !== undefined && this.options.apiKey.length > 0 ? { authorization: `Bearer ${this.options.apiKey}` } : {})
            },
            body: body === undefined ? undefined : JSON.stringify(body),
            signal: AbortSignal.timeout(this.options.timeoutMs !== undefined ? this.options.timeoutMs : 15000)
        });
        const text = await response.text();
        return { status: response.status, text, signature: response.headers.get("x-kora-signature"), data: text.startsWith("{") ? JSON.parse(text) : {} };
    }

    private fail(reply: Reply): never {
        throw new KoraError(typeof reply.data.error === "string" ? reply.data.error : `Kora answered with status ${reply.status}`, reply.status);
    }

    async call<T>(method: string, path: string, body?: unknown): Promise<T> {
        const reply = await this.send(method, path, body);
        if (reply.status >= 400) this.fail(reply);
        return reply.data as T;
    }

    private async check(path: string, body: Record<string, unknown>): Promise<Record<string, unknown>> {
        const reply = await this.send("POST", path, body);
        if (reply.status === 429 || reply.status >= 500) this.fail(reply);
        if (this.options.publicKey !== undefined && this.options.publicKey.length > 0 && !this.signature.verify(reply.text, reply.signature, this.options.publicKey)) {
            throw new KoraError("Response signature verification failed", reply.status);
        }
        return reply.data;
    }

    private body(key: string, product: string | undefined, fields: Record<string, unknown>): Record<string, unknown> {
        const name = product !== undefined ? product : this.options.product;
        return { ...fields, license_key: key, ...(name !== undefined ? { product_name: name } : {}) };
    }

    async validate(key: string, product?: string, fields: Record<string, unknown> = {}): Promise<ValidationResponse> {
        return await this.check("/licenses/validate", this.body(key, product, fields)) as unknown as ValidationResponse;
    }

    async activate(key: string, product?: string, fields: Record<string, unknown> = {}): Promise<ValidationResponse> {
        return await this.check("/licenses/activate", this.body(key, product, fields)) as unknown as ValidationResponse;
    }

    async deactivate(key: string, fields: Record<string, unknown> = {}): Promise<DeactivationResponse> {
        return await this.check("/licenses/deactivate", { ...fields, license_key: key }) as unknown as DeactivationResponse;
    }

    async file(id: number | string): Promise<string> {
        const reply = await this.send("GET", `/licenses/${encodeURIComponent(String(id))}/file`);
        if (reply.status >= 400) this.fail(reply);
        return reply.text.trim();
    }

    offline(file: string): OfflineLicense {
        if (this.options.publicKey === undefined || this.options.publicKey.length === 0) throw new KoraError("A publicKey is required to read offline licenses", 0);
        const [payload, signature] = file.trim().split(".");
        if (payload === undefined || signature === undefined) throw new KoraError("Offline license file is malformed", 0);
        const text = Buffer.from(payload, "base64url").toString("utf8");
        if (!this.signature.verify(text, Buffer.from(signature, "base64url").toString("base64"), this.options.publicKey)) throw new KoraError("Offline license signature verification failed", 0);
        const license = JSON.parse(text) as OfflineLicense;
        if (Date.parse(license.refresh_after) < Date.now()) throw new KoraError("Offline license needs a refresh", 0);
        if (license.expires_at !== null && Date.parse(license.expires_at) < Date.now()) throw new KoraError("Offline license expired", 0);
        return license;
    }

    async merge(customer: number | string, duplicate: number | string): Promise<Customer> {
        return (await this.call<{ customer: Customer }>("POST", `/customers/${encodeURIComponent(String(customer))}/merge`, { customer_id: duplicate })).customer;
    }
}
