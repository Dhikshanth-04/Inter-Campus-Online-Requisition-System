package inventory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
public class ForwardBillController {

    @Autowired
    private DataSource dataSource;

    // -------------------------
    // Bill Item class
    // -------------------------
    public static class BillItem {
        private String stock_name;
        private int quantity;

        public BillItem() {} // Default constructor for JSON

        public BillItem(String stock_name, int quantity) {
            this.stock_name = stock_name;
            this.quantity = quantity;
        }

        public String getStock_name() { return stock_name; }
        public void setStock_name(String stock_name) { this.stock_name = stock_name; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    // -------------------------
    // Bill class
    // -------------------------
    public static class Bill {
        private int bill_id;
        private String buyer_name;
        private String department;
        private String institution;
        private List<BillItem> items = new ArrayList<>();

        public Bill() {}

        public Bill(int bill_id, String buyer_name, String department, String institution) {
            this.bill_id = bill_id;
            this.buyer_name = buyer_name;
            this.department = department;
            this.institution = institution;
        }

        public int getBill_id() { return bill_id; }
        public void setBill_id(int bill_id) { this.bill_id = bill_id; }

        public String getBuyer_name() { return buyer_name; }
        public void setBuyer_name(String buyer_name) { this.buyer_name = buyer_name; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getInstitution() { return institution; }
        public void setInstitution(String institution) { this.institution = institution; }

        public List<BillItem> getItems() { return items; }
        public void setItems(List<BillItem> items) { this.items = items; }
    }

    // -------------------------
    // POST request class
    // -------------------------
    public static class BillUpdateRequest {
        private int bill_id;
        private List<BillItem> items;

        public BillUpdateRequest() {}

        public int getBill_id() { return bill_id; }
        public void setBill_id(int bill_id) { this.bill_id = bill_id; }

        public List<BillItem> getItems() { return items; }
        public void setItems(List<BillItem> items) { this.items = items; }
    }

    // -------------------------
    // GET endpoint: fetch pending bills
    // -------------------------
    @GetMapping("/forwardbill")
    public List<Bill> getPendingBills() {
        Map<Integer, Bill> billMap = new LinkedHashMap<>();
        try (Connection con = dataSource.getConnection()) {
            String sql = "SELECT bill_id, buyer_name, department, institution, stock_name, quantity " +
                         "FROM bill_details WHERE UPPER(status)='PENDING'";
            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int billId = rs.getInt("bill_id");
                    String buyerName = rs.getString("buyer_name");
                    String department = rs.getString("department");
                    String institution = rs.getString("institution");
                    String stockName = rs.getString("stock_name");
                    int quantity = rs.getInt("quantity");

                    Bill bill = billMap.get(billId);
                    if (bill == null) {
                        bill = new Bill(billId, buyerName, department, institution);
                        billMap.put(billId, bill);
                    }
                    bill.getItems().add(new BillItem(stockName, quantity));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>(billMap.values());
    }

    // -------------------------
    // POST endpoint: update quantities immediately
    // -------------------------
    @PostMapping("/forwardbill/update")
    public Map<String, String> updateBillQuantities(@RequestBody BillUpdateRequest request) {
        Map<String, String> response = new HashMap<>();
        try (Connection con = dataSource.getConnection()) {
            String sql = "UPDATE bill_details SET quantity=? WHERE bill_id=? AND stock_name=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (BillItem item : request.getItems()) {
                    ps.setInt(1, item.getQuantity());
                    ps.setInt(2, request.getBill_id());
                    ps.setString(3, item.getStock_name());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            response.put("status", "success");
            response.put("message", "Bill quantities updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Failed to update bill quantities");
        }
        return response;
    }

    // -------------------------
    // POST endpoint: forward bill (change status)
    // -------------------------
    @PostMapping("/forwardbill/forward")
    public Map<String, String> forwardBill(@RequestBody Map<String, Integer> request) {
        Map<String, String> response = new HashMap<>();
        int billId = request.get("bill_id");

        try (Connection con = dataSource.getConnection()) {
            String sql = "UPDATE bill_details SET status='FORWARDED' WHERE bill_id=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, billId);
                int updated = ps.executeUpdate();

                if (updated > 0) {
                    response.put("status", "success");
                    response.put("message", "Bill forwarded successfully");
                } else {
                    response.put("status", "error");
                    response.put("message", "Bill not found or already forwarded");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Failed to forward bill");
        }

        return response;
    }
}
