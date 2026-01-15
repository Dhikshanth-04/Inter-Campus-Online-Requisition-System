document.addEventListener("DOMContentLoaded", () => {

  const toggleIcon = document.getElementById("toggleIcon");
  const passwordInput = document.getElementById("password");

  // Safety check (VERY IMPORTANT)
  if (!toggleIcon || !passwordInput) {
    return;
  }

  // Ensure initial state
  passwordInput.type = "password";
  toggleIcon.classList.remove("fa-eye-slash");
  toggleIcon.classList.add("fa-eye");

  toggleIcon.addEventListener("click", () => {
    const isPassword = passwordInput.type === "password";

    passwordInput.type = isPassword ? "text" : "password";

    toggleIcon.classList.toggle("fa-eye", !isPassword);
    toggleIcon.classList.toggle("fa-eye-slash", isPassword);
  });

});
