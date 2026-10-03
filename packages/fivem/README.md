# kora-fivem

Official FiveM and RedM SDK for Kora licensing. It runs as server-side JavaScript and is called the same way from Lua or JavaScript.

## Installation

Copy this folder into `resources/kora-fivem` and add `ensure kora-fivem` to `server.cfg` before your own resource.

## Usage

```lua
exports["kora-fivem"]:validate({
    url = "https://api.yourdomain.com",
    product = "MyScript",
    key = GetConvar("my_script_license", ""),
    intervalMs = 300000
}, function(valid, data)
    if not valid then
        print("^1" .. data.message .. "^7")
        StopResource(GetCurrentResourceName())
    end
end)
```

`url` is your Kora API address. `data` holds `valid`, `message`, `code` (the HTTP status) and `license`.

## Options

| Field | Description |
|---|---|
| `apiKey` | Sent as `Authorization: Bearer`. Needed when Require API Key is on. |
| `intervalMs` | Re-check the license on this interval after a valid result. A failed re-check fires `kora:invalid` on the server. |

Call `exports["kora-fivem"]:stop(config)` to stop re-checking.
