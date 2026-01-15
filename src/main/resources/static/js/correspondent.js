function toggleSidebar() {
            const sidebar = document.querySelector('.sidebar');
            const content = document.querySelector('.main-content');
            sidebar.classList.toggle('hide');
            content.classList.toggle('full');
        }

        function filterTable() {
            const input = document.getElementById("searchInput").value.toUpperCase();
            const table = document.getElementById("stockTable");
            const tr = table.getElementsByTagName("tr");
            for (let i = 1; i < tr.length; i++) {
                const td = tr[i].getElementsByTagName("td")[1];
                tr[i].style.display = td && td.innerText.toUpperCase().indexOf(input) > -1 ? "" : "none";
            }
        }

        async function loadInventory() {
            try {
                const res = await fetch('/api/inventory');
                const data = await res.json();
                const tbody = document.querySelector('#stockTable tbody');
                tbody.innerHTML = '';
                if (data.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="3" style="text-align:center;">No records found in the inventory.</td></tr>';
                    return;
                }
                data.forEach(item => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${item.material_id}</td>
                        <td>${item.stock_name}</td>
                        <td><span class="status-badge">${item.quantity}</span></td>
                    `;
                    tbody.appendChild(tr);
                });
            } catch (err) {
                console.error(err);
                const tbody = document.querySelector('#stockTable tbody');
                tbody.innerHTML = '<tr><td colspan="3" style="color:red; text-align:center;">Error loading inventory.</td></tr>';
            }
        }

        window.onload = loadInventory;
		/**
 * 
 */