document.addEventListener("DOMContentLoaded", () => {

    const userBillsList = document.getElementById("userBillsList");
    const adminBillsList = document.getElementById("adminBillsList");
    const billDetails = document.getElementById("billDetails");

    /* ===================== TOAST ===================== */
    function showToast(message, type = "success") {
        const toast = document.createElement("div");
        toast.textContent = message;
        toast.style.cssText = `
            position:fixed;
            top:20px;
            right:20px;
            padding:12px 20px;
            color:#fff;
            border-radius:10px;
            z-index:9999;
            font-weight:600;
            box-shadow:0 10px 25px rgba(0,0,0,.15);
            background:${type === "error"
                ? "linear-gradient(90deg,#dc2626,#b91c1c)"
                : "linear-gradient(90deg,#16a34a,#15803d)"};
        `;
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 2600);
    }

    /* ===================== LOAD ===================== */
    function reloadAll() {
        loadBills("/approval/user-bills", userBillsList, "USER");
        loadBills("/approval/admin-bills", adminBillsList, "ADMIN");
        billDetails.innerHTML = `<p class="muted">Select a bill to view details</p>`;
    }
    reloadAll();

    function loadBills(url, container, source) {
        container.innerHTML = `<p class="muted">Loading ${source} bills...</p>`;

        fetch(url)
            .then(r => r.json())
            .then(json => renderBills(container, json.data, source))
            .catch(() => container.innerHTML = `<p>Error loading ${source} bills</p>`);
    }

    /* ===================== LIST ===================== */
    function renderBills(container, bills, source) {
        container.innerHTML = "";

        if (!bills || bills.length === 0) {
            container.innerHTML = `<p class="muted">No ${source} bills</p>`;
            return;
        }

        bills.forEach(bill => {
            const btn = document.createElement("button");
            btn.className = "bill-btn";
            btn.innerHTML = `
                <span>#${bill.bill_id}</span>
                <small>${bill.buyer_name}</small>
            `;
            btn.onclick = () => showBillDetails(bill, source);
            container.appendChild(btn);
        });
    }

    /* ===================== DETAILS ===================== */
    function showBillDetails(bill, source) {

        const items = Array.isArray(bill.items) ? bill.items : [];

        billDetails.innerHTML = `
            <h3>Bill #${bill.bill_id}</h3>

            <div class="meta">
                <p><b>Buyer:</b> ${bill.buyer_name}</p>
                <p><b>Department:</b> ${bill.department || "-"}</p>
                <p><b>Institution:</b> ${bill.institution || "-"}</p>
            </div>

            <table>
                <thead>
                    <tr>
                        <th>Stock</th>
                        <th>Qty</th>
                    </tr>
                </thead>
                <tbody>
                    ${
                        items.length === 0
                            ? `<tr><td colspan="2">No items</td></tr>`
                            : items.map(i => `
                                <tr>
                                    <td>${i.stock_name}</td>
                                    <td>${i.quantity}</td>
                                </tr>
                            `).join("")
                    }
                </tbody>
            </table>

            <!-- APPROVE / REJECT ALWAYS SHOWN -->
            <div class="actions">
                <button id="approveBtn" class="approve">Approve</button>
                <button id="rejectBtn" class="reject">Reject</button>
            </div>

            <div id="rejectBox" class="reject-box" style="display:none">
                <textarea id="rejectMsg"
                    placeholder="Enter rejection reason (required)">
                </textarea>
                <button id="confirmReject" class="reject">
                    Confirm Rejection
                </button>
            </div>
        `;

        const approveBtn = document.getElementById("approveBtn");
        const rejectBtn = document.getElementById("rejectBtn");
        const rejectBox = document.getElementById("rejectBox");

        approveBtn.onclick = () => handleUpdate("APPROVED", "");
        rejectBtn.onclick = () => rejectBox.style.display = "block";

        document.getElementById("confirmReject").onclick = () => {
            const msg = document.getElementById("rejectMsg").value.trim();
            if (!msg) {
                showToast("Rejection reason required", "error");
                return;
            }
            handleUpdate("REJECTED", msg);
        };

        function handleUpdate(status, message) {
            approveBtn.disabled = true;
            rejectBtn.disabled = true;
            approveBtn.textContent = "Processing...";
            updateStatus(bill.bill_id, status, message, source);
        }
    }

    /* ===================== UPDATE ===================== */
    function updateStatus(billId, status, message, source) {
        fetch("/approval/update", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                bill_id: billId,
                status: status,
                message: message,
                source: source
            })
        })
        .then(r => r.json())
        .then(res => {
            if (res.status === "success") {
                showToast(`Bill ${billId} ${status}`);
                reloadAll();
            } else {
                showToast(res.message || "Update failed", "error");
            }
        })
        .catch(() => showToast("Server error", "error"));
    }
});
