package ca.zerodev.kora;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Consumer;

public class KoraClient {
    private final String url;
    private final String product;
    private final String key;
    private final String apiKey;
    private final String publicKey;
    private final String hwid;
    private final String version;
    private final long intervalMs;
    private final long maxSkewMs;
    private final Consumer<KoraResponse> onInvalid;
    private final HttpClient http;
    private final Gson gson;
    private final KoraMachine machine;
    private final KoraSignature signature;
    private final SecureRandom random;
    private KoraHeartbeat heartbeat;

    public KoraClient(String url, String product, String key, String apiKey, String publicKey, String hwid, String version, long intervalMs, long maxSkewMs, Consumer<KoraResponse> onInvalid) {
        this.url = url.replaceAll("/+$", "").replaceAll("/api/v1$", "");
        this.product = product;
        this.key = key;
        this.apiKey = apiKey;
        this.publicKey = publicKey;
        this.hwid = hwid;
        this.version = version;
        this.intervalMs = intervalMs;
        this.maxSkewMs = maxSkewMs;
        this.onInvalid = onInvalid;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
        this.machine = new KoraMachine();
        this.signature = new KoraSignature();
        this.random = new SecureRandom();
        this.heartbeat = null;
    }

    public static class Builder {
        private String url;
        private String product;
        private String key;
        private String apiKey;
        private String publicKey;
        private String hwid;
        private String version;
        private long intervalMs = 0;
        private long maxSkewMs = 300000L;
        private Consumer<KoraResponse> onInvalid;

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

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder publicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public Builder hwid(String hwid) {
            this.hwid = hwid;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder intervalMs(long intervalMs) {
            this.intervalMs = intervalMs;
            return this;
        }

        public Builder maxSkewMs(long maxSkewMs) {
            this.maxSkewMs = maxSkewMs;
            return this;
        }

        public Builder onInvalid(Consumer<KoraResponse> onInvalid) {
            this.onInvalid = onInvalid;
            return this;
        }

        public KoraClient build() {
            if (this.url == null || this.product == null) {
                throw new IllegalArgumentException("url and product are required");
            }
            return new KoraClient(this.url, this.product, this.key, this.apiKey, this.publicKey, this.hwid, this.version, this.intervalMs, this.maxSkewMs, this.onInvalid);
        }
    }

    public String hwid() {
        return this.hwid != null && !this.hwid.isEmpty() ? this.hwid : this.machine.hwid();
    }

    private String target(String customKey) {
        if (customKey == null || customKey.isEmpty()) {
            throw new KoraException("License key is required");
        }
        return customKey;
    }

    private KoraResponse post(String path, String licenseKey, Map<String, Object> extra) {
        byte[] bytes = new byte[16];
        this.random.nextBytes(bytes);
        String nonce = HexFormat.of().formatHex(bytes);
        Map<String, Object> payload = new HashMap<>();
        payload.put("license_key", licenseKey);
        payload.put("product_name", this.product);
        payload.put("hwid", this.hwid());
        payload.put("device_name", this.machine.hostname());
        payload.put("platform", this.machine.platform());
        payload.put("nonce", nonce);
        if (this.version != null) {
            payload.put("version", this.version);
        }
        payload.putAll(extra);
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(this.url + "/api/v1" + path))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(this.gson.toJson(payload)));
            if (this.apiKey != null && !this.apiKey.isEmpty()) {
                builder.header("Authorization", "Bearer " + this.apiKey);
            }
            HttpResponse<String> response = this.http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (this.publicKey != null && !this.publicKey.isEmpty()) {
                this.check(response.body(), response.headers().firstValue("x-kora-signature").orElse(null), nonce);
            }
            return this.parse(response.body()).settle(response.statusCode(), "Kora answered with status " + response.statusCode());
        } catch (KoraException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new KoraException("Request to the license server failed: " + exception.getMessage(), exception);
        }
    }

    private void check(String body, String header, String nonce) {
        if (header == null || !this.signature.verify(body, header, this.publicKey)) {
            throw new KoraException("Response signature verification failed");
        }
        JsonObject parsed = JsonParser.parseString(body).getAsJsonObject();
        if (!parsed.has("nonce") || parsed.get("nonce").isJsonNull() || !nonce.equals(parsed.get("nonce").getAsString())) {
            throw new KoraException("Response does not belong to this request");
        }
        if (!parsed.has("timestamp") || Math.abs(System.currentTimeMillis() - parsed.get("timestamp").getAsLong()) > this.maxSkewMs) {
            throw new KoraException("Response is too old, check this machine's clock");
        }
    }

    private KoraResponse parse(String body) {
        try {
            KoraResponse parsed = this.gson.fromJson(body, KoraResponse.class);
            return parsed != null ? parsed : new KoraResponse();
        } catch (JsonParseException exception) {
            return new KoraResponse();
        }
    }

    public KoraResponse validate() {
        return this.validate(this.key);
    }

    public KoraResponse validate(String customKey) {
        String licenseKey = this.target(customKey);
        KoraResponse result = this.post("/licenses/validate", licenseKey, Map.of());
        if (result.isValid()) {
            this.watch(licenseKey);
        }
        return result;
    }

    public KoraResponse offline() {
        return this.post("/licenses/offline", this.target(this.key), Map.of());
    }

    public boolean verifyOffline(String content) {
        if (this.publicKey == null || this.publicKey.isEmpty()) {
            throw new KoraException("A public key is required to verify offline licenses");
        }
        try {
            JsonObject parsed = JsonParser.parseString(content).getAsJsonObject();
            String signed = parsed.remove("signature").getAsString();
            String bound = parsed.get("hwid").getAsString();
            return this.signature.verify(this.signature.canonical(parsed), signed, this.publicKey)
                && parsed.get("product").getAsString().equalsIgnoreCase(this.product)
                && (bound.isEmpty() || bound.equals(this.hwid()))
                && System.currentTimeMillis() <= parsed.get("expires_at").getAsLong() + parsed.get("grace_period_ms").getAsLong();
        } catch (Exception exception) {
            return false;
        }
    }

    public KoraResponse checkUpdate(String current, String channel) {
        return this.post("/updates/check", this.target(this.key), Map.of("version", current, "channel", channel != null ? channel : "stable"));
    }

    private synchronized void watch(String licenseKey) {
        if (this.intervalMs <= 0 || this.heartbeat != null) {
            return;
        }
        this.heartbeat = new KoraHeartbeat(this.intervalMs, () -> {
            try {
                KoraResponse result = this.validate(licenseKey);
                if (!result.isValid() && this.onInvalid != null) {
                    this.onInvalid.accept(result);
                }
            } catch (KoraException ignored) {
            }
        });
        this.heartbeat.start();
    }

    public synchronized void stop() {
        if (this.heartbeat != null) {
            this.heartbeat.stop();
            this.heartbeat = null;
        }
    }
}
