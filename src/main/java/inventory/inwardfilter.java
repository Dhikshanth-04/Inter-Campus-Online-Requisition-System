package inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/inwardfilter")
public class inwardfilter {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    // -------------------------------
    // Inventory Filter Endpoint
    // -------------------------------
    @GetMapping
    public List<Map<String, Object>> getInventory(
            @RequestParam(value="po_no", required=false) Integer poNo,
            @RequestParam(value="stock_name", required=false) String stockName,
            @RequestParam(value="store_name", required=false) String storeName,
            @RequestParam(value="from_date", required=false) String fromDate,
            @RequestParam(value="to_date", required=false) String toDate
    ) throws SQLException, ClassNotFoundException {

        List<Map<String, Object>> results = new ArrayList<>();
        Class.forName("com.mysql.cj.jdbc.Driver");

        StringBuilder sql = new StringBuilder(
            "SELECT po_no, stock_name, store_name, date, quantity, amount FROM inlet_stock WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        if (poNo != null) { sql.append(" AND po_no = ?"); params.add(poNo); }
        if (stockName != null && !stockName.isEmpty()) { sql.append(" AND stock_name = ?"); params.add(stockName); }
        if (storeName != null && !storeName.isEmpty()) { sql.append(" AND store_name = ?"); params.add(storeName); }
        if (fromDate != null && !fromDate.isEmpty()) { sql.append(" AND date >= ?"); params.add(fromDate); }
        if (toDate != null && !toDate.isEmpty()) { sql.append(" AND date <= ?"); params.add(toDate); }

        try (Connection conn = DriverManager.getConnection(url, username, password);
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("po_no", rs.getInt("po_no"));
                    row.put("stock_name", rs.getString("stock_name"));
                    row.put("store_name", rs.getString("store_name"));
                    row.put("date", rs.getString("date"));
                    row.put("quantity", rs.getInt("quantity"));
                    row.put("amount", rs.getInt("amount"));
                    results.add(row);
                }
            }
        }

        return results;
    }

    // -------------------------------
    // Dropdown Endpoints
    // -------------------------------
    @GetMapping("/po_numbers")
    public List<Integer> getPONumbers() throws SQLException, ClassNotFoundException {
        return getUniqueIntValues("po_no");
    }

    @GetMapping("/stocknames")
    public List<String> getStockNames() throws SQLException, ClassNotFoundException {
        return getUniqueStringValues("stock_name");
    }

    @GetMapping("/storename")
    public List<String> getStoreNames() throws SQLException, ClassNotFoundException {
        return getUniqueStringValues("store_name");
    }

    @GetMapping("/dates")
    public List<String> getDates() throws SQLException, ClassNotFoundException {
        return getUniqueStringValues("date");
    }

    // -------------------------------
    // Helper Methods
    // -------------------------------
    private List<Integer> getUniqueIntValues(String column) throws SQLException, ClassNotFoundException {
        List<Integer> list = new ArrayList<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT " + column + " FROM inlet_stock ORDER BY " + column)) {
            while (rs.next()) list.add(rs.getInt(1));
        }
        return list;
    }

    private List<String> getUniqueStringValues(String column) throws SQLException, ClassNotFoundException {
        List<String> list = new ArrayList<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT " + column + " FROM inlet_stock ORDER BY " + column)) {
            while (rs.next()) list.add(rs.getString(1));
        }
        return list;
    }
    
}
