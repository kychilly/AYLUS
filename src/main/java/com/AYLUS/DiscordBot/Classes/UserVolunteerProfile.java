package com.AYLUS.DiscordBot.Classes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserVolunteerProfile {

    private String userId;
    private String username;
    private double totalHours;
    private List<VolunteerEntry> entries;
    private List<PaymentEntry> paymentHistory = new ArrayList<>();
    double totalMoneyOwed;

    public UserVolunteerProfile(String userId, String username) {
        this.userId = userId;
        this.username = username;
        this.totalHours = 0.0;
        this.totalMoneyOwed = 0.0;
        this.entries = new ArrayList<>();
        this.paymentHistory = new ArrayList<>(); // How is this list null ?
    }


    // /payment-history command stuff here
    public void recordPayment(double amount, String description) {
        if (paymentHistory == null) {
            paymentHistory = new ArrayList<>(); // CHECK IF LIST IS NULL???
            System.out.println("list is null pls fix");
        }
        paymentHistory.add(new PaymentEntry(
                amount,
                description,
                LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        ));
    }

    public List<PaymentEntry> getPaymentHistory() {
        return paymentHistory;
    }

    public double getTotalPaid() {
        return paymentHistory.stream()
                .mapToDouble(PaymentEntry::getAmount)
                .sum();
    }

    public void setTotalHours(double totalHours) {
        this.totalHours = totalHours;
    }


    public void addEntry(String eventName, double hours, String date, double moneyOwed) {
        VolunteerEntry entry = new VolunteerEntry(eventName, hours, date, moneyOwed);
        entries.add(entry);
        totalHours += hours;
        totalMoneyOwed += moneyOwed;
    }

    public void clearAllEntries() {
        this.entries.clear();
        this.totalHours = 0;
        this.paymentHistory.clear(); // Clears payment history(obviously)
        this.totalMoneyOwed = 0;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public double getTotalHours() { return totalHours; }
    public double getTotalMoneyOwed() { return totalMoneyOwed; }
    public List<VolunteerEntry> getEntries() { return entries; }
    public void setTotalMoneyOwed(double totalMoneyOwed) {
        this.totalMoneyOwed = totalMoneyOwed;
    }

}

