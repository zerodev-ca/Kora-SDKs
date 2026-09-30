import { verify, createPublicKey } from "crypto";

export class Signature {
    verify(body: string, signatureBase64: string, publicKeyPem: string): boolean {
        try {
            return verify(null, Buffer.from(body, "utf8"), createPublicKey(publicKeyPem), Buffer.from(signatureBase64, "base64"));
        } catch {
            return false;
        }
    }
}
