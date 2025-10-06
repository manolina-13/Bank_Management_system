package servlet.customer;

import auth.AuthHelper;
import db.CustomerDB;
import model.Customer;

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
        HttpSession session = req.getSession(false); // Don't create if doesn't exist
        if (session != null && session.getAttribute("customer") != null) {
            // Already logged in, redirect to dashboard
            resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
        } else {
            // Not logged in, show login page
            req.getRequestDispatcher("/WEB-INF/pages/customer/login.jsp").forward(req, resp);
        }
    }

    private static void handlePost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String accountNumber = req.getParameter("accountNumber");
        String password = req.getParameter("password");
        HttpSession session = req.getSession(); // Create session now

        // Basic validation
        if (accountNumber == null || accountNumber.trim().isEmpty() || password == null || password.isEmpty()) {
            req.setAttribute("errorMessage", "Account number and password are required.");
            req.getRequestDispatcher("/WEB-INF/pages/customer/login.jsp").forward(req, resp);
            return;
        }

        // Hash the provided password
        String passwordHash = AuthHelper.hashPassword(password);

        // Validate credentials against DB
        Optional<Customer> customerOpt = CustomerDB.validateCustomerLogin(accountNumber.trim(), passwordHash);

        if (customerOpt.isPresent()) {
            // Login successful
            Customer customer = customerOpt.get();
            session.setAttribute("customer", customer); // Store customer object in session
            session.setMaxInactiveInterval(30 * 60); // Set session timeout (e.g., 30 minutes)
            System.out.println("Customer login successful: " + customer.getAccountNumber());
            resp.sendRedirect(req.getContextPath() + "/customer/dashboard"); // Redirect to dashboard
        } else {
            // Login failed
            System.out.println("Customer login failed for: " + accountNumber);
            req.setAttribute("errorMessage", "Invalid account number or password.");
            req.getRequestDispatcher("/WEB-INF/pages/customer/login.jsp").forward(req, resp);
        }
    }
}