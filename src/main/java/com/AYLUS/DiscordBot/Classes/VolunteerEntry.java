package com.AYLUS.DiscordBot.Classes;

public class VolunteerEntry {
    private String eventName;
    private double hours;
    private String date;
    private double money;

    public VolunteerEntry(String eventName, double hours, String date, double money) {
        this.eventName = eventName;
        this.hours = hours;
        this.date = date;
        this.money = money;
    }

    // Getters
    public String getEventName() { return eventName; }
    public double getHours() { return hours; }
    public String getDate() { return date; }
    public double getMoney() { return money; }
}