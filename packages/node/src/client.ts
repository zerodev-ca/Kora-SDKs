import { KoraError } from "./error.js";
import { Resource, type Transport } from "./resource.js";
import { Signature } from "./signature.js";
import type { ClientOptions, Customer, CustomerInput, License, LicenseInput, Product, ProductInput, ValidationResponse } from "./types.js";

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

    async validate(key: string, product?: string): Promise<ValidationResponse> {
        const name = product !== undefined ? product : this.options.product;
        const reply = await this.send("POST", "/licenses/validate", { license_key: key, ...(name !== undefined ? { product_name: name } : {}) });
        if (reply.status === 429 || reply.status >= 500) this.fail(reply);
        if (this.options.publicKey !== undefined && this.options.publicKey.length > 0 && !this.signature.verify(reply.text, reply.signature, this.options.publicKey)) {
            throw new KoraError("Response signature verification failed", reply.status);
        }
        return reply.data as unknown as ValidationResponse;
    }
}
