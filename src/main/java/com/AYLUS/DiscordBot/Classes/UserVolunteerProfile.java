package com.AYLUS.DiscordBot.Classes;

import java.util.ArrayList;
import java.util.List;

public class UserVolunteerProfile {

    private String userId;
    private String username;
    private double totalHours;
    private List<VolunteerEntry> entries;

    public UserVolunteerProfile(String userId, String username) {
        this.userId = userId;
        this.username = username;
        this.totalHours = 0.0;
        this.entries = new ArrayList<>();
    }

    public void setTotalHours(double totalHours) {
        this.totalHours = totalHours;
    }


    public void addEntry(String eventName, double hours, String date) {
        VolunteerEntry entry = new VolunteerEntry(eventName, hours, date);
        entries.add(entry);
        totalHours += hours;
    }


    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public double getTotalHours() { return totalHours; }
    public List<VolunteerEntry> getEntries() { return entries; }

}

