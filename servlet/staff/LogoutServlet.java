package servlet.staff;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class LogoutServlet {

     public static void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
         if ("POST".equalsIgnoreCase(req.getMethod())) {
             handlePost(req, resp);
         } else {
             resp.sendRedirect(req.getContextPath() + "/staff/login");
         }
     }

    private static void handlePost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        HttpSession session = req.getSession(false);
        if (session != null) {
             String mobile = (req.getSession().getAttribute("staff") != null)
                         ? ((model.Staff) req.getSession().getAttribute("staff")).getMobile()
                         : "unknown";
            session.invalidate();
             System.out.println("Staff logged out: " + mobile);
        } else {
            System.out.println("Staff logout attempt without active session.");
        }
        resp.sendRedirect(req.getContextPath() + "/staff/login");
    }
}