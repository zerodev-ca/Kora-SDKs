import { createPublicKey, verify } from "crypto";

export class Signature {
    verify(body: string, signature: string, publicKey: string): boolean {
        try {
            return verify(null, Buffer.from(body, "utf8"), createPublicKey(publicKey), Buffer.from(signature, "base64"));
        } catch {
            return false;
        }
    }

    canonical(value: Record<string, unknown>): string {
        return JSON.stringify(Object.fromEntries(Object.keys(value).sort().map(key => [key, value[key]])));
    }
}
