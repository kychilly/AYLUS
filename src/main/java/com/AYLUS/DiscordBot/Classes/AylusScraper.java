package com.AYLUS.DiscordBot.Classes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.AYLUS.DiscordBot.Classes.VolunteerCommands.getVolunteerManager;

public class AylusScraper {

    public static class ScrapedEntry {
        private final String volunteerName;
        private final String branchName;
        private final String eventTitle;
        private final String eventDate;
        private final double hours;
        private final double fundsRaised;
        private final String postPermalink;

        public ScrapedEntry(String rawName, String branchName, String eventTitle, String eventDate, double hours, double fundsRaised, String postPermalink) {
            this.volunteerName = sanitizeJsonString(rawName);
            this.branchName = sanitizeJsonString(branchName);
            this.eventTitle = sanitizeJsonString(eventTitle);
            this.eventDate = sanitizeJsonString(eventDate);
            this.hours = hours;
            this.fundsRaised = fundsRaised;
            this.postPermalink = postPermalink != null ? sanitizeJsonString(postPermalink) : "";
        }

        public String getVolunteerName() { return volunteerName; }
        public String getBranchName() { return branchName; }
        public String getEventTitle() { return eventTitle; }
        public String getEventDate() { return eventDate; }
        public double getHours() { return hours; }
        public double getFundsRaised() { return fundsRaised; }
        public String getPostPermalink() { return postPermalink; }
    }

    private static final Pattern NAME_HOURS_PATTERN = Pattern.compile(
            "\\b([A-Z][a-zA-Z'\\-]+(?:\\s+[A-Z][a-zA-Z'\\-]+){1,3})\\b" +
                    "\\s*[:\\-]?\\s*" +
                    "\\(?\\s*" +
                    "(?:\\d{1,2}:\\d{2}\\s*(?i:[ap]\\.?m\\.?)?\\s*-\\s*\\d{1,2}:\\d{2}\\s*(?i:[ap]\\.?m\\.?)?\\s*)?" +
                    "(\\d+(?:\\.\\d+)?)\\s*(?i:hours?|hrs?|h)\\b" +
                    "\\)?"
    );

    private static final Pattern SESSION_HEADER_PATTERN = Pattern.compile(
            "(?:session|group|shift|slot|part|wave)\\s*\\d*\\s*[\\(\\[]?(\\d+(?:\\.\\d+)?)\\s*(?:hours?|hrs?|h)(?:\\s*each)?[\\)\\]]?:?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EVENT_POST_URL_PATTERN = Pattern.compile(
            "aylus\\.org/\\d{4}/\\d{2}/\\d{2}/[^/?#]+/?$", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern HUMAN_NAME_PATTERN = Pattern.compile(
            "\\b([A-Z][a-zA-Z\\-']+(?:\\s+[A-Z]\\.?)?(?:\\s+[A-Z][a-zA-Z\\-']+){1,3})\\b"
    );

    private static final Pattern EXPLICIT_HOURS_PATTERN = Pattern.compile(
            "(?:[:\\-\\(\\s]+|^)(\\d+(?:\\.\\d+)?)\\s*(?:hours?|hrs?|h)?(?=[\\)\\s\\,\\;\\.]|$)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DEFAULT_EVENT_HOURS_PATTERN = Pattern.compile(
            "(?:duration|length|event time|total time|time|hours?):?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:hours?|hrs?|h)?\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GROUPED_VOLUNTEERS_PATTERN = Pattern.compile(
            "(?:volunteers|participants|members|attendees|helpers).*?[\\(\\[](\\d+(?:\\.\\d+)?)\\s*(?:hours?|hrs?|h)?\\s*(?:each)?[\\)\\]]:?\\s*(.*)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FUND_PATTERN = Pattern.compile(
            "(?:raised|donated|collected|total of)?\\s*\\$(\\d+(?:,\\d{3})*(?:\\.\\d{2})?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> INVALID_NAME_WORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december",
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "aylus", "branch", "volunteers", "participants", "members", "attendees", "hours", "report", "thanks", "duration",
            "president", "advisor", "secretary", "treasurer", "member", "previous", "position", "name", "email", "status",
            "reported", "written", "read", "post", "next", "page", "session", "group", "shift", "slot"
    )));

    private static final Set<String> SEEN_ENTRIES = Collections.synchronizedSet(new HashSet<>());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final AtomicInteger branchFetchFailures = new AtomicInteger(0);
    private static final AtomicInteger searchPageFetchFailures = new AtomicInteger(0);
    private static final AtomicInteger eventFetchFailures = new AtomicInteger(0);
    private static final AtomicInteger eventsWithNoMatches = new AtomicInteger(0);
    private static final AtomicInteger skippedInvalidHours = new AtomicInteger(0);

    // --- REVISED FAST-FAIL & ANTI-RATE-LIMIT CONFIGURATION ---
    private static final int MAX_SEARCH_PAGES = 10;
    private static final int HTTP_READ_TIMEOUT_MS = 3500;   // Fast-fail: skip page if loading takes >3.5 sec
    private static final int MAX_HTTP_RETRIES = 2;            // Single quick retry on fail
    private static final int CONCURRENT_WORKERS = 4;          // Optimal rate-limit safe concurrency
    private static final int POLITE_DELAY_MS = 300;           // Mild delay between requests

    /**
     * Executes Jsoup HTTP requests with fast timeout failure and rate-limit mitigation headers.
     */
    private static Document fetchDocumentWithRetry(String rawUrl) throws IOException {
        String url = normalizeProtocol(rawUrl);
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_HTTP_RETRIES; attempt++) {
            try {
                // Short polite spacing to avoid hitting WAF thresholds
                Thread.sleep(POLITE_DELAY_MS + (long) (Math.random() * 300));

                return Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Referer", "https://aylus.org/")
                        .header("Connection", "keep-alive")
                        .timeout(HTTP_READ_TIMEOUT_MS)
                        .get();

            } catch (IOException e) {
                lastException = e;
                if (attempt < MAX_HTTP_RETRIES) {
                    try {
                        Thread.sleep(250); // Fast retry delay
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", ie);
            }
        }
        throw lastException;
    }

    private static String normalizeProtocol(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        if (trimmed.startsWith("http://")) {
            return "https://" + trimmed.substring(7);
        }
        return trimmed;
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

        File storageFolder = new File("json_data_storage");
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }

        branchFetchFailures.set(0);
        searchPageFetchFailures.set(0);
        eventFetchFailures.set(0);
        eventsWithNoMatches.set(0);
        skippedInvalidHours.set(0);

        Map<String, String> branches = fetchAllBranchesFromDirectory();

        int totalBranches = branches.size();
        System.out.println("Discovered " + totalBranches + " total branches across the U.S.");

        int currentBranchIndex = 1;
        int totalRecordsScraped = 0;

        ExecutorService threadPool = Executors.newFixedThreadPool(CONCURRENT_WORKERS);

        try {
            for (Map.Entry<String, String> entry : branches.entrySet()) {
                String branchName = entry.getKey();
                String branchUrl = entry.getValue();

                String safeFileName = branchName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".json";
                File branchFile = new File(storageFolder, safeFileName);

                if (branchFile.exists() && branchFile.length() > 0) {
                    System.out.println(String.format("[%d/%d] Skipping already saved branch: %s", currentBranchIndex, totalBranches, branchName));
                    currentBranchIndex++;
                    continue;
                }

                System.out.println(String.format("[%d/%d] Scraping Branch: %s", currentBranchIndex, totalBranches, branchName));

                // --- ISOLATION PURGE BEFORE SCRAPING BRANCH ---
                SEEN_ENTRIES.clear();
                if (getVolunteerManager() != null) {
                    getVolunteerManager().clear();
                }

                int recordsFound = scrapeBranchConcurrent(branchName, branchUrl, threadPool);
                totalRecordsScraped += recordsFound;

                saveBranchData(branchFile);

                System.out.println(String.format("%d/%d finished: %s (%d records added)", currentBranchIndex, totalBranches, branchName, recordsFound));

                // --- ISOLATION PURGE AFTER SAVING BRANCH ---
                if (getVolunteerManager() != null) {
                    getVolunteerManager().clear();
                }
                SEEN_ENTRIES.clear();
                System.gc();

                currentBranchIndex++;
            }
        } finally {
            threadPool.shutdown();
        }

        System.out.println("Scrape finished! Processed " + totalRecordsScraped + " volunteer records.");
        System.out.println("Branch roster page fetch failures: " + branchFetchFailures.get());
        System.out.println("Search results page fetch failures: " + searchPageFetchFailures.get());
        System.out.println("Event post fetch failures: " + eventFetchFailures.get());
        System.out.println("Events with zero name/hour matches: " + eventsWithNoMatches.get());
        System.out.println("Individual name matches skipped for invalid hours: " + skippedInvalidHours.get());
    }

    private static void saveBranchData(File branchFile) {
        try (FileWriter writer = new FileWriter(branchFile)) {
            if (getVolunteerManager() != null && getVolunteerManager().getProfiles() != null) {
                GSON.toJson(getVolunteerManager().getProfiles(), writer);
            } else {
                GSON.toJson(new HashMap<>(), writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to write JSON for " + branchFile.getName() + ": " + e.getMessage());
        }
    }

    private static Map<String, String> fetchAllBranchesFromDirectory() {
        Map<String, String> branchMap = new LinkedHashMap<>();
        try {
            Document doc = fetchDocumentWithRetry("https://aylus.org/branches/");

            Elements links = doc.select(".entry-content li a, .entry-content p a");

            for (Element link : links) {
                String href = link.absUrl("href").trim();
                String name = link.text().trim();

                if (href.contains("aylus.org") && !href.equals("https://aylus.org/branches/")
                        && !name.isEmpty() && !href.contains("#")
                        && !href.toLowerCase().contains("application")
                        && !href.toLowerCase().contains("form")
                        && !href.toLowerCase().contains(".docx")) {

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

    private static int scrapeBranchConcurrent(String branchDisplayName, String branchPageUrl, ExecutorService threadPool) {
        String searchTerm = deriveSearchTerm(branchDisplayName, branchPageUrl);

        Set<String> eventUrls = new LinkedHashSet<>();
        eventUrls.addAll(discoverEventUrlsViaSearch(searchTerm, branchPageUrl));
        eventUrls.addAll(discoverEventUrlsViaRosterPage(branchPageUrl));

        if (eventUrls.isEmpty()) {
            return 0;
        }

        List<Future<Integer>> futures = new ArrayList<>();
        for (String url : eventUrls) {
            futures.add(threadPool.submit(() -> scrapeEventPost(url, branchDisplayName)));
        }

        int totalFound = 0;
        try {
            for (Future<Integer> future : futures) {
                try {
                    totalFound += future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    // Task-level exception logged
                }
            }
        } finally {
            futures.clear(); // Release strong references to completed tasks immediately
            eventUrls.clear();
        }

        return totalFound;
    }

    private static String deriveSearchTerm(String branchDisplayName, String branchPageUrl) {
        String path = branchPageUrl.replaceAll("/+$", "");
        int lastSlash = path.lastIndexOf('/');
        String slug = (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
        slug = slug.replaceAll("(?i)\\.docx$", "");

        if (!slug.isEmpty()) {
            String[] parts = slug.split("-");
            List<String> keep = new ArrayList<>();
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                boolean isLastAndLikelyState = (i == parts.length - 1) && part.length() == 2 && part.matches("(?i)[a-z]{2}");
                if (!isLastAndLikelyState && !part.isEmpty()) {
                    keep.add(part);
                }
            }
            String derived = String.join(" ", keep).trim();
            if (derived.length() >= 3) return derived;
        }

        String name = branchDisplayName;
        int parenIdx = name.indexOf('(');
        if (parenIdx > 0) name = name.substring(0, parenIdx);
        int commaIdx = name.indexOf(',');
        if (commaIdx > 0) name = name.substring(0, commaIdx);
        name = name.replaceAll("(?i)\\bbranch\\b", "").trim();
        return name;
    }

    private static Set<String> discoverEventUrlsViaSearch(String searchTerm, String branchPageUrl) {
        Set<String> urls = new LinkedHashSet<>();
        if (searchTerm == null || searchTerm.trim().length() < 3) return urls;

        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(searchTerm.trim(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return urls;
        }

        String normalizedBranchUrl = normalizeUrl(branchPageUrl);
        int page = 1;

        while (page <= MAX_SEARCH_PAGES) {
            String pageUrl = (page == 1)
                    ? "https://aylus.org/?s=" + encodedQuery
                    : "https://aylus.org/page/" + page + "/?s=" + encodedQuery;

            try {
                Document doc = fetchDocumentWithRetry(pageUrl);

                Elements titleLinks = doc.select(".entry-title a");

                if (titleLinks.isEmpty()) {
                    break;
                }

                int addedThisPage = 0;
                for (Element link : titleLinks) {
                    String href = link.absUrl("href").trim();
                    if (href.isEmpty()) continue;
                    if (!EVENT_POST_URL_PATTERN.matcher(href).find()) continue;
                    if (normalizeUrl(href).equals(normalizedBranchUrl)) continue;
                    if (urls.add(href)) {
                        addedThisPage++;
                    }
                }

                if (addedThisPage == 0) {
                    break;
                }

                page++;
            } catch (IOException e) {
                searchPageFetchFailures.incrementAndGet();
                break;
            }
        }

        return urls;
    }

    private static Set<String> discoverEventUrlsViaRosterPage(String branchPageUrl) {
        Set<String> urls = new LinkedHashSet<>();
        try {
            Document branchDoc = fetchDocumentWithRetry(branchPageUrl);

            Element contentElem = branchDoc.selectFirst(".entry-content, .post-content");
            if (contentElem == null) return urls;

            String normalizedBranchUrl = normalizeUrl(branchPageUrl);

            for (Element link : contentElem.select("a[href]")) {
                String href = link.absUrl("href").trim();
                if (href.isEmpty()) continue;
                if (!EVENT_POST_URL_PATTERN.matcher(href).find()) continue;
                if (normalizeUrl(href).equals(normalizedBranchUrl)) continue;
                urls.add(href);
            }
        } catch (IOException e) {
            branchFetchFailures.incrementAndGet();
            System.err.println("Skipped slow/unresponsive branch roster page: " + branchPageUrl);
        }
        return urls;
    }

    private static int scrapeEventPost(String eventUrl, String branchName) {
        try {
            Document postDoc = fetchDocumentWithRetry(eventUrl);

            Element contentElem = postDoc.selectFirst(".entry-content, .post-content");
            if (contentElem == null) return 0;

            String eventTitle = extractCleanEventTitle(postDoc);
            Element dateElem = postDoc.selectFirst(".entry-date, time, .published, .updated");
            String eventDate = (dateElem != null) ? dateElem.text().trim() : "";

            String fullText = cleanUnwantedChars(contentElem.text());

            // Fast Pre-Filter: if text lacks hour indicators, skip heavy regex routines
            String lowerText = fullText.toLowerCase();
            if (!lowerText.contains("hour") && !lowerText.contains("hrs") && !lowerText.contains("h)")) {
                eventsWithNoMatches.incrementAndGet();
                System.out.println(String.format("  ↳ [SKIP - 0 hrs] %s", truncateForConsole(eventTitle)));
                return 0;
            }

            double totalFunds = extractFunds(fullText);

            int found = parseNameHoursPairs(fullText, branchName, eventTitle, eventDate, totalFunds, eventUrl);

            if (found == 0) {
                found += parseSessionStructuredBlocks(contentElem, branchName, eventTitle, eventDate, totalFunds, eventUrl);
            }

            if (found == 0) {
                double defaultPostHours = extractDefaultPostHours(fullText);
                Elements contentNodes = contentElem.select("p, li, tr, td");
                if (!contentNodes.isEmpty()) {
                    for (Element node : contentNodes) {
                        String nodeText = cleanUnwantedChars(node.text());
                        if (nodeText.isEmpty()) continue;

                        int groupedFound = parseGroupedVolunteers(nodeText, branchName, eventTitle, eventDate, totalFunds, eventUrl);
                        if (groupedFound > 0) {
                            found += groupedFound;
                        } else {
                            found += parseTextSegment(nodeText, branchName, eventTitle, eventDate, totalFunds, eventUrl, defaultPostHours);
                        }
                    }
                }
            }

            if (found == 0) {
                eventsWithNoMatches.incrementAndGet();
                System.out.println(String.format("  ↳ [0 entries] %s", truncateForConsole(eventTitle)));
            } else {
                // LIVE TERMINAL PROGRESS FEEDBACK
                System.out.println(String.format("  ↳ [Scraped %d entries] %s (%s)", found, truncateForConsole(eventTitle), eventDate));
            }

            return found;

        } catch (IOException e) {
            eventFetchFailures.incrementAndGet();
            System.err.println("  ↳ [TIMEOUT/ERROR] Skipping event: " + eventUrl);
            return 0;
        }
    }

    // Helper method to keep terminal lines neatly formatted
    private static String truncateForConsole(String text) {
        if (text == null) return "";
        return text.length() > 60 ? text.substring(0, 57) + "..." : text;
    }

    private static int parseSessionStructuredBlocks(Element contentElem, String branchName, String eventTitle, String eventDate, double funds, String permalink) {
        String htmlText = contentElem.html().replaceAll("(?i)<br\\s*/?>", "\n");
        Document tempDoc = Jsoup.parse(htmlText);
        String textWithNewlines = tempDoc.text().replaceAll("\\n+", "\n");

        String[] lines = textWithNewlines.split("\n");
        int found = 0;
        double currentSessionHours = 0.0;

        for (String line : lines) {
            String cleanLine = line.trim();
            if (cleanLine.isEmpty()) continue;

            Matcher sessionMatcher = SESSION_HEADER_PATTERN.matcher(cleanLine);
            if (sessionMatcher.find()) {
                double parsedHours = parseDoubleSafe(sessionMatcher.group(1));
                if (isValidHourValue(parsedHours)) {
                    currentSessionHours = parsedHours;

                    String remainder = cleanLine.substring(sessionMatcher.end()).trim();
                    if (!remainder.isEmpty()) {
                        found += extractNamesFromBlock(remainder, currentSessionHours, branchName, eventTitle, eventDate, funds, permalink);
                    }
                    continue;
                }
            }

            if (currentSessionHours > 0.0) {
                int extracted = extractNamesFromBlock(cleanLine, currentSessionHours, branchName, eventTitle, eventDate, funds, permalink);
                if (extracted > 0) {
                    found += extracted;
                } else if (cleanLine.toLowerCase().contains("congratulations") || cleanLine.toLowerCase().contains("thanks")) {
                    currentSessionHours = 0.0;
                }
            }
        }
        return found;
    }

    private static int extractNamesFromBlock(String line, double hours, String branchName, String eventTitle, String eventDate, double funds, String permalink) {
        int count = 0;
        String[] candidates = line.split("[,;\\n]|\\band\\b");
        for (String cand : candidates) {
            String cleanName = stripTitlesAndRoles(cand);
            if (isValidHumanName(cleanName)) {
                ScrapedEntry entry = new ScrapedEntry(cleanName, branchName, eventTitle, eventDate, hours, funds, permalink);
                if (processAndPersistEntry(entry)) count++;
            }
        }
        return count;
    }

    private static int parseNameHoursPairs(String fullText, String branchName, String eventTitle, String eventDate, double funds, String permalink) {
        Matcher m = NAME_HOURS_PATTERN.matcher(fullText);
        int count = 0;

        while (m.find()) {
            String rawName = m.group(1).trim();
            double hours = parseDoubleSafe(m.group(2));
            String cleanName = stripTitlesAndRoles(rawName);

            if (!isValidHumanName(cleanName)) continue;

            if (!isValidHourValue(hours)) {
                skippedInvalidHours.incrementAndGet();
                continue;
            }

            ScrapedEntry entry = new ScrapedEntry(cleanName, branchName, eventTitle, eventDate, hours, funds, permalink);
            if (processAndPersistEntry(entry)) count++;
        }
        return count;
    }

    private static int parseGroupedVolunteers(String text, String branchName, String eventTitle, String eventDate, double funds, String permalink) {
        Matcher m = GROUPED_VOLUNTEERS_PATTERN.matcher(text);

        if (m.find()) {
            double hours = parseDoubleSafe(m.group(1));
            String namesBlock = m.group(2);

            if (!isValidHourValue(hours)) return 0;

            String[] possibleNames = namesBlock.split("[,;]|\\band\\b");
            int count = 0;

            for (String rawName : possibleNames) {
                String cleanName = stripTitlesAndRoles(rawName);
                if (isValidHumanName(cleanName)) {
                    ScrapedEntry entry = new ScrapedEntry(cleanName, branchName, eventTitle, eventDate, hours, funds, permalink);
                    if (processAndPersistEntry(entry)) count++;
                }
            }
            return count;
        }
        return 0;
    }

    private static int parseTextSegment(String segment, String branchName, String eventTitle, String eventDate, double totalFunds, String permalink, double defaultHours) {
        String cleanSegment = segment.trim();
        if (cleanSegment.isEmpty()) return 0;
        int found = 0;

        Matcher nameMatcher = HUMAN_NAME_PATTERN.matcher(cleanSegment);

        while (nameMatcher.find()) {
            String candidateName = nameMatcher.group(1).trim();
            String cleanedCandidate = stripTitlesAndRoles(candidateName);

            if (isValidHumanName(cleanedCandidate)) {
                int matchStart = Math.max(0, nameMatcher.start() - 15);
                int matchEnd = Math.min(cleanSegment.length(), nameMatcher.end() + 30);
                String localContext = cleanSegment.substring(matchStart, matchEnd);

                Matcher hourMatcher = EXPLICIT_HOURS_PATTERN.matcher(localContext);
                double hoursToAssign = 0.0;

                if (hourMatcher.find()) {
                    hoursToAssign = parseDoubleSafe(hourMatcher.group(1));
                } else if (defaultHours > 0.0) {
                    hoursToAssign = defaultHours;
                }

                if (isValidHourValue(hoursToAssign)) {
                    ScrapedEntry entry = new ScrapedEntry(cleanedCandidate, branchName, eventTitle, eventDate, hoursToAssign, totalFunds, permalink);
                    if (processAndPersistEntry(entry)) found++;
                } else {
                    skippedInvalidHours.incrementAndGet();
                }
            }
        }
        return found;
    }

    private static String stripTitlesAndRoles(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("(?i)\\b(president|vice president|vice leader|leader|advisor|secretary|treasurer|member|volunteer|co-president|reported by|written by|report by)\\b", "")
                .replaceAll("[\\(\\)\\[\\]\\{\\}\\:\\*\\•\\-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String sanitizeJsonString(String input) {
        if (input == null) return "";
        return input.replace("\"", "'")
                .replace("\\", "/")
                .replaceAll("[\\r\\n\\t]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean isValidHumanName(String name) {
        if (name == null) return false;
        String clean = name.trim();
        if (clean.length() < 3 || clean.length() > 35) return false;

        String[] words = clean.split("\\s+");
        if (words.length < 2 || words.length > 4) return false;

        for (String word : words) {
            String lower = word.toLowerCase().replaceAll("[^a-z]", "");
            if (INVALID_NAME_WORDS.contains(lower)) return false;
        }
        return true;
    }

    private static boolean isValidHourValue(double hours) {
        if (hours >= 1900.0 && hours <= 2100.0) return false;
        return hours > 0.0 && hours <= 24.0;
    }

    private static double extractDefaultPostHours(String fullText) {
        Matcher m = DEFAULT_EVENT_HOURS_PATTERN.matcher(fullText);
        if (m.find()) {
            double hrs = parseDoubleSafe(m.group(1));
            if (isValidHourValue(hrs)) return hrs;
        }
        return 0.0;
    }

    private static String extractCleanEventTitle(Document postDoc) {
        Element titleElem = postDoc.selectFirst(".entry-title, h1.entry-title, h1");
        if (titleElem != null) {
            String title = titleElem.text().trim();
            if (!title.isEmpty() && title.length() > 3) return sanitizeTitle(title);
        }
        String docTitle = postDoc.title();
        if (docTitle != null && !docTitle.isEmpty()) {
            String cleaned = docTitle.replaceAll("(?i)\\s*[–\\-]\\s*Alliance of Youth Leaders.*$", "").trim();
            if (!cleaned.isEmpty()) return sanitizeTitle(cleaned);
        }
        return "AYLUS Volunteer Event";
    }

    private static String cleanUnwantedChars(String text) {
        if (text == null) return "";
        return text.replace('\u00A0', ' ')
                .replaceAll("[•\t\r\u2013\u2014]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String sanitizeTitle(String title) {
        if (title == null || title.isEmpty()) return "AYLUS Volunteer Event";
        String clean = sanitizeJsonString(title);
        return clean.length() > 120 ? clean.substring(0, 117) + "..." : clean;
    }

    private static double parseDoubleSafe(String val) {
        try {
            return Double.parseDouble(val);
        } catch (Exception e) {
            return 0.0;
        }
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

    private static String normalizeUrl(String url) {
        if (url == null) return "";
        String normalized = url.trim().toLowerCase();
        normalized = normalized.replaceFirst("^https?://", "");
        normalized = normalized.replaceFirst("/+$", "");
        return normalized;
    }

    private static boolean processAndPersistEntry(ScrapedEntry entry) {
        String permalinkKey = !entry.getPostPermalink().isEmpty()
                ? entry.getPostPermalink().toLowerCase()
                : entry.getEventTitle().toLowerCase();

        String uniqueKey = entry.getVolunteerName().toLowerCase() + "|"
                + permalinkKey + "|"
                + entry.getEventDate().toLowerCase() + "|"
                + entry.getBranchName().toLowerCase();

        if (SEEN_ENTRIES.add(uniqueKey)) {
            if (getVolunteerManager() != null) {
                getVolunteerManager().addLog(
                        entry.getVolunteerName(),
                        entry.getBranchName(),
                        entry.getEventTitle(),
                        entry.getEventDate(),
                        entry.getHours(),
                        0.0 // Set money to 0.0 for every scraped entry
                );
            }
            return true;
        }
        return false;
    }
}