import { Signature } from "./signature.js";
import { OfflineLicense } from "./types.js";

export class Offline {
    private signature: Signature;

    constructor() {
        this.signature = new Signature();
    }

    verify(fileContent: string, publicKeyPem: string): OfflineLicense | null {
        try {
            const parsed = JSON.parse(fileContent) as OfflineLicense;
            if (parsed.key === undefined || parsed.signature === undefined || parsed.product === undefined) {
                return null;
            }
            const copy = { ...parsed };
            delete (copy as { signature?: string }).signature;
            const serialized = JSON.stringify(copy);
            if (!this.signature.verify(serialized, parsed.signature, publicKeyPem)) {
                return null;
            }
            const now = Date.now();
            const allowedUntil = parsed.expires_at + (parsed.grace_period_ms !== undefined ? parsed.grace_period_ms : 0);
            if (now > allowedUntil) {
                return null;
            }
            return parsed;
        } catch {
            return null;
        }
    }
}
