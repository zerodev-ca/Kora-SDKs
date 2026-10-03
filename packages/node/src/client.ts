import { randomBytes } from "crypto";
import { ClientOptions, License, OfflineLicense, OfflineResponse, Release, UpdateResponse, ValidationResponse } from "./types.js";
import { Heartbeat } from "./heartbeat.js";
import { Machine } from "./machine.js";
import { Signature } from "./signature.js";

type Answer = Record<string, unknown>;

export class Client {
    private options: ClientOptions;
    private heartbeat: Heartbeat | null;
    private machine: Machine;
    private signature: Signature;

    constructor(options: ClientOptions) {
        this.options = options;
        this.heartbeat = null;
        this.machine = new Machine();
        this.signature = new Signature();
    }

    base(): string {
        return this.options.url.replace(/\/+$/, "").replace(/\/api\/v1$/, "");
    }

    hwid(): string {
        return this.options.hwid !== undefined && this.options.hwid.length > 0 ? this.options.hwid : this.machine.hwid();
    }

    private key(customKey?: string): string {
        const key = customKey !== undefined ? customKey : this.options.key;
        if (key === undefined || key.length === 0) {
            throw new Error("License key is required");
        }
        return key;
    }

    private async post(path: string, extra: Record<string, unknown>, key: string): Promise<{ status: number; data: Answer }> {
        const nonce = randomBytes(16).toString("hex");
        const response = await fetch(`${this.base()}/api/v1${path}`, {
            method: "POST",
            headers: {
                "content-type": "application/json",
                "accept": "application/json",
                ...(this.options.apiKey !== undefined && this.options.apiKey.length > 0 ? { authorization: `Bearer ${this.options.apiKey}` } : {})
            },
            body: JSON.stringify({
                license_key: key,
                product_name: this.options.product,
                hwid: this.hwid(),
                device_name: this.machine.name(),
                platform: this.machine.platform(),
                ...(this.options.version !== undefined ? { version: this.options.version } : {}),
                nonce,
                ...extra
            }),
            signal: AbortSignal.timeout(this.options.timeoutMs !== undefined ? this.options.timeoutMs : 15000)
        });
        const text = await response.text();
        if (this.options.publicKey !== undefined && this.options.publicKey.length > 0) {
            this.check(text, response.headers.get("x-kora-signature"), nonce);
        }
        try {
            return { status: response.status, data: JSON.parse(text) as Answer };
        } catch {
            return { status: response.status, data: {} };
        }
    }

    private check(text: string, signature: string | null, nonce: string): void {
        if (signature === null || !this.signature.verify(text, signature, this.options.publicKey as string)) {
            throw new Error("Response signature verification failed");
        }
        const parsed = JSON.parse(text) as Answer;
        if (parsed.nonce !== nonce) {
            throw new Error("Response does not belong to this request");
        }
        const skew = this.options.maxSkewMs !== undefined ? this.options.maxSkewMs : 300000;
        if (typeof parsed.timestamp !== "number" || Math.abs(Date.now() - parsed.timestamp) > skew) {
            throw new Error("Response is too old, check this machine's clock");
        }
    }

    private result(status: number, data: Answer): ValidationResponse {
        return {
            valid: data.valid === true,
            message: typeof data.message === "string" ? data.message : typeof data.error === "string" ? data.error : `Kora answered with status ${status}`,
            code: status,
            license: data.license !== undefined && data.license !== null ? data.license as License : null,
            nonce: typeof data.nonce === "string" ? data.nonce : null,
            timestamp: typeof data.timestamp === "number" ? data.timestamp : null
        };
    }

    async validate(customKey?: string): Promise<ValidationResponse> {
        const key = this.key(customKey);
        const answer = await this.post("/licenses/validate", {}, key);
        const result = this.result(answer.status, answer.data);
        if (result.valid) {
            this.watch(key);
        }
        return result;
    }

    async offline(customKey?: string): Promise<OfflineResponse> {
        const answer = await this.post("/licenses/offline", {}, this.key(customKey));
        return { ...this.result(answer.status, answer.data), offline: answer.data.offline !== undefined ? answer.data.offline as OfflineLicense : null };
    }

    verifyOffline(content: string | OfflineLicense): OfflineLicense | null {
        if (this.options.publicKey === undefined || this.options.publicKey.length === 0) {
            throw new Error("A public key is required to verify offline licenses");
        }
        try {
            const parsed = (typeof content === "string" ? JSON.parse(content) : content) as OfflineLicense;
            const { signature, ...fields } = parsed;
            switch (true) {
                case typeof signature !== "string":
                case !this.signature.verify(this.signature.canonical(fields), signature, this.options.publicKey):
                case fields.product.toLowerCase() !== this.options.product.toLowerCase():
                case fields.hwid.length > 0 && fields.hwid !== this.hwid():
                case Date.now() > fields.expires_at + fields.grace_period_ms:
                    return null;
                default:
                    return parsed;
            }
        } catch {
            return null;
        }
    }

    async checkUpdate(version: string, channel: "stable" | "beta" = "stable", customKey?: string): Promise<UpdateResponse> {
        const answer = await this.post("/updates/check", { version, channel }, this.key(customKey));
        return {
            ...this.result(answer.status, answer.data),
            update_available: answer.data.update_available === true,
            current: typeof answer.data.current === "string" ? answer.data.current : null,
            latest: answer.data.latest !== undefined && answer.data.latest !== null ? answer.data.latest as Release : null,
            download_url: typeof answer.data.download_url === "string" ? answer.data.download_url : null
        };
    }

    private watch(key: string): void {
        if (this.options.intervalMs === undefined || this.options.intervalMs <= 0 || this.heartbeat !== null) {
            return;
        }
        this.heartbeat = new Heartbeat(this.options.intervalMs, async () => {
            const result = await this.validate(key);
            if (!result.valid && this.options.onInvalid !== undefined) {
                this.options.onInvalid(result);
            }
        });
        this.heartbeat.start();
    }

    stop(): void {
        if (this.heartbeat !== null) {
            this.heartbeat.stop();
            this.heartbeat = null;
        }
    }
}
