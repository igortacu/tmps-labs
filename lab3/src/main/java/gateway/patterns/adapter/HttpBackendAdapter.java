package gateway.patterns.adapter;

import gateway.domain.BackendUnavailableException;
import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import gateway.patterns.composite.BackendNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Adapter from the JDK HTTP client API to the gateway's BackendNode contract. */
public final class HttpBackendAdapter implements BackendNode {
    private final String name;
    private final URI endpoint;
    private final HttpClient client;

    public HttpBackendAdapter(String name, URI endpoint, HttpClient client) {
        this.name = Objects.requireNonNull(name);
        this.endpoint = Objects.requireNonNull(endpoint);
        this.client = Objects.requireNonNull(client);
    }

    @Override public String name() { return name; }

    @Override public InferenceResponse complete(InferenceRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
            .header("Content-Type", "text/plain; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(request.prompt(), StandardCharsets.UTF_8))
            .build();
        try {
            HttpResponse<String> reply = client.send(httpRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (reply.statusCode() < 200 || reply.statusCode() >= 300) {
                throw new BackendUnavailableException(name + " returned HTTP " + reply.statusCode());
            }
            return new InferenceResponse(reply.body(), name);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new BackendUnavailableException(name + " request interrupted");
        } catch (IOException error) {
            throw new BackendUnavailableException(name + " transport failed: " + error.getMessage());
        }
    }
}
