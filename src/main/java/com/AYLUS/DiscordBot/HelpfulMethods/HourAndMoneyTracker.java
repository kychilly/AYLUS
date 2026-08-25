package com.AYLUS.DiscordBot.HelpfulMethods;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class HourAndMoneyTracker {

    private static final String VOLUNTEER_DATA_FILE = "volunteer_data.json";
    private static final Gson gson = new Gson();

    // Sums all hours across every volunteer in volunteer_data.json
    public static double getTotalHours() {
        File file = new File(VOLUNTEER_DATA_FILE);
        if (!file.exists()) return 0.0;

        double totalHours = 0.0;
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null) return 0.0;

            for (String key : root.keySet()) {
                JsonObject volunteer = root.getAsJsonObject(key);
                if (volunteer.has("totalHours")) {
                    totalHours += volunteer.get("totalHours").getAsDouble();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return totalHours;
    }

    // Counts total unique volunteer entries in volunteer_data.json
    public static int getTotalVolunteersCount() {
        File file = new File(VOLUNTEER_DATA_FILE);
        if (!file.exists()) return 0;

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            return (root != null) ? root.keySet().size() : 0;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // Counts distinct events (title + date) recorded across all volunteers
    public static long getTotalEventsCount() {
        File file = new File(VOLUNTEER_DATA_FILE);
        if (!file.exists()) {
            System.err.println("[DEBUG] File does not exist at: " + file.getAbsolutePath());
            return 0;
        }

        try (FileReader reader = new FileReader(file)) {
            JsonElement rootElement = gson.fromJson(reader, JsonElement.class);
            if (rootElement == null || rootElement.isJsonNull()) return 0;

            // Counts every logged event entry across all volunteers (approx. 3000)
            return countAllLoggedEvents(rootElement);

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to parse events from " + VOLUNTEER_DATA_FILE);
            e.printStackTrace();
        }

        return 0;
    }

    private static long countAllLoggedEvents(JsonElement element) {
        long count = 0;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            // If this JSON object represents an event log entry, increment count
            if (isEventObject(obj)) {
                count++;
            }

            // Continue searching child properties
            for (String key : obj.keySet()) {
                count += countAllLoggedEvents(obj.get(key));
            }

        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                count += countAllLoggedEvents(child);
            }
        }

        return count;
    }

    private static boolean isEventObject(JsonObject obj) {
        // Matches any object containing event-specific log metadata
        return obj.has("eventTitle") || obj.has("eventDate")
                || obj.has("hours") || obj.has("loggedHours")
                || obj.has("fundsRaised");
    }

    private static long traverseAndExtractEvents(JsonElement element, Set<String> uniqueEvents) {
        long count = 0;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            // Check if this specific JSON Object is an Event entry
            if (isEventObject(obj)) {
                count++;
                String title = getAnyStringField(obj, "eventTitle", "title", "eventName", "description", "name", "event");
                String date = getAnyStringField(obj, "eventDate", "date", "time", "timestamp");

                title = title.toLowerCase().trim();
                date = date.trim();

                if (!title.isEmpty()) {
                    uniqueEvents.add(title + "|" + date);
                }
            }

            // Keep searching deeper into all child properties
            for (String key : obj.keySet()) {
                count += traverseAndExtractEvents(obj.get(key), uniqueEvents);
            }

        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                count += traverseAndExtractEvents(child, uniqueEvents);
            }
        }

        return count;
    }

    // Determines if a JSON Object represents an event


    // Safely grabs the first matching string value from a list of key aliases
    private static String getAnyStringField(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsString();
            }
        }
        return "";
    }

    // Sums total funds raised from event entries in volunteer_data.json
    public static double getTotalMoneyNeeded() {
        return fetchTotalNationwideFunds(); // yeah whatever lol
    }

    // Scrapes funds directly from all 172 branch posts, completely independent of student records
    public static double fetchTotalNationwideFunds() {
        double grandTotalFunds = 0.0;

        try {
            // 1. Fetch branch directory
            org.jsoup.nodes.Document dirDoc = org.jsoup.Jsoup.connect("https://aylus.org/branches/")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(5000)
                    .get();

            org.jsoup.select.Elements links = dirDoc.select(".entry-content li a, .entry-content p a");
            java.util.Set<String> branchUrls = new java.util.HashSet<>();

            for (org.jsoup.nodes.Element link : links) {
                String href = link.absUrl("href").trim();
                if (href.contains("aylus.org") && !href.equals("https://aylus.org/branches/")
                        && !href.contains("#") && !href.toLowerCase().contains("application")) {
                    branchUrls.add(href);
                }
            }

            java.util.regex.Pattern fundPattern = java.util.regex.Pattern.compile(
                    "(?:raised|donated|collected|total of)?\\s*\\$(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",
                    java.util.regex.Pattern.CASE_INSENSITIVE
            );

            // 2. Iterate through branches and sum unique post funds
            for (String url : branchUrls) {
                try {
                    org.jsoup.nodes.Document branchDoc = org.jsoup.Jsoup.connect(url)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .timeout(3000)
                            .get();

                    org.jsoup.select.Elements posts = branchDoc.select("article, .post");
                    for (org.jsoup.nodes.Element post : posts) {
                        java.util.regex.Matcher matcher = fundPattern.matcher(post.text());
                        if (matcher.find()) {
                            try {
                                double amount = Double.parseDouble(matcher.group(1).replace(",", ""));
                                // Sanity cap: Ignore unreasonable single-event values (> $50,000)
                                if (amount > 0 && amount <= 50000.0) {
                                    grandTotalFunds += amount;
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                } catch (java.io.IOException ignored) {
                    // Skip timeout/404 branch pages smoothly
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        return grandTotalFunds;
    }

    public static double getTotalMoneyPaidBack() { return 0.0; }
    public static double getTotalMoney() { return getTotalMoneyNeeded(); }
}