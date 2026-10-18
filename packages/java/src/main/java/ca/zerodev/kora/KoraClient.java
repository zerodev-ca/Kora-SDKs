package ca.zerodev.kora;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
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

    private HttpResponse<String> check(String path, Map<String, Object> body) {
        HttpResponse<String> response = this.send("POST", path, body);
        if (response.statusCode() == 429 || response.statusCode() >= 500) {
            throw this.fail(response);
        }
        if (this.publicKey != null && !this.publicKey.isEmpty() && !this.signature.verify(response.body(), response.headers().firstValue("x-kora-signature").orElse(null), this.publicKey)) {
            throw new KoraException("Response signature verification failed", response.statusCode());
        }
        return response;
    }

    private Map<String, Object> body(String key, String product, Map<String, Object> fields) {
        Map<String, Object> body = new HashMap<>(fields);
        body.put("license_key", key);
        if (product != null) {
            body.put("product_name", product);
        }
        return body;
    }

    private String encode(Object value) {
        return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
    }

    public KoraResponse validate(String key) {
        return this.validate(key, this.product, Map.of());
    }

    public KoraResponse validate(String key, String product) {
        return this.validate(key, product, Map.of());
    }

    public KoraResponse validate(String key, String product, Map<String, Object> fields) {
        return this.gson.fromJson(this.check("/licenses/validate", this.body(key, product, fields)).body(), KoraResponse.class);
    }

    public KoraResponse activate(String key, Map<String, Object> fields) {
        return this.activate(key, this.product, fields);
    }

    public KoraResponse activate(String key, String product, Map<String, Object> fields) {
        return this.gson.fromJson(this.check("/licenses/activate", this.body(key, product, fields)).body(), KoraResponse.class);
    }

    public KoraDeactivation deactivate(String key, Map<String, Object> fields) {
        return this.gson.fromJson(this.check("/licenses/deactivate", this.body(key, null, fields)).body(), KoraDeactivation.class);
    }

    public String file(Object id) {
        HttpResponse<String> response = this.send("GET", "/licenses/" + this.encode(id) + "/file", null);
        if (response.statusCode() >= 400) {
            throw this.fail(response);
        }
        return response.body().trim();
    }

    public JsonObject offline(String file) {
        if (this.publicKey == null || this.publicKey.isEmpty()) {
            throw new KoraException("A publicKey is required to read offline licenses", 0);
        }
        String[] parts = file.trim().split("\\.");
        if (parts.length != 2) {
            throw new KoraException("Offline license file is malformed", 0);
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        if (!this.signature.verify(payload, Base64.getEncoder().encodeToString(Base64.getUrlDecoder().decode(parts[1])), this.publicKey)) {
            throw new KoraException("Offline license signature verification failed", 0);
        }
        JsonObject license = JsonParser.parseString(payload).getAsJsonObject();
        if (Instant.parse(license.get("refresh_after").getAsString()).isBefore(Instant.now())) {
            throw new KoraException("Offline license needs a refresh", 0);
        }
        if (!license.get("expires_at").isJsonNull() && Instant.parse(license.get("expires_at").getAsString()).isBefore(Instant.now())) {
            throw new KoraException("Offline license expired", 0);
        }
        return license;
    }

    public JsonObject merge(Object customer, Object duplicate) {
        return this.call("POST", "/customers/" + this.encode(customer) + "/merge", Map.of("customer_id", duplicate)).getAsJsonObject("customer");
    }
}
