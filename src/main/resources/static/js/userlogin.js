// ===================== On Page Load =====================
window.addEventListener("DOMContentLoaded", () => {
  const usernameInput = document.getElementById("username");
  const passwordInput = document.getElementById("password");
  const rememberMeCheckbox = document.getElementById("rememberMe");
  const toggleIcon = document.getElementById("toggleIcon");
  const loginForm = document.getElementById("login-form");

  // Load all saved users
  let users = JSON.parse(localStorage.getItem("users") || "{}");

  // When username is typed, autofill password if saved
  usernameInput.addEventListener("input", () => {
    const uname = usernameInput.value.trim();
    if (uname && users[uname]) {
      passwordInput.value = atob(users[uname]); // decode saved password
      rememberMeCheckbox.checked = true;
    } else {
      passwordInput.value = "";
      rememberMeCheckbox.checked = false;
    }
  });

  // Password visibility toggle
  passwordInput.type = "password";
  toggleIcon.classList.remove("fa-eye-slash");
  toggleIcon.classList.add("fa-eye");
  toggleIcon.addEventListener("click", () => {
    const isPassword = passwordInput.type === "password";
    passwordInput.type = isPassword ? "text" : "password";
    toggleIcon.classList.toggle("fa-eye", !isPassword);
    toggleIcon.classList.toggle("fa-eye-slash", isPassword);
  });

  // Handle login submission
  loginForm.addEventListener("submit", e => {
    e.preventDefault();
    validateLogin();
  });
});

// ===================== Validate & Submit Login =====================
function validateLogin() {
  const username = document.getElementById("username").value.trim();
  const password = document.getElementById("password").value.trim();
  const rememberMe = document.getElementById("rememberMe").checked;

  if (!username || !password) {
    alert("Please enter both username and password.");
    return;
  }

  // Submit credentials to server
  fetch("/UserLoginServlet", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ username, password })
  })
    .then(res => {
      if (!res.ok) throw new Error("Network error");
      return res.text();
    })
    .then(result => {
      if (result.trim().toUpperCase() === "SUCCESS") {
        // Save username/password if Remember Me checked
        let users = JSON.parse(localStorage.getItem("users") || "{}");
        if (rememberMe) {
          users[username] = btoa(password); // store encoded
        } else {
          delete users[username]; // remove if unchecked
        }
        localStorage.setItem("users", JSON.stringify(users));

        // Store buyer name for buyer portal
        sessionStorage.setItem("buyerName", username);

        // Redirect
        window.location.href = "/html/buyer.html";
      } else {
        alert("Invalid username or password.");
      }
    })
    .catch(err => {
      console.error("Login error:", err);
      alert("Server error. Please try again later.");
    });
}
