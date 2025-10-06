package model;

// Corresponds to Partial_Signups table: id, status, name, email, address, mobile, created_at (implicit from DB)
public class PartialSignup {
    private int id;
    private String status; // e.g., "Pending", "Approved", "Rejected"
    private String name;
    private String email;
    private String address;
    private String mobile;
    private String createdAt; // Store the creation timestamp

    // Constructor
    public PartialSignup(int id, String status, String name, String email, String address, String mobile, String createdAt) {
        this.id = id;
        this.status = status;
        this.name = name;
        this.email = email;
        this.address = address;
        this.mobile = mobile;
        this.createdAt = createdAt;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getMobile() {
        return mobile;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}