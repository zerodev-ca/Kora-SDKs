package ca.zerodev.kora;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class KoraClient {
    private final String url;
    private final String product;
    private final String key;
    private final String publicKey;
    private final String signatureHeader;
    private final long heartbeatIntervalMs;
    private final long maxSkewMs;
    private final SecureRandom random;
    private final HttpClient http;
    private final Gson gson;
    private final KoraSignature signature;
    private final KoraMachine machine;
    private final KoraOffline offline;
    private KoraHeartbeat heartbeat;
    private String sessionToken;

    public KoraClient(String url, String product, String key, String publicKey, String signatureHeader, long heartbeatIntervalMs) {
        this(url, product, key, publicKey, signatureHeader, heartbeatIntervalMs, 300000L);
    }

    public KoraClient(String url, String product, String key, String publicKey, String signatureHeader, long heartbeatIntervalMs, long maxSkewMs) {
        this.url = url.replaceAll("/+$", "");
        this.product = product;
        this.key = key;
        this.publicKey = publicKey;
        this.signatureHeader = signatureHeader != null && !signatureHeader.isEmpty() ? signatureHeader : "x-kora-signature";
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.maxSkewMs = maxSkewMs;
        this.random = new SecureRandom();
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
        this.signature = new KoraSignature();
        this.machine = new KoraMachine();
        this.offline = new KoraOffline();
        this.heartbeat = null;
        this.sessionToken = null;
    }

    public static class Builder {
        private String url;
        private String product;
        private String key;
        private String publicKey;
        private String signatureHeader;
        private long heartbeatIntervalMs = 0;
        private long maxSkewMs = 300000L;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder product(String product) {
            this.product = product;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder publicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public Builder signatureHeader(String signatureHeader) {
            this.signatureHeader = signatureHeader;
            return this;
        }

        public Builder heartbeatIntervalMs(long heartbeatIntervalMs) {
            this.heartbeatIntervalMs = heartbeatIntervalMs;
            return this;
        }

        public Builder maxSkewMs(long maxSkewMs) {
            this.maxSkewMs = maxSkewMs;
            return this;
        }

        public KoraClient build() {
            if (this.url == null || this.product == null) {
                throw new IllegalArgumentException("url and product are required");
            }
            return new KoraClient(this.url, this.product, this.key, this.publicKey, this.signatureHeader, this.heartbeatIntervalMs, this.maxSkewMs);
        }
    }

    public String token() {
        return this.sessionToken;
    }

    public String hwid() {
        return this.machine.hwid();
    }

    public KoraOffline offline() {
        return this.offline;
    }

    public KoraResponse validate() {
        return this.validate(this.key, null);
    }

    public KoraResponse validate(String customKey) {
        return this.validate(customKey, null);
    }

    private String targetKey(String customKey) {
        String targetKey = customKey != null && !customKey.isEmpty() ? customKey : this.key;
        if (targetKey == null || targetKey.isEmpty()) {
            throw new KoraException("License key is required");
        }
        return targetKey;
    }

    private Map<String, Object> request(String targetKey, Map<String, Object> extra) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("license_key", targetKey);
        payload.put("product_name", this.product);
        payload.put("hwid", this.hwid());
        payload.put("identifier", this.hwid());
        if (this.sessionToken != null) {
            payload.put("session_id", this.sessionToken);
        }
        if (extra != null) {
            payload.putAll(extra);
        }
        return payload;
    }

    private String post(String path, Map<String, Object> payload) {
        String nonce = payload.get("nonce") instanceof String && !((String) payload.get("nonce")).isEmpty() ? (String) payload.get("nonce") : this.nonce();
        payload.put("nonce", nonce);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.url + path))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(this.gson.toJson(payload)))
                .build();

            HttpResponse<String> response = this.http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                throw new KoraException("The license server is rate limiting this machine");
            }
            String body = response.body();
            if (this.publicKey == null || this.publicKey.isEmpty()) {
                return body;
            }

            String sig = response.headers().firstValue(this.signatureHeader).orElse(null);
            if (sig == null || !this.signature.verify(body, sig, this.publicKey)) {
                throw new KoraException("Response signature verification failed");
            }
            JsonObject parsed = JsonParser.parseString(body).getAsJsonObject();
            if (!parsed.has("nonce") || parsed.get("nonce").isJsonNull() || !nonce.equals(parsed.get("nonce").getAsString())) {
                throw new KoraException("Response does not belong to this request");
            }
            if (!parsed.has("timestamp") || Math.abs(System.currentTimeMillis() - parsed.get("timestamp").getAsLong()) > this.maxSkewMs) {
                throw new KoraException("Response is too old, check this machine's clock");
            }
            return body;
        } catch (KoraException ke) {
            throw ke;
        } catch (Exception e) {
            throw new KoraException("Request to the license server failed: " + e.getMessage(), e);
        }
    }

    private String nonce() {
        byte[] bytes = new byte[16];
        this.random.nextBytes(bytes);
        StringBuilder builder = new StringBuilder();
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    public KoraResponse validate(String customKey, Map<String, Object> extra) {
        String targetKey = this.targetKey(customKey);
        KoraResponse result = this.gson.fromJson(this.post("/licenses/validate", this.request(targetKey, extra)), KoraResponse.class);
        if (result.getSessionToken() != null) {
            this.sessionToken = result.getSessionToken();
            if (this.heartbeatIntervalMs > 0 && this.heartbeat == null) {
                this.heartbeat = new KoraHeartbeat(this.heartbeatIntervalMs, () -> this.validate(targetKey));
                this.heartbeat.start();
            }
        }
        return result;
    }

    public KoraResponse deactivate() {
        return this.deactivate(this.key);
    }

    public KoraResponse deactivate(String customKey) {
        KoraResponse result = this.gson.fromJson(this.post("/licenses/deactivate", this.request(this.targetKey(customKey), null)), KoraResponse.class);
        this.stop();
        this.sessionToken = null;
        return result;
    }

    public String requestOffline() {
        return this.requestOffline(this.key);
    }

    public String requestOffline(String customKey) {
        JsonObject parsed = JsonParser.parseString(this.post("/licenses/offline", this.request(this.targetKey(customKey), null))).getAsJsonObject();
        if (!parsed.has("valid") || !parsed.get("valid").getAsBoolean() || !parsed.has("offline")) {
            throw new KoraException(parsed.has("message") ? parsed.get("message").getAsString() : "Offline license refused");
        }
        return this.gson.toJson(parsed.get("offline"));
    }

    public boolean verifyOffline(String fileContent) {
        if (this.publicKey == null || this.publicKey.isEmpty()) {
            throw new KoraException("A public key is required to verify offline licenses");
        }
        return this.offline.verify(fileContent, this.publicKey, this.hwid());
    }

    public JsonObject checkUpdate(String version, String channel) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("license_key", this.targetKey(null));
        payload.put("product_name", this.product);
        payload.put("version", version);
        payload.put("channel", channel != null ? channel : "stable");
        return JsonParser.parseString(this.post("/updates/check", payload)).getAsJsonObject();
    }

    public void stop() {
        if (this.heartbeat != null) {
            this.heartbeat.stop();
            this.heartbeat = null;
        }
    }
}
