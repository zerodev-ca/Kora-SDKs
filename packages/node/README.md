# @zerodev-ca/kora

Official Node.js and TypeScript SDK for Kora.

```bash
npm install @zerodev-ca/kora
```

## Validation

```ts
import { Client } from "@zerodev-ca/kora";

const kora = new Client({ url: "https://kora.example.com", product: "MyPlugin", publicKey: process.env.KORA_PUBLIC_KEY });

const result = await kora.validate("XXXXX-XXXXX-XXXXX-XXXXX");
if (!result.valid) throw new Error(result.message);
console.log(result.license.customer);
```

`result` holds `valid`, `code`, `message`, `license` and `timestamp`. With `publicKey` set, an answer without a valid `x-kora-signature` throws.

Extra fields go in the third argument, for example the device id a whitelist group reads: `kora.validate(key, undefined, { hwid })`.

## Devices

```ts
const activated = await kora.activate(key, undefined, { hwid: machineId });
if (activated.code === "seats_full") console.log(activated.message);

await kora.deactivate(key, { hwid: machineId });
```

`activate` takes a free seat in each whitelist group of the product for the values in the request, then validates. `deactivate` frees them and answers with `success`, `code` (`deactivated`, `not_activated`, `not_found`) and `removed`.

## Offline licenses

```ts
const file = await admin.file(license.id);
const offline = kora.offline(file);
```

`file` downloads a signed license file with an API token. `offline` checks the file against `publicKey` without a network call and throws when the signature is wrong, the license expired or `refresh_after` has passed. Validate online first and fall back to the file when Kora cannot be reached.

## Licenses, products and customers

Pass an API token with the scopes you need.

```ts
const kora = new Client({ url: "https://kora.example.com", apiKey: process.env.KORA_TOKEN });

const product = await kora.products.create({ name: "MyPlugin", version: "1.0.0" });
const customer = await kora.customers.create({ name: "Jane", email: "jane@example.com" });
const license = await kora.licenses.create({ product_id: product.id, customer_id: customer.id });

const { total, rows } = await kora.licenses.list({ status: "active", limit: 25 });
await kora.licenses.update(license.id, { status: "suspended" });
await kora.licenses.delete(license.id);
```

Every resource has `list`, `get`, `create`, `update` and `delete`. Block a customer with `customers.update(id, { blocked: true })` and fold a duplicate into a customer with `kora.merge(id, duplicateId)`. `delete(id, true)` forces removal of products and customers that still have licenses. Failed requests throw a `KoraError` with `status`.

## Options

| Field | Description |
|---|---|
| `url` | Kora API address. |
| `apiKey` | API token, sent as `Authorization: Bearer`. |
| `product` | Default product name for `validate`. |
| `publicKey` | Kora's Ed25519 public key. Turns on signature checks. |
| `timeoutMs` | Request timeout. Defaults to 15000. |
