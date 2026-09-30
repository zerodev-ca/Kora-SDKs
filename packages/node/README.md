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
import { KoraClient } from "@zerodev-ca/kora";

const client = new KoraClient({
    serverUrl: "https://api.yourdomain.com",
    product: "MySoftware",
    publicKey: "-----BEGIN PUBLIC KEY-----\n..."
});

const result = await client.validate("XXXXX-XXXXX-XXXXX-XXXXX");
if (result.valid) {
    console.log("License is valid for:", result.user);
} else {
    console.error("Validation failed:", result.message);
}
```

## Offline Verification

```typescript
const offline = client.verifyOffline(signedOfflineToken);
if (offline.valid) {
    console.log("Offline license valid until:", new Date(offline.payload.expires_at));
}
```
