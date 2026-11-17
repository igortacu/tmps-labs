package gateway.patterns.proxy;

import java.util.Set;

public final class AllowlistPolicy implements AccessPolicy {
    private final Set<String> clients;

    public AllowlistPolicy(Set<String> clients) { this.clients = Set.copyOf(clients); }

    @Override
    public boolean allows(String clientId) { return clients.contains(clientId); }
}
