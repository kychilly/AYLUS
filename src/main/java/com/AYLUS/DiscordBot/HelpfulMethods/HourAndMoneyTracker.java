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

    // Static fields to track totals
    private static double totalHours;
    private static double totalMoneySpent;
    private static double totalMoneyPaidBack;

    // Static block to load existing data on class load
    static {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            loadFromJson();
        } else {
            totalHours = 0.0;
            totalMoneySpent = 0.0;
            totalMoneyPaidBack = 0.0;
            saveToJson();
        }
    }

    // Instantly update to json
    public static void updateHoursAndMoney(double hours, double moneyNeeded, double moneyPaidBack) {
        totalHours += hours;
        totalMoneySpent += moneyNeeded;
        totalMoneyPaidBack += moneyPaidBack;
        saveToJson();
    }

    private static void saveToJson() {
        try (FileWriter writer = new FileWriter(DATA_FILE)) {
            // Only serialize the 3 totals
            Data data = new Data(totalHours, totalMoneySpent, totalMoneyPaidBack);
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Loading json static variables
    private static void loadFromJson() {
        try (FileReader reader = new FileReader(DATA_FILE)) {
            Data data = gson.fromJson(reader, Data.class);
            totalHours = data.totalHours;
            totalMoneySpent = data.totalMoneyNeeded;
            totalMoneyPaidBack = data.totalMoneyPaidBack;
        } catch (IOException e) {
            e.printStackTrace();
            totalHours = 0.0;
            totalMoneySpent = 0.0;
            totalMoneyPaidBack = 0.0;
        }
    }

    public static double getTotalHours() { return totalHours; }
    public static double getTotalMoneyNeeded() { return totalMoneySpent; }
    public static double getTotalMoneyPaidBack() { return totalMoneyPaidBack; }

    // Json serialization
    private static class Data {
        double totalHours;
        double totalMoneyNeeded;
        double totalMoneyPaidBack;

        Data(double hours, double moneyNeeded, double moneyPaidBack) {
            this.totalHours = hours;
            this.totalMoneyNeeded = moneyNeeded;
            this.totalMoneyPaidBack = moneyPaidBack;
        }
    }
}
