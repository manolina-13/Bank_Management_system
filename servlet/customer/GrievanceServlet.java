package servlet.customer;

import db.CustomerDB;
import model.Customer;
import model.Grievance;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public class GrievanceServlet {

    public static void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        // Authentication Check
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

    private static void handleGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        Customer customer = (Customer) session.getAttribute("customer");
        String accountNumber = customer.getAccountNumber();

        // Fetch past grievances
        List<Grievance> grievances = CustomerDB.getGrievancesForAccount(accountNumber);
        req.setAttribute("grievances", grievances);

        req.getRequestDispatcher("/WEB-INF/pages/customer/grievance.jsp").forward(req, resp);
    }

    private static void handlePost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        Customer customer = (Customer) session.getAttribute("customer");
        String accountNumber = customer.getAccountNumber();
        String complaint = req.getParameter("complaint");

        if (complaint == null || complaint.trim().isEmpty()) {
            session.setAttribute("errorMessage", "Complaint text cannot be empty.");
            // Redirect back to GET to show error and existing grievances
            resp.sendRedirect(req.getContextPath() + "/customer/grievance");
            return;
        }

        // Create grievance in DB
        boolean success = CustomerDB.createGrievance(accountNumber, complaint.trim());

        if (success) {
            session.setAttribute("successMessage", "Grievance submitted successfully.");
        } else {
            session.setAttribute("errorMessage", "Failed to submit grievance. Please try again.");
        }
        // Redirect back to GET to show success/error and updated grievance list
        resp.sendRedirect(req.getContextPath() + "/customer/grievance");
    }
}