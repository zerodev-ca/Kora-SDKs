# kora-fivem

Official FiveM and RedM SDK for Kora. It runs as server-side JavaScript and is called the same way from Lua or JavaScript.

## Installation

Copy this folder into `resources/kora-fivem` and add `ensure kora-fivem` to `server.cfg` before your own resource.

## Validation

```lua
local config = {
    url = "https://kora.example.com",
    product = "MyScript",
    publicKey = [[-----BEGIN PUBLIC KEY-----
...
-----END PUBLIC KEY-----]]
}

exports["kora-fivem"]:validate(config, GetConvar("my_script_license", ""), function(valid, data)
    if not valid then
        print("^1" .. data.message .. "^7")
        StopResource(GetCurrentResourceName())
    end
end)
```

`data` holds `valid`, `code`, `message` and `license`. If the request fails, `data` holds `message` and `status`.

## Licenses, products and customers

`licenses`, `products` and `customers` take an action (`list`, `get`, `create`, `update`, `delete`) and an arguments table with `id`, `fields`, `query` or `force`.

```lua
local admin = { url = "https://kora.example.com", apiKey = GetConvar("kora_token", "") }

exports["kora-fivem"]:licenses(admin, "create", { fields = { product_id = 1, customer_id = 4 } }, function(success, license)
    if success then print(license.key) end
end)

exports["kora-fivem"]:licenses(admin, "list", { query = { status = "active" } }, function(success, page)
    if success then print(page.total) end
end)
```

## Options

| Field | Description |
|---|---|
| `url` | Kora API address. |
| `apiKey` | API token, sent as `Authorization: Bearer`. |
| `product` | Default product name for `validate`. |
| `publicKey` | Kora's Ed25519 public key. Turns on signature checks. |
