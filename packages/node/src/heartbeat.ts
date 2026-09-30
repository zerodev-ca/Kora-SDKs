export class Heartbeat {
    private timer: ReturnType<typeof setInterval> | null;
    private intervalMs: number;
    private ping: () => Promise<void>;

    constructor(intervalMs: number, ping: () => Promise<void>) {
        this.intervalMs = intervalMs;
        this.ping = ping;
        this.timer = null;
    }

    start(): void {
        if (this.timer !== null) {
            return;
        }
        this.timer = setInterval(() => {
            this.ping().catch(() => {});
        }, this.intervalMs);
    }

    stop(): void {
        if (this.timer === null) {
            return;
        }
        clearInterval(this.timer);
        this.timer = null;
    }
}
