package ca.zerodev.kora;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

public class KoraSignature {
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public String canonical(JsonObject value) {
        Map<String, JsonElement> sorted = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : value.entrySet()) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return this.gson.toJson(sorted);
    }

    public boolean verify(String body, String signatureBase64, String publicKeyPem) {
        try {
            String stripped = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(stripped);
            PublicKey key = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(keyBytes));
            Signature sig = Signature.getInstance("Ed25519");
            sig.initVerify(key);
            sig.update(body.getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }
}
