package model;

// Corresponds to Customer table: id, name, mobile, email, accountno, password
public class Customer {
    private int id;
    private String name;
    private String mobile;
    private String email;
    private String accountNumber; // Changed from accountno for clarity
    private String passwordHash; // Store hash, not plain password

    // Constructor
    public Customer(int id, String name, String mobile, String email, String accountNumber, String passwordHash) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.email = email;
        this.accountNumber = accountNumber;
        this.passwordHash = passwordHash;
        // Note: Balance is not stored here, it might be fetched separately or could be added
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMobile() {
        return mobile;
    }

    public String getEmail() {
        return email;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}