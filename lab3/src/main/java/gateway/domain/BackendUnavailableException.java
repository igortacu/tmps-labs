package gateway.domain;

public class BackendUnavailableException extends RuntimeException {
    public BackendUnavailableException(String message) {
        super(message);
    }
}
