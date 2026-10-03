const crypto = require("crypto");
const http = require("http");
const https = require("https");

class Kora {
    constructor() {
        this.timers = new Map();
    }

    id(config) {
        return `${config.url}|${config.key}`;
    }

    hwid(config) {
        if (config.hwid) return String(config.hwid);
        return crypto.createHash("sha256").update(GetConvar("sv_licenseKeyToken", "fivem-server")).digest("hex");
    }

    endpoint(config, path) {
        return `${String(config.url).replace(/\/+$/, "").replace(/\/api\/v1$/, "")}/api/v1${path}`;
    }

    post(config, path, body) {
        return new Promise((resolve, reject) => {
            const target = new URL(this.endpoint(config, path));
            const payload = JSON.stringify(body);
            const headers = { "content-type": "application/json", accept: "application/json", "content-length": Buffer.byteLength(payload), ...(config.apiKey ? { authorization: `Bearer ${config.apiKey}` } : {}) };
            const request = (target.protocol === "https:" ? https : http).request(target, { method: "POST", headers, timeout: 15000 }, response => {
                const chunks = [];
                response.on("data", chunk => chunks.push(chunk));
                response.on("end", () => resolve({ status: response.statusCode, headers: response.headers, text: Buffer.concat(chunks).toString("utf8") }));
            });
            request.on("error", reject);
            request.on("timeout", () => request.destroy(new Error("The license server timed out")));
            request.end(payload);
        });
    }

    check(config, response, nonce) {
        const signature = response.headers["x-kora-signature"];
        if (typeof signature !== "string" || !crypto.verify(null, Buffer.from(response.text, "utf8"), crypto.createPublicKey(config.publicKey), Buffer.from(signature, "base64"))) throw new Error("Response signature verification failed");
        const data = JSON.parse(response.text);
        if (data.nonce !== nonce) throw new Error("Response does not belong to this request");
        if (typeof data.timestamp !== "number" || Math.abs(Date.now() - data.timestamp) > 300000) throw new Error("Response is too old, check the server clock");
    }

    async request(config, path, extra) {
        if (!config.key) throw new Error("License key is required");
        const nonce = crypto.randomBytes(16).toString("hex");
        const response = await this.post(config, path, {
            license_key: config.key,
            product_name: config.product,
            hwid: this.hwid(config),
            device_name: GetConvar("sv_hostname", "FiveM server"),
            platform: "fivem",
            ...(config.version ? { version: String(config.version) } : {}),
            nonce,
            ...(extra ? extra : {})
        });
        if (config.publicKey) this.check(config, response, nonce);
        const data = (() => {
            try {
                return JSON.parse(response.text);
            } catch {
                return {};
            }
        })();
        return {
            ...data,
            valid: data.valid === true,
            message: typeof data.message === "string" ? data.message : typeof data.error === "string" ? data.error : `Kora answered with status ${response.status}`,
            code: response.status,
            license: data.license ? data.license : null
        };
    }

    validate(config, callback) {
        this.request(config, "/licenses/validate")
            .then(data => {
                if (data.valid) this.watch(config);
                callback(data.valid, data);
            })
            .catch(error => callback(false, { valid: false, message: error.message, code: 0, license: null }));
    }

    checkUpdate(config, version, callback) {
        this.request(config, "/updates/check", { version: String(version), channel: config.channel ? String(config.channel) : "stable" })
            .then(data => callback(data.update_available === true, data))
            .catch(error => callback(false, { valid: false, message: error.message, code: 0, license: null }));
    }

    watch(config) {
        if (!config.intervalMs || this.timers.has(this.id(config))) return;
        this.timers.set(this.id(config), setInterval(() => {
            this.request(config, "/licenses/validate")
                .then(data => {
                    if (!data.valid) emit("kora:invalid", data);
                })
                .catch(error => console.log(`^3[Kora] ${error.message}^7`));
        }, Number(config.intervalMs)));
    }

    stop(config) {
        if (!this.timers.has(this.id(config))) return;
        clearInterval(this.timers.get(this.id(config)));
        this.timers.delete(this.id(config));
    }
}

const kora = new Kora();

exports("validate", (config, callback) => kora.validate(config, callback));
exports("checkUpdate", (config, version, callback) => kora.checkUpdate(config, version, callback));
exports("stop", config => kora.stop(config));
