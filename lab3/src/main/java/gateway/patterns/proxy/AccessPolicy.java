package gateway.patterns.proxy;

public interface AccessPolicy {
    boolean allows(String clientId);
}
