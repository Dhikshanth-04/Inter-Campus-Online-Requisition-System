package inventory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.sql.Date;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/inventory")
public class advancefilter {

    @Autowired
    private DataSource dataSource;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ================= Filter Inventory =================
    @GetMapping("/filter")
    public List<Map<String, Object>> filterInventory(
            @RequestParam(required = false) String buyer,
            @RequestParam(required = false) String dept,
            @RequestParam(required = false) String insti,
            @RequestParam(required = false) String stock_name,
            @RequestParam(required = false) String particular_date,
            @RequestParam(required = false) String from_date,
            @RequestParam(required = false) String to_date
    ) {
        List<Map<String, Object>> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM master WHERE 1=1");

        if (buyer != null && !buyer.isEmpty()) sql.append(" AND buyer=?");
        if (dept != null && !dept.isEmpty()) sql.append(" AND department=?");
        if (insti != null && !insti.isEmpty()) sql.append(" AND institution=?");
        if (stock_name != null && !stock_name.isEmpty()) sql.append(" AND stock_name=?");
        if (particular_date != null && !particular_date.isEmpty()) sql.append(" AND delivery_date=?");
        if (from_date != null && to_date != null && !from_date.isEmpty() && !to_date.isEmpty())
            sql.append(" AND delivery_date BETWEEN ? AND ?");

        sql.append(" ORDER BY delivery_date DESC");

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int i = 1;
            if (buyer != null && !buyer.isEmpty()) ps.setString(i++, buyer);
            if (dept != null && !dept.isEmpty()) ps.setString(i++, dept);
            if (insti != null && !insti.isEmpty()) ps.setString(i++, insti);
            if (stock_name != null && !stock_name.isEmpty()) ps.setString(i++, stock_name);
            if (particular_date != null && !particular_date.isEmpty()) ps.setString(i++, particular_date);
            if (from_date != null && to_date != null && !from_date.isEmpty() && !to_date.isEmpty()) {
                ps.setString(i++, from_date);
                ps.setString(i++, to_date);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("buyer", rs.getString("buyer"));
                row.put("department", rs.getString("department"));
                row.put("institution", rs.getString("institution"));
                row.put("stock_name", rs.getString("stock_name"));
                row.put("quantity", rs.getInt("quantity"));
                Date date = rs.getDate("delivery_date");
                row.put("delivery_date", date != null ? date.toLocalDate().format(DATE_FORMATTER) : null);
                result.add(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    // ================= Dropdown APIs =================

    @GetMapping("/buyers")
    public List<String> getBuyers() {
        return getDistinctValues("buyer");
    }

    @GetMapping("/departments")
    public List<String> getDepartments() {
        return getDistinctValues("department");
    }

    @GetMapping("/institutions")
    public List<String> getInstitutions() {
        return getDistinctValues("institution");
    }

    @GetMapping("/stocks")
    public List<String> getStocks() {
        return getDistinctValues("stock_name");
    }

    // Helper method to fetch distinct values from master table
    private List<String> getDistinctValues(String column) {
        List<String> values = new ArrayList<>();
        String sql = "SELECT DISTINCT " + column + " FROM master ORDER BY " + column;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                values.add(rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return values;
    }
}
