import { ClientOptions, ValidationRequest, ValidationResponse } from "./types.js";
import { Signature } from "./signature.js";
import { Machine } from "./machine.js";
import { Heartbeat } from "./heartbeat.js";
import { Offline } from "./offline.js";

export class Client {
    private options: ClientOptions;
    private signature: Signature;
    private machine: Machine;
    private offlineHandler: Offline;
    private heartbeatRunner: Heartbeat | null;
    private currentSessionToken: string | null;

    constructor(options: ClientOptions) {
        this.options = options;
        this.signature = new Signature();
        this.machine = new Machine();
        this.offlineHandler = new Offline();
        this.heartbeatRunner = null;
        this.currentSessionToken = null;
    }

    token(): string | null {
        return this.currentSessionToken;
    }

    hwid(): string {
        if (this.options.machineId !== undefined && this.options.machineId !== null && this.options.machineId.length > 0) {
            return this.options.machineId;
        }
        return this.machine.hwid();
    }

    async validate(customKey?: string, extra?: Partial<ValidationRequest>): Promise<ValidationResponse> {
        const key = customKey !== undefined && customKey !== null ? customKey : this.options.key;
        if (key === undefined || key === null || key.length === 0) {
            throw new Error("License key is required for validation");
        }

        const payload: ValidationRequest = {
            license_key: key,
            product_name: this.options.product,
            hwid: this.hwid(),
            identifier: this.hwid(),
            session_id: this.currentSessionToken !== null ? this.currentSessionToken : undefined,
            ...(extra !== undefined ? extra : {})
        };

        const targetUrl = `${this.options.url.replace(/\/+$/, "")}/licenses/validate`;
        const response = await fetch(targetUrl, {
            method: "POST",
            headers: {
                "content-type": "application/json",
                "accept": "application/json"
            },
            body: JSON.stringify(payload)
        });

        const text = await response.text();
        if (this.options.publicKey !== undefined && this.options.publicKey !== null && this.options.publicKey.length > 0) {
            const headerName = this.options.signatureHeader !== undefined && this.options.signatureHeader.length > 0 ? this.options.signatureHeader : "x-kora-signature";
            const signatureHeader = response.headers.get(headerName);
            if (signatureHeader === null || !this.signature.verify(text, signatureHeader, this.options.publicKey)) {
                throw new Error("Response signature verification failed");
            }
        }

        const parsed = JSON.parse(text) as ValidationResponse;
        if (parsed.session !== undefined && parsed.session !== null && parsed.session.token !== undefined) {
            this.currentSessionToken = parsed.session.token;
            if (this.options.heartbeatIntervalMs !== undefined && this.options.heartbeatIntervalMs > 0) {
                this.startHeartbeat(key, this.options.heartbeatIntervalMs);
            }
        }

        return parsed;
    }

    private startHeartbeat(key: string, intervalMs: number): void {
        if (this.heartbeatRunner !== null) {
            return;
        }
        this.heartbeatRunner = new Heartbeat(intervalMs, async () => {
            await this.validate(key);
        });
        this.heartbeatRunner.start();
    }

    stop(): void {
        if (this.heartbeatRunner !== null) {
            this.heartbeatRunner.stop();
            this.heartbeatRunner = null;
        }
    }

    offline(): Offline {
        return this.offlineHandler;
    }
}
