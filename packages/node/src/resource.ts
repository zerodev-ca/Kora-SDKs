import type { ListQuery, Page } from "./types.js";

export interface Transport {
    call<T>(method: string, path: string, body?: unknown): Promise<T>;
}

export class Resource<T, I> {
    private transport: Transport;
    private name: string;
    private single: string;

    constructor(transport: Transport, name: string, single: string) {
        this.transport = transport;
        this.name = name;
        this.single = single;
    }

    private path(id: number | string): string {
        return `/${this.name}/${encodeURIComponent(String(id))}`;
    }

    async list(query: ListQuery = {}): Promise<Page<T>> {
        const data = await this.transport.call<Record<string, unknown>>("GET", `/${this.name}?${new URLSearchParams(Object.entries(query).filter(([, value]) => value !== undefined).map(([key, value]) => [key, String(value)]))}`);
        return { total: data.total as number, rows: data[this.name] as T[] };
    }

    get(id: number | string): Promise<T> {
        return this.transport.call<T>("GET", this.path(id));
    }

    async create(input: I): Promise<T> {
        return (await this.transport.call<Record<string, T>>("POST", `/${this.name}`, input))[this.single];
    }

    async update(id: number | string, input: Partial<I>): Promise<T> {
        return (await this.transport.call<Record<string, T>>("PATCH", this.path(id), input))[this.single];
    }

    async delete(id: number | string, force = false): Promise<void> {
        await this.transport.call("DELETE", force ? `${this.path(id)}?force=true` : this.path(id));
    }
}
