$(document).ready(() => {
    const tableBody = $("#stockTable tbody");
    const itemsContainer = $("#items_container");

    // ================= ADD ITEM ROW =================
    function addItemRow() {
        const itemRow = $(`
            <div class="item-row">
                <input type="text" class="stock_name" placeholder="Stock Name" autocomplete="off" required>
                <div class="suggestions"></div>
                <input type="number" class="quantity" placeholder="Quantity" required>
                <input type="number" class="amount" placeholder="Amount" required>
                <button type="button" class="remove-item">X</button>
            </div>
        `);

        // Remove item
        itemRow.find(".remove-item").click(() => itemRow.remove());

        // Autocomplete with duplicate prevention
        itemRow.find(".stock_name").on("input", async function () {
            const query = $(this).val().trim();
            const suggestionBox = $(this).siblings(".suggestions");
            suggestionBox.empty();
            if (!query) return;

            try {
                const res = await fetch(`/purchase/search-stock?query=${query}`);
                const data = await res.json();

                // Existing stock names in current PO
                const existingStocks = $(".item-row .stock_name").map(function () {
                    return $(this).val().trim().toLowerCase();
                }).get();

                data.forEach(name => {
                    if (existingStocks.includes(name.toLowerCase())) return;
                    const div = $(`<div class="suggestion-item">${name}</div>`);
                    div.click(() => {
                        $(this).val(name);
                        suggestionBox.empty();
                    });
                    suggestionBox.append(div);
                });
            } catch (e) {
                console.error(e);
            }
        });

        itemsContainer.append(itemRow);
    }

    $("#add_item").click(addItemRow);
    addItemRow(); // Add first row by default

    // ================= LOAD PURCHASES =================
    async function loadPurchases() {
        tableBody.html('<tr class="no-data"><td colspan="6">Loading...</td></tr>');
        try {
            const res = await fetch("/purchase/all");
            const data = await res.json();
            tableBody.empty();
            if (!data.length) {
                tableBody.html('<tr class="no-data"><td colspan="6">No records</td></tr>');
                return;
            }
            data.forEach(s => {
                tableBody.append(`
                    <tr>
                        <td>${s.po_no}</td>
                        <td>${s.stock_name}</td>
                        <td>${s.date}</td>
                        <td>${s.quantity}</td>
                        <td>${s.amount}</td>
                        <td>${s.store_name}</td>
                    </tr>
                `);
            });
        } catch (e) {
            console.error(e);
            tableBody.html('<tr class="no-data"><td colspan="6">Failed to load</td></tr>');
        }
    }

    // ================= SUBMIT FORM =================
    $("#stockForm").submit(async (e) => {
        e.preventDefault();

        const po_no = parseInt($("#po_no").val());
        const date = $("#date").val();
        const store_name = $("#store_name").val();

        const items = [];
        const stockSet = new Set();
        let hasDuplicate = false;

        $(".item-row").each(function () {
            const stock_name = $(this).find(".stock_name").val().trim();
            const quantity = parseInt($(this).find(".quantity").val());
            const amount = parseInt($(this).find(".amount").val());

            if (stock_name && quantity && amount) {
                if (stockSet.has(stock_name.toLowerCase())) {
                    hasDuplicate = true;
                    return false; // break loop
                }
                stockSet.add(stock_name.toLowerCase());
                items.push({ stock_name, quantity, amount });
            }
        });

        if (hasDuplicate) {
            alert("Duplicate stock names are not allowed in a single PO!");
            return;
        }

        if (items.length === 0) {
            alert("Add at least one item");
            return;
        }

        try {
            const res = await fetch("/purchase/add-po", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ po_no, date, store_name, items })
            });

            const data = await res.json();
            if (data.status === "success") {
                alert("Purchase Order saved!");
                $("#stockForm")[0].reset();
                itemsContainer.empty();
                addItemRow();
                loadPurchases();
            } else {
                alert("Failed: " + (data.message || ""));
            }
        } catch (e) {
            console.error(e);
            alert("Server error while saving PO");
        }
    });

    loadPurchases();
});
