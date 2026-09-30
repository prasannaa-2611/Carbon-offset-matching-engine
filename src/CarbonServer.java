import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class CarbonServer {

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT", "8080")
        );

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port),
                0
        );

        server.createContext("/", CarbonServer::handleRequest);

        server.setExecutor(null);

        System.out.println("Server running on port: " + port);

        server.start();
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();

        if (path.equals("/")) {
            path = "/index.html";
        }

        File file = new File("web" + path);

        if (!file.exists() || file.isDirectory()) {
            String response = "404 - Page Not Found";

            exchange.sendResponseHeaders(404, response.length());

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }

            return;
        }

        Path filePath = file.toPath();
        byte[] response = Files.readAllBytes(filePath);

        String contentType = "text/html";

        if (path.endsWith(".css")) {
            contentType = "text/css";
        } else if (path.endsWith(".js")) {
            contentType = "application/javascript";
        } else if (path.endsWith(".png")) {
            contentType = "image/png";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        }

        exchange.getResponseHeaders().set(
                "Content-Type",
                contentType
        );

        exchange.sendResponseHeaders(200, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}