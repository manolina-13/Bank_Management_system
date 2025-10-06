package servlet.manager;

import auth.AuthHelper;
import db.ManagerDB;
import model.Staff; // Manager is a type of Staff

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

public class LoginServlet {

    public static void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        if ("GET".equalsIgnoreCase(req.getMethod())) {
            handleGet(req, resp);
        } else if ("POST".equalsIgnoreCase(req.getMethod())) {
            handlePost(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

     private static void handleGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("manager") != null) {
            // Already logged in
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
        } else {
            // Show login page
            req.getRequestDispatcher("/WEB-INF/pages/manager/login.jsp").forward(req, resp);
        }
    }

     private static void handlePost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String mobile = req.getParameter("mobile");
        String password = req.getParameter("password");
        HttpSession session = req.getSession(); // Create session

         if (mobile == null || mobile.trim().isEmpty() || password == null || password.isEmpty()) {
            req.setAttribute("errorMessage", "Mobile number and password are required.");
            req.getRequestDispatcher("/WEB-INF/pages/manager/login.jsp").forward(req, resp);
            return;
        }

        String passwordHash = AuthHelper.hashPassword(password);

        // Validate credentials (checks for role='MANAGER' internally)
        Optional<Staff> managerOpt = ManagerDB.validateManagerLogin(mobile.trim(), passwordHash);

         if (managerOpt.isPresent()) {
            // Login successful
            Staff manager = managerOpt.get();
            session.setAttribute("manager", manager); // Store manager object in session
            session.setMaxInactiveInterval(30 * 60);
             System.out.println("Manager login successful: " + manager.getMobile());
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
        } else {
            // Login failed
            System.out.println("Manager login failed for mobile: " + mobile);
            req.setAttribute("errorMessage", "Invalid mobile number or password.");
            req.getRequestDispatcher("/WEB-INF/pages/manager/login.jsp").forward(req, resp);
        }
    }
}