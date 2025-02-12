package uk.ac.mmu.advprog.hackathon;

import static spark.Spark.get;
import static spark.Spark.port;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles the setting up and starting of the web service.
 * @author You
 */
public class VolcanoWebService {

    /**
     * Main program entry point, starts the web service.
     * @param args not used
     */
    public static void main(String[] args) {
        port(8088);

        // Test endpoint
        get("/test", (req, res) -> {
            try (DB db = new DB()) {
                return "Number of volcanoes: " + db.getNumberOfVolcanoes() +
                        "<br>Number of eruptions: " + db.getNumberOfEruptions();
            }
        });

        // Get number of volcanoes by country
        get("/country", (req, res) -> {
            res.type("text/plain");
            String country = req.queryParams("search");

            if (country == null || country.isEmpty() || containsNumber(country)) {
                return "Invalid Country";
            }

            country = URLDecoder.decode(country, StandardCharsets.UTF_8);
            country = convertCases(country);

            try (DB db = new DB()) {
                int volcanoCount = db.getNumberOfVolcanoesByCountry(country);
                if (volcanoCount == 0 || !db.doesCountryExist(country)) {
                    return "0";
                }
                return String.valueOf(volcanoCount);
            } catch (Exception e) {
                return "Invalid Country";
            }
        });

        // Get eruption details by year range (JSON)
        get("/year", (req, res) -> {
            res.type("application/json");

            String fromYear = req.queryParams("from");
            String toYear = req.queryParams("to");

            if (fromYear == null || toYear == null || !fromYear.matches("\\d{4}") || !toYear.matches("\\d{4}") || Integer.parseInt(fromYear) > Integer.parseInt(toYear)) {
                return "Invalid Date Range";
            }

            try (DB db = new DB()) {
                return db.getDetailsOfEruptionsByYear(fromYear, toYear);
            }
        });

        // Get volcanoes near a location (XML)
        get("/location", (req, res) -> {
            res.type("application/xml");

            String latitudeParam = req.queryParams("latitude");
            String longitudeParam = req.queryParams("longitude");
            String eruptedSinceParam = req.queryParams("erupted_since");

            if (latitudeParam == null || longitudeParam == null || eruptedSinceParam == null) {
                return "Invalid Latitude, Longitude or Year";
            }

            try {
                double latitude = Double.parseDouble(latitudeParam);
                double longitude = Double.parseDouble(longitudeParam);
                int eruptedSince = Integer.parseInt(eruptedSinceParam);

                try (DB db = new DB()) {
                    return db.getVolcanoesByLocation(latitude, longitude, eruptedSince);
                }
            } catch (NumberFormatException e) {
                return "Invalid Latitude, Longitude or Year";
            }
        });

        System.out.println("Web Service Started. Don't forget to kill it when done testing!");
    }

    /**
     * Converts a string to title case (capitalising the first letter of each word).
     * @param input the string to convert
     * @return converted string
     */
    private static String convertCases(String input) {
        String[] words = input.toLowerCase().split(" ");
        StringBuilder capitalised = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                capitalised.append(Character.toUpperCase(word.charAt(0)));
                capitalised.append(word.substring(1)).append(" ");
            }
        }
        return capitalised.toString().trim();
    }

    /**
     * Checks if a string contains any digits.
     * @param input the string to check
     * @return true if a number is found, false otherwise
     */
    private static boolean containsNumber(String input) {
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }
}
