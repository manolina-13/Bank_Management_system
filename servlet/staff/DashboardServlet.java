package servlet.staff;

import auth.AuthHelper;
import db.CustomerDB;
import db.StaffDB;
import model.Staff;
import model.Customer; // Needed for checks

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;


public class DashboardServlet {

    public static void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        // Auth Check
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("staff") == null) {
            resp.sendRedirect(req.getContextPath() + "/staff/login");
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

    private static void handleGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Staff object is already in session, just forward to JSP
        req.getRequestDispatcher("/WEB-INF/pages/staff/dashboard.jsp").forward(req, resp);
    }

     private static void handlePost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        Staff staff = (Staff) session.getAttribute("staff"); // Already verified non-null
        String action = req.getParameter("action");

        if (action == null) {
            session.setAttribute("errorMessage", "Invalid form submission.");
            resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
            return;
        }

        switch (action) {
            case "partialSignup":
                handlePartialSignup(req, resp, session);
                break;
            case "deposit":
                handleDepositWithdrawal(req, resp, session, true); // true for deposit
                break;
            case "withdraw":
                 handleDepositWithdrawal(req, resp, session, false); // false for withdraw
                break;
            case "updateEmail":
                handleUpdateDetail(req, resp, session, "email");
                 break;
             case "updateAddress":
                 handleUpdateDetail(req, resp, session, "address");
                 break;
            case "changePassword":
                handleChangePassword(req, resp, session, staff);
                break;
            default:
                session.setAttribute("errorMessage", "Unknown staff action.");
                resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
                break;
        }
    }

     // --- POST Action Handlers ---

    private static void handlePartialSignup(HttpServletRequest req, HttpServletResponse resp, HttpSession session) throws IOException {
         String name = req.getParameter("name");
         String mobile = req.getParameter("mobile");
         String email = req.getParameter("email");
         String address = req.getParameter("address");

         if (isBlank(name) || isBlank(mobile) || isBlank(email) || isBlank(address)) {
            session.setAttribute("errorMessage", "All fields are required for new customer signup.");
            resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
            return;
        }

         // Check if customer OR staff already exists with this mobile/email
         if (StaffDB.customerExistsByMobileOrEmail(mobile, email)) {
             session.setAttribute("errorMessage", "A customer account with this mobile or email already exists.");
             resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
             return;
         }
          if (StaffDB.staffExistsByMobile(mobile)) {
             session.setAttribute("errorMessage", "A staff account with this mobile already exists.");
             resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
             return;
         }
         // Potentially check Partial_Signups table for pending request with same mobile/email? Optional.

         boolean success = StaffDB.createPartialSignup(name, mobile, email, address);

         if (success) {
            session.setAttribute("successMessage", "New customer signup request submitted for manager approval.");
        } else {
            session.setAttribute("errorMessage", "Failed to submit signup request.");
        }
        resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
    }


     private static void handleDepositWithdrawal(HttpServletRequest req, HttpServletResponse resp, HttpSession session, boolean isDeposit) throws IOException {
         String accountNumber = req.getParameter("accountNumber");
         String amountStr = req.getParameter("amount");
         String operation = isDeposit ? "Deposit" : "Withdrawal";

         if (isBlank(accountNumber) || isBlank(amountStr)) {
            session.setAttribute("errorMessage", "Account number and amount are required for " + operation + ".");
            resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
            return;
         }

         double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            session.setAttribute("errorMessage", "Invalid " + operation + " amount.");
            resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
            return;
        }

        // Check customer exists is done within performDepositWithdrawal
        // Balance check for withdrawal is also done within performDepositWithdrawal

        boolean success = StaffDB.performDepositWithdrawal(accountNumber, amount, isDeposit);

         if (success) {
            session.setAttribute("successMessage", operation + " of " + String.format("%.2f", amount) + " for account " + accountNumber + " successful.");
        } else {
             // Specific error message might have been set by StaffDB, check session first
             if (session.getAttribute("errorMessage") == null) {
                session.setAttribute("errorMessage", operation + " failed. Check balance or account number.");
             }
        }
        resp.sendRedirect(req.getContextPath() + "/staff/dashboard");

     }

      private static void handleUpdateDetail(HttpServletRequest req, HttpServletResponse resp, HttpSession session, String detailType) throws IOException {
         String accountNumber = req.getParameter("accountNumber");
         String newValue = req.getParameter("newValue");

         if (isBlank(accountNumber) || isBlank(newValue)) {
            session.setAttribute("errorMessage", "Account number and new value are required to update " + detailType + ".");
            resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
            return;
         }

          // Check customer exists
          Optional<Customer> custOpt = CustomerDB.getCustomerByAccountNumber(accountNumber);
          if(custOpt.isEmpty()){
               session.setAttribute("errorMessage", "Customer account " + accountNumber + " not found.");
               resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
               return;
          }

          boolean success = false;
          if ("email".equals(detailType)) {
              // Optional: Add email format validation here if needed
              success = StaffDB.updateCustomerEmail(accountNumber, newValue);
          } else if ("address".equals(detailType)) {
               // Address update is currently disabled in StaffDB due to schema.
               // If enabled, the call would be:
               // success = StaffDB.updateCustomerAddress(accountNumber, newValue);
               session.setAttribute("errorMessage", "Updating customer address is currently not supported by Staff.");
               resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
               return; // Don't proceed
          } else {
               session.setAttribute("errorMessage", "Invalid detail type requested for update.");
               resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
               return;
          }


         if (success) {
            session.setAttribute("successMessage", "Customer " + detailType + " for account " + accountNumber + " updated successfully.");
        } else {
             if (session.getAttribute("errorMessage") == null) { // Avoid overwriting more specific errors
                session.setAttribute("errorMessage", "Failed to update customer " + detailType + ".");
             }
        }
        resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
     }


      private static void handleChangePassword(HttpServletRequest req, HttpServletResponse resp, HttpSession session, Staff staff) throws IOException {
        String oldPassword = req.getParameter("oldPassword");
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");
        String mobile = staff.getMobile(); // Login identifier

        if (isBlank(oldPassword) || isBlank(newPassword) || isBlank(confirmPassword)) {
            session.setAttribute("errorMessage", "All password fields are required.");
            resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            session.setAttribute("errorMessage", "New passwords do not match.");
            resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
            return;
        }

        // Verify old password
        String oldPasswordHash = AuthHelper.hashPassword(oldPassword);
        if (!oldPasswordHash.equals(staff.getPasswordHash())) {
            session.setAttribute("errorMessage", "Incorrect old password.");
            resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
            return;
        }

        // Update password
        String newPasswordHash = AuthHelper.hashPassword(newPassword);
        boolean success = StaffDB.updateStaffPassword(mobile, newPasswordHash);

        if (success) {
            // Update password hash in session object
            session.setAttribute("staff", new Staff(staff.getId(), staff.getName(), staff.getMobile(), staff.getRole(), newPasswordHash));
            session.setAttribute("successMessage", "Password changed successfully.");
        } else {
            session.setAttribute("errorMessage", "Failed to change password.");
        }
        resp.sendRedirect(req.getContextPath() + "/staff/dashboard");
    }


     // Helper method
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}