package model;

// Corresponds to Loan table: id, amount, accno, int_rate, date, duration
public class Loan {
    private int id;
    private double amount; // Principal amount
    private String accountNumber;
    private double interestRate; // Annual interest rate at creation
    private String dateCreated; // Date loan was taken (e.g., "YYYY-MM-DD")
    private int durationYears; // Duration in years

    // Constructor
    public Loan(int id, double amount, String accountNumber, double interestRate, String dateCreated, int durationYears) {
        this.id = id;
        this.amount = amount;
        this.accountNumber = accountNumber;
        this.interestRate = interestRate;
        this.dateCreated = dateCreated;
        this.durationYears = durationYears;
    }

    // Getters
    public int getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public int getDurationYears() {
        return durationYears;
    }
}