const http = require("http");
const https = require("https");

class Kora {
    constructor() {
        this.timers = new Map();
    }

    id(config) {
        return `${config.url}|${config.key}`;
    }

    endpoint(config) {
        return `${String(config.url).replace(/\/+$/, "").replace(/\/api\/v1$/, "")}/api/v1/licenses/validate`;
    }

    post(config) {
        return new Promise((resolve, reject) => {
            const target = new URL(this.endpoint(config));
            const payload = JSON.stringify({ license_key: config.key, product_name: config.product });
            const headers = { "content-type": "application/json", accept: "application/json", "content-length": Buffer.byteLength(payload), ...(config.apiKey ? { authorization: `Bearer ${config.apiKey}` } : {}) };
            const request = (target.protocol === "https:" ? https : http).request(target, { method: "POST", headers, timeout: 15000 }, response => {
                const chunks = [];
                response.on("data", chunk => chunks.push(chunk));
                response.on("end", () => resolve({ status: response.statusCode, text: Buffer.concat(chunks).toString("utf8") }));
            });
            request.on("error", reject);
            request.on("timeout", () => request.destroy(new Error("The license server timed out")));
            request.end(payload);
        });
    }

    async request(config) {
        if (!config.key) throw new Error("License key is required");
        const response = await this.post(config);
        const data = (() => {
            try {
                return JSON.parse(response.text);
            } catch {
                return {};
            }
        })();
        return {
            valid: data.valid === true,
            message: typeof data.message === "string" ? data.message : typeof data.error === "string" ? data.error : `Kora answered with status ${response.status}`,
            code: response.status,
            license: data.license ? data.license : null
        };
    }

    validate(config, callback) {
        this.request(config)
            .then(data => {
                if (data.valid) this.watch(config);
                callback(data.valid, data);
            })
            .catch(error => callback(false, { valid: false, message: error.message, code: 0, license: null }));
    }

    watch(config) {
        if (!config.intervalMs || this.timers.has(this.id(config))) return;
        this.timers.set(this.id(config), setInterval(() => {
            this.request(config)
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
exports("stop", config => kora.stop(config));
