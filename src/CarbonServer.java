import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.PriorityQueue;

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
        server.createContext("/api/match", CarbonServer::handleMatch);

        server.setExecutor(null);

        System.out.println("Server running on port: " + port);

        server.start();
    }

    // =========================
    // SERVE WEBSITE FILES
    // =========================

    private static void handleRequest(HttpExchange exchange)
            throws IOException {

        String path = exchange.getRequestURI().getPath();

        if (path.equals("/")) {
            path = "/index.html";
        }

        File file = new File("web" + path);

        if (!file.exists() || file.isDirectory()) {

            String response = "404 - Page Not Found";

            byte[] bytes =
                    response.getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(404, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }

            return;
        }

        Path filePath = file.toPath();
        byte[] response = Files.readAllBytes(filePath);

        String contentType = "text/html";

        if (path.endsWith(".css")) {
            contentType = "text/css";
        }
        else if (path.endsWith(".js")) {
            contentType = "application/javascript";
        }
        else if (path.endsWith(".png")) {
            contentType = "image/png";
        }
        else if (path.endsWith(".jpg")
                || path.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        }

        exchange.getResponseHeaders().set(
                "Content-Type",
                contentType
        );

        exchange.sendResponseHeaders(
                200,
                response.length
        );

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    // =========================
    // MATCHING API
    // =========================

    private static void handleMatch(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            String response = "Method Not Allowed";

            byte[] bytes =
                    response.getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(
                    405,
                    bytes.length
            );

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }

            return;
        }

        try {

            // Read request body
            String body = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            System.out.println(
                    "Received request: " + body
            );

            // Read form values
            int credits = getInt(
                    body,
                    "credits"
            );

            double price = getDouble(
                    body,
                    "price"
            );

            String type = getString(
                    body,
                    "type"
            );

            String location = getString(
                    body,
                    "location"
            );

            String sort = getString(
                    body,
                    "sort"
            );

            System.out.println(
                    "Credits: " + credits
            );

            System.out.println(
                    "Price: " + price
            );

            System.out.println(
                    "Type: " + type
            );

            System.out.println(
                    "Location: " + location
            );

            System.out.println(
                    "Sort: " + sort
            );

            // =========================
            // FIND MATCHES
            // =========================

            PriorityQueue<MatchingEngine.Match> matches =
                    MatchingEngine.findMatches(
                            credits,
                            price,
                            type,
                            location
                    );

            // Sort results
            matches =
                    MatchingEngine.sortMatches(
                            matches,
                            sort
                    );

            // =========================
            // CREATE JSON RESPONSE
            // =========================

            StringBuilder json =
                    new StringBuilder();

            json.append("{\"matches\":[");

            boolean first = true;

            int count = 0;

            while (!matches.isEmpty()
                    && count < 10) {

                MatchingEngine.Match match =
                        matches.poll();

                if (!first) {
                    json.append(",");
                }

                first = false;

                json.append("{");

                json.append("\"projectName\":\"")
                        .append(
                                escapeJson(
                                        match.getProjectName()
                                )
                        )
                        .append("\",");

                json.append("\"projectType\":\"")
                        .append(
                                escapeJson(
                                        match.getProjectType()
                                )
                        )
                        .append("\",");

                json.append("\"location\":\"")
                        .append(
                                escapeJson(
                                        match.getLocation()
                                )
                        )
                        .append("\",");

                json.append("\"credits\":")
                        .append(
                                match.getCredits()
                        )
                        .append(",");

                json.append("\"price\":")
                        .append(
                                match.getPrice()
                        )
                        .append(",");

                json.append("\"typeScore\":")
                        .append(
                                match.getTypeScore()
                        )
                        .append(",");

                json.append("\"locationScore\":")
                        .append(
                                match.getLocationScore()
                        )
                        .append(",");

                json.append("\"priceScore\":")
                        .append(
                                match.getPriceScore()
                        )
                        .append(",");

                json.append("\"availabilityScore\":")
                        .append(
                                match.getAvailabilityScore()
                        )
                        .append(",");

                json.append("\"score\":")
                        .append(
                                match.getScore()
                        );

                json.append("}");

                count++;
            }

            json.append("]}");

            // Send response
            sendJson(
                    exchange,
                    200,
                    json.toString()
            );

        }
        catch (Exception e) {

            e.printStackTrace();

            String error =
                    "{\"error\":\"Server error while finding matches\"}";

            sendJson(
                    exchange,
                    500,
                    error
            );
        }
    }

    // =========================
    // READ INTEGER
    // =========================

    private static int getInt(
            String data,
            String key) {

        String value =
                getString(data, key);

        return Integer.parseInt(value);
    }

    // =========================
    // READ DOUBLE
    // =========================

    private static double getDouble(
            String data,
            String key) {

        String value =
                getString(data, key);

        return Double.parseDouble(value);
    }

    // =========================
    // READ FORM DATA
    // =========================

    private static String getString(
            String data,
            String key) {

        String[] pairs =
                data.split("&");

        for (String pair : pairs) {

            String[] parts =
                    pair.split("=", 2);

            if (parts.length == 2
                    && parts[0].equals(key)) {

                return parts[1]
                        .replace("+", " ")
                        .replace("%20", " ");
            }
        }

        return "";
    }

    // =========================
    // SEND JSON
    // =========================

    private static void sendJson(
            HttpExchange exchange,
            int status,
            String json)
            throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json"
        );

        byte[] response =
                json.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.sendResponseHeaders(
                status,
                response.length
        );

        try (OutputStream os =
                     exchange.getResponseBody()) {

            os.write(response);
        }
    }

    // =========================
    // ESCAPE JSON
    // =========================

    private static String escapeJson(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}