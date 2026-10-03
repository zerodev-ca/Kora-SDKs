# @zerodev-ca/kora

Official Node.js and TypeScript SDK for Kora licensing.

## Installation

```bash
npm install @zerodev-ca/kora
# or
bun add @zerodev-ca/kora
```

## Usage

```typescript
import { Client } from "@zerodev-ca/kora";

const client = new Client({
    url: "https://api.yourdomain.com",
    product: "MySoftware",
    key: process.env.LICENSE_KEY
});

const result = await client.validate();
if (!result.valid) {
    console.error(result.message);
    process.exit(1);
}

if (result.license !== null && result.license.customer !== null) {
    console.log("Licensed to", result.license.customer.name);
}
```

`url` is your Kora API address. `result` holds `valid`, `message`, `code` (the HTTP status) and `license`, which carries the key, status, expiry, product and customer.

## Options

| Option | Description |
|---|---|
| `apiKey` | Sent as `Authorization: Bearer`. Needed when Require API Key is on. |
| `intervalMs` | Re-check the license on this interval after a valid result. |
| `onInvalid` | Called with the result when a re-check fails. |
| `timeoutMs` | Request timeout, 15 seconds by default. |

Call `client.stop()` to stop re-checking.
