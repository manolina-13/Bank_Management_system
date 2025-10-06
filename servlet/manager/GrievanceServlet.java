package servlet.manager;

import db.ManagerDB;
import model.Grievance;
import model.Staff; // Manager is Staff

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public class GrievanceServlet {

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
        // Fetch pending grievances (status 'Pending')
        List<Grievance> pendingGrievances = ManagerDB.getPendingGrievances();
        req.setAttribute("pendingGrievances", pendingGrievances);

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

        req.getRequestDispatcher("/WEB-INF/pages/manager/grievance.jsp").forward(req, resp);
    }

      private static void handlePost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
          HttpSession session = req.getSession();
          String grievanceIdStr = req.getParameter("grievanceId");
          String newStatus = req.getParameter("newStatus");
          String remarks = req.getParameter("remarks");

           if (isBlank(grievanceIdStr) || isBlank(newStatus) || isBlank(remarks)) {
             session.setAttribute("errorMessage", "Grievance ID, New Status, and Remarks are required.");
             resp.sendRedirect(req.getContextPath() + "/manager/grievance");
             return;
         }

         // Validate status value if needed (e.g., must be one of "Pending", "In Process", "Resolved")
         if (!List.of("Pending", "In Process", "Resolved").contains(newStatus)) {
              session.setAttribute("errorMessage", "Invalid status value provided.");
              resp.sendRedirect(req.getContextPath() + "/manager/grievance");
              return;
         }


         int grievanceId;
         try {
             grievanceId = Integer.parseInt(grievanceIdStr);
         } catch (NumberFormatException e) {
             session.setAttribute("errorMessage", "Invalid grievance ID format.");
             resp.sendRedirect(req.getContextPath() + "/manager/grievance");
             return;
         }

          boolean success = ManagerDB.updateGrievance(grievanceId, newStatus, remarks.trim());

          if (success) {
             session.setAttribute("successMessage", "Grievance ID " + grievanceId + " updated successfully.");
         } else {
             session.setAttribute("errorMessage", "Failed to update grievance ID " + grievanceId + ".");
         }
         resp.sendRedirect(req.getContextPath() + "/manager/grievance"); // Redirect back to GET
      }


    // Helper method
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}