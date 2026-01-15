window.onload = () => {
    loadBuyerBills();
    loadAdminOrders();
};

/* ================= LOAD BUYER BILLS ================= */
async function loadBuyerBills() {
    try {
        const res = await fetch("http://localhost:8081/bills");
        const data = await res.json();
        if (!data) return;

        fillTable("userApproved", data, "APPROVED");
        fillTable("userRejected", data, "REJECTED");
        fillTable("userDelivered", data, "DELIVERED");
    } catch (err) {
        console.error("Error loading buyer bills:", err);
    }
}

/* ================= LOAD ADMIN ORDERS ================= */
async function loadAdminOrders() {
    try {
        const res = await fetch("http://localhost:8081/orders");
        const data = await res.json();
        if (!data) return;

        fillTable("adminApproved", data, "APPROVED");
        fillTable("adminRejected", data, "REJECTED");
    } catch (err) {
        console.error("Error loading admin orders:", err);
    }
}

/* ================= FILL TABLE BY STATUS ================= */
function fillTable(tableId, data, status) {
    const table = document.getElementById(tableId);
    if (!table) {
        console.error(`Table with ID "${tableId}" not found`);
        return;
    }

    table.innerHTML = "";

    const filteredData = data.filter(item => item.status === status);

    if (filteredData.length === 0) {
        const colspan = status === "REJECTED" ? 9 : 8;
        table.innerHTML = `<tr><td colspan="${colspan}" class="no-data">No records found</td></tr>`;
        return;
    }

    filteredData.forEach(item => {
        const id = item.billId ?? item.orderId ?? "-";
        const created = item.createdAt ? new Date(item.createdAt).toLocaleString() : "-";
        const messageCell = status === "REJECTED" ? `<td>${item.message || "-"}</td>` : "";

        table.innerHTML += `
            <tr>
                <td>${id}</td>
                <td>${item.buyerName || "-"}</td>
                <td>${item.department || "-"}</td>
                <td>${item.institution || "-"}</td>
                <td>${item.stockName || "-"}</td>
                <td>${item.quantity ?? "-"}</td>
                <td class="${status.toLowerCase()}">${item.status || "-"}</td>
                ${messageCell}
                <td>${created}</td>
            </tr>
        `;
    });
}

/* ===================== PRINT FUNCTION ===================== */
function printSection(sectionId) {
    const section = document.getElementById(sectionId);
    if (!section) {
        alert(`Section "${sectionId}" not found`);
        return;
    }

    // Clone the section to avoid modifying original DOM
    const clone = section.cloneNode(true);

    // Remove buttons from clone
    clone.querySelectorAll('button, .btn-print').forEach(el => el.remove());

    // Add table heading from h3
    const headingText = section.closest('.bill-section')?.querySelector('h3')?.innerText || '';
    if (headingText) {
        const heading = document.createElement('h3');
        heading.innerText = headingText;
        clone.insertBefore(heading, clone.firstChild);
    }

    // Fix columns: hide only "Message" if it exists
    const table = clone.querySelector('table');
    if (table) {
        const ths = table.querySelectorAll('th');
        const messageIndex = Array.from(ths).findIndex(th => th.innerText.toLowerCase() === 'message');

        if (messageIndex !== -1) {
            // Hide Message column
            ths[messageIndex].style.display = 'none';
            table.querySelectorAll('tbody tr').forEach(tr => {
                const tds = tr.querySelectorAll('td');
                if (tds[messageIndex]) tds[messageIndex].style.display = 'none';
            });
        }
    }

    // Open print window
    const printWindow = window.open('', '', 'width=1200,height=900');
    printWindow.document.write(`
        <html>
        <head>
            <title>Print Records</title>
            <link rel="stylesheet" href="/css/records.css">
            <style>
                body {
                    background: #fff;
                    color: #000;
                    font-family: 'Inter', sans-serif;
                    margin: 20px;
                    font-size: 12pt;
                }
                h3 { margin: 12px 0; font-weight: 600; color: #000; }
                table { width: 100%; border-collapse: collapse; margin-bottom: 24px; }
                th, td { border: 1px solid #000; padding: 6px 8px; text-align: center; word-break: break-word; }
                thead th { background-color: #f3f4f6 !important; -webkit-print-color-adjust: exact; font-weight: 600; }
                tr { page-break-inside: avoid; page-break-after: auto; }
                .no-data { text-align: center; font-style: normal; color: #000; }
                button, .btn-print { display: none !important; }
            </style>
        </head>
        <body>
            ${clone.outerHTML}
        </body>
        </html>
    `);
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
    printWindow.close();
}
