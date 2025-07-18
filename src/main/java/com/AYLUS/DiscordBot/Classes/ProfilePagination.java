package com.AYLUS.DiscordBot.Classes;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfilePagination {
    private static final Map<String, List<List<VolunteerEntry>>> PAGINATION_CACHE = new HashMap<>();

    public static void sendPaginatedProfile(SlashCommandInteractionEvent event, EmbedBuilder baseEmbed,
                                            List<List<VolunteerEntry>> chunks, String displayName, int page) {
        // Store chunks in cache with a unique identifier
        String cacheKey = event.getUser().getId() + ":" + event.getChannel().getId();
        PAGINATION_CACHE.put(cacheKey, chunks);

        // Build embed for current page
        EmbedBuilder embed = new EmbedBuilder(baseEmbed.build())
                .setFooter("Page " + (page + 1) + "/" + chunks.size())
                .addField("Event Breakdown", formatEventBreakdown(chunks.get(page)), false);

        // Create buttons
        ActionRow buttons = ActionRow.of(
                Button.secondary("profile:prev:" + cacheKey + ":" + page, "◀")
                        .withDisabled(page == 0),
                Button.secondary("profile:next:" + cacheKey + ":" + page, "▶")
                        .withDisabled(page == chunks.size() - 1)
        );

        event.replyEmbeds(embed.build())
                .addComponents(buttons)
                .queue();
    }

    public static void handleButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (args.length < 4) {
            event.reply("Invalid button interaction").setEphemeral(true).queue();
            return;
        }

        String action = args[1]; // "prev" or "next"
        String cacheKey = args[2] + ":" + args[3]; // userID:channelID
        int currentPage = Integer.parseInt(args[4]);

        List<List<VolunteerEntry>> chunks = PAGINATION_CACHE.get(cacheKey);
        if (chunks == null) {
            event.reply("This menu has expired. Please run the command again.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        int newPage = action.equals("next") ? currentPage + 1 : currentPage - 1;

        // Validate page bounds
        if (newPage < 0 || newPage >= chunks.size()) {
            event.reply("Invalid page navigation").setEphemeral(true).queue();
            return;
        }

        // Get original embed and update it
        MessageEmbed originalEmbed = event.getMessage().getEmbeds().get(0);
        EmbedBuilder embed = new EmbedBuilder(originalEmbed)
                .clearFields()
                .setFooter("Page " + (newPage + 1) + "/" + chunks.size())
                .addField("Event Breakdown", formatEventBreakdown(chunks.get(newPage)), false);

        // Update buttons
        ActionRow newButtons = ActionRow.of(
                Button.secondary("profile:prev:" + cacheKey + ":" + newPage, "◀")
                        .withDisabled(newPage == 0),
                Button.secondary("profile:next:" + cacheKey + ":" + newPage, "▶")
                        .withDisabled(newPage == chunks.size() - 1)
        );

        event.editMessageEmbeds(embed.build())
                .setComponents(newButtons)
                .queue();
    }

    private static String formatEventBreakdown(List<VolunteerEntry> entries) {
        // Reuse the same method from ProfileCommand
        StringBuilder sb = new StringBuilder("```\n");
        sb.append("EVENT                HOURS   DATE       OWED\n");
        sb.append("--------------------------------------------\n");
        for (VolunteerEntry entry : entries) {
            sb.append(String.format("%-20s %-5.1f   %-10s $%.2f\n",
                    entry.getEventName(),
                    entry.getHours(),
                    entry.getDate(),
                    entry.getMoney()));
        }
        sb.append("```");
        return sb.toString();
    }
}