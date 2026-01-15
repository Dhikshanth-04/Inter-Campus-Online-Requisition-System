package inventory;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.*;

/* ===========================
   MAIN APPLICATION
=========================== */
@SpringBootApplication
public class inputbill {
    public static void main(String[] args) {
        SpringApplication.run(inputbill.class, args);
    }
}

/* ===========================
   BILL ENTITY
=========================== */
@Entity
@Table(name = "bill_details")
class BillDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "bill_id", nullable = false)
    private Integer billId;

    @Column(name = "buyer_name", nullable = false)
    private String buyerName;

    private String department;
    private String institution;
    private String status;
    private String message;

    @Column(name = "stock_name")
    private String stockName;

    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public Integer getBillId() { return billId; }
    public void setBillId(Integer billId) { this.billId = billId; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

/* ===========================
   STOCK ENTITY
=========================== */
@Entity
@Table(name = "stocklist")
class Stock {

    @Id
    @Column(name = "material_id")
    private Integer materialId;

    @Column(name = "stock_name")
    private String stockName;

    public Integer getMaterialId() { return materialId; }
    public String getStockName() { return stockName; }
}

/* ===========================
   REPOSITORIES
=========================== */
interface BillRepository extends JpaRepository<BillDetail, Integer> {

    List<BillDetail> findAllByBillId(Integer billId);

    @Query("SELECT COALESCE(MAX(b.billId), 0) FROM BillDetail b")
    Integer findMaxBillId();

    /* -------- ALL PAST BILLS OF SPECIFIED BUYER -------- */
    @Query(value = """
        SELECT 
            b.bill_id,
            b.buyer_name,
            MIN(b.created_at) AS createdAt,
            CASE 
                WHEN SUM(CASE WHEN b.status = 'PENDING' THEN 1 ELSE 0 END) > 0
                THEN 'PENDING'
                ELSE 'DELIVERED'
            END AS status
        FROM bill_details b
        WHERE b.buyer_name = :buyerName
        GROUP BY b.bill_id, b.buyer_name
        ORDER BY createdAt DESC
    """, nativeQuery = true)
    List<Object[]> fetchAllBillsByBuyer(@Param("buyerName") String buyerName);
}

interface StockRepository extends JpaRepository<Stock, Integer> {

    @Query("SELECT s FROM Stock s ORDER BY s.stockName ASC")
    List<Stock> findAllStocks();
}

/* ===========================
   CONTROLLER
=========================== */
@RestController
@RequestMapping("/api/buyer")
@CrossOrigin(origins = "*")
class BuyerController {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private StockRepository stockRepository;

    /* =======================
       SUBMIT BILL
    ======================= */
    @PostMapping("/submit")
    @Transactional
    public ResponseEntity<?> submitBill(@RequestBody Map<String, Object> request) {

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items =
                (List<Map<String, Object>>) request.get("items");

        if (items == null || items.isEmpty())
            return ResponseEntity.badRequest().body("No items provided");

        Integer newBillId = billRepository.findMaxBillId() + 1;
        List<BillDetail> rows = new ArrayList<>();

        for (Map<String, Object> item : items) {
            BillDetail b = new BillDetail();
            b.setBuyerName((String) request.get("buyerName"));
            b.setDepartment((String) request.get("department"));
            b.setInstitution((String) request.get("institution"));
            b.setStockName((String) item.get("stockName"));
            b.setQuantity(((Number) item.get("quantity")).intValue());
            b.setStatus("PENDING");
            b.setMessage("New");
            b.setBillId(newBillId);
            rows.add(b);
        }

        billRepository.saveAll(rows);
        return ResponseEntity.ok(Collections.singletonMap("billId", newBillId));
    }

    /* =======================
       BUYER FULL BILL HISTORY
    ======================= */
    @GetMapping("/history/{buyerName}")
    public ResponseEntity<?> getBuyerHistory(@PathVariable String buyerName) {

        List<Object[]> rows = billRepository.fetchAllBillsByBuyer(buyerName);
        List<Map<String, Object>> response = new ArrayList<>();

        for (Object[] r : rows) {
            Map<String, Object> bill = new HashMap<>();
            bill.put("billId", ((Number) r[0]).intValue());
            bill.put("buyerName", r[1]);
            bill.put("createdAt", r[2]);
            bill.put("status", r[3]);
            response.add(bill);
        }

        return ResponseEntity.ok(response);
    }

    /* =======================
       BILL DETAILS
    ======================= */
    @GetMapping("/bill/{billId}")
    public ResponseEntity<?> getBill(@PathVariable Integer billId) {

        List<BillDetail> rows = billRepository.findAllByBillId(billId);
        if (rows.isEmpty()) return ResponseEntity.notFound().build();

        BillDetail first = rows.get(0);

        Map<String, Object> response = new HashMap<>();
        response.put("billId", billId);
        response.put("buyerName", first.getBuyerName());
        response.put("status", first.getStatus());
        response.put("createdAt", first.getCreatedAt());

        List<Map<String, Object>> items = new ArrayList<>();
        for (BillDetail b : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("stockName", b.getStockName());
            item.put("quantity", b.getQuantity());
            items.add(item);
        }

        response.put("items", items);
        return ResponseEntity.ok(response);
    }

    /* =======================
       GET ALL PRODUCTS
    ======================= */
    @GetMapping("/products")
    public List<Map<String, Object>> getProducts() {

        List<Map<String, Object>> result = new ArrayList<>();

        for (Stock s : stockRepository.findAllStocks()) {
            Map<String, Object> map = new HashMap<>();
            map.put("materialId", s.getMaterialId());
            map.put("stockName", s.getStockName());
            result.add(map);
        }

        return result;
    }
}
