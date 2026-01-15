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
public class CorrespondentLoginController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/correspondent-login")
    public String loginPage() {
        return "correspondentlogin"; // if using Thymeleaf
        // OR return "redirect:/html/correspondentlogin.html";
    }

    @PostMapping("/CorrespondentLoginServlet")
    public String login(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String remember = request.getParameter("rememberMe");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM user_admin WHERE username=? AND password=?")) {

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                HttpSession session = request.getSession();
                session.setAttribute("username", username);

                if ("on".equals(remember)) {
                    Cookie cookie = new Cookie("rememberedUsername", username);
                    cookie.setMaxAge(7 * 24 * 60 * 60);
                    response.addCookie(cookie);
                }

                return "redirect:/html/correspondent.html";
            } else {
                return "redirect:/html/correspondentlogin.html?error=1";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/html/correspondentlogin.html?error=server";
        }
    }
}
