package com.AYLUS.DiscordBot.Classes;

import java.util.ArrayList;
import java.util.List;

public class UserVolunteerProfile {

    private String userId;
    private String username;
    private double totalHours;
    private List<VolunteerEntry> entries;
    double totalMoneyOwed;

    public UserVolunteerProfile(String userId, String username) {
        this.userId = userId;
        this.username = username;
        this.totalHours = 0.0;
        this.entries = new ArrayList<>();
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

    public void clearEntries() {
        this.entries.clear();
        this.totalHours = 0;
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

