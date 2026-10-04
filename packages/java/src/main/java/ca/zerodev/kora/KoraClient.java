package ca.zerodev.kora;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class KoraClient {
    private final String url;
    private final String apiKey;
    private final String product;
    private final String publicKey;
    private final HttpClient http;
    private final Gson gson;
    private final KoraSignature signature;
    private final KoraResource licenses;
    private final KoraResource products;
    private final KoraResource customers;

    public KoraClient(String url, String apiKey, String product, String publicKey) {
        this.url = url.replaceAll("/+$", "").replaceAll("/api/v1$", "");
        this.apiKey = apiKey;
        this.product = product;
        this.publicKey = publicKey;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
        this.signature = new KoraSignature();
        this.licenses = new KoraResource(this, "licenses", "license");
        this.products = new KoraResource(this, "products", "product");
        this.customers = new KoraResource(this, "customers", "customer");
    }

    public static class Builder {
        private String url;
        private String apiKey;
        private String product;
        private String publicKey;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder product(String product) {
            this.product = product;
            return this;
        }

        public Builder publicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public KoraClient build() {
            if (this.url == null) {
                throw new IllegalArgumentException("url is required");
            }
            return new KoraClient(this.url, this.apiKey, this.product, this.publicKey);
        }
    }

    public KoraResource licenses() {
        return this.licenses;
    }

    public KoraResource products() {
        return this.products;
    }

    public KoraResource customers() {
        return this.customers;
    }

    private HttpResponse<String> send(String method, String path, Object body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(this.url + "/api/v1" + path))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(15))
            .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(this.gson.toJson(body)));
        if (this.apiKey != null && !this.apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + this.apiKey);
        }
        try {
            return this.http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception exception) {
            throw new KoraException("Request to Kora failed: " + exception.getMessage(), exception);
        }
    }

    private JsonObject parse(String body) {
        return body.startsWith("{") ? JsonParser.parseString(body).getAsJsonObject() : new JsonObject();
    }

    private KoraException fail(HttpResponse<String> response) {
        JsonObject data = this.parse(response.body());
        return new KoraException(data.has("error") ? data.get("error").getAsString() : "Kora answered with status " + response.statusCode(), response.statusCode());
    }

    JsonObject call(String method, String path, Object body) {
        HttpResponse<String> response = this.send(method, path, body);
        if (response.statusCode() >= 400) {
            throw this.fail(response);
        }
        return this.parse(response.body());
    }

    public KoraResponse validate(String key) {
        return this.validate(key, this.product);
    }

    public KoraResponse validate(String key, String product) {
        Map<String, Object> body = new HashMap<>();
        body.put("license_key", key);
        if (product != null) {
            body.put("product_name", product);
        }
        HttpResponse<String> response = this.send("POST", "/licenses/validate", body);
        if (response.statusCode() == 429 || response.statusCode() >= 500) {
            throw this.fail(response);
        }
        if (this.publicKey != null && !this.publicKey.isEmpty() && !this.signature.verify(response.body(), response.headers().firstValue("x-kora-signature").orElse(null), this.publicKey)) {
            throw new KoraException("Response signature verification failed", response.statusCode());
        }
        return this.gson.fromJson(response.body(), KoraResponse.class);
    }
}
