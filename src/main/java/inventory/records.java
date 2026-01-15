package inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
@RestController
@CrossOrigin
public class records {

    @Autowired
    private BillRepo billRepo;

    @Autowired
    private OrderRepo orderRepo;

    public static void main(String[] args) {
        SpringApplication.run(records.class, args);
    }

    /* ================= API ENDPOINTS ================= */

    // Get all bills
    @GetMapping("/bills")
    public List<Bill> getBills() {
        return billRepo.findAll();
    }

    // Update bill status (Admin approves/rejects/delivers)
    @PutMapping("/bills/{id}/{status}")
    public void updateBillStatus(@PathVariable int id, @PathVariable String status) {
        Bill bill = billRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        bill.setStatus(status);
        billRepo.save(bill);

        // Record in admin orders
        Order order = new Order(bill);
        orderRepo.save(order);
    }

    // Get all admin orders
    @GetMapping("/orders")
    public List<Order> getOrders() {
        return orderRepo.findAll();
    }
}

/* ================= BILL ENTITY ================= */

@Entity
@Table(name = "bill_details")
class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "buyer_name", nullable = false)
    private String buyerName;

    private String department;
    private String institution;

    @Column(name = "stock_name", nullable = false)
    private String stockName;

    private int quantity;
    private String status;
    private String message;

    @Column(name = "bill_id")
    private int billId;

    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /* ===== Getters & Setters ===== */
    public int getId() { return id; }
    public String getBuyerName() { return buyerName; }
    public String getDepartment() { return department; }
    public String getInstitution() { return institution; }
    public String getStockName() { return stockName; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public int getBillId() { return billId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setStatus(String status) { this.status = status; }
}

/* ================= ORDER ENTITY ================= */

@Entity
@Table(name = "orders")
class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private int orderId;

    private String buyerName;
    private String department;
    private String institution;
    private String stockName;
    private int quantity;
    private String status;
    private String message;

    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public Order() {}

    public Order(Bill bill) {
        this.buyerName = bill.getBuyerName();
        this.department = bill.getDepartment();
        this.institution = bill.getInstitution();
        this.stockName = bill.getStockName();
        this.quantity = bill.getQuantity();
        this.status = bill.getStatus();
        this.message = bill.getMessage();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /* ===== Getters ===== */
    public int getOrderId() { return orderId; }
    public String getBuyerName() { return buyerName; }
    public String getDepartment() { return department; }
    public String getInstitution() { return institution; }
    public String getStockName() { return stockName; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

/* ================= REPOSITORIES ================= */

interface BillRepo extends org.springframework.data.jpa.repository.JpaRepository<Bill, Integer> {}
interface OrderRepo extends org.springframework.data.jpa.repository.JpaRepository<Order, Integer> {}
