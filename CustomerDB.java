package db;

import model.Customer;
import model.Grievance;
import model.Loan;
import model.Transaction;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal; // For precise financial calculations
import java.math.RoundingMode; // For precise financial calculations


public class CustomerDB {

    // --- Customer Authentication & Details ---

    public static Optional<Customer> validateCustomerLogin(String accountNumber, String passwordHash) {
        String sql = "SELECT id, name, mobile, email, accountno, password FROM Customer WHERE accountno = ? AND password = ? LIMIT 1";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, passwordHash); // Compare against the stored hash
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new Customer(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("mobile"),
                        rs.getString("email"),
                        rs.getString("accountno"),
                        rs.getString("password") // Stored hash
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error validating customer login: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return Optional.empty();
    }

     public static Optional<Customer> getCustomerByAccountNumber(String accountNumber) {
        String sql = "SELECT id, name, mobile, email, accountno, password FROM Customer WHERE accountno = ? LIMIT 1";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, accountNumber);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                 return Optional.of(new Customer(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("mobile"),
                        rs.getString("email"),
                        rs.getString("accountno"),
                        rs.getString("password") // Stored hash
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting customer by account number: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return Optional.empty();
    }


    public static double getCustomerBalance(String accountNumber) {
        // Balance isn't stored directly in Customer table per schema.
        // It must be calculated from Transactions or assumed to be managed elsewhere.
        // For simplicity, let's assume a function to calculate it, or return 0.
        // A more realistic approach involves summing transactions.
        // Let's simulate by summing transaction amounts for this account.

        String sql = "SELECT " +
                     "  COALESCE(SUM(CASE WHEN to_acc = ? THEN amount ELSE 0 END), 0) - " +
                     "  COALESCE(SUM(CASE WHEN from_acc = ? THEN amount ELSE 0 END), 0) as balance " +
                     "FROM Transactions " +
                     "WHERE to_acc = ? OR from_acc = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        double balance = 0.0;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, accountNumber);
            pstmt.setString(3, accountNumber);
            pstmt.setString(4, accountNumber);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                balance = rs.getDouble("balance");
            }
        } catch (SQLException e) {
            System.err.println("Error calculating customer balance: " + e.getMessage());
            // Return 0 or throw exception depending on desired handling
        } finally {
            DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return balance;
    }


    public static boolean updateCustomerPassword(String accountNumber, String newPasswordHash) {
        String sql = "UPDATE Customer SET password = ? WHERE accountno = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, newPasswordHash);
            pstmt.setString(2, accountNumber);
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating customer password: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return success;
    }

    // --- Transactions ---

    public static List<Transaction> getTransactionsForAccount(String accountNumber) {
        List<Transaction> transactions = new ArrayList<>();
        // Get transactions where the account is either sender or receiver
        // Ordering by ID descending to get newest first
        String sql = "SELECT id, accno, amount, to_acc, from_acc, type, date(Timestamp) as date, time(Timestamp) as time FROM Transactions WHERE accno = ? OR to_acc = ? OR from_acc = ? ORDER BY id DESC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, accountNumber);
             pstmt.setString(2, accountNumber); // Match to_acc
              pstmt.setString(3, accountNumber); // Match from_acc

            rs = pstmt.executeQuery();

            while (rs.next()) {
                 // Reconstruct timestamp if needed, or use separate date/time if DB stores them
                 String dateTime = rs.getString("date") + " " + rs.getString("time"); // Adjust based on actual DB column name/format
                transactions.add(new Transaction(
                        rs.getInt("id"),
                        accountNumber, // The context account
                        rs.getDouble("amount"),
                        rs.getString("to_acc"),
                        rs.getString("from_acc"),
                        rs.getString("type"),
                        dateTime // Pass the timestamp string
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting transactions for account: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return transactions;
    }

    /**
     * Performs a transfer between two accounts. Assumes sufficient funds check happens before calling.
     * Adds two transaction records (one debit, one credit).
     */
    public static boolean performTransfer(String fromAccount, String toAccount, double amount) {
       String sqlInsert = "INSERT INTO Transactions (accno, amount, to_acc, from_acc, type, Timestamp) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
       Connection conn = null;
       PreparedStatement pstmtDebit = null;
       PreparedStatement pstmtCredit = null;
       boolean success = false;

       try {
           conn = DatabaseUtil.getConnection();
           conn.setAutoCommit(false); // Start transaction

           // 1. Record Debit from sender
           pstmtDebit = conn.prepareStatement(sqlInsert);
           pstmtDebit.setString(1, fromAccount); // Primary account for this record
           pstmtDebit.setDouble(2, amount);
           pstmtDebit.setString(3, toAccount);
           pstmtDebit.setString(4, fromAccount);
           pstmtDebit.setString(5, "Transfer Out"); // Or just "Transfer"
           pstmtDebit.executeUpdate();

           // 2. Record Credit to receiver
           pstmtCredit = conn.prepareStatement(sqlInsert);
           pstmtCredit.setString(1, toAccount); // Primary account for this record
           pstmtCredit.setDouble(2, amount);
           pstmtCredit.setString(3, toAccount);
           pstmtCredit.setString(4, fromAccount);
           pstmtCredit.setString(5, "Transfer In"); // Or just "Transfer"
           pstmtCredit.executeUpdate();

           conn.commit(); // Commit transaction
           success = true;

       } catch (SQLException e) {
           System.err.println("Error performing transfer: " + e.getMessage());
           if (conn != null) {
               try {
                   conn.rollback();
                   System.err.println("Transfer transaction rolled back.");
               } catch (SQLException ex) {
                   System.err.println("Error rolling back transfer transaction: " + ex.getMessage());
               }
           }
       } finally {
           DatabaseUtil.closeQuietly(pstmtDebit);
            DatabaseUtil.closeQuietly(pstmtCredit);
           if (conn != null) {
               try {
                   conn.setAutoCommit(true); // Restore auto-commit mode
               } catch (SQLException e) { /* ignore */ }
               DatabaseUtil.closeQuietly(conn);
           }
       }
       return success;
   }

    // --- Loans ---

    public static List<Loan> getActiveLoansForAccount(String accountNumber) {
        List<Loan> loans = new ArrayList<>();
        // Assuming 'active' means not fully repaid. This might require checking related 'Loan Repaid' transactions.
        // Simplified: Get all loans for the account. Repayment status handled in servlet/JSP via calculation.
        String sql = "SELECT id, amount, accno, int_rate, date, duration FROM Loan WHERE accno = ? ORDER BY date DESC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, accountNumber);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                loans.add(new Loan(
                        rs.getInt("id"),
                        rs.getDouble("amount"),
                        rs.getString("accno"),
                        rs.getDouble("int_rate"),
                        rs.getString("date"), // Assuming date is stored as text (YYYY-MM-DD)
                        rs.getInt("duration")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting loans for account: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return loans;
    }


    public static Optional<Loan> getLoanById(int loanId) {
        String sql = "SELECT id, amount, accno, int_rate, date, duration FROM Loan WHERE id = ? LIMIT 1";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, loanId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new Loan(
                    rs.getInt("id"),
                    rs.getDouble("amount"),
                    rs.getString("accno"),
                    rs.getDouble("int_rate"),
                    rs.getString("date"),
                    rs.getInt("duration")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting loan by ID: " + e.getMessage());
        } finally {
           DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return Optional.empty();
    }


    /**
     * Records a loan repayment transaction. Assumes sufficient funds check happens before calling.
     * Returns true on success.
     */
    public static boolean repayLoan(String accountNumber, int loanId, double repayAmount) {
        String sqlInsert = "INSERT INTO Transactions (accno, amount, from_acc, type, Timestamp) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        // Note: `to_acc` is NULL for loan repayment (money leaves bank system concept)
        // We also need to potentially mark the Loan as 'repaid' or delete it if fully paid.
        // For simplicity, we only add the transaction record. Full repayment logic is complex.

        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Insert transaction record
            pstmt = conn.prepareStatement(sqlInsert);
            pstmt.setString(1, accountNumber);
            pstmt.setDouble(2, repayAmount);
            pstmt.setString(3, accountNumber); // Money comes from the customer's account
            pstmt.setString(4, "Loan Repaid (ID: " + loanId + ")"); // Type indicates repayment and loan ID
            pstmt.executeUpdate();

            // Simplification: We are NOT updating the Loan table status here.
            // A real system would check if repayAmount covers the outstanding balance
            // and update the Loan status or delete the Loan record if fully paid.

            conn.commit();
            success = true;

        } catch (SQLException e) {
            System.err.println("Error recording loan repayment: " + e.getMessage());
             if (conn != null) {
               try {
                   conn.rollback();
                   System.err.println("Loan repayment transaction rolled back.");
               } catch (SQLException ex) {
                   System.err.println("Error rolling back loan repayment transaction: " + ex.getMessage());
               }
           }
        } finally {
            DatabaseUtil.closeQuietly(pstmt);
             if (conn != null) {
               try {
                   conn.setAutoCommit(true); // Restore auto-commit mode
               } catch (SQLException e) { /* ignore */ }
               DatabaseUtil.closeQuietly(conn);
           }
        }
        return success;
    }

     /**
      * Calculates the amount needed to repay a loan based on the provided formula.
      * Uses BigDecimal for precision.
      */
     public static BigDecimal calculateLoanRepayAmount(Loan loan) {
         LocalDate creationDate = LocalDate.parse(loan.getDateCreated()); // Assumes YYYY-MM-DD format
         LocalDate today = LocalDate.now();

         // Use BigDecimal for principal and rates
         BigDecimal principal = BigDecimal.valueOf(loan.getAmount());
         BigDecimal normalAnnualRatePercent = BigDecimal.valueOf(loan.getInterestRate()); // e.g., 5.0 for 5%

         long totalDaysPassed = ChronoUnit.DAYS.between(creationDate, today);
         // Convert total days passed to years (can be fractional) for comparison and calculation
         // Using 365.25 days per year average for BigDecimal calculation
         BigDecimal daysInYear = new BigDecimal("365.25");
         BigDecimal totalYearsPassed = new BigDecimal(totalDaysPassed).divide(daysInYear, 10, RoundingMode.HALF_UP); // 10 decimal places for intermediate calc

         BigDecimal loanDurationYears = BigDecimal.valueOf(loan.getDurationYears());

         BigDecimal effectiveAnnualRatePercent;

         if (totalYearsPassed.compareTo(loanDurationYears) <= 0) {
             // Within normal duration
             effectiveAnnualRatePercent = normalAnnualRatePercent;
         } else {
             // Overdue - calculate penalty
             BigDecimal extraTimeYears = totalYearsPassed.subtract(loanDurationYears);
             // Calculate number of full half-years passed in extra time
             // extraTimeYears / 0.5 = extraTimeYears * 2
             BigDecimal halfYearsPassedDecimal = extraTimeYears.multiply(new BigDecimal("2"));
             // Floor this value to get number of full half-year periods for penalty
             long halfYearsPassed = halfYearsPassedDecimal.setScale(0, RoundingMode.FLOOR).longValue();

             // Penalty is 1% per full half-year overdue
             BigDecimal penaltyPercent = new BigDecimal(halfYearsPassed).multiply(BigDecimal.ONE); // 1% is 1.0

             effectiveAnnualRatePercent = normalAnnualRatePercent.add(penaltyPercent);
             System.out.println("Loan ID " + loan.getId() + " overdue. Extra Half-Years: " + halfYearsPassed + ", Penalty: " + penaltyPercent + "%, Effective Rate: " + effectiveAnnualRatePercent + "%");
         }

         // Calculate quarterly compounding
         // Ensure rate is not negative if penalties somehow exceed initial rate (unlikely)
         if (effectiveAnnualRatePercent.compareTo(BigDecimal.ZERO) < 0) {
             effectiveAnnualRatePercent = BigDecimal.ZERO;
         }

         BigDecimal annualRateDecimal = effectiveAnnualRatePercent.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
         BigDecimal quarterlyRateDecimal = annualRateDecimal.divide(new BigDecimal("4"), 10, RoundingMode.HALF_UP);

         // Number of quarters = total years passed * 4
         // Use double for Math.pow, accepting potential minor precision loss at exponentiation stage, or use a loop for BigDecimal power.
         // Using Math.pow for simplicity as requested.
         double numberOfQuarters = totalYearsPassed.multiply(new BigDecimal("4")).doubleValue();

         // final_amount = principal * (1 + quarterly_rate) ^ quarters
         double base = BigDecimal.ONE.add(quarterlyRateDecimal).doubleValue();
         double finalAmountDouble = principal.doubleValue() * Math.pow(base, numberOfQuarters);

         // Return as BigDecimal rounded to 2 decimal places (currency)
         return BigDecimal.valueOf(finalAmountDouble).setScale(2, RoundingMode.HALF_UP);
     }


    // --- Grievances ---

    public static boolean createGrievance(String accountNumber, String complaint) {
        // Status defaults to 'Pending', remarks null initially
        String sql = "INSERT INTO Grievance (accno, complain, status, Timestamp) VALUES (?, ?, 'Pending', CURRENT_TIMESTAMP)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, accountNumber);
            pstmt.setString(2, complaint);
            int rowsAffected = pstmt.executeUpdate();
            success = rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error creating grievance: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return success;
    }

    public static List<Grievance> getGrievancesForAccount(String accountNumber) {
        List<Grievance> grievances = new ArrayList<>();
        String sql = "SELECT id, accno, complain, status, remarks, Timestamp FROM Grievance WHERE accno = ? ORDER BY id DESC";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, accountNumber);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                grievances.add(new Grievance(
                        rs.getInt("id"),
                        rs.getString("accno"),
                        rs.getString("complain"),
                        rs.getString("status"),
                        rs.getString("remarks"),
                        rs.getString("Timestamp") // Get timestamp as string
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting grievances for account: " + e.getMessage());
        } finally {
            DatabaseUtil.closeQuietly(rs);
            DatabaseUtil.closeQuietly(pstmt);
            DatabaseUtil.closeQuietly(conn);
        }
        return grievances;
    }
}