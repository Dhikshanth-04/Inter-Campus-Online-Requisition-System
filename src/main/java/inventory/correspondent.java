package inventory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class correspondent {

    private final JdbcTemplate jdbc;

    public correspondent(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/inventory")
    public List<Map<String, Object>> getInventory() {
        String sql = "SELECT material_id, stock_name, quantity FROM stocklist";
        return jdbc.queryForList(sql);
    }
}
