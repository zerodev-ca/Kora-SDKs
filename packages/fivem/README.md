# kora-fivem

Official FiveM and RedM SDK for Kora licensing. It runs as server-side JavaScript and is called the same way from Lua or JavaScript.

## Installation

Copy this folder into `resources/kora-fivem` and add `ensure kora-fivem` to `server.cfg` before your own resource.

## Usage

```lua
local config = {
    url = "https://api.yourdomain.com",
    product = "MyScript",
    key = GetConvar("my_script_license", ""),
    publicKey = [[-----BEGIN PUBLIC KEY-----
...
-----END PUBLIC KEY-----]],
    intervalMs = 300000
}

exports["kora-fivem"]:validate(config, function(valid, data)
    if not valid then
        print("^1" .. data.message .. "^7")
        StopResource(GetCurrentResourceName())
    end
end)
```

`data` holds `valid`, `message`, `code` (the HTTP status) and `license`. With `publicKey` set, every answer must carry a valid `x-kora-signature`, the nonce the SDK sent and a recent timestamp.

The hardware ID is a hash of the server's `sv_licenseKeyToken`, so one Cfx.re server key counts as one device. Pass `hwid` to override it.

## Options

| Field | Description |
|---|---|
| `apiKey` | Sent as `Authorization: Bearer`. Needed when Require API Key is on. |
| `publicKey` | Kora's Ed25519 public key. Turns on signature checks. |
| `version` | Your script version, shown on the device in Kora. |
| `intervalMs` | Re-check on this interval after a valid result. A failed re-check fires `kora:invalid` on the server. |

## Updates

```lua
exports["kora-fivem"]:checkUpdate(config, "1.4.2", function(available, data)
    if available then
        print("Version " .. data.latest.version .. " is available")
    end
end)
```

Call `exports["kora-fivem"]:stop(config)` to stop re-checking.
