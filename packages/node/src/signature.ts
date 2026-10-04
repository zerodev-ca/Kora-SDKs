import { createPublicKey, verify } from "crypto";

export class Signature {
    verify(body: string, signature: string | null, publicKey: string): boolean {
        if (signature === null) return false;
        try {
            return verify(null, Buffer.from(body, "utf8"), createPublicKey(publicKey), Buffer.from(signature, "base64"));
        } catch {
            return false;
        }
    }
}
