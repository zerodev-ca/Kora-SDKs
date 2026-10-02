import { Signature } from "./signature.js";
import { OfflineLicense } from "./types.js";

export class Offline {
    private signature: Signature;

    constructor() {
        this.signature = new Signature();
    }

    canonical(value: Record<string, unknown>): string {
        return JSON.stringify(Object.fromEntries(Object.keys(value).sort().map(key => [key, value[key]])));
    }

    verify(fileContent: string, publicKeyPem: string, hwid?: string): OfflineLicense | null {
        try {
            const parsed = JSON.parse(fileContent) as OfflineLicense;
            if (parsed.key === undefined || parsed.signature === undefined || parsed.product === undefined) {
                return null;
            }
            const copy: Record<string, unknown> = { ...parsed };
            delete copy.signature;
            if (!this.signature.verify(this.canonical(copy), parsed.signature, publicKeyPem)) {
                return null;
            }
            if (hwid !== undefined && parsed.hwid !== undefined && parsed.hwid !== hwid) {
                return null;
            }
            const allowedUntil = parsed.expires_at + (parsed.grace_period_ms !== undefined ? parsed.grace_period_ms : 0);
            if (Date.now() > allowedUntil) {
                return null;
            }
            return parsed;
        } catch {
            return null;
        }
    }
}
