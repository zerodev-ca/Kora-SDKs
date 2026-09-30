package ca.zerodev.kora;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class KoraOffline {
    private final KoraSignature signature;
    private final Gson gson;

    public KoraOffline() {
        this.signature = new KoraSignature();
        this.gson = new Gson();
    }

    public boolean verify(String jsonContent, String publicKeyPem) {
        try {
            JsonObject root = this.gson.fromJson(jsonContent, JsonObject.class);
            if (!root.has("signature") || !root.has("expires_at")) {
                return false;
            }
            String sig = root.get("signature").getAsString();
            JsonObject payload = root.deepCopy();
            payload.remove("signature");
            String serialized = this.gson.toJson(payload);
            if (!this.signature.verify(serialized, sig, publicKeyPem)) {
                return false;
            }
            long expiresAt = root.get("expires_at").getAsLong();
            long grace = root.has("grace_period_ms") ? root.get("grace_period_ms").getAsLong() : 0L;
            return System.currentTimeMillis() <= (expiresAt + grace);
        } catch (Exception e) {
            return false;
        }
    }
}
