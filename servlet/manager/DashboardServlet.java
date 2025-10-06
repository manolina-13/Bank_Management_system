package servlet.manager;

import auth.AuthHelper;
import db.ManagerDB;
import db.CustomerDB; // Needed to check if customer exists for loan
import model.Staff; // Manager is Staff

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class DashboardServlet {

    public static void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        // Auth Check
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("manager") == null) {
            resp.sendRedirect(req.getContextPath() + "/manager/login");
            return;
        }

        if ("GET".equalsIgnoreCase(req.getMethod())) {
            handleGet(req, resp);
        } else if ("POST".equalsIgnoreCase(req.getMethod())) {
            handlePost(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    private static void handleGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Forward to dashboard JSP
        System.out.println("Dashboard servlet GET Handler.");
        req.getRequestDispatcher("/WEB-INF/pages/manager/dashboard.jsp").forward(req, resp);
    }

    private static void handlePost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession();
        Staff manager = (Staff) session.getAttribute("manager"); // Already verified non-null
        String action = req.getParameter("action");

        if (action == null) {
            session.setAttribute("errorMessage", "Invalid form submission.");
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
            return;
        }

        switch (action) {
            case "createLoan":
                handleCreateLoan(req, resp, session);
                break;
            case "changeName":
                handleChangeName(req, resp, session, manager);
                break;
            case "changeMobile":
                handleChangeMobile(req, resp, session, manager);
                break;
            case "changePassword":
                handleChangePassword(req, resp, session, manager);
                break;
            default:
                session.setAttribute("errorMessage", "Unknown manager action.");
                resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
                break;
        }
    }

    // --- POST Action Handlers ---

    private static void handleCreateLoan(HttpServletRequest req, HttpServletResponse resp, HttpSession session)
            throws IOException {
        String accountNumber = req.getParameter("accountNumber");
        String amountStr = req.getParameter("amount");
        String rateStr = req.getParameter("interestRate");
        String yearsStr = req.getParameter("years");

        // Validation
        if (isBlank(accountNumber) || isBlank(amountStr) || isBlank(rateStr) || isBlank(yearsStr)) {
            session.setAttribute("errorMessage", "All fields are required to create a loan.");
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
            return;
        }

        double amount, rate;
        int years;
        try {
            amount = Double.parseDouble(amountStr);
            rate = Double.parseDouble(rateStr);
            years = Integer.parseInt(yearsStr);
            if (amount <= 0 || rate < 0 || years <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            session.setAttribute("errorMessage",
                    "Invalid loan parameters (Amount/Rate/Years must be positive numbers, Rate can be 0).");
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
            return;
        }

        // Check if customer account exists before attempting to create loan
        if (CustomerDB.getCustomerByAccountNumber(accountNumber).isEmpty()) {
            session.setAttribute("errorMessage",
                    "Cannot create loan: Customer account " + accountNumber + " not found.");
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
            return;
        }

        boolean success = ManagerDB.createLoan(accountNumber, amount, rate, years);

        if (success) {
            session.setAttribute("successMessage", "Loan created successfully for account " + accountNumber + ".");
        } else {
            if (session.getAttribute("errorMessage") == null) { // Avoid overwriting specific DB errors
                session.setAttribute("errorMessage", "Failed to create loan.");
            }
        }
        resp.sendRedirect(req.getContextPath() + "/manager/dashboard");

    }

    private static void handleChangeName(HttpServletRequest req, HttpServletResponse resp, HttpSession session,
            Staff manager) throws IOException {
        String newName = req.getParameter("newName");
        String mobile = manager.getMobile(); // Use current manager's mobile

        if (isBlank(newName)) {
            session.setAttribute("errorMessage", "New name cannot be empty.");
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
            return;
        }

        boolean success = ManagerDB.updateManagerName(mobile, newName.trim());

        if (success) {
            // Update name in session object
            session.setAttribute("manager", new Staff(manager.getId(), newName.trim(), manager.getMobile(),
                    manager.getRole(), manager.getPasswordHash()));
            session.setAttribute("successMessage", "Your name updated successfully.");
        } else {
            session.setAttribute("errorMessage", "Failed to update your name.");
        }
        resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
    }

    private static void handleChangeMobile(HttpServletRequest req, HttpServletResponse resp, HttpSession session,
            Staff manager) throws IOException {
        String newMobile = req.getParameter("newMobile");
        String oldMobile = manager.getMobile();

        if (isBlank(newMobile) || !newMobile.matches("\\d{10}")) { // Basic 10 digit check
            session.setAttribute("errorMessage", "New mobile number must be 10 digits.");
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
            return;
        }

        if (newMobile.trim().equals(oldMobile)) {
            session.setAttribute("errorMessage", "New mobile number is the same as the old one.");
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
            return;
        }

        // DB method includes check for existing mobile
        boolean success = ManagerDB.updateManagerMobile(oldMobile, newMobile.trim());

        if (success) {
            // Update mobile in session object
            session.setAttribute("manager", new Staff(manager.getId(), manager.getName(), newMobile.trim(),
                    manager.getRole(), manager.getPasswordHash()));
            session.setAttribute("successMessage",
                    "Your mobile number updated successfully. Please use the new mobile number to log in next time.");
        } else {
            if (session.getAttribute("errorMessage") == null) {
                session.setAttribute("errorMessage", "Failed to update mobile number. It might already be in use.");
            }
        }
        resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
    }

    private static void handleChangePassword(HttpServletRequest req, HttpServletResponse resp, HttpSession session,
            Staff manager) throws IOException {
        String oldPassword = req.getParameter("oldPassword");
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");
        String mobile = manager.getMobile(); // Login identifier

        if (isBlank(oldPassword) || isBlank(newPassword) || isBlank(confirmPassword)) {
            session.setAttribute("errorMessage", "All password fields are required.");
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            session.setAttribute("errorMessage", "New passwords do not match.");
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
            return;
        }

        // Verify old password
        String oldPasswordHash = AuthHelper.hashPassword(oldPassword);
        if (!oldPasswordHash.equals(manager.getPasswordHash())) {
            session.setAttribute("errorMessage", "Incorrect old password.");
            resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
            return;
        }

        // Update password
        String newPasswordHash = AuthHelper.hashPassword(newPassword);
        boolean success = ManagerDB.updateManagerPassword(mobile, newPasswordHash);

        if (success) {
            // Update password hash in session object
            session.setAttribute("manager", new Staff(manager.getId(), manager.getName(), manager.getMobile(),
                    manager.getRole(), newPasswordHash));
            session.setAttribute("successMessage", "Password changed successfully.");
        } else {
            session.setAttribute("errorMessage", "Failed to change password.");
        }
        resp.sendRedirect(req.getContextPath() + "/manager/dashboard");
    }

    // Helper method
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

}