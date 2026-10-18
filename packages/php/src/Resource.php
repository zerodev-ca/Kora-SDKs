<?php

namespace ZeroDev\Kora;

class Resource
{
    public function __construct(private Client $client, private string $name, private string $single)
    {
    }

    private function path(int|string $id): string
    {
        return "/{$this->name}/" . rawurlencode((string) $id);
    }

    public function list(array $query = []): array
    {
        $data = $this->client->call("GET", "/{$this->name}" . ($query ? "?" . http_build_query($query) : ""));
        return ["total" => $data["total"], "rows" => $data[$this->name]];
    }

    public function get(int|string $id): array
    {
        return $this->client->call("GET", $this->path($id));
    }

    public function create(array $fields): array
    {
        return $this->client->call("POST", "/{$this->name}", $fields)[$this->single];
    }

    public function update(int|string $id, array $fields): array
    {
        return $this->client->call("PATCH", $this->path($id), $fields)[$this->single];
    }

    public function delete(int|string $id, bool $force = false): void
    {
        $this->client->call("DELETE", $this->path($id) . ($force ? "?force=true" : ""));
    }
}
