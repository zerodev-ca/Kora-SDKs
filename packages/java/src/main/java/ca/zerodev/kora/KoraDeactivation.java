package ca.zerodev.kora;

public class KoraDeactivation {
    private boolean success;
    private String code;
    private String message;
    private int removed;
    private long timestamp;

    public boolean isSuccess() {
        return this.success;
    }

    public String getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    public int getRemoved() {
        return this.removed;
    }

    public long getTimestamp() {
        return this.timestamp;
    }
}
