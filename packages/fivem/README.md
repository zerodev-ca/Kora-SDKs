# kora-fivem

Official FiveM and RedM SDK for Kora licensing. It runs as server-side JavaScript so it can check Ed25519 signatures, and it is called the same way from Lua or JavaScript.

## Installation

Copy this folder into `resources/kora-fivem` and add `ensure kora-fivem` to `server.cfg` before your own resource.

## Usage

```lua
exports["kora-fivem"]:validate({
    url = "https://api.yourdomain.com/api/v1",
    product = "MyScript",
    key = GetConvar("my_script_license", ""),
    publicKey = [[-----BEGIN PUBLIC KEY-----
...
-----END PUBLIC KEY-----]],
    heartbeatMs = 300000
}, function(valid, data)
    if not valid then
        print("^1" .. data.message .. "^7")
        StopResource(GetCurrentResourceName())
    end
end)
```

With `publicKey` set, every answer must carry a valid `x-kora-signature`, the nonce the SDK sent, and a recent timestamp. `heartbeatMs` keeps the session alive and fires `kora:invalid` on the server when a later check fails.

The hardware ID is a hash of the server's `sv_licenseKeyToken`, so one Cfx.re server key counts as one device. Pass `hwid` in the config to override it.

Call `exports["kora-fivem"]:deactivate(config)` to release the device, for example when moving the script to another server.
