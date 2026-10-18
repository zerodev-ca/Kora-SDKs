using System.Text.Json.Nodes;

namespace Kora;

public sealed record KoraPage(int Total, JsonArray Rows);

public sealed class KoraResource
{
    private readonly KoraClient client;
    private readonly string name;
    private readonly string single;

    internal KoraResource(KoraClient client, string name, string single)
    {
        this.client = client;
        this.name = name;
        this.single = single;
    }

    private string Path(string id) => $"{name}/{Uri.EscapeDataString(id)}";

    public async Task<KoraPage> ListAsync(Dictionary<string, string>? query = null, CancellationToken token = default)
    {
        var search = query is null || query.Count == 0 ? "" : "?" + string.Join("&", query.Select(pair => $"{Uri.EscapeDataString(pair.Key)}={Uri.EscapeDataString(pair.Value)}"));
        var data = await client.Call(HttpMethod.Get, name + search, null, token);
        return new KoraPage(data["total"]!.GetValue<int>(), data[name]!.AsArray());
    }

    public Task<JsonObject> GetAsync(string id, CancellationToken token = default) => client.Call(HttpMethod.Get, Path(id), null, token);

    public async Task<JsonObject> CreateAsync(object fields, CancellationToken token = default) => (await client.Call(HttpMethod.Post, name, fields, token))[single]!.AsObject();

    public async Task<JsonObject> UpdateAsync(string id, object fields, CancellationToken token = default) => (await client.Call(HttpMethod.Patch, Path(id), fields, token))[single]!.AsObject();

    public Task DeleteAsync(string id, bool force = false, CancellationToken token = default) => client.Call(HttpMethod.Delete, force ? Path(id) + "?force=true" : Path(id), null, token);
}
