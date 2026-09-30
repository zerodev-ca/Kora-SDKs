package ca.zerodev.kora;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private final HttpClient http;
    private final Gson gson;
    private final KoraSignature signature;
    private final KoraMachine machine;
    private final KoraOffline offline;
    private KoraHeartbeat heartbeat;
    private String sessionToken;

    public KoraClient(String url, String product, String key, String publicKey, String signatureHeader, long heartbeatIntervalMs) {
        this.url = url.replaceAll("/+$", "");
        this.product = product;
        this.key = key;
        this.publicKey = publicKey;
        this.signatureHeader = signatureHeader != null && !signatureHeader.isEmpty() ? signatureHeader : "x-kora-signature";
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.gson = new Gson();
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

        public KoraClient build() {
            if (this.url == null || this.product == null) {
                throw new IllegalArgumentException("url and product are required");
            }
            return new KoraClient(this.url, this.product, this.key, this.publicKey, this.signatureHeader, this.heartbeatIntervalMs);
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

    public KoraResponse validate(String customKey, Map<String, Object> extra) {
        String targetKey = customKey != null && !customKey.isEmpty() ? customKey : this.key;
        if (targetKey == null || targetKey.isEmpty()) {
            throw new KoraException("License key is required");
        }

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

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.url + "/licenses/validate"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(this.gson.toJson(payload)))
                .build();

            HttpResponse<String> response = this.http.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (this.publicKey != null && !this.publicKey.isEmpty()) {
                String sig = response.headers().firstValue(this.signatureHeader).orElse(null);
                if (sig == null || !this.signature.verify(body, sig, this.publicKey)) {
                    throw new KoraException("Response signature verification failed");
                }
            }

            KoraResponse result = this.gson.fromJson(body, KoraResponse.class);
            if (result.getSessionToken() != null) {
                this.sessionToken = result.getSessionToken();
                if (this.heartbeatIntervalMs > 0 && this.heartbeat == null) {
                    this.heartbeat = new KoraHeartbeat(this.heartbeatIntervalMs, () -> this.validate(targetKey));
                    this.heartbeat.start();
                }
            }
            return result;
        } catch (KoraException ke) {
            throw ke;
        } catch (Exception e) {
            throw new KoraException("Validation request failed: " + e.getMessage(), e);
        }
    }

    public void stop() {
        if (this.heartbeat != null) {
            this.heartbeat.stop();
            this.heartbeat = null;
        }
    }
}
