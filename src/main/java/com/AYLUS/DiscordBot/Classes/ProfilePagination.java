package com.AYLUS.DiscordBot.Classes;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfilePagination {

    // Cache to store pagination data (user ID -> chunks)
    private static final Map<String, List<List<VolunteerEntry>>> PAGINATION_CACHE = new HashMap<>();

    public static void sendPaginatedProfile(SlashCommandInteractionEvent event, EmbedBuilder baseEmbed,
                                            List<List<VolunteerEntry>> chunks, String displayName, int page) {
        // Store chunks in cache
        PAGINATION_CACHE.put(event.getUser().getId(), chunks);

        // Build embed for current page
        EmbedBuilder embed = new EmbedBuilder(baseEmbed.build())
                .setFooter("Page " + (page + 1) + "/" + chunks.size())
                .addField("Event Breakdown (Page " + (page + 1) + ")",
                        formatEventBreakdown(chunks.get(page)), false);

        // Create buttons
        ActionRow buttons = ActionRow.of(
                Button.secondary("profile:prev:" + event.getUser().getId(), "◀").withDisabled(page == 0),
                Button.secondary("profile:next:" + event.getUser().getId(), "▶").withDisabled(page == chunks.size() - 1)
        );

        event.replyEmbeds(embed.build()).addActionRow(buttons).queue();
    }

    public static void handleButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        String action = args[1]; // "prev" or "next"
        String userId = args[2]; // Original requester's ID
        int currentPage = Integer.parseInt(args[3]); // Current page index

        List<List<VolunteerEntry>> chunks = PAGINATION_CACHE.get(userId);
        if (chunks == null) {
            event.reply("This menu expired. Run the command again.").setEphemeral(true).queue();
            return;
        }

        int newPage = action.equals("next") ? currentPage + 1 : currentPage - 1;
        EmbedBuilder embed = new EmbedBuilder(event.getMessage().getEmbeds().get(0));

        // Update footer and field
        embed.getFields().clear();
        embed.setFooter("Page " + (newPage + 1) + "/" + chunks.size())
                .addField("Event Breakdown (Page " + (newPage + 1) + ")",
                        formatEventBreakdown(chunks.get(newPage)), false);

        // Update buttons
        ActionRow newButtons = ActionRow.of(
                Button.secondary("profile:prev:" + userId, "◀").withDisabled(newPage == 0),
                Button.secondary("profile:next:" + userId, "▶").withDisabled(newPage == chunks.size() - 1)
        );

        event.editMessageEmbeds(embed.build()).setComponents(newButtons).queue();
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
                    entry.getTotalMoneyOwed()));
        }
        sb.append("```");
        return sb.toString();
    }
}