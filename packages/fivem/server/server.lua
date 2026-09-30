Kora = {}

function Kora.validate(config, callback)
    local endpoint = string.gsub(config.url, "/+$", "") .. "/licenses/validate"
    local payload = json.encode({
        license_key = config.key,
        product_name = config.product,
        hwid = GetConvar("sv_licenseKeyToken", "fivem-server"),
        identifier = GetConvar("sv_licenseKeyToken", "fivem-server")
    })

    PerformHttpRequest(endpoint, function(statusCode, responseText, headers)
        if statusCode ~= 200 or not responseText then
            callback(false, { message = "Connection error (" .. tostring(statusCode) .. ")" })
            return
        end

        local data = json.decode(responseText)
        if not data then
            callback(false, { message = "Invalid JSON response" })
            return
        end

        callback(data.valid == true, data)
    end, "POST", payload, {
        ["Content-Type"] = "application/json",
        ["Accept"] = "application/json"
    })
end

exports("validate", Kora.validate)
