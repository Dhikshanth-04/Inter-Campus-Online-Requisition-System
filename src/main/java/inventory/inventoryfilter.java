package inventory;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/inventory")
public class inventoryfilter {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPass;

    @GetMapping(value = "/filter", produces = MediaType.TEXT_HTML_VALUE)
    public String filterInventory(
            @RequestParam(required=false) String buyer,
            @RequestParam(required=false) String dept,
            @RequestParam(required=false) String insti,
            @RequestParam(required=false) String stock,
            @RequestParam(required=false) String particular_date,
            @RequestParam(required=false) String from_date,
            @RequestParam(required=false) String to_date
    ) {
        StringBuilder html = new StringBuilder();
        try (Connection con = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            Class.forName("com.mysql.cj.jdbc.Driver");
            StringBuilder query = new StringBuilder("SELECT * FROM master WHERE 1=1");
            Map<String,String> filterMap = new LinkedHashMap<>();

            if (buyer != null && !buyer.isEmpty()) { query.append(" AND buyer='").append(buyer).append("'"); filterMap.put("Buyer", buyer); }
            if (dept != null && !dept.isEmpty()) { query.append(" AND department='").append(dept).append("'"); filterMap.put("Department", dept); }
            if (insti != null && !insti.isEmpty()) { query.append(" AND institution='").append(insti).append("'"); filterMap.put("Institution", insti); }
            if (stock != null && !stock.isEmpty()) { query.append(" AND stock_name='").append(stock).append("'"); filterMap.put("Stock Name", stock); }
            if (particular_date != null && !particular_date.isEmpty()) { query.append(" AND delivery_date='").append(particular_date).append("'"); filterMap.put("Particular Date", particular_date); }
            if (from_date != null && !from_date.isEmpty() && to_date != null && !to_date.isEmpty()) { query.append(" AND delivery_date BETWEEN '").append(from_date).append("' AND '").append(to_date).append("'"); filterMap.put("Period", from_date + " to " + to_date); }

            query.append(" ORDER BY delivery_date DESC");

            try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(query.toString())) {
                html.append("<div id='printContent'>");
                html.append("<h2>PAAVAI VARAM EDUCATIONAL TRUST</h2>");
                html.append("<h3>PAAVAI INSTITUTIONS</h3>");

                if (!filterMap.isEmpty()) {
                    html.append("<p><b>Filters Applied:</b><br>");
                    filterMap.forEach((k,v) -> html.append(k).append(": ").append(v).append("<br>"));
                    html.append("</p>");
                }

                boolean hasData = false;
                html.append("<table><tr>");
                if (!filterMap.containsKey("Buyer")) html.append("<th>Buyer</th>");
                if (!filterMap.containsKey("Department")) html.append("<th>Department</th>");
                if (!filterMap.containsKey("Institution")) html.append("<th>Institution</th>");
                if (!filterMap.containsKey("Stock Name")) html.append("<th>Stock Name</th>");
                html.append("<th>Quantity</th>");
                if (!filterMap.containsKey("Particular Date") && !filterMap.containsKey("Period")) html.append("<th>Delivery Date</th>");
                html.append("</tr>");

                while(rs.next()) {
                    hasData = true;
                    html.append("<tr>");
                    if (!filterMap.containsKey("Buyer")) html.append("<td>").append(rs.getString("buyer")).append("</td>");
                    if (!filterMap.containsKey("Department")) html.append("<td>").append(rs.getString("department")).append("</td>");
                    if (!filterMap.containsKey("Institution")) html.append("<td>").append(rs.getString("institution")).append("</td>");
                    if (!filterMap.containsKey("Stock Name")) html.append("<td>").append(rs.getString("stock_name")).append("</td>");
                    html.append("<td>").append(rs.getInt("quantity")).append("</td>");
                    if (!filterMap.containsKey("Particular Date") && !filterMap.containsKey("Period")) html.append("<td>").append(rs.getDate("delivery_date")).append("</td>");
                    html.append("</tr>");
                }
                html.append("</table>");
                if (!hasData) html.append("<p style='color:red; text-align:center;'>No items found.</p>");
                html.append("</div>");
                if (hasData) html.append("<br><button onclick='printTable()' class='print-btn'>Print PDF</button>");
            }

        } catch (Exception e) {
            html.append("<p style='color:red'>Error: ").append(e.getMessage()).append("</p>");
        }
        return html.toString();
    }
}
