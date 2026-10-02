package ca.zerodev.kora;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.TreeSet;

public class KoraOffline {
    private final KoraSignature signature;
    private final Gson gson;

    public KoraOffline() {
        this.signature = new KoraSignature();
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    public String canonical(JsonObject root) {
        JsonObject sorted = new JsonObject();
        for (String name : new TreeSet<>(root.keySet())) {
            if (!name.equals("signature")) {
                sorted.add(name, root.get(name));
            }
        }
        return this.gson.toJson(sorted);
    }

    public boolean verify(String jsonContent, String publicKeyPem) {
        return this.verify(jsonContent, publicKeyPem, null);
    }

    public boolean verify(String jsonContent, String publicKeyPem, String hwid) {
        try {
            JsonObject root = this.gson.fromJson(jsonContent, JsonObject.class);
            if (!root.has("signature") || !root.has("expires_at")) {
                return false;
            }
            if (!this.signature.verify(this.canonical(root), root.get("signature").getAsString(), publicKeyPem)) {
                return false;
            }
            if (hwid != null && root.has("hwid") && !root.get("hwid").getAsString().equals(hwid)) {
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
