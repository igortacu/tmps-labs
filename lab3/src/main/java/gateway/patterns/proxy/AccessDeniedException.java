package gateway.patterns.proxy;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String clientId) {
        super("client " + clientId + " is not authorized");
    }
}
