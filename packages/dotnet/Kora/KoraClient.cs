using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.Crypto.Signers;

namespace Kora;

public sealed class KoraException : Exception
{
    public KoraException(string message, int status = 0) : base(message)
    {
        Status = status;
    }

    public int Status { get; }
}

public sealed record ValidationResponse(bool Valid, string Code, string Message, JsonObject? License, long Timestamp);

public sealed record DeactivationResponse(bool Success, string Code, string Message, int Removed, long Timestamp);

public sealed class KoraClient : IDisposable
{
    private static readonly JsonSerializerOptions Json = new() { PropertyNameCaseInsensitive = true };
    private readonly HttpClient http;
    private readonly string? product;
    private readonly string? publicKey;

    public KoraClient(string url, string? apiKey = null, string? product = null, string? publicKey = null, TimeSpan? timeout = null)
    {
        var trimmed = url.TrimEnd('/');
        http = new HttpClient { BaseAddress = new Uri((trimmed.EndsWith("/api/v1") ? trimmed : trimmed + "/api/v1") + "/"), Timeout = timeout.HasValue ? timeout.Value : TimeSpan.FromSeconds(15) };
        http.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        if (!string.IsNullOrEmpty(apiKey)) http.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", apiKey);
        this.product = product;
        this.publicKey = publicKey;
        Licenses = new KoraResource(this, "licenses", "license");
        Products = new KoraResource(this, "products", "product");
        Customers = new KoraResource(this, "customers", "customer");
    }

    public KoraResource Licenses { get; }

    public KoraResource Products { get; }

    public KoraResource Customers { get; }

    private async Task<(HttpStatusCode Status, string Text, string? Signature)> Send(HttpMethod method, string path, object? body, CancellationToken token)
    {
        using var request = new HttpRequestMessage(method, path.TrimStart('/'));
        if (body is not null) request.Content = new StringContent(JsonSerializer.Serialize(body), Encoding.UTF8, "application/json");
        using var response = await http.SendAsync(request, token);
        return (response.StatusCode, await response.Content.ReadAsStringAsync(token), response.Headers.TryGetValues("x-kora-signature", out var values) ? values.First() : null);
    }

    private static KoraException Fail(HttpStatusCode status, string text)
    {
        try
        {
            var data = JsonNode.Parse(text);
            if (data is JsonObject body && body["error"] is JsonValue error) return new KoraException(error.GetValue<string>(), (int)status);
        }
        catch (JsonException)
        {
        }
        return new KoraException($"Kora answered with status {(int)status}", (int)status);
    }

    internal async Task<JsonObject> Call(HttpMethod method, string path, object? body = null, CancellationToken token = default)
    {
        var (status, text, _) = await Send(method, path, body, token);
        if ((int)status >= 400) throw Fail(status, text);
        return string.IsNullOrEmpty(text) ? new JsonObject() : JsonNode.Parse(text)!.AsObject();
    }

    private byte[] Key()
    {
        var der = Convert.FromBase64String(string.Concat(publicKey!.Split('\n').Where(line => !line.StartsWith("-----")).Select(line => line.Trim())));
        return der[^32..];
    }

    private bool Verify(byte[] message, byte[] signature)
    {
        var verifier = new Ed25519Signer();
        verifier.Init(false, new Ed25519PublicKeyParameters(Key(), 0));
        verifier.BlockUpdate(message, 0, message.Length);
        return verifier.VerifySignature(signature);
    }

    private async Task<T> Check<T>(string path, Dictionary<string, object?> body, CancellationToken token)
    {
        var (status, text, signature) = await Send(HttpMethod.Post, path, body, token);
        if (status == HttpStatusCode.TooManyRequests || (int)status >= 500) throw Fail(status, text);
        if (!string.IsNullOrEmpty(publicKey) && (signature is null || !Verify(Encoding.UTF8.GetBytes(text), Convert.FromBase64String(signature)))) throw new KoraException("Response signature verification failed", (int)status);
        return JsonSerializer.Deserialize<T>(text, Json)!;
    }

    private Dictionary<string, object?> Body(string key, Dictionary<string, string>? fields, string? name)
    {
        var body = fields is null ? new Dictionary<string, object?>() : fields.ToDictionary(pair => pair.Key, pair => (object?)pair.Value);
        body["license_key"] = key;
        var chosen = name is not null ? name : product;
        if (chosen is not null) body["product_name"] = chosen;
        return body;
    }

    public Task<ValidationResponse> ValidateAsync(string key, string? product = null, Dictionary<string, string>? fields = null, CancellationToken token = default) => Check<ValidationResponse>("licenses/validate", Body(key, fields, product), token);

    public Task<ValidationResponse> ActivateAsync(string key, string? product = null, Dictionary<string, string>? fields = null, CancellationToken token = default) => Check<ValidationResponse>("licenses/activate", Body(key, fields, product), token);

    public Task<DeactivationResponse> DeactivateAsync(string key, Dictionary<string, string>? fields = null, CancellationToken token = default)
    {
        var body = Body(key, fields, null);
        body.Remove("product_name");
        return Check<DeactivationResponse>("licenses/deactivate", body, token);
    }

    public async Task<string> FileAsync(string id, CancellationToken token = default)
    {
        var (status, text, _) = await Send(HttpMethod.Get, $"licenses/{Uri.EscapeDataString(id)}/file", null, token);
        if ((int)status >= 400) throw Fail(status, text);
        return text.Trim();
    }

    private static byte[] Decode(string value) => Convert.FromBase64String(value.Replace('-', '+').Replace('_', '/').PadRight(value.Length + (4 - value.Length % 4) % 4, '='));

    public JsonObject Offline(string file)
    {
        if (string.IsNullOrEmpty(publicKey)) throw new KoraException("A publicKey is required to read offline licenses");
        var parts = file.Trim().Split('.');
        if (parts.Length != 2) throw new KoraException("Offline license file is malformed");
        var payload = Decode(parts[0]);
        if (!Verify(payload, Decode(parts[1]))) throw new KoraException("Offline license signature verification failed");
        var license = JsonNode.Parse(payload)!.AsObject();
        if (DateTimeOffset.Parse(license["refresh_after"]!.GetValue<string>()) < DateTimeOffset.UtcNow) throw new KoraException("Offline license needs a refresh");
        var expires = license["expires_at"];
        if (expires is not null && DateTimeOffset.Parse(expires.GetValue<string>()) < DateTimeOffset.UtcNow) throw new KoraException("Offline license expired");
        return license;
    }

    public async Task<JsonObject> MergeAsync(string customer, string duplicate, CancellationToken token = default) => (await Call(HttpMethod.Post, $"customers/{Uri.EscapeDataString(customer)}/merge", new { customer_id = duplicate }, token))["customer"]!.AsObject();

    public void Dispose() => http.Dispose();
}
