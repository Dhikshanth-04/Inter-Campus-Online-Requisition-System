document.addEventListener("DOMContentLoaded", () => {
  const poSelect = document.getElementById("poSelect");
  const stockSelect = document.getElementById("stockSelect");
  const storeSelect = document.getElementById("storeSelect");
  const fromDateInput = document.getElementById("from_date");
  const toDateInput = document.getElementById("to_date");
  const resultsTable = document.querySelector("#resultsTable tbody");
  const filtersText = document.getElementById("filtersText");
  const printBtn = document.getElementById("printBtn");
  const resetBtn = document.getElementById("resetBtn");
  const form = document.getElementById("filterForm");

  printBtn.style.display = "none";

  // Populate dropdown with data and placeholder
  function populateDropdown(select, data, selectedValue, placeholder) {
    select.innerHTML = `<option value="">${placeholder}</option>`;
    data.forEach(item => {
      const option = document.createElement("option");
      option.value = item;
      option.textContent = item;
      if (item == selectedValue) option.selected = true;
      select.appendChild(option);
    });
  }

  // Get current filters from URL
  function getCurrentFilters() {
    const urlParams = new URLSearchParams(window.location.search);
    return {
      po_no: urlParams.get('po_no') || '',
      stock_name: urlParams.get('stock_name') || '',
      store_name: urlParams.get('store_name') || '',
      from_date: urlParams.get('from_date') || '',
      to_date: urlParams.get('to_date') || ''
    };
  }

  // Load dropdowns from backend
  async function loadDropdowns(currentFilters) {
    try {
      const [poNumbers, stockNames, storeNames] = await Promise.all([
        fetch("/api/inward1filter/po_numbers").then(res => res.json()),
        fetch("/api/inward1filter/stocknames").then(res => res.json()),
        fetch("/api/inward1filter/storename").then(res => res.json())
      ]);

      populateDropdown(poSelect, poNumbers, currentFilters.po_no, "Select PO Number");
      populateDropdown(stockSelect, stockNames, currentFilters.stock_name, "Select Stock Name");
      populateDropdown(storeSelect, storeNames, currentFilters.store_name, "Select Store Name");
    } catch (err) {
      console.error("Error loading dropdowns:", err);
    }
  }

  // Generate text for selected filters
  function generateFilterText() {
    const filters = [];
    if (poSelect.value) filters.push(`PO Number: ${poSelect.value}`);
    if (stockSelect.value) filters.push(`Stock Name: ${stockSelect.value}`);
    if (storeSelect.value) filters.push(`Store Name: ${storeSelect.value}`);
    if (fromDateInput.value) filters.push(`From Date: ${fromDateInput.value}`);
    if (toDateInput.value) filters.push(`To Date: ${toDateInput.value}`);
    return filters.length ? filters.join(' | ') : "No filters applied.";
  }

  // Load inventory based on filters
  async function loadInventory() {
    try {
      const params = new URLSearchParams();
      if (poSelect.value) params.append('po_no', poSelect.value);
      if (stockSelect.value) params.append('stock_name', stockSelect.value);
      if (storeSelect.value) params.append('store_name', storeSelect.value);
      if (fromDateInput.value) params.append('from_date', fromDateInput.value);
      if (toDateInput.value) params.append('to_date', toDateInput.value);

      const res = await fetch("/api/inward1filter?" + params.toString());
      const data = await res.json();

      resultsTable.innerHTML = "";
      if (!data.length) {
        resultsTable.innerHTML = "<tr><td colspan='6' style='text-align:center;'>No records found</td></tr>";
        filtersText.textContent = "No records to display.";
        printBtn.style.display = "none";
        return;
      }

      data.forEach(item => {
        const row = document.createElement("tr");
        row.innerHTML = `
          <td>${item.po_no}</td>
          <td>${item.stock_name}</td>
          <td>${item.store_name}</td>
          <td>${item.date}</td>
          <td>${item.quantity}</td>
          <td>${item.amount}</td>
        `;
        resultsTable.appendChild(row);
      });

      filtersText.textContent = generateFilterText();
      printBtn.style.display = "block";
    } catch (err) {
      console.error("Error loading inventory:", err);
      resultsTable.innerHTML = "<tr><td colspan='6' style='text-align:center;'>Error loading data</td></tr>";
    }
  }

  // Form submission
  form.addEventListener("submit", e => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (poSelect.value) params.append('po_no', poSelect.value);
    if (stockSelect.value) params.append('stock_name', stockSelect.value);
    if (storeSelect.value) params.append('store_name', storeSelect.value);
    if (fromDateInput.value) params.append('from_date', fromDateInput.value);
    if (toDateInput.value) params.append('to_date', toDateInput.value);
    window.history.pushState({}, '', '?' + params.toString());
    loadInventory();
  });

  // Reset button
  resetBtn.addEventListener("click", () => {
    form.reset();
    resultsTable.innerHTML = "<tr><td colspan='6' style='text-align:center;'>No records found</td></tr>";
    filtersText.textContent = "";
    printBtn.style.display = "none";
    window.history.pushState({}, '', window.location.pathname);
  });

  // Print button
  printBtn.addEventListener("click", () => window.print());

  // Initialize page
  const currentFilters = getCurrentFilters();
  loadDropdowns(currentFilters).then(loadInventory);
});
// Push a dummy state on page load
history.pushState(null, null, location.href);

// Listen for popstate events (back or forward)
window.addEventListener('popstate', function (event) {
    // Push the state again to prevent navigation
    history.pushState(null, null, location.href);
});
