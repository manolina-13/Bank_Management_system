package servlet.staff;

import auth.AuthHelper;
import db.StaffDB;
import model.Staff;

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
        if (session != null && session.getAttribute("staff") != null) {
            // Already logged in, redirect to dashboard
            resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
        } else {
            // Not logged in, show login page
            req.getRequestDispatcher("/WEB-INF/pages/staff/login.jsp").forward(req, resp);
        }
    }

    private static void handlePost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Using mobile number for login as per simplified schema/model
        String mobile = req.getParameter("mobile");
        String password = req.getParameter("password");
        HttpSession session = req.getSession(); // Create session

        if (mobile == null || mobile.trim().isEmpty() || password == null || password.isEmpty()) {
            req.setAttribute("errorMessage", "Mobile number and password are required.");
            req.getRequestDispatcher("/WEB-INF/pages/staff/login.jsp").forward(req, resp);
            return;
        }

        String passwordHash = AuthHelper.hashPassword(password);

        // Validate credentials (checks for role='STAFF' internally)
        Optional<Staff> staffOpt = StaffDB.validateStaffLogin(mobile.trim(), passwordHash);

        if (staffOpt.isPresent()) {
            // Login successful
            Staff staff = staffOpt.get();
            session.setAttribute("staff", staff); // Store staff object in session
            session.setMaxInactiveInterval(30 * 60);
            System.out.println("Staff login successful: " + staff.getMobile());
            resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
        } else {
            // Login failed
            System.out.println("Staff login failed for mobile: " + mobile);
            req.setAttribute("errorMessage", "Invalid mobile number or password.");
            req.getRequestDispatcher("/WEB-INF/pages/staff/login.jsp").forward(req, resp);
        }
    }
}