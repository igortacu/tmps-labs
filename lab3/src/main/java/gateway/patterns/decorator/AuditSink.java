package gateway.patterns.decorator;

public interface AuditSink {
    void record(AuditEvent event);
}
