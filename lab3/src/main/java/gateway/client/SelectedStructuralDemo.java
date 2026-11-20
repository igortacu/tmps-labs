package gateway.client;

import com.sun.net.httpserver.HttpServer;
import gateway.patterns.adapter.HttpBackendAdapter;
import gateway.patterns.bridge.InstructionMode;
import gateway.patterns.decorator.AuditEvent;
import gateway.patterns.facade.InferenceGatewayFacade;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Facade -> Decorators -> Bridge -> Adapter -> JDK HttpClient. */
public final class SelectedStructuralDemo {
    private SelectedStructuralDemo() {}

    public static void main(String[] args) throws Exception {
        var calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/complete", exchange -> {
            String prompt = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            int status = calls.incrementAndGet() == 1 ? 503 : 200;
            byte[] body = (status == 200 ? "Vendor replied: " + prompt : "temporarily unavailable")
                .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();

        try {
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/complete");
            var adapter = new HttpBackendAdapter("http-vendor", endpoint, HttpClient.newHttpClient());
            var mode = new InstructionMode(adapter, "Answer briefly:");
            List<AuditEvent> events = new ArrayList<>();
            var gateway = new InferenceGatewayFacade(mode, Set.of("student")::contains, events::add, 2);

            System.out.println("Path: Facade -> AuditDecorator -> RetryDecorator -> Bridge -> Adapter -> HttpClient");
            System.out.println("Result: " + gateway.ask("Explain Bridge", "student"));
            System.out.println("HTTP attempts: " + calls.get());
            System.out.println("Audit: " + events);
        } finally {
            server.stop(0);
        }
    }
}
