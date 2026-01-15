document.addEventListener("DOMContentLoaded", () => {
    const deliveryList = document.getElementById("deliveryList");

    // ===================== TOAST =====================
    function showToast(msg, type = "success") {
        const toast = document.createElement("div");
        toast.textContent = msg;
        toast.className = `toast ${type}`;
        toast.style.position = "fixed";
        toast.style.top = "20px";
        toast.style.right = "20px";
        toast.style.padding = "12px 20px";
        toast.style.color = "#fff";
        toast.style.borderRadius = "6px";
        toast.style.zIndex = "9999";
        toast.style.fontWeight = "600";
        toast.style.boxShadow = "0 2px 8px rgba(0,0,0,0.3)";
        toast.style.backgroundColor =
            type === "error" ? "#dc3545" :
            type === "info" ? "#007bff" : "#28a745";
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 3000);
    }

    // ===================== LOAD DELIVERY BILLS =====================
    async function loadDeliveryBills() {
        deliveryList.innerHTML = "<p class='no-data'>Loading...</p>";

        try {
            const res = await fetch("/delivery/approved-bills");
            if (!res.ok) throw new Error("Network response not ok");
            const bills = await res.json();
            renderBills(bills);
        } catch (err) {
            deliveryList.innerHTML = "<p class='no-data'>Failed to load bills.</p>";
            showToast("Failed to fetch bills", "error");
            console.error(err);
        }
    }

    // ===================== RENDER BILL LIST =====================
    function renderBills(bills) {
        deliveryList.innerHTML = "";

        if (!Array.isArray(bills) || bills.length === 0) {
            deliveryList.innerHTML = "<p class='no-data'>No approved bills for delivery.</p>";
            return;
        }

        bills.forEach(bill => {
            const billCard = document.createElement("div");
            billCard.className = "bill-card";
            billCard.style.background = "#fff";
            billCard.style.padding = "14px";
            billCard.style.borderRadius = "8px";
            billCard.style.boxShadow = "0 2px 8px rgba(0,0,0,0.1)";
            billCard.style.marginBottom = "12px";

            // Bill Header
            const header = document.createElement("p");
            header.innerHTML = `<strong>Bill ID:</strong> ${bill.bill_id} | <strong>Buyer:</strong> ${bill.buyer_name}`;
            header.style.marginBottom = "10px";
            billCard.appendChild(header);

            // Items Table
            const table = document.createElement("table");
            table.style.width = "100%";
            table.style.borderCollapse = "collapse";

            const thead = document.createElement("thead");
            thead.innerHTML = `
                <tr style="background:#2563eb;color:#fff;">
                    <th style="padding:8px;text-align:left;">Stock</th>
                    <th style="padding:8px;text-align:center;">Quantity</th>
                    <th style="padding:8px;text-align:center;">Action</th>
                </tr>`;
            table.appendChild(thead);

            const tbody = document.createElement("tbody");

            (bill.items || []).forEach(item => {
                const tr = document.createElement("tr");
                tr.id = `row-${bill.bill_id}-${item.stock_name.replace(/\s+/g, "_")}`;
                tr.innerHTML = `
                    <td style="padding:6px;">${item.stock_name}</td>
                    <td style="padding:6px;text-align:center;">${item.quantity}</td>
                    <td style="padding:6px;text-align:center;">
                        <button class="btn-success deliver-item-btn">Deliver</button>
                    </td>`;

                // Deliver button per item
                const btn = tr.querySelector(".deliver-item-btn");
                btn.onclick = () => deliverItem(bill.bill_id, item.stock_name, tr, btn);

                tbody.appendChild(tr);
            });

            table.appendChild(tbody);
            billCard.appendChild(table);
            deliveryList.appendChild(billCard);
        });
    }

    // ===================== DELIVER SINGLE ITEM =====================
    async function deliverItem(billId, stockName, rowElement, buttonElement) {
        buttonElement.disabled = true; // prevent double click
        try {
            const res = await fetch("/delivery/mark-delivered", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ bill_id: billId, stock_name: stockName })
            });
            const data = await res.json();

            if (data.status === "success") {
                if (data.delivered.includes(stockName)) {
                    rowElement.style.backgroundColor = "#d4edda"; // green
                } else if (data.skipped.includes(stockName)) {
                    rowElement.style.backgroundColor = "#f8d7da"; // red
                    showToast(`${stockName} skipped, not enough stock`, "error");
                    buttonElement.disabled = false;
                }

                let msg = `Delivered: ${data.delivered.join(", ")}`;
                if (data.skipped.length) msg += ` | Skipped: ${data.skipped.join(", ")}`;
                showToast(msg);

                // Reload bills if all items in bill are delivered
                const remainingRows = Array.from(rowElement.parentElement.querySelectorAll("tr"))
                    .filter(tr => tr.style.backgroundColor === "" || tr.style.backgroundColor === ""); // still pending
                if (remainingRows.length === 0) setTimeout(loadDeliveryBills, 1000);

            } else {
                showToast(data.message || "Failed to update", "error");
                buttonElement.disabled = false;
            }
        } catch (err) {
            showToast("Server error", "error");
            console.error(err);
            buttonElement.disabled = false;
        }
    }

    loadDeliveryBills();
});
