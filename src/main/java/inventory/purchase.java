package inventory;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/purchase")
public class purchase {

    @Autowired
    private DataSource dataSource;

    // ===================== MODELS =====================
    public static class PurchaseItem {
        private String stock_name;
        private int quantity;
        private int amount;

        public String getStock_name() { return stock_name; }
        public void setStock_name(String stock_name) { this.stock_name = stock_name; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
    }

    public static class PurchaseOrder {
        private Integer po_no; // nullable to auto-generate
        private String date;
        private String store_name;
        private List<PurchaseItem> items;

        public Integer getPo_no() { return po_no; }
        public void setPo_no(Integer po_no) { this.po_no = po_no; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getStore_name() { return store_name; }
        public void setStore_name(String store_name) { this.store_name = store_name; }
        public List<PurchaseItem> getItems() { return items; }
        public void setItems(List<PurchaseItem> items) { this.items = items; }
    }

    // ===================== GET ALL PURCHASES =====================
    @GetMapping("/all")
    public List<Map<String,Object>> getAllPurchases() {
        List<Map<String,Object>> list = new ArrayList<>();
        String sql = "SELECT po_no, stock_name, date, quantity, amount, store_name FROM inlet_stock ORDER BY po_no DESC";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String,Object> p = new HashMap<>();
                p.put("po_no", rs.getInt("po_no"));
                p.put("stock_name", rs.getString("stock_name"));
                p.put("date", rs.getString("date"));
                p.put("quantity", rs.getInt("quantity"));
                p.put("amount", rs.getInt("amount"));
                p.put("store_name", rs.getString("store_name"));
                list.add(p);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // ===================== ADD PURCHASE ORDER =====================
    @PostMapping("/add-po")
    public Map<String,Object> addPurchaseOrder(@RequestBody PurchaseOrder po) {
        Map<String,Object> res = new HashMap<>();

        if (po.getItems() == null || po.getItems().isEmpty()) {
            res.put("status", "error");
            res.put("message", "No items in purchase order");
            return res;
        }

        // Check duplicate stock in same PO
        Set<String> stockSet = new HashSet<>();
        for (PurchaseItem item : po.getItems()) {
            String stockNameTrimmed = item.getStock_name().trim();
            if (stockNameTrimmed.length() > 100) {
                stockNameTrimmed = stockNameTrimmed.substring(0, 100); // trim to fit DB
                item.setStock_name(stockNameTrimmed);
            }
            if (!stockSet.add(stockNameTrimmed.toLowerCase())) {
                res.put("status", "error");
                res.put("message", "Duplicate stock '" + stockNameTrimmed + "' in the same PO");
                return res;
            }
        }

        try (Connection con = dataSource.getConnection()) {

            // Auto-generate PO number if null
            if (po.getPo_no() == null) {
                try (PreparedStatement psMax = con.prepareStatement("SELECT IFNULL(MAX(po_no), 0) + 1 AS next_po FROM inlet_stock");
                     ResultSet rs = psMax.executeQuery()) {
                    if (rs.next()) {
                        po.setPo_no(rs.getInt("next_po"));
                    }
                }
            }

            for (PurchaseItem item : po.getItems()) {
                // -------------------- Insert into inlet_stock --------------------
                String sqlPurchase = "INSERT INTO inlet_stock (po_no, stock_name, date, quantity, amount, store_name) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sqlPurchase)) {
                    ps.setInt(1, po.getPo_no());
                    ps.setString(2, item.getStock_name());
                    ps.setString(3, po.getDate());
                    ps.setInt(4, item.getQuantity());
                    ps.setInt(5, item.getAmount());
                    ps.setString(6, po.getStore_name());
                    ps.executeUpdate();
                }

                // -------------------- Update stocklist --------------------
                String sqlCheck = "SELECT quantity FROM stocklist WHERE stock_name = ?";
                try (PreparedStatement psCheck = con.prepareStatement(sqlCheck)) {
                    psCheck.setString(1, item.getStock_name());
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) {
                            int currentQty = rs.getInt("quantity");
                            String sqlUpdate = "UPDATE stocklist SET quantity = ? WHERE stock_name = ?";
                            try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdate)) {
                                psUpdate.setInt(1, currentQty + item.getQuantity());
                                psUpdate.setString(2, item.getStock_name());
                                psUpdate.executeUpdate();
                            }
                        } else {
                            String sqlInsert = "INSERT INTO stocklist (stock_name, quantity) VALUES (?, ?)";
                            try (PreparedStatement psInsert = con.prepareStatement(sqlInsert)) {
                                psInsert.setString(1, item.getStock_name());
                                psInsert.setInt(2, item.getQuantity());
                                psInsert.executeUpdate();
                            }
                        }
                    }
                }
            }

            res.put("status", "success");
            res.put("message", "PO added successfully");
            res.put("po_no", po.getPo_no());

        } catch (Exception e) {
            e.printStackTrace();
            res.put("status", "error");
            res.put("message", "Database error: " + e.getMessage());
        }

        return res;
    }

    // ===================== AUTOCOMPLETE STOCK NAME =====================
    @GetMapping("/search-stock")
    public List<String> searchStock(@RequestParam String query) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT stock_name FROM stocklist WHERE stock_name LIKE ? ORDER BY stock_name LIMIT 10";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + query + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("stock_name"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
