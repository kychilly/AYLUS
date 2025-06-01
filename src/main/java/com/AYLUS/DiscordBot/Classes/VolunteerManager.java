package com.AYLUS.DiscordBot.Classes;

// VolunteerManager.java
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;


import java.io.*;
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

    private void saveData() {
        try (Writer writer = new FileWriter(DATA_FILE)) {
            gson.toJson(profiles, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public UserVolunteerProfile getProfile(String userId, String username) {
        return profiles.computeIfAbsent(userId, k -> new UserVolunteerProfile(userId, username));
    }

    public void logHours(String userId, String username, String eventName, double hours, String date) {
        UserVolunteerProfile profile = getProfile(userId, username);
        profile.addEntry(eventName, hours, date);
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

        // Case-insensitive comparison and exact date match
        boolean removed = profile.getEntries().removeIf(entry ->
                entry.getEventName().equalsIgnoreCase(eventName) &&
                        entry.getDate().equals(date)
        );

        if (removed) {
            // Recalculate total hours
            profile.setTotalHours(
                    profile.getEntries().stream()
                            .mapToDouble(VolunteerEntry::getHours)
                            .sum()
            );
            saveData();
        }
        return removed;
    }

}