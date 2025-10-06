package servlet.customer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class LogoutServlet {

     public static void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
         // Logout should typically be POST, but handle GET redirect just in case
         if ("POST".equalsIgnoreCase(req.getMethod())) {
             handlePost(req, resp);
         } else {
             resp.sendRedirect(req.getContextPath() + "/customer/login");
         }
     }

    private static void handlePost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        HttpSession session = req.getSession(false); // Get existing session, don't create new
        if (session != null) {
            String accNo = (req.getSession().getAttribute("customer") != null)
                         ? ((model.Customer) req.getSession().getAttribute("customer")).getAccountNumber()
                         : "unknown";
            session.invalidate(); // Invalidate the session
            System.out.println("Customer logged out: " + accNo);
        } else {
             System.out.println("Logout attempt without active session.");
        }
        resp.sendRedirect(req.getContextPath() + "/customer/login"); // Redirect to login page
    }
}