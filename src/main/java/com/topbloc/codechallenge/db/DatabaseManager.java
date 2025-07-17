package com.topbloc.codechallenge.db;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DatabaseManager {

    private static final String jdbcPrefix = "jdbc:sqlite:";
    private static final String dbName = "challenge.db";
    private static String connectionString;
    private static Connection conn;

    static {
        File dbFile = new File(dbName);
        connectionString = jdbcPrefix + dbFile.getAbsolutePath();
    }

    public static void connect() {
        try {
            Connection connection = DriverManager.getConnection(connectionString);
            System.out.println("Connection to SQLite has been established.");
            conn = connection;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    // Schema function to reset the database if needed - do not change
    public static void resetDatabase() {
        try {
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        File dbFile = new File(dbName);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        connectionString = jdbcPrefix + dbFile.getAbsolutePath();
        connect();
        applySchema();
        seedDatabase();
    }

    // Schema function to reset the database if needed - do not change
    private static void applySchema() {
        String itemsSql = "CREATE TABLE IF NOT EXISTS items (\n"
                + "id integer PRIMARY KEY,\n"
                + "name text NOT NULL UNIQUE\n"
                + ");";
        String inventorySql = "CREATE TABLE IF NOT EXISTS inventory (\n"
                + "id integer PRIMARY KEY,\n"
                + "item integer NOT NULL UNIQUE references items(id) ON DELETE CASCADE,\n"
                + "stock integer NOT NULL,\n"
                + "capacity integer NOT NULL\n"
                + ");";
        String distributorSql = "CREATE TABLE IF NOT EXISTS distributors (\n"
                + "id integer PRIMARY KEY,\n"
                + "name text NOT NULL UNIQUE\n"
                + ");";
        String distributorPricesSql = "CREATE TABLE IF NOT EXISTS distributor_prices (\n"
                + "id integer PRIMARY KEY,\n"
                + "distributor integer NOT NULL references distributors(id) ON DELETE CASCADE,\n"
                + "item integer NOT NULL references items(id) ON DELETE CASCADE,\n"
                + "cost float NOT NULL\n" +
                ");";

        try {
            System.out.println("Applying schema");
            conn.createStatement().execute(itemsSql);
            conn.createStatement().execute(inventorySql);
            conn.createStatement().execute(distributorSql);
            conn.createStatement().execute(distributorPricesSql);
            System.out.println("Schema applied");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Schema function to reset the database if needed - do not change
    private static void seedDatabase() {
        String itemsSql = "INSERT INTO items (id, name) VALUES (1, 'Licorice'), (2, 'Good & Plenty'),\n"
            + "(3, 'Smarties'), (4, 'Tootsie Rolls'), (5, 'Necco Wafers'), (6, 'Wax Cola Bottles'), (7, 'Circus Peanuts'), (8, 'Candy Corn'),\n"
            + "(9, 'Twix'), (10, 'Snickers'), (11, 'M&Ms'), (12, 'Skittles'), (13, 'Starburst'), (14, 'Butterfinger'), (15, 'Peach Rings'), (16, 'Gummy Bears'), (17, 'Sour Patch Kids')";
        String inventorySql = "INSERT INTO inventory (item, stock, capacity) VALUES\n"
                + "(1, 22, 25), (2, 4, 20), (3, 15, 25), (4, 30, 50), (5, 14, 15), (6, 8, 10), (7, 10, 10), (8, 30, 40), (9, 17, 70), (10, 43, 65),\n" +
                "(11, 32, 55), (12, 25, 45), (13, 8, 45), (14, 10, 60), (15, 20, 30), (16, 15, 35), (17, 14, 60)";
        String distributorSql = "INSERT INTO distributors (id, name) VALUES (1, 'Candy Corp'), (2, 'The Sweet Suite'), (3, 'Dentists Hate Us')";
        String distributorPricesSql = "INSERT INTO distributor_prices (distributor, item, cost) VALUES \n" +
                "(1, 1, 0.81), (1, 2, 0.46), (1, 3, 0.89), (1, 4, 0.45), (2, 2, 0.18), (2, 3, 0.54), (2, 4, 0.67), (2, 5, 0.25), (2, 6, 0.35), (2, 7, 0.23), (2, 8, 0.41), (2, 9, 0.54),\n" +
                "(2, 10, 0.25), (2, 11, 0.52), (2, 12, 0.07), (2, 13, 0.77), (2, 14, 0.93), (2, 15, 0.11), (2, 16, 0.42), (3, 10, 0.47), (3, 11, 0.84), (3, 12, 0.15), (3, 13, 0.07), (3, 14, 0.97),\n" +
                "(3, 15, 0.39), (3, 16, 0.91), (3, 17, 0.85)";

        try {
            System.out.println("Seeding database");
            conn.createStatement().execute(itemsSql);
            conn.createStatement().execute(inventorySql);
            conn.createStatement().execute(distributorSql);
            conn.createStatement().execute(distributorPricesSql);
            System.out.println("Database seeded");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // Helper methods to convert ResultSet to JSON - change if desired, but should not be required
    private static JSONArray convertResultSetToJson(ResultSet rs) throws SQLException{
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        List<String> colNames = IntStream.range(0, columns)
                .mapToObj(i -> {
                    try {
                        return md.getColumnName(i + 1);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .collect(Collectors.toList());

        JSONArray jsonArray = new JSONArray();
        while (rs.next()) {
            jsonArray.add(convertRowToJson(rs, colNames));
        }
        return jsonArray;
    }

    private static JSONObject convertRowToJson(ResultSet rs, List<String> colNames) throws SQLException {
        JSONObject obj = new JSONObject();
        for (String colName : colNames) {
            obj.put(colName, rs.getObject(colName));
        }
        return obj;
    }

    // Controller functions - add your routes here. getItems is provided as an example
    public static JSONArray getItems() {
        String sql = "SELECT * FROM items";
        try {
            ResultSet set = conn.createStatement().executeQuery(sql);
            return convertResultSetToJson(set);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    // shared SELECT clause for inventory queries
    private static final String ALL_INVENTORY_SQL =
            "SELECT i.id"
                    + " , i.item       AS item_id"
                    + " , it.name     AS item_name"
                    + " , i.stock     AS amount_in_stock"
                    + " , i.capacity  AS total_capacity"
                    + " FROM inventory i"
                    + " JOIN items it ON it.id = i.item";



    // version helper
    public static JSONObject getVersion() {
        JSONObject o = new JSONObject();
        o.put("version", "TopBloc Code Challenge v1.0");
        return o;
    }

    // return every inventory record
    public static JSONArray getAllInventory() {
        try (ResultSet rs = conn.createStatement().executeQuery(ALL_INVENTORY_SQL)) {
            return convertResultSetToJson(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }


    // inventory items with stock == 0
    public static JSONArray getOutOfStock() {
        String sql = ALL_INVENTORY_SQL + " WHERE i.stock = 0";
        try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
            return convertResultSetToJson(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    // inventory items with stock > capacity
    public static JSONArray getOverstocked() {
        String sql = ALL_INVENTORY_SQL + " WHERE i.stock > i.capacity";
        try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
            return convertResultSetToJson(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    // inventory items below 35% capacity
    public static JSONArray getLowStock() {
        String sql = ALL_INVENTORY_SQL + " WHERE i.stock * 1.0 / i.capacity < 0.35";
        try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
            return convertResultSetToJson(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    // single inventory item by its inventory id
    public static JSONArray getInventoryById(int id) {
        String sql = ALL_INVENTORY_SQL + " WHERE i.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return convertResultSetToJson(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }
    // 1. List all distributors
    public static JSONArray getDistributors() {
        String sql =
                "SELECT id, name " +
                        "  FROM distributors";
        try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
            return convertResultSetToJson(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    // 2. Given a distributor ID, list its offerings
    public static JSONArray getOfferingsByDistributor(int distributorId) {
        String sql =
                "SELECT dp.id" +
                        "     , it.name    AS item_name" +
                        "     , dp.cost" +
                        "  FROM distributor_prices dp" +
                        "  JOIN items it   ON dp.item = it.id" +
                        " WHERE dp.distributor = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, distributorId);
            try (ResultSet rs = ps.executeQuery()) {
                return convertResultSetToJson(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    // 3. Given an item ID, list all distributor offerings
    public static JSONArray getOfferingsByItem(int itemId) {
        String sql =
                "SELECT dp.id" +
                        "     , d.name     AS distributor_name" +
                        "     , dp.cost" +
                        "  FROM distributor_prices dp" +
                        "  JOIN distributors d ON dp.distributor = d.id" +
                        " WHERE dp.item = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return convertResultSetToJson(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }
    public static JSONObject getCheapestOffer(int itemId, int quantity) {
        String sql =
                "SELECT dp.distributor AS distributor_id" +
                        "     , d.name        AS distributor_name" +
                        "     , dp.cost       AS unit_cost" +
                        "     , dp.cost * ?   AS total_cost" +
                        "  FROM distributor_prices dp" +
                        "  JOIN distributors d ON dp.distributor = d.id" +
                        " WHERE dp.item = ?" +
                        " ORDER BY dp.cost ASC" +
                        " LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                // convertResultSetToJson returns a JSONArray of rows
                JSONArray results = convertResultSetToJson(rs);
                if (results.isEmpty()) {
                    // no offerings found
                    JSONObject none = new JSONObject();
                    none.put("message", "No offerings found for item " + itemId);
                    return none;
                }

                return (JSONObject) results.get(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JSONObject error = new JSONObject();
            error.put("error", e.getMessage());
            return error;
        }
    }
    private static final JSONParser PARSER = new JSONParser();

    public static JSONObject addItem(String body) {
        try {
            JSONObject in = (JSONObject) PARSER.parse(body);
            String name = ((String) in.get("name")).trim();
            // insert the new item
            String sql = "INSERT INTO items(name) VALUES(?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.executeUpdate();
            }

            long id;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                id = rs.getLong(1);
            }
            // build success JSON
            JSONObject out = new JSONObject();
            out.put("id", id);
            out.put("name", name);
            return out;
        } catch (SQLException e) {
            JSONObject err = new JSONObject();
            String msg = e.getMessage();
            if (msg.contains("UNIQUE constraint failed")) {
                err.put("error", "Item already exists");
            } else {
                err.put("error", msg);
            }
            return err;
        } catch (ParseException e) {
            JSONObject err = new JSONObject();
            err.put("error", "Invalid JSON body");
            return err;
        }
    }



    public static JSONObject addInventory(String body) {
        try {
            JSONObject in = (JSONObject) PARSER.parse(body);
            long itemId   = (long) in.get("item");
            long stock    = (long) in.get("stock");
            long capacity = (long) in.get("capacity");

            String sql = "INSERT INTO inventory(item, stock, capacity) VALUES(?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, itemId);
                ps.setLong(2, stock);
                ps.setLong(3, capacity);
                ps.executeUpdate();
            }

            long newInvId;
            try (ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                newInvId = rs.getLong(1);
            }

            String lookup =
                    "SELECT i.id      AS inv_id,"
                            + "       i.item    AS item_id,"
                            + "       it.name   AS item_name,"
                            + "       i.stock   AS amount_in_stock,"
                            + "       i.capacity AS total_capacity"
                            + "  FROM inventory i"
                            + "  JOIN items it ON it.id = i.item"
                            + " WHERE i.id = ?";
            try (PreparedStatement ps = conn.prepareStatement(lookup)) {
                ps.setLong(1, newInvId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Failed to retrieve new inventory row");
                    JSONObject out = new JSONObject();
                    out.put("id",               rs.getLong("inv_id"));         // you can keep this or drop it later
                    out.put("item_id",          rs.getLong("item_id"));
                    out.put("item_name",        rs.getString("item_name"));
                    out.put("amount_in_stock",  rs.getLong("amount_in_stock"));
                    out.put("total_capacity",   rs.getLong("total_capacity"));
                    return out;
                }
            }
        } catch (SQLException e) {
            JSONObject err = new JSONObject();
            err.put("error", e.getMessage());
            return err;
        } catch (ParseException e) {
            JSONObject err = new JSONObject();
            err.put("error", "Invalid JSON body for inventory");
            return err;
        }
    }



    public static JSONObject updateInventory(int id, String body) {
        try {
            JSONObject in = (JSONObject) PARSER.parse(body);
            String sql = "UPDATE inventory SET stock = ?, capacity = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, (Long) in.get("stock"));
                ps.setLong(2, (Long) in.get("capacity"));
                ps.setInt(3, id);
                int updated = ps.executeUpdate();
                JSONObject out = new JSONObject();
                out.put("status", updated > 0 ? "ok" : "not_found");
                return out;
            }
        } catch (SQLException|ParseException e) {
            JSONObject err = new JSONObject();
            err.put("error", e.getMessage());
            return err;
        }
    }


    public static JSONObject deleteInventory(int id) {
        try {
            String sql = "DELETE FROM inventory WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int deleted = ps.executeUpdate();
                JSONObject out = new JSONObject();
                out.put("status", deleted > 0 ? "ok" : "not_found");
                return out;
            }
        } catch (SQLException e) {
            JSONObject err = new JSONObject();
            err.put("error", e.getMessage());
            return err;
        }
    }

    public static JSONObject addDistributor(String body) {
        try {
            JSONObject in = (JSONObject) PARSER.parse(body);
            String sql = "INSERT INTO distributors(name) VALUES(?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, (String) in.get("name"));
                ps.executeUpdate();
            }
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT last_insert_rowid() AS id");
            int id = rs.next() ? rs.getInt("id") : -1;
            JSONObject out = new JSONObject();
            out.put("status", "ok");
            out.put("id", id);
            return out;

        } catch (SQLException | ParseException e) {
            JSONObject err = new JSONObject();
            err.put("error", e.getMessage());
            return err;
        }
    }


    public static JSONObject addPrice(int distributorId, String body) {
        try {
            JSONObject in = (JSONObject) PARSER.parse(body);
            String sql = "INSERT INTO distributor_prices(distributor, item, cost) VALUES(?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, distributorId);
                ps.setLong(2, (Long) in.get("item"));
                ps.setDouble(3, (Double) in.get("cost"));
                ps.executeUpdate();
            }
            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT last_insert_rowid() AS id");
            int id = rs.next() ? rs.getInt("id") : -1;
            JSONObject out = new JSONObject();
            out.put("status", "ok");
            out.put("id", id);
            return out;

        } catch (SQLException | ParseException e) {
            JSONObject err = new JSONObject();
            err.put("error", e.getMessage());
            return err;
        }
    }

    public static JSONObject updatePrice(int distributorId, int itemId, String body) {
        try {
            JSONObject in = (JSONObject) PARSER.parse(body);
            String sql = "UPDATE distributor_prices SET cost = ? WHERE distributor = ? AND item = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, (Double) in.get("cost"));
                ps.setInt(2, distributorId);
                ps.setInt(3, itemId);
                int updated = ps.executeUpdate();
                JSONObject out = new JSONObject();
                out.put("status", updated > 0 ? "ok" : "not_found");
                return out;
            }
        } catch (SQLException|ParseException e) {
            JSONObject err = new JSONObject();
            err.put("error", e.getMessage());
            return err;
        }
    }

    public static JSONObject deleteDistributor(int id) {
        try {
            String sql = "DELETE FROM distributors WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int deleted = ps.executeUpdate();
                JSONObject out = new JSONObject();
                out.put("status", deleted > 0 ? "ok" : "not_found");
                return out;
            }
        } catch (SQLException e) {
            JSONObject err = new JSONObject();
            err.put("error", e.getMessage());
            return err;
        }
    }
    public static JSONObject deleteItem(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM items WHERE id = ?")) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            JSONObject out = new JSONObject();
            out.put("status", deleted > 0 ? "ok" : "not_found");
            return out;
        } catch (SQLException e) {
            JSONObject err = new JSONObject();
            err.put("error", e.getMessage());
            return err;
        }
    }

    /* ─────────── CSV export ─────────── */

    public static String exportTableAsCsv(String table) {

        String sql;
        switch (table) {
            case "items":
            case "distributors":
                sql = "SELECT * FROM " + table;
                break;

            case "inventory":
                sql =
                        "SELECT i.id                       AS inventory_id," +
                                "       it.name                    AS item_name," +
                                "       i.stock                    AS amount_in_stock," +
                                "       i.capacity                 AS total_capacity " +
                                "  FROM inventory i " +
                                "  JOIN items it ON it.id = i.item";
                break;

            case "distributor_prices":
                sql =
                        "SELECT dp.id                      AS price_id," +
                                "       d.name                     AS distributor_name," +
                                "       it.name                    AS item_name," +
                                "       dp.cost                    AS unit_cost " +
                                "  FROM distributor_prices dp " +
                                "  JOIN distributors d ON d.id = dp.distributor " +
                                "  JOIN items it       ON it.id = dp.item";
                break;

            default:
                return "error,invalid_table\n";
        }

        StringBuilder csv = new StringBuilder();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();

            /* header */
            for (int i = 1; i <= cols; i++) {
                csv.append(md.getColumnName(i));
                if (i < cols) csv.append(",");
            }
            csv.append("\n");

            /* rows */
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    String cell = rs.getString(i);
                    if (cell == null) cell = "";
                    cell = cell.replace("\"", "\"\"");  // escape quotes
                    if (cell.contains(",") || cell.contains("\""))
                        cell = "\"" + cell + "\"";
                    csv.append(cell);
                    if (i < cols) csv.append(",");
                }
                csv.append("\n");
            }
        } catch (SQLException e) {
            return "error,\"" + e.getMessage().replace("\"", "\"\"") + "\"\n";
        }
        return csv.toString();
    }
}