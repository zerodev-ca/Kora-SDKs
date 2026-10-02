export type LicenseStatus = "ACTIVE" | "SUSPENDED" | "REVOKED" | "EXPIRED";

export interface ClientOptions {
    url: string;
    product: string;
    key?: string;
    publicKey?: string;
    signatureHeader?: string;
    heartbeatIntervalMs?: number;
    machineId?: string;
    maxSkewMs?: number;
}

export interface ValidationRequest {
    license_key: string;
    product_name?: string;
    nonce?: string;
    hwid?: string;
    identifier?: string;
    session_id?: string;
    metadata?: Record<string, unknown>;
}

export interface SessionData {
    token: string;
    timeout?: number;
}

export interface ValidationResponse {
    valid: boolean;
    code?: string;
    message: string;
    product?: string | null;
    user?: string | null;
    status?: LicenseStatus | null;
    expires_at?: string | null;
    addons?: string[];
    nonce?: string | null;
    timestamp?: number;
    valid_until?: number | null;
    session?: SessionData | null;
    device?: { id: number; hwid: string; name: string } | null;
    license?: Record<string, unknown> | null;
    offline?: OfflineLicense;
}

export interface UpdateResponse {
    success: boolean;
    code?: string;
    message: string;
    update_available?: boolean;
    current?: string | null;
    latest?: { version: string; channel: string; notes: string; required: boolean; size: number; sha256: string | null; published_at: string } | null;
    download_url?: string | null;
    nonce?: string | null;
    timestamp?: number;
}

export interface MachineInfo {
    hwid: string;
    platform: string;
    arch: string;
    hostname: string;
    mac: string;
}

export interface OfflineLicense {
    key: string;
    product: string;
    status: LicenseStatus;
    issued_at: number;
    expires_at: number;
    grace_period_ms: number;
    hwid?: string;
    signature: string;
}
