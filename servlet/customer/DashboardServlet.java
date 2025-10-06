package servlet.customer;

import auth.AuthHelper;
import db.CustomerDB;
import model.Customer;
import model.Loan;
import model.Transaction;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DashboardServlet {

    public static void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        // Authentication Check (already done in Router, but good practice)
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("customer") == null) {
            resp.sendRedirect(req.getContextPath() + "/customer/login");
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

    // Display dashboard data
    private static void handleGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        Customer customer = (Customer) session.getAttribute("customer");
        String accountNumber = customer.getAccountNumber();

        // Fetch data
        double balance = CustomerDB.getCustomerBalance(accountNumber);
        List<Loan> activeLoans = CustomerDB.getActiveLoansForAccount(accountNumber);
        List<Transaction> transactions = CustomerDB.getTransactionsForAccount(accountNumber);

        // Calculate repayment amounts for loans
        Map<Integer, BigDecimal> loanRepayAmounts = new HashMap<>();
        for (Loan loan : activeLoans) {
            BigDecimal repayAmount = CustomerDB.calculateLoanRepayAmount(loan);
            loanRepayAmounts.put(loan.getId(), repayAmount);
        }

        // Set attributes for JSP
        req.setAttribute("customer", customer); // Re-set just in case, though already in session
        req.setAttribute("balance", balance);
        req.setAttribute("loans", activeLoans);
        req.setAttribute("transactions", transactions);
        req.setAttribute("loanRepayAmounts", loanRepayAmounts);

        // Forward to JSP
        req.getRequestDispatcher("/WEB-INF/pages/customer/dashboard.jsp").forward(req, resp);
    }

    // Handle form submissions from dashboard
    private static void handlePost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        Customer customer = (Customer) session.getAttribute("customer");
        String accountNumber = customer.getAccountNumber();
        String action = req.getParameter("action"); // Hidden field to identify form

        if (action == null) {
            session.setAttribute("errorMessage", "Invalid form submission.");
            resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
            return;
        }

        switch (action) {
            case "transfer":
                handleTransfer(req, resp, session, customer);
                break;
            case "changePassword":
                handleChangePassword(req, resp, session, customer);
                break;
            case "repayLoan":
                handleRepayLoan(req, resp, session, customer);
                break;
            default:
                session.setAttribute("errorMessage", "Unknown action.");
                resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
                break;
        }
    }

    // --- POST Action Handlers ---

    private static void handleTransfer(HttpServletRequest req, HttpServletResponse resp, HttpSession session, Customer customer) throws IOException {
        String toAccountNumber = req.getParameter("toAccountNumber");
        String amountStr = req.getParameter("amount");
        String fromAccountNumber = customer.getAccountNumber();

        // Validation
        if (isBlank(toAccountNumber) || isBlank(amountStr)) {
            session.setAttribute("errorMessage", "Recipient account and amount are required for transfer.");
            resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
            return;
        }
        if (toAccountNumber.equals(fromAccountNumber)) {
             session.setAttribute("errorMessage", "Cannot transfer funds to the same account.");
             resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
             return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            session.setAttribute("errorMessage", "Invalid transfer amount.");
            resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
            return;
        }

        // Check recipient exists
        if (CustomerDB.getCustomerByAccountNumber(toAccountNumber).isEmpty()) {
            session.setAttribute("errorMessage", "Recipient account number not found.");
            resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
            return;
        }

        // Check sufficient balance
        double currentBalance = CustomerDB.getCustomerBalance(fromAccountNumber);
        if (currentBalance < amount) {
            session.setAttribute("errorMessage", "Insufficient funds for transfer.");
            resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
            return;
        }

        // Perform transfer
        boolean success = CustomerDB.performTransfer(fromAccountNumber, toAccountNumber, amount);

        if (success) {
            session.setAttribute("successMessage", String.format("Successfully transferred %.2f to account %s.", amount, toAccountNumber));
        } else {
            session.setAttribute("errorMessage", "Transfer failed. Please try again later.");
        }
        resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
    }

    private static void handleChangePassword(HttpServletRequest req, HttpServletResponse resp, HttpSession session, Customer customer) throws IOException {
        String oldPassword = req.getParameter("oldPassword");
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");
        String accountNumber = customer.getAccountNumber();

        if (isBlank(oldPassword) || isBlank(newPassword) || isBlank(confirmPassword)) {
            session.setAttribute("errorMessage", "All password fields are required.");
            resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            session.setAttribute("errorMessage", "New passwords do not match.");
            resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
            return;
        }

        // Verify old password
        String oldPasswordHash = AuthHelper.hashPassword(oldPassword);
        if (!oldPasswordHash.equals(customer.getPasswordHash())) {
            session.setAttribute("errorMessage", "Incorrect old password.");
            resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
            return;
        }

        // Update password
        String newPasswordHash = AuthHelper.hashPassword(newPassword);
        boolean success = CustomerDB.updateCustomerPassword(accountNumber, newPasswordHash);

        if (success) {
            // Update password hash in session object as well
            session.setAttribute("customer", new Customer(customer.getId(), customer.getName(), customer.getMobile(), customer.getEmail(), customer.getAccountNumber(), newPasswordHash));
            session.setAttribute("successMessage", "Password changed successfully.");
        } else {
            session.setAttribute("errorMessage", "Failed to change password.");
        }
        resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
    }

    private static void handleRepayLoan(HttpServletRequest req, HttpServletResponse resp, HttpSession session, Customer customer) throws IOException {
        String loanIdStr = req.getParameter("loanId");
        String accountNumber = customer.getAccountNumber();

        if (isBlank(loanIdStr)) {
            session.setAttribute("errorMessage", "Loan ID is required for repayment.");
            resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
            return;
        }

        int loanId;
        try {
            loanId = Integer.parseInt(loanIdStr);
        } catch (NumberFormatException e) {
             session.setAttribute("errorMessage", "Invalid Loan ID format.");
             resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
             return;
        }

        // Get loan details to calculate repayment amount
        Optional<Loan> loanOpt = CustomerDB.getLoanById(loanId);
        if(loanOpt.isEmpty() || !loanOpt.get().getAccountNumber().equals(accountNumber)) {
             session.setAttribute("errorMessage", "Loan not found or does not belong to this account.");
             resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
             return;
        }

        Loan loan = loanOpt.get();
        BigDecimal repayAmountDecimal = CustomerDB.calculateLoanRepayAmount(loan);
        double repayAmount = repayAmountDecimal.doubleValue();

         // Check sufficient balance
        double currentBalance = CustomerDB.getCustomerBalance(accountNumber);
        if (currentBalance < repayAmount) {
            session.setAttribute("errorMessage", String.format("Insufficient funds (%.2f) to repay loan ID %d (requires %.2f).", currentBalance, loanId, repayAmount));
            resp.sendRedirect(req.getContextPath() + "/customer/dashboard");
            return;
        }

        // Perform repayment (records transaction)
        boolean success = CustomerDB.repayLoan(accountNumber, loanId, repayAmount);

         if (success) {
            session.setAttribute("successMessage", String.format("Successfully repaid %.2f for loan ID %d.", repayAmount, loanId));
        } else {
            session.setAttribute("errorMessage", "Loan repayment failed. Please try again later.");
        }
        resp.sendRedirect(req.getContextPath() + "/customer/dashboard");

    }

    // Helper method
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}