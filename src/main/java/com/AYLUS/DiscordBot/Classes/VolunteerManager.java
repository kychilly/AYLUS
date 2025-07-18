package com.AYLUS.DiscordBot.Classes;

// VolunteerManager.java
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;


import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import com.AYLUS.DiscordBot.Classes.UserVolunteerProfile;
import com.AYLUS.DiscordBot.Classes.VolunteerManager;


public class VolunteerManager {
    private static final String DATA_FILE = "volunteer_data.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<String, UserVolunteerProfile> profiles;

    public VolunteerManager() {
        this.profiles = loadData();
    }

    private Map<String, UserVolunteerProfile> loadData() {
        try (Reader reader = new FileReader(DATA_FILE)) {
            Type type = new TypeToken<Map<String, UserVolunteerProfile>>(){}.getType();
            Map<String, UserVolunteerProfile> data = gson.fromJson(reader, type);
            return data != null ? data : new HashMap<>();
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    public void saveData() {
        try (Writer writer = new FileWriter(DATA_FILE)) {
            gson.toJson(profiles, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Optional payment tracking thing that I don't wanna implement
    public void logPayment(String userId, String username, double amount) {
        logHours(userId, username, "Payment", 0,
                LocalDate.now().toString(), -amount);
    }

    //dangerous dangerous
    public void clearProfile(String userId) {
        UserVolunteerProfile profile = profiles.get(userId);
        if (profile != null) {
            profile.getEntries().clear();
            profile.setTotalHours(0);
            profile.setTotalMoneyOwed(0);
            saveData();
        }
    }

    public UserVolunteerProfile getProfile(String userId, String username) {
        return profiles.computeIfAbsent(userId, k -> new UserVolunteerProfile(userId, username));
    }

    public void logHours(String userId, String username, String eventName, double hours, String date, double moneyOwed) {
        UserVolunteerProfile profile = getProfile(userId, username);
        profile.addEntry(eventName, hours, date, moneyOwed);
        saveData();
    }

    public List<UserVolunteerProfile> getLeaderboard() {
        return profiles.values().stream()
                .sorted((a, b) -> Double.compare(b.getTotalHours(), a.getTotalHours()))
                .collect(Collectors.toList());
    }

    public boolean removeEvent(String userId, String eventName, String date) {
        UserVolunteerProfile profile = profiles.get(userId);
        if (profile == null) return false;

        // Find and remove the matching entry
        for (Iterator<VolunteerEntry> it = profile.getEntries().iterator(); it.hasNext();) {
            VolunteerEntry entry = it.next();
            if (entry.getEventName().equalsIgnoreCase(eventName) &&
                    entry.getDate().equals(date)) {

                // Subtract the entry's hours and money from totals
                profile.setTotalHours(profile.getTotalHours() - entry.getHours());
                profile.totalMoneyOwed -= entry.getMoney();  // Add this line

                it.remove();
                saveData();
                return true;
            }
        }
        return false;
    }

}