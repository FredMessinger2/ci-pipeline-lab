package ci.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Minimal HTTP service using the JDK's built-in server. Same contract as
 * py-service: /health/live is 200 once the process answers; /health/ready
 * is 503 until READY_DELAY_SECONDS after start (simulated warmup), then 200.
 */
public class App {

    private static final long START_NANOS = System.nanoTime();
    private static final double READY_DELAY_SECONDS =
            Double.parseDouble(System.getenv().getOrDefault("READY_DELAY_SECONDS", "0"));

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/health/live",
                ex -> respond(ex, 200, "{\"status\": \"alive\"}"));

        server.createContext("/health/ready", ex -> {
            double upSeconds = (System.nanoTime() - START_NANOS) / 1e9;
            if (upSeconds >= READY_DELAY_SECONDS) {
                respond(ex, 200, "{\"status\": \"ready\"}");
            } else {
                respond(ex, 503, "{\"status\": \"warming up\"}");
            }
        });

        server.start();
        System.out.println("java-service listening on port " + port);
    }

    private static void respond(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
