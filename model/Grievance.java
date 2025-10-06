package model;

// Corresponds to Grievance table: id, accno, complain, status, remarks, datetime (implicit from DB)
public class Grievance {
    private int id;
    private String accountNumber;
    private String complain;
    private String status;
    private String remarks;
    private String createdAt; // Store the creation timestamp

    // Constructor
    public Grievance(int id, String accountNumber, String complain, String status, String remarks, String createdAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.complain = complain;
        this.status = status;
        this.remarks = remarks;
        this.createdAt = createdAt;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getComplain() {
        return complain;
    }

    public String getStatus() {
        return status;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}