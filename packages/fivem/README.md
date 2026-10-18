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

## Devices

Put the values your whitelist groups read in `config.fields`. They are sent with every validation, activation and deactivation.

```lua
config.fields = { hwid = GetConvar("sv_hostname", "") }

exports["kora-fivem"]:activate(config, GetConvar("my_script_license", ""), function(valid, data)
    if not valid then print("^1" .. data.message .. "^7") end
end)

exports["kora-fivem"]:deactivate(config, GetConvar("my_script_license", ""), function(success, data)
    print(data.message)
end)
```

## Offline licenses

```lua
exports["kora-fivem"]:offline(config, LoadResourceFile(GetCurrentResourceName(), "license.lic"), function(success, license)
    if success then print(license.key) end
end)
```

The file comes from the license page in the dashboard or `GET /api/v1/licenses/:id/file`. It is checked against `publicKey` without a network call and fails when the signature is wrong, the license expired or `refresh_after` has passed.

## Licenses, products and customers

`licenses`, `products` and `customers` take an action (`list`, `get`, `create`, `update`, `delete`) and an arguments table with `id`, `fields`, `query` or `force`. `customers` also takes `merge` with `id` and `customer_id`.

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
| `fields` | Extra request fields, such as `hwid`. |
