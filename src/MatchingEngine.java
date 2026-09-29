import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.PriorityQueue;
import java.util.Scanner;

public class MatchingEngine {

    // ==============================
    // AIVEN DATABASE CONFIGURATION
    // ==============================

    static String host =
            "mysql-d45df12-prasanna07abburi-3c62.a.aivencloud.com";

    static String port = "13973";
    static String database = "defaultdb";
    static String username = "avnadmin";

    // PUT YOUR CURRENT AIVEN PASSWORD HERE
    static String password = "DB_Password";


    // ==============================
    // MATCH CLASS
    // ==============================

    public static class Match implements Comparable<Match> {

        String projectName;
        String projectType;
        String location;

        int credits;
        double price;

        int typeScore;
        int locationScore;
        int priceScore;
        int availabilityScore;

        int score;


        public Match(
                String projectName,
                String projectType,
                String location,
                int credits,
                double price,
                int typeScore,
                int locationScore,
                int priceScore,
                int availabilityScore) {

            this.projectName = projectName;
            this.projectType = projectType;
            this.location = location;

            this.credits = credits;
            this.price = price;

            this.typeScore = typeScore;
            this.locationScore = locationScore;
            this.priceScore = priceScore;
            this.availabilityScore = availabilityScore;

            this.score =
                    typeScore
                    + locationScore
                    + priceScore
                    + availabilityScore;
        }


        // Higher score comes first
        @Override
        public int compareTo(Match other) {
            return Integer.compare(other.score, this.score);
        }


        public String getProjectName() {
            return projectName;
        }

        public String getProjectType() {
            return projectType;
        }

        public String getLocation() {
            return location;
        }

        public int getCredits() {
            return credits;
        }

        public double getPrice() {
            return price;
        }

        public int getTypeScore() {
            return typeScore;
        }

        public int getLocationScore() {
            return locationScore;
        }

        public int getPriceScore() {
            return priceScore;
        }

        public int getAvailabilityScore() {
            return availabilityScore;
        }

        public int getScore() {
            return score;
        }
    }


    // ==============================
    // FIND MATCHES
    // ==============================

    public static PriorityQueue<Match> findMatches(
            int requiredCredits,
            double maxPrice,
            String preferredType,
            String preferredLocation) {

        PriorityQueue<Match> matches =
                new PriorityQueue<>();


        String url =
                "jdbc:mysql://"
                + host
                + ":"
                + port
                + "/"
                + database
                + "?sslMode=REQUIRED";


        try {

            // Load MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");


            // Connect to Aiven
            Connection con =
                    DriverManager.getConnection(
                            url,
                            username,
                            password);


            /*
             * HARD FILTERS
             *
             * 1. Available credits must be enough
             * 2. Price must be within budget
             * 3. Project type must match
             * 4. Location must match
             */

            String query =
                    "SELECT * FROM carbon_projects "
                    + "WHERE available_credits >= ? "
                    + "AND price_per_credit <= ? "
                    + "AND project_type = ? "
                    + "AND location = ?";


            PreparedStatement ps =
                    con.prepareStatement(query);


            // Required credits
            ps.setInt(1, requiredCredits);

            // Maximum price
            ps.setDouble(2, maxPrice);

            // Project type
            ps.setString(3, preferredType);

            // Location
            ps.setString(4, preferredLocation);


            ResultSet rs =
                    ps.executeQuery();


            // ==============================
            // PROCESS MATCHING PROJECTS
            // ==============================

            while (rs.next()) {

                String projectName =
                        rs.getString("project_name");

                String projectType =
                        rs.getString("project_type");

                String location =
                        rs.getString("location");

                int credits =
                        rs.getInt("available_credits");

                double price =
                        rs.getDouble("price_per_credit");


                // ==============================
                // MATCH SCORE CALCULATION
                // ==============================

                int typeScore = 0;
                int locationScore = 0;
                int priceScore = 0;
                int availabilityScore = 0;


                // Type matches because it passed
                // the hard filter
                if (projectType.equalsIgnoreCase(
                        preferredType)) {

                    typeScore = 30;
                }


                // Location matches because it passed
                // the hard filter
                if (location.equalsIgnoreCase(
                        preferredLocation)) {

                    locationScore = 25;
                }


                // Price score
                if (price <= maxPrice * 0.80) {

                    priceScore = 25;

                } else {

                    priceScore = 15;
                }


                // Availability score
                if (credits >= requiredCredits * 2) {

                    availabilityScore = 20;

                } else {

                    availabilityScore = 10;
                }


                // Create match object
                Match match =
                        new Match(
                                projectName,
                                projectType,
                                location,
                                credits,
                                price,
                                typeScore,
                                locationScore,
                                priceScore,
                                availabilityScore
                        );


                // Add to PriorityQueue
                matches.add(match);
            }


            // Close resources
            rs.close();
            ps.close();
            con.close();


        } catch (ClassNotFoundException e) {

            System.out.println(
                    "MySQL JDBC Driver not found!");

            e.printStackTrace();


        } catch (Exception e) {

            System.out.println(
                    "Database connection error!");

            e.printStackTrace();
        }


        return matches;
    }


    // ==============================
    // SORT MATCHES
    // ==============================

    public static PriorityQueue<Match> sortMatches(
            PriorityQueue<Match> originalMatches,
            String sortBy) {


        PriorityQueue<Match> sortedMatches;


        if (sortBy == null) {
            sortBy = "score";
        }


        // Sort by lowest price
        if (sortBy.equalsIgnoreCase("price")) {

            sortedMatches =
                    new PriorityQueue<>(
                            (a, b) ->
                                    Double.compare(
                                            a.getPrice(),
                                            b.getPrice()
                                    )
                    );


        // Sort by highest credits
        } else if (
                sortBy.equalsIgnoreCase("credits")) {

            sortedMatches =
                    new PriorityQueue<>(
                            (a, b) ->
                                    Integer.compare(
                                            b.getCredits(),
                                            a.getCredits()
                                    )
                    );


        // Default: highest match score
        } else {

            sortedMatches =
                    new PriorityQueue<>(
                            (a, b) ->
                                    Integer.compare(
                                            b.getScore(),
                                            a.getScore()
                                    )
                    );
        }


        // Copy all matches
        sortedMatches.addAll(originalMatches);


        return sortedMatches;
    }


    // ==============================
    // MAIN METHOD
    // ==============================

    public static void main(String[] args) {

        Scanner sc =
                new Scanner(System.in);


        System.out.println(
                "======================================");

        System.out.println(
                "     CARBON OFFSET MATCHING ENGINE");

        System.out.println(
                "======================================");


        System.out.print(
                "Enter required carbon credits: ");

        int requiredCredits =
                sc.nextInt();


        System.out.print(
                "Enter maximum price per credit: ");

        double maxPrice =
                sc.nextDouble();


        sc.nextLine();


        System.out.print(
                "Enter preferred project type: ");

        String preferredType =
                sc.nextLine();


        System.out.print(
                "Enter preferred location: ");

        String preferredLocation =
                sc.nextLine();


        // Find matches
        PriorityQueue<Match> matches =
                findMatches(
                        requiredCredits,
                        maxPrice,
                        preferredType,
                        preferredLocation
                );


        System.out.println();

        System.out.println(
                "======================================");

        System.out.println(
                "             TOP MATCHES");

        System.out.println(
                "======================================");


        // No results
        if (matches.isEmpty()) {

            System.out.println(
                    "No suitable carbon projects found.");

        } else {

            int rank = 1;


            while (
                    !matches.isEmpty()
                    && rank <= 5) {


                Match m =
                        matches.poll();


                System.out.println();

                System.out.println(
                        "Rank " + rank);

                System.out.println(
                        "------------------------------");


                System.out.println(
                        "Project      : "
                        + m.projectName);


                System.out.println(
                        "Type         : "
                        + m.projectType);


                System.out.println(
                        "Location     : "
                        + m.location);


                System.out.println(
                        "Credits      : "
                        + m.credits);


                System.out.printf(
                        "Price/Credit : Rs. %.2f%n",
                        m.price);


                System.out.println();


                System.out.println(
                        "Type Score         : "
                        + m.typeScore
                        + "/30");


                System.out.println(
                        "Location Score     : "
                        + m.locationScore
                        + "/25");


                System.out.println(
                        "Price Score        : "
                        + m.priceScore
                        + "/25");


                System.out.println(
                        "Availability Score : "
                        + m.availabilityScore
                        + "/20");


                System.out.println(
                        "Match Score        : "
                        + m.score
                        + "/100");


                rank++;
            }
        }


        sc.close();
    }
}
