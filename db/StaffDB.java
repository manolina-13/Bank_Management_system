package db;

import model.Customer;
import model.PartialSignup;
import model.Staff;

import java.sql.*;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class StaffDB {

    // --- Staff Authentication & Details ---

    public static Optional<Staff> validateStaffLogin(String mobile, String passwordHash) {
        String sql = "SELECT id, name, mobile, role, password FROM Staff WHERE mobile = ? AND password = ? AND role = 'STAFF' LIMIT 1";
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
            System.err.println("Error validating staff login: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return Optional.empty();
    }

     public static Optional<Staff> getStaffByMobile(String mobile) {
        String sql = "SELECT id, name, mobile, role, password FROM Staff WHERE mobile = ? AND role = 'STAFF' LIMIT 1";
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
            System.err.println("Error getting staff by mobile: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return Optional.empty();
    }

      // Check if any staff (Staff or Manager) exists with this mobile
     public static boolean staffExistsByMobile(String mobile) {
        String sql = "SELECT 1 FROM Staff WHERE mobile = ? LIMIT 1";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean exists = false;
        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, mobile);
            rs = pstmt.executeQuery();
            exists = rs.next(); // True if a record is found
        } catch (SQLException e) {
            System.err.println("Error checking staff existence by mobile: " + e.getMessage());
            // Optionally, return true on error to be safe, or handle differently
        } finally {
            DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return exists;
    }


    public static boolean updateStaffPassword(String mobile, String newPasswordHash) {
        String sql = "UPDATE Staff SET password = ? WHERE mobile = ? AND role = 'STAFF'";
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
            System.err.println("Error updating staff password: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return success;
    }

    // --- Customer Operations by Staff ---

     // Check if a customer account exists with the given mobile or email.
    public static boolean customerExistsByMobileOrEmail(String mobile, String email) {
        String sql = "SELECT 1 FROM Customer WHERE mobile = ? OR email = ? LIMIT 1";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean exists = false;
        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, mobile);
            pstmt.setString(2, email);
            rs = pstmt.executeQuery();
            exists = rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking customer existence: " + e.getMessage());
        } finally {
             DatabaseUtil.closeQuietly(rs);
             DatabaseUtil.closeQuietly(pstmt);
             DatabaseUtil.closeQuietly(conn);
        }
        return exists;
    }

     // Create a partial signup request
    public static boolean createPartialSignup(String name, String mobile, String email, String address) {
        String sql = "INSERT INTO Partial_Signups (status, name, email, address, mobile, Timestamp) VALUES ('Pending', ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        // Optional: Check if a PENDING signup for this mobile/email already exists
        // to prevent duplicates before manager approval.

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, address);
            pstmt.setString(4, mobile);
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error creating partial signup: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return success;
    }


    // Deposit or Withdraw
    public static boolean performDepositWithdrawal(String accountNumber, double amount, boolean isDeposit) {
        String type = isDeposit ? "Deposit" : "Withdrawal";
        String toAcc = isDeposit ? accountNumber : null;
        String fromAcc = isDeposit ? null : accountNumber;

        String sqlInsert = "INSERT INTO Transactions (accno, amount, to_acc, from_acc, type, Timestamp) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        // Ensure amount is positive
        if (amount <= 0) {
            System.err.println("Deposit/Withdrawal amount must be positive.");
            return false;
        }

        // Check if customer account exists
        if (CustomerDB.getCustomerByAccountNumber(accountNumber).isEmpty()) {
             System.err.println("Cannot perform " + type + ": Account " + accountNumber + " not found.");
             return false;
        }

        // If withdrawal, check balance (using the balance calculation method)
        if (!isDeposit) {
            double currentBalance = CustomerDB.getCustomerBalance(accountNumber);
            if (currentBalance < amount) {
                 System.err.println("Cannot perform withdrawal: Insufficient balance in account " + accountNumber);
                 return false;
            }
        }

        try {
            conn = DatabaseUtil.getConnection();
            // Note: No transaction block needed here as only one insert is happening.
            pstmt = conn.prepareStatement(sqlInsert);
            pstmt.setString(1, accountNumber); // The account affected
            pstmt.setDouble(2, amount);
            pstmt.setString(3, toAcc);
            pstmt.setString(4, fromAcc);
            pstmt.setString(5, type);
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error performing " + type + ": " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return success;
    }


    // Update Customer Email
    public static boolean updateCustomerEmail(String accountNumber, String newEmail) {
        String sql = "UPDATE Customer SET email = ? WHERE accountno = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newEmail);
            pstmt.setString(2, accountNumber);
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating customer email: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return success;
    }

    // Update Customer Address
    public static boolean updateCustomerAddress(String accountNumber, String newAddress) {
        // Assuming Customer table has an 'address' column based on requirements.
        // If not, this method is invalid.
        // Let's assume Customer table DOES NOT have address, so this update isn't possible directly.
        // Staff should only update email as per schema.
        // Returning false or removing method. Let's return false with a message.
        System.err.println("Function updateCustomerAddress called, but Customer table schema lacks 'address'. Update ignored.");
        // To implement: Add 'address' column to Customer table and uncomment below.
        /*
        String sql = "UPDATE Customer SET address = ? WHERE accountno = ?";
         Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;
        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newAddress);
            pstmt.setString(2, accountNumber);
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating customer address: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return success;
        */
        return false; // Address cannot be updated based on provided Customer schema
    }

}