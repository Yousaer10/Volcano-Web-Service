package uk.ac.mmu.advprog.hackathon;

import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.StringWriter;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * The DB class provides methods to interact with the database that stores information about volcanoes and eruptions.
 * It provides functionalities to query the database for counts, details of eruptions, and volcanoes filtered by country and location.
 * Additionally, it supports error handling for SQL operations.
 * Implements AutoCloseable to allow automatic closing of database connections.
 */
public class DB implements AutoCloseable {

    /**
     * The connection string used to connect to the SQLite database.
     */
    private static final String JDBC_CONNECTION_STRING = "jdbc:sqlite:./data/volcanoes.db";
    
    /**
     * The connection object used to interact with the database.
     */
    private Connection connection = null;

    /**
     * Constructs a new DB instance and establishes a connection to the database.
     * If the connection fails, an error message is logged.
     */
    public DB() {
        try {
            connection = DriverManager.getConnection(JDBC_CONNECTION_STRING);
        } catch (SQLException sqle) {
            error(sqle);
        }
    }

    /**
     * Retrieves the total number of volcanoes from the database.
     * 
     * @return The number of volcanoes.
     */
    public int getNumberOfVolcanoes() {
        return getCount("SELECT COUNT(*) AS count FROM Volcanoes");
    }

    /**
     * Retrieves the total number of eruptions from the database.
     * 
     * @return The number of eruptions.
     */
    public int getNumberOfEruptions() {
        return getCount("SELECT COUNT(*) AS count FROM Eruptions");
    }

    /**
     * Helper method to execute a SQL query that returns a count of results.
     * 
     * @param query The SQL query to execute.
     * @return The count of results.
     */
    private int getCount(String query) {
        int result = -1;
        try (Statement s = connection.createStatement();
             ResultSet results = s.executeQuery(query)) {
            if (results.next()) {
                result = results.getInt("count");
            }
        } catch (SQLException sqle) {
            error(sqle);
        }
        return result;
    }

    /**
     * Retrieves the number of volcanoes in a given country.
     * 
     * @param country The name of the country.
     * @return The number of volcanoes in the specified country.
     */
    public int getNumberOfVolcanoesByCountry(String country) {
        int result = 0;
        String sql = "SELECT COUNT(*) AS Number FROM Volcanoes WHERE LOWER(Country) = LOWER(?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, country);
            ResultSet results = statement.executeQuery();
            if (results.next()) {
                result = results.getInt("Number");
            }
        } catch (SQLException sqle) {
            error(sqle);
        }
        return result;
    }

    /**
     * Checks if a given country exists in the database (i.e., has volcanoes listed).
     * 
     * @param country The name of the country.
     * @return True if the country exists in the database, false otherwise.
     */
    public boolean doesCountryExist(String country) {
        String sql = "SELECT COUNT(*) AS count FROM Volcanoes WHERE LOWER(Country) = LOWER(?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, country);
            ResultSet results = statement.executeQuery();
            if (results.next()) {
                return results.getInt("count") > 0;
            }
        } catch (SQLException sqle) {
            error(sqle);
        }
        return false;
    }

    /**
     * Retrieves the details of eruptions that occurred between two specified years.
     * 
     * @param fromYear The start year.
     * @param toYear The end year.
     * @return A JSON array containing details of the eruptions.
     */
    public String getDetailsOfEruptionsByYear(String fromYear, String toYear) {
        String sql = "SELECT * FROM Eruptions INNER JOIN Volcanoes ON Eruptions.Volcano_ID = Volcanoes.ID "
                + "WHERE CAST(Eruptions.Date AS INTEGER) >= ? AND CAST(Eruptions.Date AS INTEGER) <= ? "
                + "ORDER BY Eruptions.Date ASC";

        JSONArray jsonArray = new JSONArray();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Integer.parseInt(fromYear));
            statement.setInt(2, Integer.parseInt(toYear));
            ResultSet results = statement.executeQuery();

            while (results.next()) {
                JSONObject eruption = new JSONObject();
                eruption.put("date", results.getString("Date"));
                eruption.put("name", results.getString("Name"));

                JSONObject location = new JSONObject();
                location.put("latitude", results.getDouble("Latitude"));
                location.put("longitude", results.getDouble("Longitude"));
                location.put("elevation", results.getInt("Elevation"));
                location.put("country", results.getString("Country"));

                eruption.put("location", location);
                eruption.put("deaths", results.getObject("Deaths") != null ? results.getInt("Deaths") : 0);
                eruption.put("missing", results.getObject("Missing") != null ? results.getInt("Missing") : 0);
                eruption.put("injuries", results.getObject("Injuries") != null ? results.getInt("Injuries") : 0);

                jsonArray.put(eruption);
            }
        } catch (SQLException sqle) {
            error(sqle);
        }

        return jsonArray.toString();
    }

    /**
     * Retrieves a list of volcanoes by location (latitude and longitude) and filtered by eruptions occurring since a given year.
     * 
     * @param latitude The latitude of the reference location.
     * @param longitude The longitude of the reference location.
     * @param eruptedSince The year since which eruptions are considered.
     * @return An XML representation of the volcanoes, sorted by proximity to the given latitude and longitude.
     */
    public String getVolcanoesByLocation(double latitude, double longitude, int eruptedSince) {
        String sql = "SELECT MAX(Date) AS Last_Erupted, Volcano_ID, Name, Country, Latitude, Longitude, Elevation, Type "
                + "FROM Eruptions INNER JOIN Volcanoes ON Eruptions.Volcano_ID = Volcanoes.ID "
                + "WHERE CAST(Date AS INTEGER) >= ? "
                + "GROUP BY Volcano_ID "
                + "ORDER BY ((? - Latitude) * (? - Latitude)) + (0.595 * ((? - Longitude) * (? - Longitude))) ASC "
                + "LIMIT 10;";

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element rootElement = doc.createElement("Volcanoes");
            doc.appendChild(rootElement);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, eruptedSince);
                statement.setDouble(2, latitude);
                statement.setDouble(3, latitude);
                statement.setDouble(4, longitude);
                statement.setDouble(5, longitude);
                ResultSet results = statement.executeQuery();

                while (results.next()) {
                    Element volcano = doc.createElement("Volcano");
                    volcano.setAttribute("id", String.valueOf(results.getInt("Volcano_ID")));

                    Element name = doc.createElement("Name");
                    name.appendChild(doc.createTextNode(results.getString("Name")));
                    volcano.appendChild(name);

                    Element lastErupted = doc.createElement("LastErupted");
                    lastErupted.appendChild(doc.createTextNode(results.getString("Last_Erupted")));
                    volcano.appendChild(lastErupted);

                    Element location = doc.createElement("Location");

                    Element lat = doc.createElement("Latitude");
                    lat.appendChild(doc.createTextNode(String.valueOf(results.getDouble("Latitude"))));
                    location.appendChild(lat);

                    Element lon = doc.createElement("Longitude");
                    lon.appendChild(doc.createTextNode(String.valueOf(results.getDouble("Longitude"))));
                    location.appendChild(lon);

                    Element elevation = doc.createElement("Elevation");
                    elevation.appendChild(doc.createTextNode(String.valueOf(results.getInt("Elevation"))));
                    location.appendChild(elevation);

                    Element country = doc.createElement("Country");
                    country.appendChild(doc.createTextNode(results.getString("Country")));
                    location.appendChild(country);

                    volcano.appendChild(location);
                    rootElement.appendChild(volcano);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return "<error>Failed to generate XML</error>";
        }
    }

    /**
     * Closes the database connection.
     * 
     * @throws SQLException if the connection cannot be closed properly.
     */
    @Override
    public void close() throws SQLException {
        if (connection != null) connection.close();
    }

    /**
     * Logs an error message related to a SQL exception.
     * 
     * @param sqle The SQL exception that occurred.
     */
    private void error(SQLException sqle) {
        System.err.println("Database Error: " + sqle.getMessage());
        sqle.printStackTrace();
    }
}
