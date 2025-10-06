package servlet.customer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomerRouter extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        route(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        route(req, resp);
    }

    private void route(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String path = req.getPathInfo();
        String contextPath = req.getContextPath();

        // Basic Auth Check Filter (Simplified - Check if session exists)
        // More robust filter would be separate, but for simplicity, check in router/servlets
        boolean isLoggedIn = req.getSession(false) != null && req.getSession(false).getAttribute("customer") != null;

        // Allow access to login regardless of session state
        if ("/login".equals(path)) {
            LoginServlet.handle(req, resp);
            return;
        }

        // Redirect to login if trying to access protected pages without being logged in
        if (!isLoggedIn && !"/login".equals(path)) {
            System.out.println("CustomerRouter: Not logged in, redirecting to login.");
            resp.sendRedirect(contextPath + "/customer/login");
            return;
        }

        // Route logged-in users
        if (path == null || path.equals("/") || "/dashboard".equals(path)) {
             DashboardServlet.handle(req, resp); // Dashboard handles multiple actions via POST
        } else if ("/grievance".equals(path)) {
            GrievanceServlet.handle(req, resp);
        } else if ("/logout".equals(path)) {
            LogoutServlet.handle(req, resp);
        } else {
            System.out.println("CustomerRouter: Path not found: " + path);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}