<?php

namespace ZeroDev\Kora;

class Client
{
    public readonly Resource $licenses;
    public readonly Resource $products;
    public readonly Resource $customers;
    private string $base;

    public function __construct(string $url, private ?string $apiKey = null, private ?string $product = null, private ?string $publicKey = null, private int $timeout = 15)
    {
        $this->base = preg_replace("#/api/v1$#", "", rtrim($url, "/"));
        $this->licenses = new Resource($this, "licenses", "license");
        $this->products = new Resource($this, "products", "product");
        $this->customers = new Resource($this, "customers", "customer");
    }

    private function send(string $method, string $path, ?array $body = null): array
    {
        $signature = null;
        $handle = curl_init($this->base . "/api/v1" . $path);
        curl_setopt_array($handle, [
            CURLOPT_CUSTOMREQUEST => $method,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => $this->timeout,
            CURLOPT_HTTPHEADER => array_merge(["Content-Type: application/json", "Accept: application/json"], $this->apiKey ? ["Authorization: Bearer {$this->apiKey}"] : []),
            CURLOPT_POSTFIELDS => $body === null ? null : json_encode($body),
            CURLOPT_HEADERFUNCTION => function ($curl, string $header) use (&$signature): int {
                if (stripos($header, "x-kora-signature:") === 0) {
                    $signature = trim(substr($header, 17));
                }
                return strlen($header);
            }
        ]);
        $text = curl_exec($handle);
        if ($text === false) {
            throw new KoraException("Request to Kora failed: " . curl_error($handle));
        }
        $status = curl_getinfo($handle, CURLINFO_RESPONSE_CODE);
        $data = json_decode($text, true);
        return ["status" => $status, "text" => $text, "data" => is_array($data) ? $data : [], "signature" => $signature];
    }

    private function fail(array $reply): KoraException
    {
        return new KoraException(isset($reply["data"]["error"]) ? $reply["data"]["error"] : "Kora answered with status {$reply["status"]}", $reply["status"]);
    }

    public function call(string $method, string $path, ?array $body = null): array
    {
        $reply = $this->send($method, $path, $body);
        if ($reply["status"] >= 400) {
            throw $this->fail($reply);
        }
        return $reply["data"];
    }

    private function key(): string
    {
        $der = base64_decode(preg_replace("#-----[A-Z ]+-----|\s#", "", $this->publicKey));
        return substr($der, -SODIUM_CRYPTO_SIGN_PUBLICKEYBYTES);
    }

    private function verify(string $text, string $signature): bool
    {
        $raw = base64_decode($signature, true);
        return $raw !== false && strlen($raw) === SODIUM_CRYPTO_SIGN_BYTES && sodium_crypto_sign_verify_detached($raw, $text, $this->key());
    }

    private function check(string $path, array $body): array
    {
        $reply = $this->send("POST", $path, $body);
        if ($reply["status"] === 429 || $reply["status"] >= 500) {
            throw $this->fail($reply);
        }
        if ($this->publicKey && !$this->verify($reply["text"], (string) $reply["signature"])) {
            throw new KoraException("Response signature verification failed", $reply["status"]);
        }
        return $reply["data"];
    }

    private function body(string $key, array $fields, ?string $product): array
    {
        $name = $product !== null ? $product : $this->product;
        return array_merge($fields, ["license_key" => $key], $name !== null ? ["product_name" => $name] : []);
    }

    public function validate(string $key, array $fields = [], ?string $product = null): array
    {
        return $this->check("/licenses/validate", $this->body($key, $fields, $product));
    }

    public function activate(string $key, array $fields = [], ?string $product = null): array
    {
        return $this->check("/licenses/activate", $this->body($key, $fields, $product));
    }

    public function deactivate(string $key, array $fields = []): array
    {
        return $this->check("/licenses/deactivate", array_merge($fields, ["license_key" => $key]));
    }

    public function file(int|string $id): string
    {
        $reply = $this->send("GET", "/licenses/" . rawurlencode((string) $id) . "/file");
        if ($reply["status"] >= 400) {
            throw $this->fail($reply);
        }
        return trim($reply["text"]);
    }

    public function offline(string $file): array
    {
        if (!$this->publicKey) {
            throw new KoraException("A publicKey is required to read offline licenses");
        }
        $parts = explode(".", trim($file));
        if (count($parts) !== 2) {
            throw new KoraException("Offline license file is malformed");
        }
        $decode = fn (string $value): string => base64_decode(strtr($value, "-_", "+/") . str_repeat("=", (4 - strlen($value) % 4) % 4));
        $payload = $decode($parts[0]);
        $signature = $decode($parts[1]);
        if (strlen($signature) !== SODIUM_CRYPTO_SIGN_BYTES || !sodium_crypto_sign_verify_detached($signature, $payload, $this->key())) {
            throw new KoraException("Offline license signature verification failed");
        }
        $license = json_decode($payload, true);
        if (strtotime($license["refresh_after"]) < time()) {
            throw new KoraException("Offline license needs a refresh");
        }
        if ($license["expires_at"] !== null && strtotime($license["expires_at"]) < time()) {
            throw new KoraException("Offline license expired");
        }
        return $license;
    }

    public function merge(int|string $customer, int|string $duplicate): array
    {
        return $this->call("POST", "/customers/" . rawurlencode((string) $customer) . "/merge", ["customer_id" => $duplicate])["customer"];
    }
}
