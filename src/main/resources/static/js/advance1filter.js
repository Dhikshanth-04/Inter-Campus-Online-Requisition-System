$(document).ready(function () {

    // ===================== Populate Dropdowns =====================
    function populateDropdown(selectId, apiUrl, placeholder) {
        const $select = $(selectId);
        $select.html(`<option value="">-- Select ${placeholder} --</option>`);

        $.ajax({
            url: apiUrl,
            type: "GET",
            success: function (data) {
                if (!Array.isArray(data)) return;
                data.forEach(value => {
                    $select.append(`<option value="${value}">${value}</option>`);
                });
            },
            error: function () {
                console.error(`Failed to load ${placeholder}`);
            }
        });
    }

    // Initialize dropdowns
    populateDropdown("#buyerSelect", "/inventory/buyers", "Buyer");
    populateDropdown("#deptSelect", "/inventory/departments", "Department");
    populateDropdown("#instiSelect", "/inventory/institutions", "Institution");
    populateDropdown("#stockSelect", "/inventory/stocks", "Stock");

    // ===================== Update Selected Filters =====================
    function updateSelectedFilters() {
        const filters = [];
        const buyer = $("#buyerSelect").val();
        const dept = $("#deptSelect").val();
        const insti = $("#instiSelect").val();
        const stock = $("#stockSelect").val();
        const particular_date = $("#particular_date").val();
        const from_date = $("#from_date").val();
        const to_date = $("#to_date").val();

        if (buyer) filters.push(`Buyer: ${buyer}`);
        if (dept) filters.push(`Department: ${dept}`);
        if (insti) filters.push(`Institution: ${insti}`);
        if (stock) filters.push(`Stock: ${stock}`);
        if (particular_date) filters.push(`Date: ${particular_date}`);
        if (from_date && to_date) filters.push(`From: ${from_date}, To: ${to_date}`);

        $("#selectedFilters").text(filters.length ? "Selected Filters: " + filters.join(" | ") : "Selected Filters: None");
    }

    // ===================== Render Table =====================
    function renderTable(data) {
        let html = `
            <table id="printableTable" border="1" cellspacing="0" cellpadding="5" style="width:100%; text-align:center;">
                <thead>
                    <tr>
                        <th>Buyer</th>
                        <th>Department</th>
                        <th>Institution</th>
                        <th>Stock</th>
                        <th>Quantity</th>
                        <th>Date</th>
                    </tr>
                </thead>
                <tbody>
        `;

        if (!data || data.length === 0) {
            html += `<tr><td colspan="6">No records found</td></tr>`;
        } else {
            data.forEach(item => {
                html += `
                    <tr>
                        <td>${item.buyer || '-'}</td>
                        <td>${item.department || '-'}</td>
                        <td>${item.institution || '-'}</td>
                        <td>${item.stock_name || '-'}</td>
                        <td>${item.quantity != null ? item.quantity : '-'}</td>
                        <td>${item.delivery_date || '-'}</td>
                    </tr>
                `;
            });
        }

        html += `</tbody></table>`;
        $("#resultArea").html(`<div id="selectedFilters">${$("#selectedFilters").text()}</div>` + html);
    }

    // ===================== Filter Inventory =====================
    window.filterData = function () {
        updateSelectedFilters();
        const formData = $("#filterForm").serialize();

        $.ajax({
            url: "/inventory/filter",
            type: "GET",
            data: formData,
            success: function (res) {
                renderTable(res);
            },
            error: function () {
                $("#resultArea").html('<p style="text-align:center; color:red;">Failed to load data. Please try again.</p>');
            }
        });
    };

    // ===================== Reset Filter Form =====================
    $("#resetBtn").on("click", function () {
        $("#filterForm")[0].reset();
        $("#buyerSelect, #deptSelect, #instiSelect, #stockSelect").prop("selectedIndex", 0);
        $("#selectedFilters").text("Selected Filters: None");
        renderTable([]);
    });

    // ===================== Initial Table Load =====================
    renderTable([]); // Show empty table on page load

});

// ===================== PRINT DYNAMIC FILTER RESULTS =====================
window.printResults = function () {
    const resultArea = document.getElementById("resultArea");
    if (!resultArea) {
        alert("No results to print.");
        return;
    }

    // Clone result area (including filters and table)
    const clone = resultArea.cloneNode(true);

    // Remove buttons or inputs if any
    clone.querySelectorAll('button, input').forEach(el => el.remove());

    // Open print window
    const printWindow = window.open('', '', 'width=1200,height=900');
    printWindow.document.write(`
        <html>
        <head>
            <title>Print Inventory Filter Results</title>
            <link rel="stylesheet" href="/css/advance1filter.css">
            <style>
                body { font-family: 'Inter', sans-serif; margin: 20px; font-size: 12pt; color: #000; }
                #selectedFilters { font-weight: 600; margin-bottom: 12px; }
                table { width: 100%; border-collapse: collapse; margin-bottom: 24px; }
                th, td { border: 1px solid #000; padding: 6px 8px; text-align: center; }
                thead th { background-color: #f3f4f6 !important; -webkit-print-color-adjust: exact; font-weight: 600; }
                tr { page-break-inside: avoid; page-break-after: auto; }
                .no-data { text-align: center; font-style: normal; color: #000; }
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
};
