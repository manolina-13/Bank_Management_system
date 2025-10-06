package model;

// Corresponds to Transactions table: id, accno, amount, to_acc, from_acc, type, date (implicit from DB)
public class Transaction {
    private int id;
    private String accountNumber; // The primary account involved (e.g., the customer viewing the statement)
    private double amount;
    private String toAccount;
    private String fromAccount;
    private String type; // "Deposit", "Withdrawal", "Transfer", "Loan Taken", "Loan Repaid"
    private String createdAt; // Timestamp of the transaction

    // Constructor
    public Transaction(int id, String accountNumber, double amount, String toAccount, String fromAccount, String type, String createdAt) {
        this.id = id;
        this.accountNumber = accountNumber; // Might be redundant if using to/from, but good for context
        this.amount = amount;
        this.toAccount = toAccount;
        this.fromAccount = fromAccount;
        this.type = type;
        this.createdAt = createdAt;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getAmount() {
        return amount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public String getType() {
        return type;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}   