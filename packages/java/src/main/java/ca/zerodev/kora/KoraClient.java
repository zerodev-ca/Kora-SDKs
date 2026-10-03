package ca.zerodev.kora;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

public class KoraClient {
    private final String url;
    private final String product;
    private final String key;
    private final String apiKey;
    private final long intervalMs;
    private final Consumer<KoraResponse> onInvalid;
    private final HttpClient http;
    private final Gson gson;
    private KoraHeartbeat heartbeat;

    public KoraClient(String url, String product, String key, String apiKey, long intervalMs, Consumer<KoraResponse> onInvalid) {
        this.url = url.replaceAll("/+$", "").replaceAll("/api/v1$", "");
        this.product = product;
        this.key = key;
        this.apiKey = apiKey;
        this.intervalMs = intervalMs;
        this.onInvalid = onInvalid;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
        this.heartbeat = null;
    }

    public static class Builder {
        private String url;
        private String product;
        private String key;
        private String apiKey;
        private long intervalMs = 0;
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

        public Builder intervalMs(long intervalMs) {
            this.intervalMs = intervalMs;
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
            return new KoraClient(this.url, this.product, this.key, this.apiKey, this.intervalMs, this.onInvalid);
        }
    }

    public String endpoint() {
        return this.url + "/api/v1/licenses/validate";
    }

    public KoraResponse validate() {
        return this.validate(this.key);
    }

    public KoraResponse validate(String customKey) {
        if (customKey == null || customKey.isEmpty()) {
            throw new KoraException("License key is required");
        }
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(this.endpoint()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(this.gson.toJson(Map.of("license_key", customKey, "product_name", this.product))));
            if (this.apiKey != null && !this.apiKey.isEmpty()) {
                builder.header("Authorization", "Bearer " + this.apiKey);
            }
            HttpResponse<String> response = this.http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            KoraResponse result = this.parse(response.body()).settle(response.statusCode(), "Kora answered with status " + response.statusCode());
            if (result.isValid()) {
                this.watch(customKey);
            }
            return result;
        } catch (KoraException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new KoraException("Request to the license server failed: " + exception.getMessage(), exception);
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

    private synchronized void watch(String customKey) {
        if (this.intervalMs <= 0 || this.heartbeat != null) {
            return;
        }
        this.heartbeat = new KoraHeartbeat(this.intervalMs, () -> {
            try {
                KoraResponse result = this.validate(customKey);
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
