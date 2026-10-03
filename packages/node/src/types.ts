export interface ClientOptions {
    url: string;
    product: string;
    key?: string;
    apiKey?: string;
    publicKey?: string;
    hwid?: string;
    version?: string;
    intervalMs?: number;
    timeoutMs?: number;
    maxSkewMs?: number;
    onInvalid?: (result: ValidationResponse) => void;
}

export interface Product {
    id: number;
    name: string;
    version: string;
}

export interface Customer {
    id: number;
    name: string;
    email: string | null;
    discord_id: string | null;
}

export interface License {
    key: string;
    status: string;
    expires_at: string | null;
    product: Product;
    customer: Customer | null;
    data: Record<string, unknown>;
}

export interface OfflineLicense {
    expires_at: number;
    grace_period_ms: number;
    hwid: string;
    issued_at: number;
    key: string;
    product: string;
    status: string;
    signature: string;
}

export interface Release {
    version: string;
    channel: string;
    notes: string;
    required: boolean;
    file_name: string;
    size: number;
    sha256: string;
    published_at: string;
}

export interface ValidationResponse {
    valid: boolean;
    message: string;
    code: number;
    license: License | null;
    nonce: string | null;
    timestamp: number | null;
}

export interface OfflineResponse extends ValidationResponse {
    offline: OfflineLicense | null;
}

export interface UpdateResponse extends ValidationResponse {
    update_available: boolean;
    current: string | null;
    latest: Release | null;
    download_url: string | null;
}
