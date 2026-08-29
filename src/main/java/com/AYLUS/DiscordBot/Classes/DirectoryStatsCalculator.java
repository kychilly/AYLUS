package com.AYLUS.DiscordBot.Classes;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

public class DirectoryStatsCalculator {

    public static class BranchStats {
        public double totalHours = 0.0;
        public int totalVolunteers = 0;
        public long totalEvents = 0;
        public int totalBranches = 0;
    }

    public static BranchStats calculateDirectoryStats(String folderPath) {
        BranchStats stats = new BranchStats();
        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            return stats;
        }

        File[] jsonFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            return stats;
        }

        stats.totalBranches = jsonFiles.length;
        Set<String> uniqueVolunteers = new HashSet<>();

        for (File file : jsonFiles) {
            try (Reader reader = new FileReader(file)) {
                JsonElement rootElement = JsonParser.parseReader(reader);
                if (!rootElement.isJsonObject()) continue;

                JsonObject profiles = rootElement.getAsJsonObject();

                for (String volunteerName : profiles.keySet()) {
                    uniqueVolunteers.add(volunteerName.toLowerCase().trim());
                    JsonObject profile = profiles.getAsJsonObject(volunteerName);

                    if (profile.has("totalHours")) {
                        stats.totalHours += profile.get("totalHours").getAsDouble();
                    }

                    if (profile.has("entries") && profile.get("entries").isJsonArray()) {
                        JsonArray entries = profile.getAsJsonArray("entries");
                        stats.totalEvents += entries.size();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error reading JSON file " + file.getName() + ": " + e.getMessage());
            }
        }

        stats.totalVolunteers = uniqueVolunteers.size();
        return stats;
    }
}