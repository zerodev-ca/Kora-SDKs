export class KoraError extends Error {
    readonly status: number;

    constructor(message: string, status: number) {
        super(message);
        this.name = "KoraError";
        this.status = status;
    }
}
