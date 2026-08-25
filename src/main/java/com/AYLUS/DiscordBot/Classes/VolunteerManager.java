package com.AYLUS.DiscordBot.Classes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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

    // Returns set of all tracked user IDs / formatted names
    public Set<String> getAllVolunteers() {
        return profiles.keySet();
    }

    // Sums up all individual volunteer entries recorded across every profile
    public int getTotalEventsCount() {
        int total = 0;
        for (UserVolunteerProfile profile : profiles.values()) {
            if (profile.getEntries() != null) {
                total += profile.getEntries().size();
            }
        }
        return total;
    }

    // Optional payment tracking
    public void logPayment(String userId, String username, double amount) {
        logHours(userId, username, "Payment", 0,
                LocalDate.now().toString(), -amount);
    }

    public void clearProfile(String userId) {
        UserVolunteerProfile profile = profiles.get(userId);
        if (profile != null) {
            profile.getEntries().clear();
            profile.setTotalHours(0);
            profile.setTotalMoneyOwed(0);
            profile.clearAllEntries();
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
                profile.setTotalMoneyOwed(profile.getTotalMoneyOwed() - entry.getMoney());

                it.remove();
                saveData();
                return true;
            }
        }
        return false;
    }

    public static void repairCorruptJsonFile() {
        File file = new File("volunteer_data.json");
        if (!file.exists()) return;

        try {
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));

            // 1. If it was cut off mid-string or mid-object, close the open braces
            content = content.trim();
            if (!content.endsWith("}")) {
                // Trim any hanging unterminated string or key/value pair at the end
                int lastValidBrace = content.lastIndexOf("}");
                if (lastValidBrace != -1) {
                    content = content.substring(0, lastValidBrace + 1) + "\n}";
                }
            }

            // 2. Parse into JsonObject to ensure valid structure
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(content).getAsJsonObject();

            // 3. Clean all massive eventName strings across all 16,000 lines
            root.entrySet().forEach(entry -> {
                com.google.gson.JsonObject profile = entry.getValue().getAsJsonObject();
                if (profile.has("entries")) {
                    com.google.gson.JsonArray entries = profile.getAsJsonArray("entries");
                    entries.forEach(e -> {
                        com.google.gson.JsonObject event = e.getAsJsonObject();
                        if (event.has("eventName")) {
                            String name = event.get("eventName").getAsString();
                            if (name.length() > 120) {
                                event.addProperty("eventName", name.substring(0, 117) + "...");
                            }
                        }
                    });
                }
            });

            // 4. Overwrite file with fully valid, clean JSON
            try (java.io.FileWriter writer = new java.io.FileWriter("volunteer_data.json")) {
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }
            System.out.println("✅ Successfully repaired volunteer_data.json!");

        } catch (Exception e) {
            System.err.println("Repair failed: " + e.getMessage());
        }
    }

    // Get the event
    public VolunteerEntry getEvent(String userId, String eventName, String date) {
        UserVolunteerProfile profile = profiles.get(userId);
        if (profile == null) return null;

        for (VolunteerEntry entry : profile.getEntries()) {
            if (entry.getEventName().equalsIgnoreCase(eventName) &&
                    entry.getDate().equals(date)) {
                return entry;
            }
        }
        return null;
    }
}