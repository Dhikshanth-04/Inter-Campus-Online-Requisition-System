package inventory;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/approval")
public class approval {

    @Autowired
    private DataSource dataSource;

    /* ===================== MODELS ===================== */

    public static class BillItem {
        private String stock_name;
        private int quantity;

        public String getStock_name() { return stock_name; }
        public void setStock_name(String stock_name) { this.stock_name = stock_name; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public static class Bill {
        private int bill_id;
        private String buyer_name;
        private String department;
        private String institution;
        private List<BillItem> items = new ArrayList<>();
        private String message;

        public Bill(int bill_id, String buyer_name) {
            this.bill_id = bill_id;
            this.buyer_name = buyer_name;
        }

        public int getBill_id() { return bill_id; }
        public String getBuyer_name() { return buyer_name; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getInstitution() { return institution; }
        public void setInstitution(String institution) { this.institution = institution; }

        public List<BillItem> getItems() { return items; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class UpdateRequest {
        public int bill_id;
        public String status;
        public String message;
        public String source; // USER or ADMIN
    }

    /* ===================== ID COLUMN ===================== */

    private String getIdColumn(String table) {
        return table.equals("bill_details") ? "bill_id" : "order_id";
    }

    /* ===================== FETCH APIs ===================== */

    @GetMapping("/user-bills")
    public Map<String, Object> getUserBills() {
        return Map.of(
            "status", "success",
            "data", fetchBills("bill_details", "FORWARDED")
        );
    }

    @GetMapping("/admin-bills")
    public Map<String, Object> getAdminBills() {
        return Map.of(
            "status", "success",
            "data", fetchBills("orders", "PENDING")
        );
    }

    /* ===================== CORE FETCH ===================== */

    private List<Bill> fetchBills(String table, String status) {

        Map<Integer, Bill> billMap = new LinkedHashMap<>();
        String idCol = getIdColumn(table);

        String sql =
            "SELECT " + idCol + " AS bill_id, " +
            "buyer_name, department, institution, " +
            "stock_name, quantity, " +
            "COALESCE(message, '') AS message " +
            "FROM " + table +
            " WHERE UPPER(status) = ? " +
            "ORDER BY " + idCol + " DESC";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int billId = rs.getInt("bill_id");

                    billMap.putIfAbsent(
                        billId,
                        new Bill(billId, rs.getString("buyer_name"))
                    );

                    Bill bill = billMap.get(billId);

                    // ✅ SET ONCE PER BILL
                    bill.setDepartment(rs.getString("department"));
                    bill.setInstitution(rs.getString("institution"));

                    BillItem item = new BillItem();
                    item.setStock_name(rs.getString("stock_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    bill.getItems().add(item);

                    bill.setMessage(rs.getString("message"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>(billMap.values());
    }

    /* ===================== UPDATE API ===================== */

    @PostMapping("/update")
    public Map<String, String> updateBill(@RequestBody UpdateRequest req) {

        if ("REJECTED".equalsIgnoreCase(req.status)
                && (req.message == null || req.message.trim().isEmpty())) {

            return Map.of(
                "status", "error",
                "message", "Rejection message is required"
            );
        }

        String table;

        if ("ADMIN".equalsIgnoreCase(req.source)) {
            table = "orders";
        } else if ("USER".equalsIgnoreCase(req.source)) {
            table = "bill_details";
        } else {
            return Map.of(
                "status", "error",
                "message", "Invalid bill source"
            );
        }

        String idCol = getIdColumn(table);

        try (Connection con = dataSource.getConnection()) {

            con.setAutoCommit(false);

            String sql =
                "UPDATE " + table +
                " SET status = ?, message = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE " + idCol + " = ?";

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, req.status.toUpperCase());
                ps.setString(2, req.message);
                ps.setInt(3, req.bill_id);

                int updated = ps.executeUpdate();
                if (updated == 0) {
                    con.rollback();
                    return Map.of(
                        "status", "error",
                        "message", "Bill not found"
                    );
                }
            }

            con.commit();
            return Map.of("status", "success");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                "status", "error",
                "message", "Database error"
            );
        }
    }
}
