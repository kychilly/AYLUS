package com.AYLUS.DiscordBot.Classes;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.AYLUS.DiscordBot.Classes.VolunteerCommands.getVolunteerManager;

public class AylusScraper {

    public static class ScrapedEntry {
        private final String formattedName;
        private final String branchName;
        private final String eventTitle;
        private final String eventDate;
        private final double hours;
        private final double fundsRaised;

        public ScrapedEntry(String rawName, String branchName, String eventTitle, String eventDate, double hours, double fundsRaised) {
            this.branchName = branchName;
            this.formattedName = rawName + " - " + branchName;
            this.eventTitle = eventTitle;
            this.eventDate = eventDate;
            this.hours = hours;
            this.fundsRaised = fundsRaised;
        }

        public String getFormattedName() { return formattedName; }
        public String getBranchName() { return branchName; }
        public String getEventTitle() { return eventTitle; }
        public String getEventDate() { return eventDate; }
        public double getHours() { return hours; }
        public double getFundsRaised() { return fundsRaised; }
    }

    // Capture standard names: "First Last" or "First M. Last"
    private static final String NAME_PATTERN_STR = "([A-Z][a-zA-Z\\'\\-]{1,20}(?:\\s+[A-Z]\\.)?\\s+[A-Z][a-zA-Z\\'\\-]{1,20})";

    // Matches: [Name] ... [Number] [hrs/hours/h]
    private static final Pattern FORGIVING_NAME_THEN_HOURS = Pattern.compile(
            "\\b" + NAME_PATTERN_STR + "\\b.{0,40}?\\b(\\d+(?:\\.\\d+)?)\\s*(?:hours?|hrs?|h)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Matches: [Number] [hrs/hours/h] ... [Name] OR [Number] ... [hrs/hours/h] ... [Name]
    private static final Pattern FORGIVING_HOURS_THEN_NAME = Pattern.compile(
            "\\b(\\d+(?:\\.\\d+)?)\\s*(?:hours?|hrs?|h)?\\b.{0,25}?\\b" + NAME_PATTERN_STR + "\\b",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern FUND_PATTERN = Pattern.compile(
            "(?:raised|donated|collected|total of)?\\s*\\$(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> SEEN_ENTRIES = new HashSet<>();

    public static void testScrape(int branchLimit) {
        new Thread(() -> {
            System.out.println("🧪 Running TEST scrape for " + branchLimit + " branches...");
            Map<String, String> branches = fetchAllBranchesFromDirectory();

            int totalRecordsScraped = 0;
            int count = 0;

            for (Map.Entry<String, String> entry : branches.entrySet()) {
                if (count >= branchLimit) break;

                String branchName = entry.getKey();
                String branchUrl = entry.getValue();

                System.out.println(String.format("[%d/%d] Test Scraping: %s", count + 1, branchLimit, branchName));
                int recordsFound = scrapeBranchPage(branchUrl, branchName);
                totalRecordsScraped += recordsFound;

                count++;
            }

            getVolunteerManager().saveData();
            System.out.println("✅ Test finished! Processed " + totalRecordsScraped + " volunteer records.");
        }, "AylusScraper-TestThread").start();
    }

    public static void runFullScrapeAsync() {
        new Thread(() -> {
            try {
                runFullScrape();
            } catch (Exception e) {
                System.err.println("Scraper thread error: " + e.getMessage());
            }
        }, "AylusScraper-Thread").start();
    }

    public static void runFullScrape() {
        System.out.println("Starting full AYLUS branch scrape...");
        Map<String, String> branches = fetchAllBranchesFromDirectory();

        int totalBranches = branches.size();
        System.out.println("==================================================");
        System.out.println("Discovered " + totalBranches + " total branches across the U.S.");
        System.out.println("==================================================");

        int currentBranchIndex = 1;
        int totalRecordsScraped = 0;

        for (Map.Entry<String, String> entry : branches.entrySet()) {
            String branchName = entry.getKey();
            String branchUrl = entry.getValue();

            System.out.println(String.format("[%d/%d] Scraping Branch: %s", currentBranchIndex, totalBranches, branchName));

            int recordsFound = scrapeBranchPage(branchUrl, branchName);
            totalRecordsScraped += recordsFound;

            currentBranchIndex++;
        }

        getVolunteerManager().saveData();
        System.out.println("Scrape finished! Processed " + totalRecordsScraped + " volunteer records. Saved to volunteer_data.json.");
    }

    private static Map<String, String> fetchAllBranchesFromDirectory() {
        Map<String, String> branchMap = new LinkedHashMap<>();
        try {
            Document doc = Jsoup.connect("https://aylus.org/branches/")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(5000)
                    .get();

            Elements links = doc.select(".entry-content li a, .entry-content p a");

            for (Element link : links) {
                String href = link.absUrl("href").trim();
                String name = link.text().trim();

                if (href.contains("aylus.org") && !href.equals("https://aylus.org/branches/")
                        && !name.isEmpty() && !href.contains("#")
                        && !href.toLowerCase().contains("application")
                        && !href.toLowerCase().contains("form")) {

                    if (!name.toLowerCase().contains("branch") && !name.contains("(")) {
                        name = name + " Branch";
                    }

                    if (!branchMap.containsKey(name)) {
                        branchMap.put(name, href);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error fetching main branch directory: " + e.getMessage());
        }
        return branchMap;
    }

    private static int scrapeBranchPage(String branchUrl, String branchName) {
        int pageNum = 1;
        int maxPagesPerBranch = 10;
        int branchRecords = 0;
        String lastFirstPostTitle = "";

        while (pageNum <= maxPagesPerBranch) {
            String currentUrl = (pageNum == 1)
                    ? branchUrl
                    : branchUrl.replaceAll("/$", "") + "/page/" + pageNum + "/";

            try {
                Document doc = Jsoup.connect(currentUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(4000)
                        .get();

                Elements posts = doc.select("article, .post");

                if (posts.isEmpty()) break;

                Element firstTitleElement = posts.first().selectFirst(".entry-title, h2, h1, .post-title");
                String currentFirstPostTitle = (firstTitleElement != null) ? firstTitleElement.text() : "";

                if (!lastFirstPostTitle.isEmpty() && lastFirstPostTitle.equals(currentFirstPostTitle)) {
                    break;
                }
                lastFirstPostTitle = currentFirstPostTitle;

                for (Element post : posts) {
                    Element titleElem = post.selectFirst(".entry-title, h2, h1, .post-title");
                    String eventTitle = sanitizeTitle((titleElem != null) ? titleElem.text() : "AYLUS Event");

                    Element dateElem = post.selectFirst(".entry-date, time, .published");
                    String eventDate = (dateElem != null) ? dateElem.text().trim() : "";

                    double totalFunds = extractFunds(post.text());

                    Element contentElem = post.selectFirst(".entry-content, .post-content, .entry");
                    String fullText = (contentElem != null) ? contentElem.text() : post.text();

                    // Split post by line breaks, commas, or list delimiters
                    String[] textSegments = fullText.split("(?<=[\\.\\n\\r;,])|\\s+-\\s+");

                    for (String segment : textSegments) {
                        String cleanSegment = segment.trim();
                        if (cleanSegment.isEmpty()) continue;

                        // Check Pattern 1: Name then Hours
                        Matcher m1 = FORGIVING_NAME_THEN_HOURS.matcher(cleanSegment);
                        while (m1.find()) {
                            String candidateName = m1.group(1).trim();
                            double hours = parseDoubleSafe(m1.group(2));

                            if (isValidHourValue(hours) && isValidName(candidateName)) {
                                ScrapedEntry entry = new ScrapedEntry(candidateName, branchName, eventTitle, eventDate, hours, totalFunds);
                                if (processAndPersistEntry(entry)) branchRecords++;
                            }
                        }

                        // Check Pattern 2: Hours then Name
                        Matcher m2 = FORGIVING_HOURS_THEN_NAME.matcher(cleanSegment);
                        while (m2.find()) {
                            double hours = parseDoubleSafe(m2.group(1));
                            String candidateName = m2.group(2).trim();

                            if (isValidHourValue(hours) && isValidName(candidateName)) {
                                ScrapedEntry entry = new ScrapedEntry(candidateName, branchName, eventTitle, eventDate, hours, totalFunds);
                                if (processAndPersistEntry(entry)) branchRecords++;
                            }
                        }
                    }
                }
                pageNum++;

            } catch (IOException e) {
                break;
            }
        }
        return branchRecords;
    }

    private static boolean isValidHourValue(double hours) {
        // Ignores years (e.g., 19xx, 20xx) and unrealistic hour logs
        if (hours >= 1900.0 && hours <= 2100.0) return false;
        return hours > 0.0 && hours <= 24.0;
    }

    private static String sanitizeTitle(String title) {
        if (title == null || title.isEmpty()) return "AYLUS Volunteer Event";
        String clean = title.replaceAll("[\"\\\\\\r\\n\\t]", " ").replaceAll("\\s+", " ").trim();
        if (clean.length() > 120) {
            clean = clean.substring(0, 117) + "...";
        }
        return clean;
    }

    private static double parseDoubleSafe(String val) {
        try {
            return Double.parseDouble(val);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static boolean isValidName(String name) {
        if (name == null || name.length() < 3 || name.length() > 30) return false;
        String lower = name.toLowerCase();

        // Filters common header phrases/locations while capturing valid student names
        return !lower.contains("aylus")
                && !lower.contains("branch")
                && !lower.contains("event")
                && !lower.contains("report")
                && !lower.contains("thanks")
                && !lower.contains("congratulations")
                && !lower.contains("president")
                && !lower.contains("advisor")
                && !lower.contains("high school")
                && !lower.contains("united states")
                && !lower.contains("san diego")
                && !lower.contains("columnist")
                && !lower.contains("presents")
                && !lower.contains("hosted")
                && !lower.contains("organized")
                && !lower.contains("volunteers")
                && !lower.contains("participants")
                && !lower.contains("members");
    }

    private static double extractFunds(String text) {
        Matcher matcher = FUND_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1).replace(",", ""));
            } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }

    private static boolean processAndPersistEntry(ScrapedEntry entry) {
        String uniqueKey = entry.getFormattedName().toLowerCase() + "|"
                + entry.getEventTitle() + "|"
                + entry.getEventDate() + "|"
                + entry.getHours();

        if (SEEN_ENTRIES.contains(uniqueKey)) {
            return false;
        }
        SEEN_ENTRIES.add(uniqueKey);

        getVolunteerManager().logHours(
                entry.getFormattedName(),
                entry.getFormattedName(),
                entry.getEventTitle(),
                entry.getHours(),
                entry.getEventDate(),
                0.0
        );
        return true;
    }
}