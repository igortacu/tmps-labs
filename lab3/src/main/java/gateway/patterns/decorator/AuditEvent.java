package gateway.patterns.decorator;

public record AuditEvent(String clientId, String outcome, String provider) {}
