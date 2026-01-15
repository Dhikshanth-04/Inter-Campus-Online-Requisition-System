/* =====================================================
   POPUP HANDLING
===================================================== */
function showPopup() {
  document.getElementById("popup").style.display = "block";
  document.getElementById("overlay").style.display = "block";
}

function hidePopup() {
  document.getElementById("popup").style.display = "none";
  document.getElementById("overlay").style.display = "none";
}

document.getElementById("thresholdBtn").addEventListener("click", () => loadThresholdStock(true));
document.getElementById("closePopupBtn").addEventListener("click", hidePopup);

/* =====================================================
   LOAD THRESHOLD STOCK
===================================================== */
async function loadThresholdStock(showPopupFlag = false) {
  const container = document.querySelector(".table-container");
  container.innerHTML = "<p>Loading threshold stock...</p>";

  try {
    const res = await fetch("/api/admin/threshold");
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();

    if (!Array.isArray(data) || data.length === 0) {
      container.innerHTML = "<p>All stocks are sufficient ✅</p>";
      return;
    }

    let html = `<table class="threshold-table"><thead><tr><th>Stock Name</th><th>Quantity</th></tr></thead><tbody>`;
    data.forEach(item => {
      html += `<tr class="low-stock"><td>${item.name}</td><td>${item.quantity}</td></tr>`;
    });
    html += "</tbody></table>";

    container.innerHTML = html;
    if (showPopupFlag) showPopup();
  } catch (err) {
    console.error("Threshold load failed:", err);
    container.innerHTML = "<p>Failed to load threshold data ❌</p>";
  }
}

/* =====================================================
   LOAD STOCK LIST
===================================================== */
let allStocks = [];
async function loadStockList() {
  const tbody = document.getElementById("stockTableBody");
  tbody.innerHTML = "<tr><td colspan='3'>Loading...</td></tr>";

  try {
    const res = await fetch("/api/admin/stocks");
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    allStocks = await res.json();
    renderStockTable(allStocks);

    // Dynamic search filter
    document.getElementById("stockSearch").addEventListener("input", e => {
      const query = e.target.value.toLowerCase();
      const filtered = allStocks.filter(s => s.name.toLowerCase().includes(query));
      renderStockTable(filtered);
    });

  } catch (err) {
    console.error("Stock load failed:", err);
    tbody.innerHTML = "<tr><td colspan='3'>Error loading stock ❌</td></tr>";
  }
}

function renderStockTable(data) {
  const tbody = document.getElementById("stockTableBody");
  tbody.innerHTML = "";
  if (!data.length) {
    tbody.innerHTML = "<tr><td colspan='3'>No stock available</td></tr>";
    return;
  }
  data.forEach((item, idx) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td>${idx + 1}</td><td>${item.name}</td><td>${item.quantity}</td>`;
    tbody.appendChild(tr);
  });
}

/* =====================================================
   ADD NEW STOCK
===================================================== */
document.getElementById("addNewStockBtn").addEventListener("click", async () => {
  const name = document.getElementById("newStockName").value.trim();
  const qty = parseInt(document.getElementById("newStockQty").value, 10);

  if (!name || qty <= 0) return alert("Enter valid stock name and quantity");
  if (allStocks.some(s => s.name.toLowerCase() === name.toLowerCase())) {
    return alert(`❌ Stock "${name}" already exists`);
  }

  try {
    const res = await fetch("/api/admin/add-stock", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, quantity: qty })
    });
    const data = await res.json();
    if (data.status === "failed") return alert(`❌ ${data.message}`);

    alert(`✅ ${data.message}`);
    document.getElementById("newStockName").value = "";
    document.getElementById("newStockQty").value = "";
    await loadStockList();
  } catch (err) {
    console.error("Add stock failed:", err);
    alert("❌ Failed to add stock due to network or server error");
  }
});

/* =====================================================
   MULTI-ITEM ORDER HANDLING
===================================================== */
const orderItems = [];
const orderStockInput = document.getElementById("orderStock");
const orderQtyInput = document.getElementById("orderQty");
const orderTableBody = document.querySelector("#orderItemsTable tbody");
const autocompleteList = document.getElementById("autocomplete-list");

// Add item to order
document.getElementById("addOrderItemBtn").addEventListener("click", () => {
  const stock = orderStockInput.value.trim();
  const quantity = parseInt(orderQtyInput.value, 10);

  if (!stock) return alert("Enter stock name");
  if (!quantity || quantity <= 0) return alert("Enter valid quantity");
  if (!allStocks.some(s => s.name.toLowerCase() === stock.toLowerCase())) {
    return alert(`❌ Stock "${stock}" not found`);
  }
  if (orderItems.some(i => i.stock.toLowerCase() === stock.toLowerCase())) {
    return alert(`❌ Stock "${stock}" already added`);
  }

  orderItems.push({ stock, quantity });
  renderOrderItems();
  orderStockInput.value = "";
  orderQtyInput.value = "";
  autocompleteList.innerHTML = "";
});

function renderOrderItems() {
  orderTableBody.innerHTML = "";
  if (!orderItems.length) {
    orderTableBody.innerHTML = `<tr><td colspan="3" class="no-data">No items added yet</td></tr>`;
    return;
  }
  orderItems.forEach((item, idx) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${item.stock}</td>
      <td>${item.quantity}</td>
      <td>
        <button class="btn btn-danger" onclick="removeOrderItem(${idx})">
          <i class="fas fa-trash"></i>
        </button>
      </td>
    `;
    orderTableBody.appendChild(tr);
  });
}

function removeOrderItem(idx) {
  orderItems.splice(idx, 1);
  renderOrderItems();
}

// Submit multi-item order
document.getElementById("submitOrderBtn").addEventListener("click", async () => {
  const buyer = document.getElementById("buyerName").value.trim();
  const department = document.getElementById("buyerDept").value.trim();
  const institution = document.getElementById("buyerInst").value.trim();

  if (!buyer) return alert("Enter buyer name");
  if (!department) return alert("Enter department");
  if (!institution) return alert("Enter institution");
  if (!orderItems.length) return alert("Add at least one stock item");

  try {
    const res = await fetch("/api/admin/place-order", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ buyer, department, institution, items: orderItems })
    });

    const data = await res.json();
    if (data.status === "failed") return alert(`❌ ${data.message}`);

    alert(`✅ Order placed successfully (Order ID: ${data.order_id})`);
    orderItems.length = 0;
    renderOrderItems();
    await loadStockList();
    await loadThresholdStock(false);
  } catch (err) {
    console.error("Order failed:", err);
    alert("❌ Order placement failed due to network or server error");
  }
});

/* =====================================================
   AUTOCOMPLETE FOR STOCK INPUT
===================================================== */
orderStockInput.addEventListener("input", () => {
  const query = orderStockInput.value.toLowerCase();
  autocompleteList.innerHTML = "";
  if (!query) return;

  const matches = allStocks.filter(s => s.name.toLowerCase().includes(query)).slice(0, 5);
  matches.forEach(match => {
    const div = document.createElement("div");
    div.className = "autocomplete-suggestion";
    div.textContent = match.name;
    div.addEventListener("click", () => {
      orderStockInput.value = match.name;
      autocompleteList.innerHTML = "";
    });
    autocompleteList.appendChild(div);
  });
});

document.addEventListener("click", e => {
  if (e.target !== orderStockInput) autocompleteList.innerHTML = "";
});

/* =====================================================
   PAGE LOAD
===================================================== */
window.addEventListener("DOMContentLoaded", () => {
  loadStockList();
});
