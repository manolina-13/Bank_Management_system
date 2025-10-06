package db;

import model.Customer;
import model.Grievance;
import model.PartialSignup;
import model.Staff;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson; // Needed if parsing JSON from PartialSignup

public class ManagerDB {

    // --- Manager Authentication & Details ---

    // Validate login based on mobile number and password hash
    public static Optional<Staff> validateManagerLogin(String mobile, String passwordHash) {
        String sql = "SELECT id, name, mobile, role, password FROM Staff WHERE mobile = ? AND password = ? AND role = 'MANAGER' LIMIT 1";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, mobile);
            pstmt.setString(2, passwordHash);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new Staff(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("mobile"),
                        rs.getString("role"),
                        rs.getString("password") // Stored hash
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error validating manager login: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return Optional.empty();
    }

    // Get manager details by mobile (e.g., after session validation)
    public static Optional<Staff> getManagerByMobile(String mobile) {
        String sql = "SELECT id, name, mobile, role, password FROM Staff WHERE mobile = ? AND role = 'MANAGER' LIMIT 1";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, mobile);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new Staff(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("mobile"),
                        rs.getString("role"),
                        rs.getString("password") // Stored hash
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting manager by mobile: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return Optional.empty();
    }

    public static boolean updateManagerPassword(String mobile, String newPasswordHash) {
        String sql = "UPDATE Staff SET password = ? WHERE mobile = ? AND role = 'MANAGER'";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newPasswordHash);
            pstmt.setString(2, mobile);
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating manager password: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return success;
    }

    // Update Manager's own name
    public static boolean updateManagerName(String mobile, String newName) {
        String sql = "UPDATE Staff SET name = ? WHERE mobile = ? AND role = 'MANAGER'";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newName);
            pstmt.setString(2, mobile);
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating manager name: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return success;
    }

    // Update Manager's own mobile - CAUTION: This is the login identifier!
    // Requires careful handling to ensure the manager can log in again.
    // Consider if this feature is truly needed due to complexity/risk.
    // For simplicity, let's implement it but add a strong warning.
    public static boolean updateManagerMobile(String oldMobile, String newMobile) {
        // Add check: ensure new mobile doesn't already exist for another staff/manager
        if (StaffDB.staffExistsByMobile(newMobile)) {
            System.err.println("Error updating manager mobile: New mobile '" + newMobile + "' already exists.");
            return false;
        }

        String sql = "UPDATE Staff SET mobile = ? WHERE mobile = ? AND role = 'MANAGER'";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newMobile);
            pstmt.setString(2, oldMobile);
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
            if (success) {
                System.out.println("Manager mobile updated from " + oldMobile + " to " + newMobile
                        + ". Manager must use new mobile to log in.");
            }
        } catch (SQLException e) {
            System.err.println("Error updating manager mobile: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return success;
    }

    // --- Loan Creation ---

    public static boolean createLoan(String accountNumber, double amount, double interestRate, int durationYears) {
        String insertLoanSQL = "INSERT INTO Loan (accno, amount, int_rate, date, duration) VALUES (?, ?, ?, CURRENT_DATE, ?)";
        String insertTransactionSQL = "INSERT INTO Transactions (accno, amount, to_acc, type, Timestamp) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        Connection conn = null;
        PreparedStatement pstmtLoan = null;
        PreparedStatement pstmtTransaction = null;
        boolean success = false;

        // Basic validation
        if (amount <= 0 || interestRate < 0 || durationYears <= 0) {
            System.err.println("Invalid loan parameters provided.");
            return false;
        }

        // Check if customer exists (optional but good practice)
        if (CustomerDB.getCustomerByAccountNumber(accountNumber).isEmpty()) {
            System.err.println("Cannot create loan: Customer account " + accountNumber + " not found.");
            return false;
        }

        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Insert into Loan table
            pstmtLoan = conn.prepareStatement(insertLoanSQL);
            pstmtLoan.setString(1, accountNumber);
            pstmtLoan.setDouble(2, amount);
            pstmtLoan.setDouble(3, interestRate);
            pstmtLoan.setInt(4, durationYears);
            pstmtLoan.executeUpdate();

            // 2. Insert into Transactions table (money given to customer)
            pstmtTransaction = conn.prepareStatement(insertTransactionSQL);
            pstmtTransaction.setString(1, accountNumber);
            pstmtTransaction.setDouble(2, amount);
            pstmtTransaction.setString(3, accountNumber); // Money goes to the customer account
            pstmtTransaction.setString(4, "Loan Taken");
            pstmtTransaction.executeUpdate();

            conn.commit();
            success = true;

        } catch (SQLException e) {
            System.err.println("Error creating loan: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Loan creation transaction rolled back.");
                } catch (SQLException ex) {
                    System.err.println("Error rolling back: " + ex.getMessage());
                }
            }
        } finally {
            DatabaseUtil.closeQuietly(pstmtLoan);
            DatabaseUtil.closeQuietly(pstmtTransaction);
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    /* ignore */ }
                DatabaseUtil.closeQuietly(conn);
            }
        }
        return success;
    }

    // --- Customer Signup Approval ---

    public static List<PartialSignup> getPendingSignups() {
        List<PartialSignup> signups = new ArrayList<>();
        String sql = "SELECT id, status, name, email, address, mobile, Timestamp FROM Partial_Signups WHERE status = 'Pending' ORDER BY id ASC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                signups.add(new PartialSignup(
                        rs.getInt("id"),
                        rs.getString("status"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("address"),
                        rs.getString("mobile"),
                        rs.getString("Timestamp")));
            }
        } catch (SQLException e) {
            System.err.println("Error getting pending signups: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return signups;
    }

    // Approve a signup: Update Partial_Signups status, create Customer, add initial
    // deposit Transaction
    public static boolean approveSignup(int partialSignupId, String accountNumber, String passwordHash,
            double initialDeposit) {
        String fetchSignupSQL = "SELECT name, mobile, email, address FROM Partial_Signups WHERE id = ? AND status = 'Pending'";
        String updateSignupSQL = "UPDATE Partial_Signups SET status = 'Approved' WHERE id = ?";
        String insertCustomerSQL = "INSERT INTO Customer (name, mobile, email, accountno, password) VALUES (?, ?, ?, ?, ?)";
        String insertDepositSQL = "INSERT INTO Transactions (accno, amount, to_acc, type, Timestamp) VALUES (?, ?, ?, 'Deposit', CURRENT_TIMESTAMP)";
        Connection conn = null;
        PreparedStatement pstmtFetch = null;
        PreparedStatement pstmtUpdate = null;
        PreparedStatement pstmtInsertCust = null;
        PreparedStatement pstmtInsertDeposit = null;
        ResultSet rsFetch = null;
        boolean success = false;

        // Basic validation
        if (initialDeposit < 0) {
            System.err.println("Initial deposit cannot be negative.");
            return false;
        }
        // Check if account number already exists in Customer table
        if (CustomerDB.getCustomerByAccountNumber(accountNumber).isPresent()) {
            System.err.println("Cannot approve signup: Account number '" + accountNumber + "' already exists.");
            return false;
        }

        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Fetch details from Partial_Signups
            pstmtFetch = conn.prepareStatement(fetchSignupSQL);
            pstmtFetch.setInt(1, partialSignupId);
            rsFetch = pstmtFetch.executeQuery();

            if (!rsFetch.next()) {
                System.err.println("Pending signup with ID " + partialSignupId + " not found.");
                conn.rollback(); // Rollback before returning
                return false;
            }
            String name = rsFetch.getString("name");
            String mobile = rsFetch.getString("mobile");
            String email = rsFetch.getString("email");
            String address = rsFetch.getString("address"); // Assuming address is needed, else remove. Schema doesn't
                                                           // list address in Customer table. Let's assume address IS
                                                           // needed for Customer table based on model. If not, remove
                                                           // address handling here and in Customer table/model.
            // Self-correction: Original Customer schema was id, name, mobile, email,
            // accountno, password. Address seems to come from Partial_Signups. Let's assume
            // Customer table *should* have address. If not, the INSERT SQL and Customer
            // model need adjusting. Adjusting INSERT SQL assuming address IS NOT in
            // Customer table.
            // String insertCustomerSQL = "INSERT INTO Customer (name, mobile, email,
            // accountno, password) VALUES (?, ?, ?, ?, ?)"; // Corrected based on schema

            // 2. Insert into Customer table
            pstmtInsertCust = conn.prepareStatement(insertCustomerSQL);
            pstmtInsertCust.setString(1, name);
            pstmtInsertCust.setString(2, mobile);
            pstmtInsertCust.setString(3, email);
            pstmtInsertCust.setString(4, accountNumber); // Manager assigned account number
            pstmtInsertCust.setString(5, passwordHash); // Manager set password (hashed)
            pstmtInsertCust.executeUpdate();

            // 3. Insert initial deposit transaction if amount > 0
            if (initialDeposit > 0) {
                pstmtInsertDeposit = conn.prepareStatement(insertDepositSQL);
                pstmtInsertDeposit.setString(1, accountNumber);
                pstmtInsertDeposit.setDouble(2, initialDeposit);
                pstmtInsertDeposit.setString(3, accountNumber); // Money goes to this new account
                pstmtInsertDeposit.executeUpdate();
            }

            // 4. Update Partial_Signups status to 'Approved'
            pstmtUpdate = conn.prepareStatement(updateSignupSQL);
            pstmtUpdate.setInt(1, partialSignupId);
            pstmtUpdate.executeUpdate();

            conn.commit(); // Commit transaction
            success = true;

        } catch (SQLException e) {
            System.err.println("Error approving signup: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Signup approval transaction rolled back.");
                } catch (SQLException ex) {
                    System.err.println("Error rolling back: " + ex.getMessage());
                }
            }
        } finally {
            DatabaseUtil.closeQuietly(rsFetch);
            DatabaseUtil.closeQuietly(pstmtFetch);
            DatabaseUtil.closeQuietly(pstmtUpdate);
            DatabaseUtil.closeQuietly(pstmtInsertCust);
            DatabaseUtil.closeQuietly(pstmtInsertDeposit);
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    /* ignore */ }
                DatabaseUtil.closeQuietly(conn);
            }
        }
        return success;
    }

    // Reject a signup: Update Partial_Signups status
    public static boolean rejectSignup(int partialSignupId) {
        String sql = "UPDATE Partial_Signups SET status = 'Rejected' WHERE id = ? AND status = 'Pending'";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, partialSignupId);
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0; // Success only if a pending record was found and updated
        } catch (SQLException e) {
            System.err.println("Error rejecting signup: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return success;
    }

    // --- Grievance Management ---

    public static List<Grievance> getPendingGrievances() {
        List<Grievance> grievances = new ArrayList<>();
        // Fetch grievances with status 'Pending' or 'In Process' (or just 'Pending' as
        // per spec?)
        // Spec says "status=active". Let's assume 'Active' means 'Pending'.
        String sql = "SELECT id, accno, complain, status, remarks, Timestamp FROM Grievance WHERE status != 'Resolved' ORDER BY id ASC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                grievances.add(new Grievance(
                        rs.getInt("id"),
                        rs.getString("accno"),
                        rs.getString("complain"),
                        rs.getString("status"),
                        rs.getString("remarks"),
                        rs.getString("Timestamp")));
            }
        } catch (SQLException e) {
            System.err.println("Error getting pending grievances: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return grievances;
    }

    public static boolean updateGrievance(int grievanceId, String newStatus, String remarks) {
        String sql = "UPDATE Grievance SET status = ?, remarks = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newStatus);
            pstmt.setString(2, remarks);
            pstmt.setInt(3, grievanceId);
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating grievance: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return success;
    }
}