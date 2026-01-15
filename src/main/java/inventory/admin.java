package inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class admin {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private static final int THRESHOLD = 10;

    /* =====================================================
       1️⃣ THRESHOLD STOCK
    ===================================================== */
    @GetMapping("/threshold")
    public List<Map<String, Object>> getThresholdStock() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT material_id, stock_name, quantity FROM stocklist WHERE quantity <= ? ORDER BY stock_name";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, THRESHOLD);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getInt("material_id"));
                    row.put("name", rs.getString("stock_name"));
                    row.put("quantity", rs.getInt("quantity"));
                    result.add(row);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /* =====================================================
       2️⃣ ALL STOCKS
    ===================================================== */
    @GetMapping("/stocks")
    public List<Map<String, Object>> getAllStocks() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT material_id, stock_name, quantity FROM stocklist ORDER BY stock_name";

        try (Connection conn = DriverManager.getConnection(url, username, password);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("material_id"));
                row.put("name", rs.getString("stock_name"));
                row.put("quantity", rs.getInt("quantity"));
                result.add(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /* =====================================================
       3️⃣ ADD STOCK
    ===================================================== */
    @PostMapping("/add-stock")
    public Map<String, String> addStock(@RequestBody Map<String, Object> payload) {
        String name = payload.get("name").toString().trim();
        int quantity = Integer.parseInt(payload.get("quantity").toString());

        if (name.isEmpty() || quantity <= 0) {
            return Map.of("status", "failed", "message", "Invalid stock data");
        }

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // Check for duplicate
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM stocklist WHERE stock_name = ?")) {
                check.setString(1, name);
                ResultSet rs = check.executeQuery();
                rs.next();
                if (rs.getInt(1) > 0) {
                    return Map.of("status", "failed", "message", "Stock already exists");
                }
            }

            // Insert stock
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO stocklist (stock_name, quantity) VALUES (?, ?)")) {
                insert.setString(1, name);
                insert.setInt(2, quantity);
                insert.executeUpdate();
            }

            return Map.of("status", "success", "message", "Stock added");

        } catch (SQLException e) {
            e.printStackTrace();
            return Map.of("status", "failed", "message", "Database error");
        }
    }

    /* =====================================================
       4️⃣ PLACE ORDER (MULTI-ITEM SINGLE TABLE)
    ===================================================== */
    @PostMapping("/place-order")
    public Map<String, Object> placeOrder(@RequestBody Map<String, Object> payload) {
        String buyer = payload.getOrDefault("buyer", "Administration").toString();
        String department = payload.getOrDefault("department", "").toString();
        String institution = payload.getOrDefault("institution", "").toString();

        // Multi-item orders
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        if (items == null || items.isEmpty()) {
            return Map.of("status", "failed", "message", "No order items provided");
        }

        String insertSQL = "INSERT INTO orders (buyer_name, department, institution, stock_name, quantity, status, created_at, updated_at) " +
                           "VALUES (?, ?, ?, ?, ?, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            conn.setAutoCommit(false);
            int lastOrderId = -1;

            for (Map<String, Object> item : items) {
                String stock = item.get("stock").toString().trim();
                int quantity = Integer.parseInt(item.get("quantity").toString());

                if (stock.isEmpty() || quantity <= 0) {
                    conn.rollback();
                    return Map.of("status", "failed", "message", "Invalid stock or quantity for item: " + stock);
                }

                // Check stock exists
                try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM stocklist WHERE stock_name = ?")) {
                    check.setString(1, stock);
                    ResultSet rs = check.executeQuery();
                    rs.next();
                    if (rs.getInt(1) == 0) {
                        conn.rollback();
                        return Map.of("status", "failed", "message", "Stock not found: " + stock);
                    }
                }

                // Insert order item
                try (PreparedStatement ps = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, buyer);
                    ps.setString(2, department);
                    ps.setString(3, institution);
                    ps.setString(4, stock);
                    ps.setInt(5, quantity);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) lastOrderId = rs.getInt(1);
                    }
                }
            }

            conn.commit();
            return Map.of("status", "success", "order_id", lastOrderId, "message", "Order placed successfully");

        } catch (SQLException e) {
            e.printStackTrace();
            return Map.of("status", "failed", "message", "Order failed due to database error");
        }
    }
}
