export type LicenseStatus = "ACTIVE" | "SUSPENDED" | "REVOKED" | "EXPIRED";

export interface ClientOptions {
    url: string;
    product: string;
    key?: string;
    publicKey?: string;
    signatureHeader?: string;
    heartbeatIntervalMs?: number;
    machineId?: string;
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
}

export interface ValidationResponse {
    valid: boolean;
    message: string;
    product?: string;
    user?: string | null;
    status?: LicenseStatus;
    expires_at?: string | null;
    addons?: string[];
    nonce?: string;
    timestamp?: number;
    variables?: Record<string, unknown>;
    user_variables?: Record<string, unknown>;
    session?: SessionData | null;
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
    features: string[];
    signature: string;
}
