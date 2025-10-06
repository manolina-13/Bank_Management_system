package model;

// Corresponds to Staff table: id, name, mobile, role, password
public class Staff {
    private int id;
    private String name;
    private String mobile;
    private String role; // "STAFF" or "MANAGER"
    private String passwordHash; // Store hash, not plain password
    private String staffNumber; // Assuming staff number is derived or needed, adding it. Check DB schema. If not present, remove. Let's assume 'id' or a separate 'staff_number' field exists. I'll use mobile as the unique identifier for login if staff_number isn't in schema. Re-checking schema: 'name', 'mobile', 'role', 'password'. Ok, let's use mobile for login/identification.

    // Constructor (Assuming login via mobile)
    public Staff(int id, String name, String mobile, String role, String passwordHash) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.role = role;
        this.passwordHash = passwordHash;
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

    public String getRole() {
        return role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}