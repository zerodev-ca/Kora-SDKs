package ca.zerodev.kora;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class KoraResource {
    private final KoraClient client;
    private final String name;
    private final String single;

    KoraResource(KoraClient client, String name, String single) {
        this.client = client;
        this.name = name;
        this.single = single;
    }

    private String path(Object id) {
        return "/" + this.name + "/" + URLEncoder.encode(String.valueOf(id), StandardCharsets.UTF_8);
    }

    public KoraPage list() {
        return this.list(Map.of());
    }

    public KoraPage list(Map<String, ?> query) {
        JsonObject data = this.client.call("GET", "/" + this.name + "?" + query.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8)).collect(Collectors.joining("&")), null);
        return new KoraPage(data.get("total").getAsInt(), data.getAsJsonArray(this.name).asList().stream().map(JsonElement::getAsJsonObject).toList());
    }

    public JsonObject get(Object id) {
        return this.client.call("GET", this.path(id), null);
    }

    public JsonObject create(Map<String, ?> fields) {
        return this.client.call("POST", "/" + this.name, fields).getAsJsonObject(this.single);
    }

    public JsonObject update(Object id, Map<String, ?> fields) {
        return this.client.call("PATCH", this.path(id), fields).getAsJsonObject(this.single);
    }

    public void delete(Object id) {
        this.delete(id, false);
    }

    public void delete(Object id, boolean force) {
        this.client.call("DELETE", force ? this.path(id) + "?force=true" : this.path(id), null);
    }
}
