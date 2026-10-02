const crypto = require("crypto");
const http = require("http");
const https = require("https");

class Kora {
    constructor() {
        this.sessions = new Map();
        this.timers = new Map();
    }

    hwid(config) {
        if (config.hwid) return String(config.hwid);
        return crypto.createHash("sha256").update(GetConvar("sv_licenseKeyToken", "fivem-server")).digest("hex");
    }

    post(url, body) {
        return new Promise((resolve, reject) => {
            const target = new URL(url);
            const payload = JSON.stringify(body);
            const request = (target.protocol === "https:" ? https : http).request(target, { method: "POST", headers: { "content-type": "application/json", accept: "application/json", "content-length": Buffer.byteLength(payload) }, timeout: 15000 }, response => {
                const chunks = [];
                response.on("data", chunk => chunks.push(chunk));
                response.on("end", () => resolve({ status: response.statusCode, headers: response.headers, text: Buffer.concat(chunks).toString("utf8") }));
            });
            request.on("error", reject);
            request.on("timeout", () => request.destroy(new Error("The license server timed out")));
            request.end(payload);
        });
    }

    async request(config, path, extra) {
        const nonce = crypto.randomBytes(16).toString("hex");
        const key = `${config.url}|${config.key}`;
        const response = await this.post(`${String(config.url).replace(/\/+$/, "")}${path}`, {
            license_key: config.key,
            product_name: config.product,
            hwid: this.hwid(config),
            identifier: this.hwid(config),
            session_id: this.sessions.has(key) ? this.sessions.get(key) : undefined,
            metadata: { name: GetConvar("sv_hostname", "FiveM server"), version: config.version ? String(config.version) : undefined },
            nonce,
            ...(extra ? extra : {})
        });
        if (response.status === 429) throw new Error("The license server is rate limiting this server");
        const data = JSON.parse(response.text);
        if (config.publicKey) {
            const signature = response.headers["x-kora-signature"];
            if (typeof signature !== "string" || !crypto.verify(null, Buffer.from(response.text, "utf8"), crypto.createPublicKey(config.publicKey), Buffer.from(signature, "base64"))) throw new Error("Response signature verification failed");
            if (data.nonce !== nonce) throw new Error("Response does not belong to this request");
            if (typeof data.timestamp !== "number" || Math.abs(Date.now() - data.timestamp) > 300000) throw new Error("Response is too old, check the server clock");
        }
        if (data.session && data.session.token) this.sessions.set(key, data.session.token);
        return data;
    }

    validate(config, callback) {
        this.request(config, "/licenses/validate")
            .then(data => {
                this.heartbeat(config);
                callback(data.valid === true, data);
            })
            .catch(error => callback(false, { valid: false, code: "error", message: error.message }));
    }

    heartbeat(config) {
        const key = `${config.url}|${config.key}`;
        if (!config.heartbeatMs || this.timers.has(key)) return;
        this.timers.set(key, setInterval(() => {
            this.request(config, "/licenses/validate")
                .then(data => {
                    if (data.valid !== true) emit("kora:invalid", data);
                })
                .catch(error => console.log(`^3[Kora] ${error.message}^7`));
        }, Number(config.heartbeatMs)));
    }

    deactivate(config, callback) {
        const key = `${config.url}|${config.key}`;
        if (this.timers.has(key)) clearInterval(this.timers.get(key));
        this.timers.delete(key);
        this.request(config, "/licenses/deactivate")
            .then(data => {
                this.sessions.delete(key);
                if (callback) callback(data.valid === true, data);
            })
            .catch(error => {
                if (callback) callback(false, { valid: false, code: "error", message: error.message });
            });
    }
}

const kora = new Kora();

exports("validate", (config, callback) => kora.validate(config, callback));
exports("deactivate", (config, callback) => kora.deactivate(config, callback));
