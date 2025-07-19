package com.AYLUS.DiscordBot.Classes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PaymentEntry {
    private final double amount;
    private final String description;
    private final String date;

    public PaymentEntry(double amount, String description, String date) {
        this.amount = amount;
        this.description = description;
        this.date = date;
    }

    // Getters
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
}