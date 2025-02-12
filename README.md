# Volcano Web Service

## Introduction

The Volcano Web Service provides access to a database of volcanoes and their recorded eruptions worldwide. Using data from the National Oceanic and Atmospheric Administration (NOAA), this service allows users to query information about volcano locations, eruptions, and other related details.

This service is built using:

- **Spark Java** microservice framework
- **SQLite JDBC Driver** for database access
- **Reference JSON parser** for JSON formatting
- **Java's built-in DOM XML parser** for XML handling

---

## Getting Started

### Importing the Starter Project

1. Download the Eclipse project ZIP file from Moodle.
2. Open **Eclipse** and navigate to `File → Import`.
3. Select **"Existing Projects Into Workspace"** under the "General" category.
4. Choose **"Select archive file"** and browse to the downloaded ZIP file.
5. Ensure that the "VolcanoWebService" project is selected, then click **"Finish"**.

### Project Structure

- `src/`: Contains the source code, including the `VolcanoWebService` class.
- `lib/`: Includes required libraries for JSON parsing, SQLite JDBC, and Spark Java.
- `data/`: Stores the SQLite database containing volcano and eruption data.

### Running the Service

To start the web service, run the `main()` method in the `VolcanoWebService` class. Once running, the server will listen on [**http://localhost:8088/**](http://localhost:8088/).

To verify that the setup is correct, visit [**http://localhost:8088/test**](http://localhost:8088/test) in a web browser. If successful, it should return the number of volcanoes and eruptions in the database.

---

## Database Structure

The SQLite database contains two tables:

### **Volcanoes Table**

| Column Name | Type    | Example Value |
| ----------- | ------- | ------------- |
| ID          | INTEGER | 809           |
| Name        | TEXT    | Etna          |
| Country     | TEXT    | Italy         |
| Location    | TEXT    | Italy         |
| Latitude    | REAL    | 37.748        |
| Longitude   | REAL    | 14.999        |
| Elevation   | INTEGER | 3357          |
| Type        | TEXT    | Stratovolcano |

### **Eruptions Table**

| Column Name       | Type    | Example Value |
| ----------------- | ------- | ------------- |
| Date              | TEXT    | 1987-04-17    |
| Volcano\_ID       | INTEGER | 809           |
| Deaths            | INTEGER | 2             |
| Missing           | INTEGER | NULL          |
| Injuries          | INTEGER | 7             |
| Damage            | REAL    | NULL          |
| Houses\_Destroyed | INTEGER | NULL          |

---

## API Endpoints

### **1. Get Number of Volcanoes in a Country**

- **Endpoint:** `/country`
- **Query Parameter:** `search` (Country Name)
- **Example Usage:**
  - `http://localhost:8088/country?search=Italy`
  - `http://localhost:8088/country?search=Peru`
- **Response:**
  - Returns a plaintext number indicating the number of volcanoes.
  - Returns `0` if no volcanoes are found.
  - Returns `Invalid Country` if the query is missing or malformed.

### **2. Get Eruptions in a Year Range**

- **Endpoint:** `/year`
- **Query Parameters:** `from` (Start Year), `to` (End Year)
- **Example Usage:**
  - `http://localhost:8088/year?from=2020&to=2024`
  - `http://localhost:8088/year?from=1990&to=1999`
- **Response:** JSON array containing eruption details, including:
  ```json
  [
    {
      "date": "2001-05-14",
      "name": "Etna",
      "location": {
        "latitude": 37.748,
        "longitude": 14.999,
        "elevation": 3357,
        "country": "Italy"
      },
      "deaths": 1,
      "missing": 0,
      "injuries": 0
    }
  ]
  ```
  - Returns an empty array `[]` if no results are found.
  - Returns `Invalid Date Range` if query parameters are incorrect or missing.

### **3. Get Closest Volcanoes by Location & Last Eruption**

- **Endpoint:** `/location`
- **Query Parameters:** `latitude`, `longitude`, `erupted_since`
- **Example Usage:**
  - `http://localhost:8088/location?latitude=53.472&longitude=-2.244&erupted_since=1`
  - `http://localhost:8088/location?latitude=29.975&longitude=31.1375&erupted_since=1990`
- **Response:** XML formatted list of up to 10 closest volcanoes:
  ```xml
  <Volcanoes>
    <Volcano id="809">
      <Name>Etna</Name>
      <LastErupted>2001-05-14</LastErupted>
      <Type>Stratovolcano</Type>
      <Location>
        <Latitude>37.748</Latitude>
        <Longitude>14.999</Longitude>
        <Elevation>3357</Elevation>
        <Country>Italy</Country>
      </Location>
    </Volcano>
  </Volcanoes>
  ```
  - Returns `Invalid Latitude`, `Invalid Longitude`, or `Invalid Year` if input is incorrect.

---

## Development Notes

- **SQL Queries**: Use parameterised queries to prevent SQL injection.
- **Error Handling**: Ensure appropriate messages are returned for invalid input.
- **Data Handling**:
  - NULL values in the database should be replaced with `0` for numbers and `""` for text fields.
  - Use `CAST(Date AS INTEGER)` when filtering by year.
- **Headers**:
  - Set `Content-Type: application/json` for JSON responses.
  - Set `Content-Type: application/xml` for XML responses.

---

## Additional Resources

- [**Spark Java**](https://sparkjava.com/)
- [**SQLite JDBC Driver**](https://github.com/xerial/sqlite-jdbc)
- [**NOAA Volcano Data**](https://www.ngdc.noaa.gov/hazard/volcano.shtml)

---

