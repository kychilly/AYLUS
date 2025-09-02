package com.AYLUS.DiscordBot.Classes;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfilePagination {
    private static final Map<String, List<List<VolunteerEntry>>> PAGINATION_CACHE = new HashMap<>();
    private static final Map<String, BaseEmbedInfo> BASE_EMBED_CACHE = new HashMap<>();

    // Static class to store info needed to recreate base embed for each page
    public static class BaseEmbedInfo {
        public final String displayName;
        public final String avatarUrl;
        public final double totalHours;
        public final double totalMoneyOwed;

        public BaseEmbedInfo(String displayName, String avatarUrl, double totalHours, double totalMoneyOwed) {
            this.displayName = displayName;
            this.avatarUrl = avatarUrl;
            this.totalHours = totalHours;
            this.totalMoneyOwed = totalMoneyOwed;
        }
    }

    // Create base embed for top of all pages
    public static EmbedBuilder createBaseEmbed(String displayName, String avatarUrl, double totalHours, double totalMoneyOwed) {
        return new EmbedBuilder()
                .setTitle(displayName + "'s Volunteer Profile")
                .setColor(Color.BLUE)
                .setThumbnail(avatarUrl)
                .addField("Total Hours", String.format("⏱️ **%.1f hours**", totalHours), true)
                .addField(totalMoneyOwed > 0 ? "Total Owed(w reimburse)" : "Total AYLUS Owes(w reimburse)",
                        String.format("💰 **$%.2f**", Math.abs(totalMoneyOwed)), true);
    }

    // Format events with descending counter
    public static String formatEventBreakdown(List<VolunteerEntry> entries, int page, int pageSize, int totalEntries) {
        StringBuilder sb = new StringBuilder();
        int counter = totalEntries - (page * pageSize);

        for (VolunteerEntry entry : entries) {
            sb.append("```(").append(counter--).append(")EVENT: ").append(entry.getEventName()).append("\n")
                    .append("\n - Date: ").append(entry.getDate())
                    .append("\n - Hours: ").append(String.format("%.1f", entry.getHours()))
                    .append("\n - Owed: $").append(String.format("%.2f", entry.getMoney()))
                    .append("```\n");
        }
        return sb.toString().trim();
    }

    // Send paginated profile
    public static void sendPaginatedProfile(SlashCommandInteractionEvent event,
                                            String displayName,
                                            String avatarUrl,
                                            double totalHours,
                                            double totalMoneyOwed,
                                            List<List<VolunteerEntry>> chunks,
                                            int page) {
        String cacheKey = event.getUser().getId() + ":" + event.getChannel().getId();
        PAGINATION_CACHE.put(cacheKey, chunks);
        BASE_EMBED_CACHE.put(cacheKey, new BaseEmbedInfo(displayName, avatarUrl, totalHours, totalMoneyOwed));

        int totalEntries = chunks.stream().mapToInt(List::size).sum();

        EmbedBuilder embed = createBaseEmbed(displayName, avatarUrl, totalHours, totalMoneyOwed)
                .setFooter("Page " + (page + 1) + "/" + chunks.size())
                .addField("Event Breakdown", formatEventBreakdown(chunks.get(page), page, 5, totalEntries), false);

        ActionRow buttons = ActionRow.of(
                Button.secondary("profile:prev:" + cacheKey + ":" + page, "◀").withDisabled(page == 0),
                Button.secondary("profile:next:" + cacheKey + ":" + page, "▶").withDisabled(page == chunks.size() - 1)
        );

        event.replyEmbeds(embed.build())
                .addComponents(buttons)
                .queue();
    }

    // Handle pagination button clicks
    public static void handleButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (args.length < 4) {
            event.reply("Invalid button interaction").setEphemeral(true).queue();
            return;
        }

        String action = args[1];
        String cacheKey = args[2] + ":" + args[3];
        int currentPage = Integer.parseInt(args[4]);

        List<List<VolunteerEntry>> chunks = PAGINATION_CACHE.get(cacheKey);
        BaseEmbedInfo baseInfo = BASE_EMBED_CACHE.get(cacheKey);

        if (chunks == null || baseInfo == null) {
            event.reply("This menu has expired. Please run the command again.").setEphemeral(true).queue();
            return;
        }

        int newPage = action.equals("next") ? currentPage + 1 : currentPage - 1;
        if (newPage < 0 || newPage >= chunks.size()) {
            event.reply("Invalid page navigation").setEphemeral(true).queue();
            return;
        }

        int totalEntries = chunks.stream().mapToInt(List::size).sum();

        // Always recreate the base embed on each page
        EmbedBuilder embed = createBaseEmbed(baseInfo.displayName, baseInfo.avatarUrl,
                baseInfo.totalHours, baseInfo.totalMoneyOwed)
                .setFooter("Page " + (newPage + 1) + "/" + chunks.size())
                .addField("Event Breakdown",
                        formatEventBreakdown(chunks.get(newPage), newPage, 5, totalEntries), false);

        ActionRow newButtons = ActionRow.of(
                Button.secondary("profile:prev:" + cacheKey + ":" + newPage, "◀").withDisabled(newPage == 0),
                Button.secondary("profile:next:" + cacheKey + ":" + newPage, "▶").withDisabled(newPage == chunks.size() - 1)
        );

        event.editMessageEmbeds(embed.build())
                .setComponents(newButtons)
                .queue();
    }
}
