export interface ClientOptions {
    url: string;
    product: string;
    key?: string;
    apiKey?: string;
    intervalMs?: number;
    timeoutMs?: number;
    onInvalid?: (result: ValidationResponse) => void;
}

export interface Product {
    id: number;
    name: string;
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
}

export interface ValidationResponse {
    valid: boolean;
    message: string;
    code: number;
    license: License | null;
}
