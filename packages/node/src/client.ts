import { ClientOptions, License, ValidationResponse } from "./types.js";
import { Heartbeat } from "./heartbeat.js";

export class Client {
    private options: ClientOptions;
    private heartbeat: Heartbeat | null;

    constructor(options: ClientOptions) {
        this.options = options;
        this.heartbeat = null;
    }

    endpoint(): string {
        return `${this.options.url.replace(/\/+$/, "").replace(/\/api\/v1$/, "")}/api/v1/licenses/validate`;
    }

    private key(customKey?: string): string {
        const key = customKey !== undefined ? customKey : this.options.key;
        if (key === undefined || key.length === 0) {
            throw new Error("License key is required");
        }
        return key;
    }

    async validate(customKey?: string): Promise<ValidationResponse> {
        const key = this.key(customKey);
        const response = await fetch(this.endpoint(), {
            method: "POST",
            headers: {
                "content-type": "application/json",
                "accept": "application/json",
                ...(this.options.apiKey !== undefined && this.options.apiKey.length > 0 ? { authorization: `Bearer ${this.options.apiKey}` } : {})
            },
            body: JSON.stringify({ license_key: key, product_name: this.options.product }),
            signal: AbortSignal.timeout(this.options.timeoutMs !== undefined ? this.options.timeoutMs : 15000)
        });
        const data = await response.json().catch(() => ({})) as { valid?: unknown; message?: unknown; error?: unknown; license?: unknown };
        const result: ValidationResponse = {
            valid: data.valid === true,
            message: typeof data.message === "string" ? data.message : typeof data.error === "string" ? data.error : `Kora answered with status ${response.status}`,
            code: response.status,
            license: data.license !== undefined && data.license !== null ? data.license as License : null
        };
        if (result.valid) {
            this.watch(key);
        }
        return result;
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
