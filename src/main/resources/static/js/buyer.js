/* ================================
   GLOBAL STATE
================================ */
let productsMaster = [];
let rowCount = 0;

/* ================================
   INIT
================================ */
document.addEventListener("DOMContentLoaded", async () => {
  const buyerName = sessionStorage.getItem("buyerName");

  if (!buyerName) {
    alert("Session expired. Please login again.");
    window.location.href = "/login.html";
    return;
  }

  document.getElementById("buyer").value = buyerName;
  document.getElementById("buyerNameDisplay").innerText = `Welcome, ${buyerName}`;

  try {
    await loadProductsFromDB();
    addRow();
    await loadAllBuyerBills(); // load full history
  } catch (e) {
    console.error(e);
    alert("Initialization failed");
  }

  document.addEventListener("keydown", e => {
    if (e.key === "Escape") closeModal();
  });

  window.onclick = e => {
    const modal = document.getElementById("billModal");
    if (e.target === modal) closeModal();
  };
});

/* ================================
   LOAD PRODUCTS
================================ */
async function loadProductsFromDB() {
  const res = await fetch("http://localhost:8081/api/buyer/products");
  if (!res.ok) throw new Error("Failed to load products");

  const data = await res.json();
  productsMaster = data.map(p => ({ name: p.stockName }));
  refreshAllDropdowns();
}

/* ================================
   ROW MANAGEMENT
================================ */
function addRow() {
  const tbody = document.getElementById("itemTableBody");
  rowCount++;

  const tr = document.createElement("tr");
  tr.innerHTML = `
    <td>${rowCount}</td>
    <td><select required onchange="handleProductChange()"></select></td>
    <td><input type="number" min="1" value="1" required oninput="updatePreview()"></td>
    <td><button type="button" onclick="removeRow(this)">Remove</button></td>
  `;
  tbody.appendChild(tr);
  populateDropdown(tr.querySelector("select"));
  updatePreview();
}

function removeRow(btn) {
  btn.closest("tr").remove();
  rowCount--;
  updateRowNumbers();
  applyDuplicateProtection();
  updatePreview();
}

function updateRowNumbers() {
  document.querySelectorAll("#itemTableBody tr").forEach((tr, i) => {
    tr.children[0].innerText = i + 1;
  });
}

/* ================================
   DROPDOWNS
================================ */
function populateDropdown(select) {
  select.innerHTML = `<option value="">-- Select Product --</option>`;
  productsMaster.forEach(p => {
    const opt = document.createElement("option");
    opt.value = p.name;
    opt.textContent = p.name;
    select.appendChild(opt);
  });
  applyDuplicateProtection();
}

function refreshAllDropdowns() {
  document.querySelectorAll("#itemTableBody select").forEach(populateDropdown);
}

function handleProductChange() {
  applyDuplicateProtection();
  updatePreview();
}

function applyDuplicateProtection() {
  const selected = [...document.querySelectorAll("#itemTableBody select")]
    .map(s => s.value)
    .filter(Boolean);

  document.querySelectorAll("#itemTableBody select").forEach(select => {
    [...select.options].forEach(opt => {
      opt.disabled =
        opt.value &&
        selected.includes(opt.value) &&
        opt.value !== select.value;
    });
  });
}

/* ================================
   BILL PREVIEW
================================ */
function updatePreview() {
  const body = document.getElementById("billPreviewBody");
  body.innerHTML = "";

  document.querySelectorAll("#itemTableBody tr").forEach((row, i) => {
    const product = row.querySelector("select").value;
    const qty = row.querySelector("input").value;
    if (!product) return;

    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${i + 1}</td>
      <td>${product}</td>
      <td>${qty}</td>
    `;
    body.appendChild(tr);
  });

  document.getElementById("billPreviewSection").style.display =
    body.children.length ? "block" : "none";
}

/* ================================
   SUBMIT BILL
================================ */
async function submitBill() {
  const buyerName = document.getElementById("buyer").value;
  const department = document.getElementById("dept").value;
  const institution = document.getElementById("insti").value;

  const items = [];
  document.querySelectorAll("#itemTableBody tr").forEach(row => {
    const stockName = row.querySelector("select").value;
    const quantity = parseInt(row.querySelector("input").value, 10);
    if (stockName && quantity > 0) items.push({ stockName, quantity });
  });

  if (!items.length) {
    alert("Add at least one item");
    return;
  }

  const res = await fetch("http://localhost:8081/api/buyer/submit", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ buyerName, department, institution, items })
  });

  if (!res.ok) {
    alert("Failed to submit bill");
    return;
  }

  alert("Bill submitted successfully");

  document.getElementById("itemTableBody").innerHTML = "";
  rowCount = 0;
  addRow();
  updatePreview();

  await loadAllBuyerBills();
}

/* ================================
   LOAD ALL BUYER BILLS
================================ */
async function loadAllBuyerBills() {
  const buyerName = sessionStorage.getItem("buyerName");
  const pendingBody = document.getElementById("pendingBody");
  const deliveredBody = document.getElementById("deliveredBody");

  pendingBody.innerHTML = "";
  deliveredBody.innerHTML = "";

  try {
    const res = await fetch(
      `http://localhost:8081/api/buyer/history/${buyerName}`
    );
    if (!res.ok) throw new Error("Failed to fetch bills");

    const bills = await res.json();

    if (!bills.length) {
      pendingBody.innerHTML =
        `<tr><td colspan="2" align="center">No pending bills</td></tr>`;
      deliveredBody.innerHTML =
        `<tr><td colspan="2" align="center">No delivered bills</td></tr>`;
      return;
    }

    bills.forEach(bill => {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>
          <a href="javascript:void(0)" onclick="showBillDetails(${bill.billId})">
            ${bill.billId}
          </a>
        </td>
        <td>${bill.createdAt ? new Date(bill.createdAt).toLocaleString() : "-"}</td>
      `;

      if (bill.status.toUpperCase() === "PENDING") {
        pendingBody.appendChild(tr);
      } else {
        deliveredBody.appendChild(tr);
      }
    });

  } catch (err) {
    console.error(err);
    pendingBody.innerHTML = deliveredBody.innerHTML =
      `<tr><td colspan="2" align="center">Failed to load bills</td></tr>`;
  }
}

/* ================================
   BILL DETAILS MODAL
================================ */
function showBillDetails(billId) {
  fetch(`http://localhost:8081/api/buyer/bill/${billId}`)
    .then(res => res.json())
    .then(bill => {
      if (bill.buyerName !== sessionStorage.getItem("buyerName")) {
        alert("Unauthorized access");
        return;
      }

      document.getElementById("modalBody").innerHTML = `
        <p><b>Bill ID:</b> ${bill.billId}</p>
        <p><b>Status:</b> ${bill.status}</p>
        <p><b>Created At:</b> ${
          bill.createdAt ? new Date(bill.createdAt).toLocaleString() : "-"
        }</p>
        <hr>
        <ul>
          ${(bill.items || [])
            .map(i => `<li>${i.stockName} - ${i.quantity}</li>`)
            .join("")}
        </ul>
      `;

      document.getElementById("billModal").style.display = "flex";
    })
    .catch(() => alert("Failed to load bill details"));
}

function closeModal() {
  document.getElementById("billModal").style.display = "none";
}

/* ================================
   LOGOUT
================================ */
function logout() {
  sessionStorage.clear();
  window.location.href = "/html/index.html";
}
