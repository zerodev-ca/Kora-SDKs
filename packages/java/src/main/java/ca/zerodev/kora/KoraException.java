package ca.zerodev.kora;

public class KoraException extends RuntimeException {
    private final int status;

    public KoraException(String message, int status) {
        super(message);
        this.status = status;
    }

    public KoraException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
    }

    public int getStatus() {
        return this.status;
    }
}
