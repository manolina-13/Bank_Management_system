package model;

public class FD {
    public int id;
    public String accNumber;
    public String dateCreated;
    public double amount;
    public float interestRate;
    public int termYears;
    public String status;

    public FD(int id, String accNumber, String dateCreated, double amount, float interestRate, int termYears,
            String status) {
        this.id = id;
        this.accNumber = accNumber;
        this.dateCreated = dateCreated;
        this.amount = amount;
        this.interestRate = interestRate;
        this.termYears = termYears;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getAccNumber() {
        return accNumber;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public double getAmount() {
        return amount;
    }

    public float getInterestRate() {
        return interestRate;
    }

    public int getTermYears() {
        return termYears;
    }

    public String getStatus() {
        return status;
    }

}
