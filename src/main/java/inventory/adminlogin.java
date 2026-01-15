package inventory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.sql.*;

@Controller
public class adminlogin {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/admin-login")
    public String loginPage() {
        return "adminlogin"; // Your HTML login page
    }

    @PostMapping("/adminLoginServlet")
    public String login(HttpServletRequest request, HttpServletResponse response) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String remember = request.getParameter("rememberMe");

        System.out.println("Login attempt - username: " + username + ", password: " + password);

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            System.out.println("Username or password empty");
            return "redirect:/html/adminlogin.html?error=empty";
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM user_admin WHERE username=?")) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                System.out.println("User not found: " + username);
                return "redirect:/html/adminlogin.html?error=invalidUser";
            }

            String storedPassword = rs.getString("password");
            System.out.println("Stored password from DB: " + storedPassword);

            // Plain-text check
            if (password.equals(storedPassword)) {
                System.out.println("Password matched for user: " + username);

                HttpSession session = request.getSession();
                session.setAttribute("username", username);

                if ("on".equals(remember)) {
                    Cookie cookie = new Cookie("rememberedUsername", username);
                    cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }

                return "redirect:/html/admin.html";
            } else {
                System.out.println("Password mismatch for user: " + username);
                return "redirect:/html/adminlogin.html?error=invalidPassword";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "redirect:/html/adminlogin.html?error=server";
        }
    }
}
