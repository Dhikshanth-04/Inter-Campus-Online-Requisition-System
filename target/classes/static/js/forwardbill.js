// ===================== TOAST SYSTEM (GLOBAL) =====================
const toastContainer = document.createElement("div");
toastContainer.id = "toastContainer";
toastContainer.style.position = "fixed";
toastContainer.style.top = "20px";
toastContainer.style.right = "20px";
toastContainer.style.zIndex = "2000";
document.body.appendChild(toastContainer);

function showToast(message, type = "success") {
  const toast = document.createElement("div");
  toast.textContent = message;

  toast.style.padding = "10px 18px";
  toast.style.marginTop = "10px";
  toast.style.borderRadius = "12px";
  toast.style.color = "#fff";
  toast.style.fontWeight = "500";
  toast.style.boxShadow = "0 6px 20px rgba(0,0,0,0.15)";
  toast.style.opacity = "0.95";
  toast.style.transition = "opacity 0.4s ease";

  if (type === "success") toast.style.backgroundColor = "#10b981";
  else if (type === "error") toast.style.backgroundColor = "#ef4444";
  else toast.style.backgroundColor = "#2563eb";

  toastContainer.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = "0";
    setTimeout(() => toast.remove(), 400);
  }, 2200);
}

// ===================== STATE =====================
let allBills = [];

// ===================== RENDER FUNCTION =====================
function renderBills(bills) {
  const billsContainer = document.getElementById("billsContainer");
  billsContainer.innerHTML = "";

  if (!bills || bills.length === 0) {
    billsContainer.innerHTML = `<div class="no-data">No matching bills found</div>`;
    return;
  }

  bills.forEach(bill => {
    const billCard = document.createElement("div");
    billCard.className = "bill-card";

    billCard.innerHTML = `
      <h3 style="margin-bottom:12px; color:#2563eb;">
        🧾 Bill #${bill.bill_id}
        <span style="color:#6b7280; font-size:0.9rem;">
          — ${bill.buyer_name} (${bill.department || '-'}, ${bill.institution || '-'})
        </span>
      </h3>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Stock Name</th>
              <th>Quantity</th>
            </tr>
          </thead>
          <tbody>
            ${bill.items.map(item => `
              <tr>
                <td>${item.stock_name}</td>
                <td>
                  <input
                    type="number"
                    min="0"
                    value="${item.quantity}"
                    data-bill="${bill.bill_id}"
                    data-stock="${item.stock_name}"
                  />
                </td>
              </tr>
            `).join("")}
          </tbody>
        </table>
      </div>

      <div style="display:flex; justify-content:flex-end; margin-top:16px;">
        <button class="forward-btn" data-bill="${bill.bill_id}">
          <i class="fas fa-paper-plane"></i> Forward Bill
        </button>
      </div>
    `;

    billsContainer.appendChild(billCard);

    // Quantity update
    billCard.querySelectorAll("input[type='number']").forEach(input => {
      input.addEventListener("change", () => {
        const payload = {
          bill_id: Number(input.dataset.bill),
          items: [{
            stock_name: input.dataset.stock,
            quantity: Number(input.value)
          }]
        };

        fetch("/forwardbill/update", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload)
        })
        .then(res => res.json())
        .then(data => {
          if (data.status === "success") {
            showToast(`Updated ${input.dataset.stock}`);
            // Update local state
            const billIndex = allBills.findIndex(b => b.bill_id === payload.bill_id);
            if (billIndex >= 0) {
              const itemIndex = allBills[billIndex].items.findIndex(i => i.stock_name === payload.items[0].stock_name);
              if (itemIndex >= 0) allBills[billIndex].items[itemIndex].quantity = payload.items[0].quantity;
            }
          } else {
            showToast("Update failed", "error");
          }
        })
        .catch(() => showToast("Update error", "error"));
      });
    });

    // Forward bill button
    billCard.querySelector(".forward-btn").addEventListener("click", () => {
      forwardBill(bill.bill_id);
    });
  });
}

// ===================== MAIN =====================
document.addEventListener("DOMContentLoaded", () => {

  const billSearch = document.getElementById("billSearch");

  fetch("/forwardbill", {
    method: "GET",
    headers: { "Accept": "application/json" }
  })
  .then(res => {
    if (!res.ok) throw new Error("HTTP error " + res.status);
    return res.json();
  })
  .then(data => {
    allBills = Array.isArray(data) ? data : [];
    renderBills(allBills);
  })
  .catch(() => showToast("Failed to load pending bills", "error"));

  // ===================== BILL ID FILTER =====================
  billSearch.addEventListener("input", () => {
    const value = billSearch.value.trim();

    if (value === "") {
      renderBills(allBills);
      return;
    }

    const billId = Number(value);
    const filtered = allBills.filter(b => b.bill_id === billId);
    renderBills(filtered);
  });
});

// ===================== FORWARD BILL =====================
function forwardBill(billId) {
  if (!billId) return;

  fetch("/forwardbill/forward", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ bill_id: billId })
  })
  .then(res => res.json())
  .then(data => {
    if (data.status === "success") {
      showToast(`Bill #${billId} forwarded`);
      // Remove forwarded bill from local state and re-render
      allBills = allBills.filter(b => b.bill_id !== billId);
      renderBills(allBills);
    } else {
      showToast(data.message || "Forward failed", "error");
    }
  })
  .catch(() => showToast("Error forwarding bill", "error"));
}
