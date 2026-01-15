package inventory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.sql.*;

@Controller
public class userlogin {

    @Autowired
    private DataSource dataSource;

    // ================= LOGIN PAGE =================
    @GetMapping("/user-login")
    public String loginPage() {
        return "userlogin"; // Thymeleaf page
    }

    // ================= LOGIN PROCESS =================
    @PostMapping("/UserLoginServlet")
    @ResponseBody
    public String login(HttpServletRequest request, HttpServletResponse response) {

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String remember = request.getParameter("rememberMe");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM signup WHERE username=? AND password=?")) {

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Create session
                HttpSession session = request.getSession(true);
                session.setAttribute("username", username);

                // Remember me cookie
                if ("true".equals(remember)) {
                    Cookie cookie = new Cookie("rememberedUsername", username);
                    cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }

                return "SUCCESS"; // ✅ IMPORTANT for JS
            } else {
                return "FAIL";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }
}
