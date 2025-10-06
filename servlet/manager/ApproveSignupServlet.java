package servlet.manager;

import auth.AuthHelper;
import db.ManagerDB;
import model.PartialSignup;
import model.Staff; // Manager is Staff

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public class ApproveSignupServlet {

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

    private static void handleGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Fetch pending signups
        List<PartialSignup> pendingSignups = ManagerDB.getPendingSignups();
        req.setAttribute("pendingSignups", pendingSignups);

        // Check for messages from previous POST requests
        HttpSession session = req.getSession();
        if (session.getAttribute("errorMessage") != null) {
            req.setAttribute("errorMessage", session.getAttribute("errorMessage"));
            session.removeAttribute("errorMessage");
        }
        if (session.getAttribute("successMessage") != null) {
             req.setAttribute("successMessage", session.getAttribute("successMessage"));
            session.removeAttribute("successMessage");
        }


        req.getRequestDispatcher("/WEB-INF/pages/manager/approve_signup.jsp").forward(req, resp);
    }

     private static void handlePost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        String action = req.getParameter("decision"); // "approve" or "reject"
        String signupIdStr = req.getParameter("signupId");

         if (isBlank(action) || isBlank(signupIdStr)) {
             session.setAttribute("errorMessage", "Missing decision or signup ID.");
             resp.sendRedirect(req.getContextPath() + "/manager/approve_signup");
             return;
         }

         int signupId;
         try {
             signupId = Integer.parseInt(signupIdStr);
         } catch (NumberFormatException e) {
             session.setAttribute("errorMessage", "Invalid signup ID format.");
             resp.sendRedirect(req.getContextPath() + "/manager/approve_signup");
             return;
         }

         if ("reject".equals(action)) {
             boolean success = ManagerDB.rejectSignup(signupId);
             if (success) {
                 session.setAttribute("successMessage", "Signup request ID " + signupId + " rejected successfully.");
             } else {
                 session.setAttribute("errorMessage", "Failed to reject signup request ID " + signupId + ". It might have been already processed.");
             }
             resp.sendRedirect(req.getContextPath() + "/manager/approve_signup"); // Redirect back to GET

         } else if ("approve".equals(action)) {
             // Get approval details from form
             String accountNumber = req.getParameter("accountNumber");
             String password = req.getParameter("initialPassword");
             String depositStr = req.getParameter("initialDeposit");

             if (isBlank(accountNumber) || isBlank(password) || isBlank(depositStr)) {
                 session.setAttribute("errorMessage", "Account number, initial password, and initial deposit are required for approval (Signup ID: " + signupId + ").");
                 resp.sendRedirect(req.getContextPath() + "/manager/approve_signup");
                 return;
             }

              double initialDeposit;
              try {
                  initialDeposit = Double.parseDouble(depositStr);
                  if (initialDeposit < 0) throw new NumberFormatException();
              } catch (NumberFormatException e) {
                  session.setAttribute("errorMessage", "Invalid initial deposit amount (Signup ID: " + signupId + ").");
                  resp.sendRedirect(req.getContextPath() + "/manager/approve_signup");
                  return;
              }

             // Hash the password
             String passwordHash = AuthHelper.hashPassword(password);

             // Perform the approval transaction in DB
             boolean success = ManagerDB.approveSignup(signupId, accountNumber.trim(), passwordHash, initialDeposit);

             if (success) {
                 session.setAttribute("successMessage", "Signup request ID " + signupId + " approved. Customer account " + accountNumber + " created.");
             } else {
                  // Specific error message might have been set by ManagerDB, check session first
                 if (session.getAttribute("errorMessage") == null) {
                    session.setAttribute("errorMessage", "Failed to approve signup request ID " + signupId + ". Account number might already exist or another error occurred.");
                 }
             }
             resp.sendRedirect(req.getContextPath() + "/manager/approve_signup"); // Redirect back to GET
         } else {
              session.setAttribute("errorMessage", "Invalid decision specified.");
              resp.sendRedirect(req.getContextPath() + "/manager/approve_signup");
         }
    }

     // Helper method
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}