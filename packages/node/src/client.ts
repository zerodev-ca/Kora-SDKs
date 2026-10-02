import { randomBytes } from "crypto";
import { ClientOptions, OfflineLicense, UpdateResponse, ValidationRequest, ValidationResponse } from "./types.js";
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

    private key(customKey?: string): string {
        const key = customKey !== undefined && customKey !== null ? customKey : this.options.key;
        if (key === undefined || key === null || key.length === 0) {
            throw new Error("License key is required");
        }
        return key;
    }

    private async post<T extends { nonce?: string | null; timestamp?: number }>(path: string, body: Record<string, unknown>): Promise<T> {
        const nonce = typeof body.nonce === "string" && body.nonce.length > 0 ? body.nonce : randomBytes(16).toString("hex");
        const response = await fetch(`${this.options.url.replace(/\/+$/, "")}${path}`, {
            method: "POST",
            headers: {
                "content-type": "application/json",
                "accept": "application/json"
            },
            body: JSON.stringify({ ...body, nonce })
        });

        const text = await response.text();
        if (response.status === 429) {
            throw new Error("The license server is rate limiting this machine");
        }

        const parsed = JSON.parse(text) as T;
        if (this.options.publicKey === undefined || this.options.publicKey === null || this.options.publicKey.length === 0) {
            return parsed;
        }

        const headerName = this.options.signatureHeader !== undefined && this.options.signatureHeader.length > 0 ? this.options.signatureHeader : "x-kora-signature";
        const signatureHeader = response.headers.get(headerName);
        if (signatureHeader === null || !this.signature.verify(text, signatureHeader, this.options.publicKey)) {
            throw new Error("Response signature verification failed");
        }
        if (parsed.nonce !== nonce) {
            throw new Error("Response does not belong to this request");
        }
        const skew = this.options.maxSkewMs !== undefined ? this.options.maxSkewMs : 300000;
        if (typeof parsed.timestamp !== "number" || Math.abs(Date.now() - parsed.timestamp) > skew) {
            throw new Error("Response is too old, check this machine's clock");
        }
        return parsed;
    }

    private request(key: string, extra?: Partial<ValidationRequest>): Record<string, unknown> {
        return {
            license_key: key,
            product_name: this.options.product,
            hwid: this.hwid(),
            identifier: this.hwid(),
            session_id: this.currentSessionToken !== null ? this.currentSessionToken : undefined,
            ...(extra !== undefined ? extra : {})
        };
    }

    async validate(customKey?: string, extra?: Partial<ValidationRequest>): Promise<ValidationResponse> {
        const key = this.key(customKey);
        const parsed = await this.post<ValidationResponse>("/licenses/validate", this.request(key, extra));
        if (parsed.session !== undefined && parsed.session !== null && parsed.session.token !== undefined) {
            this.currentSessionToken = parsed.session.token;
            if (this.options.heartbeatIntervalMs !== undefined && this.options.heartbeatIntervalMs > 0) {
                this.startHeartbeat(key, this.options.heartbeatIntervalMs);
            }
        }
        return parsed;
    }

    async deactivate(customKey?: string): Promise<ValidationResponse> {
        const key = this.key(customKey);
        const parsed = await this.post<ValidationResponse>("/licenses/deactivate", this.request(key));
        this.stop();
        this.currentSessionToken = null;
        return parsed;
    }

    async requestOffline(customKey?: string): Promise<OfflineLicense> {
        const parsed = await this.post<ValidationResponse>("/licenses/offline", this.request(this.key(customKey)));
        if (!parsed.valid || parsed.offline === undefined) {
            throw new Error(parsed.message);
        }
        return parsed.offline;
    }

    async checkUpdate(version: string, channel: "stable" | "beta" = "stable", customKey?: string): Promise<UpdateResponse> {
        return this.post<UpdateResponse>("/updates/check", { license_key: this.key(customKey), product_name: this.options.product, version, channel });
    }

    verifyOffline(fileContent: string): OfflineLicense | null {
        if (this.options.publicKey === undefined || this.options.publicKey === null || this.options.publicKey.length === 0) {
            throw new Error("A public key is required to verify offline licenses");
        }
        return this.offlineHandler.verify(fileContent, this.options.publicKey, this.hwid());
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
