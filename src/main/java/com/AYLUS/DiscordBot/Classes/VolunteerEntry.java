package com.AYLUS.DiscordBot.Classes;

public class VolunteerEntry {
    private String eventName;
    private double hours;
    private String date;

    public VolunteerEntry(String eventName, double hours, String date) {
        this.eventName = eventName;
        this.hours = hours;
        this.date = date;
    }

    // Getters
    public String getEventName() { return eventName; }
    public double getHours() { return hours; }
    public String getDate() { return date; }
}