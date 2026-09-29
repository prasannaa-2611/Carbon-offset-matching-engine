import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CarbonServer {

    // ==========================================
    // SERVER START
    // ==========================================

    public static void main(String[] args) throws Exception {

        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(8080),
                        0
                );

        // Homepage
        server.createContext("/", CarbonServer::handleHome);

        // CSS
        server.createContext("/style.css", CarbonServer::handleCss);

        // Matching API
        server.createContext("/api/match", CarbonServer::handleMatch);

        server.setExecutor(null);

        System.out.println("======================================");
        System.out.println("      CARBON MATCH SERVER");
        System.out.println("======================================");
        System.out.println("Server running at:");
        System.out.println("http://localhost:8080");
        System.out.println("======================================");

        server.start();
    }


    // ==========================================
    // HOME PAGE
    // ==========================================

    private static void handleHome(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed",
                    "text/plain"
            );

            return;
        }

        try {

            String filePath =
                    "web/index.html";

            String html =
                    new String(
                            Files.readAllBytes(
                                    Paths.get(filePath)
                            ),
                            StandardCharsets.UTF_8
                    );

            sendResponse(
                    exchange,
                    200,
                    html,
                    "text/html; charset=UTF-8"
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    createErrorPage(
                            "Could not load homepage."
                    ),
                    "text/html; charset=UTF-8"
            );
        }
    }


    // ==========================================
    // CSS
    // ==========================================

    private static void handleCss(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed",
                    "text/plain"
            );

            return;
        }

        try {

            String filePath =
                    "web/style.css";

            String css =
                    new String(
                            Files.readAllBytes(
                                    Paths.get(filePath)
                            ),
                            StandardCharsets.UTF_8
                    );

            sendResponse(
                    exchange,
                    200,
                    css,
                    "text/css; charset=UTF-8"
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    "Could not load CSS.",
                    "text/plain"
            );
        }
    }


    // ==========================================
    // MATCH API
    // ==========================================

    private static void handleMatch(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {

            sendResponse(
                    exchange,
                    405,
                    "Method Not Allowed",
                    "text/plain"
            );

            return;
        }


        try {

            // ==================================
            // READ REQUEST DATA
            // ==================================

            String requestBody =
                    new String(
                            exchange.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8
                    );


            Map<String, String> params =
                    parseFormData(requestBody);


            // ==================================
            // GET INPUT VALUES
            // ==================================

            int requiredCredits =
                    Integer.parseInt(
                            params.getOrDefault(
                                    "credits",
                                    "0"
                            )
                    );


            double maxPrice =
                    Double.parseDouble(
                            params.getOrDefault(
                                    "price",
                                    "0"
                            )
                    );


            String preferredType =
                    params.getOrDefault(
                            "type",
                            ""
                    );


            String preferredLocation =
                    params.getOrDefault(
                            "location",
                            ""
                    );


            String sortBy =
                    params.getOrDefault(
                            "sort",
                            "score"
                    );


            // ==================================
            // VALIDATION
            // ==================================

            if (requiredCredits <= 0) {

                sendResponse(
                        exchange,
                        400,
                        createErrorPage(
                                "Required credits must be greater than 0."
                        ),
                        "text/html; charset=UTF-8"
                );

                return;
            }


            if (maxPrice <= 0) {

                sendResponse(
                        exchange,
                        400,
                        createErrorPage(
                                "Maximum price must be greater than 0."
                        ),
                        "text/html; charset=UTF-8"
                );

                return;
            }


            if (preferredType.isEmpty()
                    || preferredLocation.isEmpty()) {

                sendResponse(
                        exchange,
                        400,
                        createErrorPage(
                                "Please select project type and location."
                        ),
                        "text/html; charset=UTF-8"
                );

                return;
            }


            // ==================================
            // FIND MATCHES
            // ==================================

            PriorityQueue<MatchingEngine.Match> matches =
                    MatchingEngine.findMatches(
                            requiredCredits,
                            maxPrice,
                            preferredType,
                            preferredLocation
                    );


            // ==================================
            // SORT MATCHES
            // ==================================

            PriorityQueue<MatchingEngine.Match> sortedMatches =
                    MatchingEngine.sortMatches(
                            matches,
                            sortBy
                    );


            // ==================================
            // BUILD RESULTS PAGE
            // ==================================

            String html =
                    buildResultsPage(
                            sortedMatches,
                            requiredCredits,
                            maxPrice,
                            preferredType,
                            preferredLocation,
                            sortBy
                    );


            sendResponse(
                    exchange,
                    200,
                    html,
                    "text/html; charset=UTF-8"
            );


        } catch (NumberFormatException e) {

            sendResponse(
                    exchange,
                    400,
                    createErrorPage(
                            "Please enter valid numeric values."
                    ),
                    "text/html; charset=UTF-8"
            );


        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(
                    exchange,
                    500,
                    createErrorPage(
                            "Something went wrong while finding matches."
                    ),
                    "text/html; charset=UTF-8"
            );
        }
    }


    // ==========================================
    // BUILD RESULTS PAGE
    // ==========================================

    private static String buildResultsPage(
            PriorityQueue<MatchingEngine.Match> matches,
            int requiredCredits,
            double maxPrice,
            String preferredType,
            String preferredLocation,
            String sortBy) {


        StringBuilder html =
                new StringBuilder();


        // ==================================
        // HTML START
        // ==================================

        html.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">

                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">

                    <title>Carbon Match Results</title>

                    <link rel="stylesheet" href="/style.css">
                </head>

                <body>
                """);


        // ==================================
        // NAVBAR
        // ==================================

        html.append("""
                <nav class="navbar">

                    <div class="logo">
                        Carbon<span>Match</span>
                    </div>

                    <div class="nav-links">

                        <a href="/">
                            Home
                        </a>

                        <a href="/#marketplace">
                            Marketplace
                        </a>

                        <a href="/#matching">
                            Find Match
                        </a>

                    </div>

                </nav>
                """);


        // ==================================
        // RESULTS SECTION
        // ==================================

        html.append("""
                <section class="results-page">

                    <div class="section-heading">

                        <p class="small-title">
                            MATCHING ENGINE
                        </p>

                        <h2>
                            Your Carbon Matches
                        </h2>

                        <p>
                            Projects matching your carbon offset requirements
                        </p>

                    </div>
                """);


        // ==================================
        // REQUIREMENTS CARD
        // ==================================

        html.append("""
                    <div class="requirements-card">

                        <h2>
                            Your Requirements
                        </h2>

                        <div class="requirement-grid">

                            <div>
                                <strong>
                                    Required Credits
                                </strong>
                """);

        html.append("<span>")
                .append(requiredCredits)
                .append(" Credits</span>");

        html.append("""
                            </div>

                            <div>
                                <strong>
                                    Maximum Price
                                </strong>
                """);

        html.append("<span>₹")
                .append(String.format(
                        Locale.US,
                        "%.2f",
                        maxPrice
                ))
                .append(" / Credit</span>");

        html.append("""
                            </div>

                            <div>
                                <strong>
                                    Project Type
                                </strong>
                """);

        html.append("<span>")
                .append(escapeHtml(preferredType))
                .append("</span>");

        html.append("""
                            </div>

                            <div>
                                <strong>
                                    Location
                                </strong>
                """);

        html.append("<span>")
                .append(escapeHtml(preferredLocation))
                .append("</span>");

        html.append("""
                            </div>

                        </div>

                    </div>
                """);


        // ==================================
        // SORT INFORMATION
        // ==================================

        String sortText =
                getSortText(sortBy);


        html.append("""
                    <div class="active-filters">

                        <h3>
                            Results Sorted By
                        </h3>

                        <p>
                """);

        html.append(escapeHtml(sortText));

        html.append("""
                        </p>

                    </div>
                """);


        // ==================================
        // RESULT COUNT
        // ==================================

        int resultCount =
                matches.size();


        html.append("""
                    <div class="results-header">

                        <h2>
                            Matching Projects
                        </h2>

                        <span class="result-count">
                """);

        html.append(resultCount)
                .append(
                        resultCount == 1
                                ? " project"
                                : " projects"
                );

        html.append("""
                        </span>

                    </div>

                    <div class="results-container">
                """);


        // ==================================
        // NO RESULTS
        // ==================================

        if (matches.isEmpty()) {

            html.append("""
                        <div class="no-results">

                            <h2>
                                No Suitable Projects Found
                            </h2>

                            <p>
                                No carbon project matches all your
                                selected requirements.
                            </p>

                            <a href="/" class="back-button">
                                Try Different Requirements
                            </a>

                        </div>
                    """);

        } else {


            // ==================================
            // MATCH RESULTS
            // ==================================

            int rank = 1;


            while (!matches.isEmpty()) {

                MatchingEngine.Match match =
                        matches.poll();


                html.append("""
                            <div class="match-card">

                                <div class="match-top">

                                    <div>

                                        <span class="rank-badge">
                                            Rank
                """);

                html.append(rank);

                html.append("""
                                        </span>

                                        <h2>
                """);

                html.append(
                        escapeHtml(
                                match.getProjectName()
                        )
                );

                html.append("""
                                        </h2>

                                    </div>

                                    <div class="score-badge">
                                        Score:
                """);

                html.append(match.getScore());

                html.append("""
                                        /100
                                    </div>

                                </div>
                """);


                // ==================================
                // PROJECT DETAILS
                // ==================================

                html.append("""
                                <div class="project-details">

                                    <div>

                                        <span>
                                            Project Type
                                        </span>

                                        <strong>
                """);

                html.append(
                        escapeHtml(
                                match.getProjectType()
                        )
                );

                html.append("""
                                        </strong>

                                    </div>


                                    <div>

                                        <span>
                                            Location
                                        </span>

                                        <strong>
                """);

                html.append(
                        escapeHtml(
                                match.getLocation()
                        )
                );

                html.append("""
                                        </strong>

                                    </div>


                                    <div>

                                        <span>
                                            Available Credits
                                        </span>

                                        <strong>
                """);

                html.append(
                        match.getCredits()
                );

                html.append("""
                                        </strong>

                                    </div>


                                    <div>

                                        <span>
                                            Price Per Credit
                                        </span>

                                        <strong>
                                            ₹
                """);

                html.append(
                        String.format(
                                Locale.US,
                                "%.2f",
                                match.getPrice()
                        )
                );

                html.append("""
                                        </strong>

                                    </div>

                                </div>
                """);


                // ==================================
                // SCORE BREAKDOWN
                // ==================================

                html.append("""
                                <div class="score-breakdown">

                                    <h3>
                                        Match Score Breakdown
                                    </h3>

                                    <div class="score-row">

                                        <span>
                                            Project Type Match
                                        </span>

                                        <strong>
                """);

                html.append(
                        match.getTypeScore()
                );

                html.append("""
                                            /30
                                        </strong>

                                    </div>


                                    <div class="score-row">

                                        <span>
                                            Location Match
                                        </span>

                                        <strong>
                """);

                html.append(
                        match.getLocationScore()
                );

                html.append("""
                                            /25
                                        </strong>

                                    </div>


                                    <div class="score-row">

                                        <span>
                                            Price Score
                                        </span>

                                        <strong>
                """);

                html.append(
                        match.getPriceScore()
                );

                html.append("""
                                            /25
                                        </strong>

                                    </div>


                                    <div class="score-row">

                                        <span>
                                            Availability Score
                                        </span>

                                        <strong>
                """);

                html.append(
                        match.getAvailabilityScore()
                );

                html.append("""
                                            /20
                                        </strong>

                                    </div>


                                    <div class="score-row">

                                        <span>
                                            Total Match Score
                                        </span>

                                        <strong>
                """);

                html.append(
                        match.getScore()
                );

                html.append("""
                                            /100
                                        </strong>

                                    </div>

                                </div>
                """);


                // ==================================
                // VIEW DETAILS
                // ==================================

                html.append("""
                                <div class="project-details"
                                     style="margin-top:20px;">

                                    <div>

                                        <span>
                                            Verification
                                        </span>

                                        <strong>
                                            Verified Carbon Project
                                        </strong>

                                    </div>

                                    <div>

                                        <span>
                                            Matching Status
                                        </span>

                                        <strong>
                                            Suitable Match
                                        </strong>

                                    </div>

                                </div>

                            </div>
                """);


                rank++;
            }
        }


        // ==================================
        // CLOSE RESULTS
        // ==================================

        html.append("""
                    </div>

                    <div style="
                        text-align:center;
                        margin-top:30px;
                    ">

                        <a href="/"
                           class="back-button">

                            ← Search Again

                        </a>

                    </div>

                </section>
                """);


        // ==================================
        // FOOTER
        // ==================================

        html.append("""
                <footer>

                    <div class="logo">
                        Carbon<span>Match</span>
                    </div>

                    <p>
                        Carbon Offset Marketplace
                        Matching Engine
                    </p>

                </footer>

                </body>
                </html>
                """);


        return html.toString();
    }


    // ==========================================
    // SORT TEXT
    // ==========================================

    private static String getSortText(String sortBy) {

        if (sortBy == null) {
            return "Match Score — Highest First";
        }


        if (sortBy.equalsIgnoreCase("price")) {

            return "Lowest Price First";

        } else if (
                sortBy.equalsIgnoreCase("credits")) {

            return "Highest Available Credits First";

        } else {

            return "Match Score — Highest First";
        }
    }


    // ==========================================
    // FORM DATA PARSER
    // ==========================================

    private static Map<String, String> parseFormData(
            String body) {

        Map<String, String> data =
                new HashMap<>();


        if (body == null || body.isEmpty()) {
            return data;
        }


        String[] pairs =
                body.split("&");


        for (String pair : pairs) {

            String[] keyValue =
                    pair.split("=", 2);


            if (keyValue.length == 2) {

                try {

                    String key =
                            URLDecoder.decode(
                                    keyValue[0],
                                    StandardCharsets.UTF_8
                            );


                    String value =
                            URLDecoder.decode(
                                    keyValue[1],
                                    StandardCharsets.UTF_8
                            );


                    data.put(key, value);

                } catch (Exception e) {

                    e.printStackTrace();
                }
            }
        }


        return data;
    }


    // ==========================================
    // HTML ESCAPE
    // ==========================================

    private static String escapeHtml(String text) {

        if (text == null) {
            return "";
        }


        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }


    // ==========================================
    // ERROR PAGE
    // ==========================================

    private static String createErrorPage(
            String message) {

        return """
                <!DOCTYPE html>

                <html>

                <head>

                    <meta charset="UTF-8">

                    <meta name="viewport"
                          content="width=device-width,
                                   initial-scale=1.0">

                    <title>Error</title>

                    <link rel="stylesheet"
                          href="/style.css">

                </head>

                <body>

                    <nav class="navbar">

                        <div class="logo">
                            Carbon<span>Match</span>
                        </div>

                    </nav>


                    <div class="error-page">

                        <h1>
                            Something Went Wrong
                        </h1>

                        <p style="margin-top:15px;">
                """
                + escapeHtml(message)
                + """
                        </p>

                        <a href="/"
                           class="back-button">

                            Back to Home

                        </a>

                    </div>

                </body>

                </html>
                """;
    }


    // ==========================================
    // SEND HTTP RESPONSE
    // ==========================================

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String response,
            String contentType)
            throws IOException {


        byte[] bytes =
                response.getBytes(
                        StandardCharsets.UTF_8
                );


        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        contentType
                );


        exchange.sendResponseHeaders(
                statusCode,
                bytes.length
        );


        try (OutputStream os =
                     exchange.getResponseBody()) {

            os.write(bytes);
        }
    }
}