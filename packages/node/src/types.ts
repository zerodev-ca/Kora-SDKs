export interface ClientOptions {
    url: string;
    apiKey?: string;
    product?: string;
    publicKey?: string;
    timeoutMs?: number;
}

export interface Product {
    id: number;
    name: string;
    description: string;
    version: string;
    price: number;
    currency: string;
    duration: number;
    duration_unit: "days" | "weeks" | "months" | "years";
    active: boolean;
    created_at: string;
    updated_at: string;
}

export interface Customer {
    id: number;
    name: string;
    email: string | null;
    discord_id: string | null;
    blocked: boolean;
    notes: string;
    created_at: string;
    updated_at: string;
}

export interface Addon {
    id: number;
    name: string;
    price: number;
    currency: string;
}

export interface License {
    id: number;
    key: string;
    status: string;
    product_id: number;
    customer_id: number;
    product_name: string | null;
    customer_name: string | null;
    product: Pick<Product, "id" | "name" | "version" | "active"> | null;
    customer: Pick<Customer, "id" | "name" | "email" | "discord_id"> | null;
    addons: number[];
    addon_names: string[];
    leaked: boolean;
    expires_at: string | null;
    last_validated_at: string | null;
    created_at: string;
    updated_at: string;
}

export interface ProductInput {
    name: string;
    description?: string;
    version?: string;
    price?: number;
    currency?: string;
    duration?: number;
    duration_unit?: Product["duration_unit"];
    active?: boolean;
}

export interface CustomerInput {
    name: string;
    email?: string | null;
    discord_id?: string | null;
    blocked?: boolean;
    notes?: string;
}

export interface LicenseInput {
    product_id: number | string;
    customer_id: number | string;
    key?: string;
    status?: string;
    addons?: number[];
    expires_at?: string | null;
    leaked?: boolean;
}

export interface ListQuery {
    search?: string;
    limit?: number;
    offset?: number;
    [filter: string]: string | number | boolean | undefined;
}

export interface Page<T> {
    total: number;
    rows: T[];
}

export interface ValidatedLicense {
    key: string;
    status: string;
    product: Pick<Product, "id" | "name" | "version" | "active">;
    customer: Pick<Customer, "id" | "name" | "email" | "discord_id" | "blocked"> | null;
    addons: Addon[];
    expires_at: string | null;
    whitelists: Record<string, string[]>;
}

export interface ValidationResponse {
    valid: boolean;
    code: string;
    message: string;
    license: ValidatedLicense | null;
    timestamp: number;
}

export interface DeactivationResponse {
    success: boolean;
    code: "deactivated" | "not_activated" | "not_found" | "invalid_request" | "unauthorized";
    message: string;
    removed: number;
    timestamp: number;
}

export interface OfflineLicense {
    key: string;
    license_id: number;
    status: string;
    product: Pick<Product, "id" | "name">;
    customer: Pick<Customer, "id" | "name" | "email"> | null;
    addons: string[];
    whitelists: Record<string, string[]>;
    expires_at: string | null;
    issued_at: string;
    refresh_after: string;
}
