package gateway;

import com.sun.net.httpserver.HttpServer;
import gateway.domain.BackendUnavailableException;
import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import gateway.domain.InferenceService;
import gateway.patterns.adapter.HttpBackendAdapter;
import gateway.patterns.bridge.InstructionMode;
import gateway.patterns.bridge.PlainMode;
import gateway.patterns.composite.SimulatedBackend;
import gateway.patterns.decorator.AuditEvent;
import gateway.patterns.facade.InferenceGatewayFacade;
import gateway.patterns.proxy.AccessDeniedException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SelectedStructuralTest {
    private static final InferenceRequest REQUEST = new InferenceRequest("hello", "alice");

    private static final class LocalVendor implements AutoCloseable {
        private final HttpServer server;
        private final AtomicInteger calls = new AtomicInteger();
        private volatile String lastPrompt;

        LocalVendor(boolean failFirst) throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/complete", exchange -> {
                lastPrompt = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                int status = failFirst && calls.incrementAndGet() == 1 ? 503 : 200;
                if (!failFirst) calls.incrementAndGet();
                byte[] body = (status == 200 ? "remote: " + lastPrompt : "unavailable")
                    .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
        }

        URI endpoint() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/complete"); }
        int calls() { return calls.get(); }
        String lastPrompt() { return lastPrompt; }
        @Override public void close() { server.stop(0); }
    }

    @Test void adapterTranslatesRequestAndResponse() throws Exception {
        try (var vendor = new LocalVendor(false)) {
            var adapter = new HttpBackendAdapter("http", vendor.endpoint(), HttpClient.newHttpClient());
            assertEquals(new InferenceResponse("remote: hello", "http"), adapter.complete(REQUEST));
            assertEquals("hello", vendor.lastPrompt());
        }
    }

    @Test void adapterNormalizesHttpFailure() throws Exception {
        try (var vendor = new LocalVendor(true)) {
            var adapter = new HttpBackendAdapter("http", vendor.endpoint(), HttpClient.newHttpClient());
            assertThrows(BackendUnavailableException.class, () -> adapter.complete(REQUEST));
        }
    }

    @Test void bridgeModesVaryWithTheSameBackend() {
        var backend = new SimulatedBackend("local", "local:");
        InferenceService plain = new PlainMode(backend);
        InferenceService instructed = new InstructionMode(backend, "Be brief:");
        assertEquals("local: hello", plain.complete(REQUEST).text());
        assertEquals("local: Be brief:\nhello", instructed.complete(REQUEST).text());
    }

    @Test void bridgeModeWorksWithDifferentBackendImplementations() throws Exception {
        try (var vendor = new LocalVendor(false)) {
            InferenceService local = new InstructionMode(new SimulatedBackend("local", "local:"), "Rule:");
            InferenceService remote = new InstructionMode(
                new HttpBackendAdapter("http", vendor.endpoint(), HttpClient.newHttpClient()), "Rule:");
            assertEquals("local: Rule:\nhello", local.complete(REQUEST).text());
            assertEquals("remote: Rule:\nhello", remote.complete(REQUEST).text());
        }
    }

    @Test void facadeBuildsRetryAndAuditInTheRightOrder() {
        var backend = new SimulatedBackend("local", "local:", 1);
        List<AuditEvent> events = new ArrayList<>();
        var facade = new InferenceGatewayFacade(new PlainMode(backend),
            Set.of("alice")::contains, events::add, 2);
        assertEquals("local: hello", facade.ask("hello", "alice").text());
        assertEquals(2, backend.calls());
        assertEquals(List.of("success"), events.stream().map(AuditEvent::outcome).toList());
    }

    @Test void facadeBlocksBeforeCallingBackend() {
        var backend = new SimulatedBackend("local", "local:");
        List<AuditEvent> events = new ArrayList<>();
        var facade = new InferenceGatewayFacade(new PlainMode(backend),
            Set.of("alice")::contains, events::add, 1);
        assertThrows(AccessDeniedException.class, () -> facade.ask("hello", "guest"));
        assertEquals(0, backend.calls());
        assertTrue(events.isEmpty());
    }

    @Test void adapterBridgeFacadeAndDecoratorRunTogether() throws Exception {
        try (var vendor = new LocalVendor(true)) {
            var adapter = new HttpBackendAdapter("http", vendor.endpoint(), HttpClient.newHttpClient());
            var mode = new InstructionMode(adapter, "Be brief:");
            List<AuditEvent> events = new ArrayList<>();
            var facade = new InferenceGatewayFacade(mode, Set.of("alice")::contains, events::add, 2);
            assertEquals("remote: Be brief:\nhello", facade.ask("hello", "alice").text());
            assertEquals(2, vendor.calls());
            assertEquals("Be brief:\nhello", vendor.lastPrompt());
            assertEquals(List.of("success"), events.stream().map(AuditEvent::outcome).toList());
        }
    }
}
