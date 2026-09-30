package ca.zerodev.kora;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KoraHeartbeat {
    private ScheduledExecutorService executor;
    private final long intervalMs;
    private final Runnable task;

    public KoraHeartbeat(long intervalMs, Runnable task) {
        this.intervalMs = intervalMs;
        this.task = task;
        this.executor = null;
    }

    public synchronized void start() {
        if (this.executor != null && !this.executor.isShutdown()) {
            return;
        }
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "kora-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        this.executor.scheduleAtFixedRate(this.task, this.intervalMs, this.intervalMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (this.executor == null) {
            return;
        }
        this.executor.shutdownNow();
        this.executor = null;
    }
}
