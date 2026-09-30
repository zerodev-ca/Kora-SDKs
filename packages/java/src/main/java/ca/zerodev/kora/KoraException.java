package ca.zerodev.kora;

public class KoraException extends RuntimeException {
    public KoraException(String message) {
        super(message);
    }

    public KoraException(String message, Throwable cause) {
        super(message, cause);
    }
}
