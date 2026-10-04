const crypto = require("crypto");
const http = require("http");
const https = require("https");

class Kora {
    endpoint(config, path) {
        return `${String(config.url).replace(/\/+$/, "").replace(/\/api\/v1$/, "")}/api/v1${path}`;
    }

    send(config, method, path, body) {
        return new Promise((resolve, reject) => {
            const payload = body === undefined ? "" : JSON.stringify(body);
            const target = new URL(this.endpoint(config, path));
            const request = (target.protocol === "https:" ? https : http).request(target, {
                method,
                headers: { "content-type": "application/json", accept: "application/json", "content-length": Buffer.byteLength(payload), ...(config.apiKey ? { authorization: `Bearer ${config.apiKey}` } : {}) },
                timeout: 15000
            }, response => {
                const chunks = [];
                response.on("data", chunk => chunks.push(chunk));
                response.on("end", () => {
                    const text = Buffer.concat(chunks).toString("utf8");
                    resolve({ status: response.statusCode, signature: response.headers["x-kora-signature"], text, data: text.startsWith("{") ? JSON.parse(text) : {} });
                });
            });
            request.on("error", reject);
            request.on("timeout", () => request.destroy(new Error("Kora timed out")));
            request.end(payload);
        });
    }

    fail(reply) {
        return Object.assign(new Error(typeof reply.data.error === "string" ? reply.data.error : `Kora answered with status ${reply.status}`), { status: reply.status });
    }

    verified(config, reply) {
        if (!config.publicKey) return true;
        try {
            return typeof reply.signature === "string" && crypto.verify(null, Buffer.from(reply.text, "utf8"), crypto.createPublicKey(config.publicKey), Buffer.from(reply.signature, "base64"));
        } catch {
            return false;
        }
    }

    async validate(config, key, product) {
        const name = product ? product : config.product;
        const reply = await this.send(config, "POST", "/licenses/validate", { license_key: key, ...(name ? { product_name: name } : {}) });
        if (reply.status === 429 || reply.status >= 500) throw this.fail(reply);
        if (!this.verified(config, reply)) throw Object.assign(new Error("Response signature verification failed"), { status: reply.status });
        return reply.data;
    }

    async call(config, method, path, body) {
        const reply = await this.send(config, method, path, body);
        if (reply.status >= 400) throw this.fail(reply);
        return reply.data;
    }

    async resource(config, name, single, action, args) {
        const path = `/${name}/${encodeURIComponent(String(args.id))}`;
        switch (action) {
            case "list": {
                const data = await this.call(config, "GET", `/${name}?${new URLSearchParams(args.query ? Object.entries(args.query).map(([key, value]) => [key, String(value)]) : [])}`);
                return { total: data.total, rows: data[name] };
            }
            case "get":
                return this.call(config, "GET", path);
            case "create":
                return (await this.call(config, "POST", `/${name}`, args.fields))[single];
            case "update":
                return (await this.call(config, "PATCH", path, args.fields))[single];
            case "delete":
                return this.call(config, "DELETE", args.force ? `${path}?force=true` : path);
            default:
                throw new Error(`Unknown action ${action}`);
        }
    }

    reply(promise, callback) {
        promise.then(data => callback(true, data)).catch(error => callback(false, { message: error.message, status: error.status ? error.status : 0 }));
    }
}

const kora = new Kora();

exports("validate", (config, key, callback, product) => kora.reply(kora.validate(config, key, product), (success, data) => callback(success && data.valid === true, data)));
exports("licenses", (config, action, args, callback) => kora.reply(kora.resource(config, "licenses", "license", action, args), callback));
exports("products", (config, action, args, callback) => kora.reply(kora.resource(config, "products", "product", action, args), callback));
exports("customers", (config, action, args, callback) => kora.reply(kora.resource(config, "customers", "customer", action, args), callback));
