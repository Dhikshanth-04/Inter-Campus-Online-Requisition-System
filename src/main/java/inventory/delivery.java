package inventory;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/delivery")
public class delivery {

    @Autowired
    private DataSource dataSource;

    // ===================== DATA CLASSES =====================
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
        private List<BillItem> items = new ArrayList<>();
        public int getBill_id() { return bill_id; }
        public String getBuyer_name() { return buyer_name; }
        public List<BillItem> getItems() { return items; }
        public Bill(int id, String name) { this.bill_id = id; this.buyer_name = name; }
    }

    // ===================== FETCH APPROVED BILLS =====================
    @GetMapping("/approved-bills")
    public List<Bill> getApprovedBills() {
        List<Bill> bills = new ArrayList<>();
        Map<Integer, Bill> map = new LinkedHashMap<>();
        String sql = "SELECT bill_id, buyer_name, stock_name, quantity FROM bill_details WHERE status='APPROVED' ORDER BY bill_id";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("bill_id");
                map.putIfAbsent(id, new Bill(id, rs.getString("buyer_name")));
                Bill bill = map.get(id);

                BillItem item = new BillItem();
                item.setStock_name(rs.getString("stock_name"));
                item.setQuantity(rs.getInt("quantity"));
                bill.getItems().add(item);
            }

            bills.addAll(map.values());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bills;
    }

    // ===================== MARK SINGLE ITEM AS DELIVERED =====================
    public static class DeliverRequest {
        public int bill_id;
        public String stock_name; // per-item delivery
    }

    @PostMapping("/mark-delivered")
    public Map<String, Object> markDelivered(@RequestBody DeliverRequest req) {
        Map<String, Object> res = new HashMap<>();
        List<String> delivered = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        String fetchStock = "SELECT quantity FROM stocklist WHERE stock_name=?";
        String fetchItemQty = "SELECT quantity FROM bill_details WHERE bill_id=? AND stock_name=?";
        String updateStock = "UPDATE stocklist SET quantity=? WHERE stock_name=?";
        String updateItemStatus = "UPDATE bill_details SET status='DELIVERED', updated_at=CURRENT_TIMESTAMP WHERE bill_id=? AND stock_name=?";

        try (Connection con = dataSource.getConnection()) {

            // Fetch stock quantity
            int available = 0;
            try (PreparedStatement psStock = con.prepareStatement(fetchStock)) {
                psStock.setString(1, req.stock_name);
                try (ResultSet rsStock = psStock.executeQuery()) {
                    if (rsStock.next()) available = rsStock.getInt("quantity");
                    else { skipped.add(req.stock_name); res.put("status", "success"); res.put("delivered", delivered); res.put("skipped", skipped); return res; }
                }
            }

            // Fetch item quantity in bill
            int itemQty = 0;
            try (PreparedStatement psItemQty = con.prepareStatement(fetchItemQty)) {
                psItemQty.setInt(1, req.bill_id);
                psItemQty.setString(2, req.stock_name);
                try (ResultSet rsItem = psItemQty.executeQuery()) {
                    if (rsItem.next()) itemQty = rsItem.getInt("quantity");
                    else { skipped.add(req.stock_name); res.put("status", "success"); res.put("delivered", delivered); res.put("skipped", skipped); return res; }
                }
            }

            if (available >= itemQty) {
                // Reduce stock
                try (PreparedStatement psUpdateStock = con.prepareStatement(updateStock)) {
                    psUpdateStock.setInt(1, available - itemQty);
                    psUpdateStock.setString(2, req.stock_name);
                    psUpdateStock.executeUpdate();
                }

                // Mark item delivered
                try (PreparedStatement psUpdateItem = con.prepareStatement(updateItemStatus)) {
                    psUpdateItem.setInt(1, req.bill_id);
                    psUpdateItem.setString(2, req.stock_name);
                    psUpdateItem.executeUpdate();
                }

                delivered.add(req.stock_name);

                // Check if all items in the bill are delivered
                String checkRemaining = "SELECT COUNT(*) AS remaining FROM bill_details WHERE bill_id=? AND status='APPROVED'";
                try (PreparedStatement psCheck = con.prepareStatement(checkRemaining)) {
                    psCheck.setInt(1, req.bill_id);
                    try (ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next() && rsCheck.getInt("remaining") == 0) {
                            // Mark all bill items as DELIVERED
                            String updateAll = "UPDATE bill_details SET status='DELIVERED' WHERE bill_id=?";
                            try (PreparedStatement psAll = con.prepareStatement(updateAll)) {
                                psAll.setInt(1, req.bill_id);
                                psAll.executeUpdate();
                            }
                        }
                    }
                }

            } else {
                skipped.add(req.stock_name);
            }

            res.put("status", "success");
            res.put("delivered", delivered);
            res.put("skipped", skipped);

        } catch (Exception e) {
            e.printStackTrace();
            res.put("status", "error");
            res.put("message", "Database error");
        }

        return res;
    }
}
