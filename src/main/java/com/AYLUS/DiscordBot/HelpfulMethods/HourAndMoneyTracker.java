package com.AYLUS.DiscordBot.HelpfulMethods;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class HourAndMoneyTracker {

    private static final String DATA_FILE = "volunteer_info.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static double totalHours;
    private static double totalMoneySpent; // The total money the person owes aylus(probably for food stuff lol)
    private static double totalMoneyPaidBack;

    public HourAndMoneyTracker(double totalHours, double totalMoneySpent, double totalMoneyPaidBack) {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            // Load existing data
            HourAndMoneyTracker loaded = loadFromJson();
            HourAndMoneyTracker.totalHours = loaded.totalHours;
            HourAndMoneyTracker.totalMoneySpent = loaded.totalMoneySpent;
            HourAndMoneyTracker.totalMoneyPaidBack = loaded.totalMoneyPaidBack;
        } else {
            // File does not exist, start at 0 and create JSON
            HourAndMoneyTracker.totalHours = 0.0;
            HourAndMoneyTracker.totalMoneySpent = 0.0;
            HourAndMoneyTracker.totalMoneyPaidBack = 0.0;
            saveToJson();
        }
        saveToJson();
    }


    public void updateHoursAndMoney(double hours, double moneyNeeded, double moneyPaidBack) {
        totalHours += hours;
        totalMoneySpent += moneyNeeded;
        totalMoneyPaidBack += moneyPaidBack;
        saveToJson();
    }

    private void saveToJson() {
        try (FileWriter writer = new FileWriter(DATA_FILE)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HourAndMoneyTracker loadFromJson() {
        try (FileReader reader = new FileReader(DATA_FILE)) {
            return gson.fromJson(reader, HourAndMoneyTracker.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new HourAndMoneyTracker(0,0,0); // fallback
        }
    }

    public static double getTotalHours() { return totalHours; }
    public static double getTotalMoneyNeeded() { return totalMoneySpent; }
    public static double getTotalMoneyPaidBack() { return totalMoneyPaidBack; }

}
