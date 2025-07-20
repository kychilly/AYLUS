package com.AYLUS.DiscordBot.Classes;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.List;

import static com.AYLUS.DiscordBot.Classes.VolunteerCommands.volunteerManager;

public class VolunteeringLeaderboardCommand {


    static void handleLeaderboardCommand(SlashCommandInteractionEvent event) {
        List<UserVolunteerProfile> leaderboard = volunteerManager.getLeaderboard();

        if (leaderboard.isEmpty()) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Volunteering Leaderboard")
                    .setColor(Color.ORANGE)
                    .setDescription("No volunteer hours logged yet!");
            event.replyEmbeds(embed.build()).queue();
            return;
        }

        // Create pagination with 10 entries per page
        LeaderboardPagination pagination = new LeaderboardPagination(leaderboard, 10);

        // Send first page (pageIndex 0) and store the message
        sendLeaderboardPage(event, pagination, 0);
    }

    // For SlashCommandInteractionEvent
    public static void sendLeaderboardPage(SlashCommandInteractionEvent event, LeaderboardPagination pagination, int pageIndex) {
        List<UserVolunteerProfile> page = pagination.getPage(pageIndex);

        EmbedBuilder embed = buildLeaderboardEmbed(event, pagination, pageIndex, page);

        // Buttons
        ActionRow actionRow = buildLeaderboardActionRow(pagination, pageIndex);

        // Reply and keep the message reference for future edits
        event.replyEmbeds(embed.build())
                .setComponents(actionRow)
                .queue(interactionHook -> {
                    // You might want to store this message ID somewhere if you need to reference it later
                });
    }

    // For ButtonInteractionEvent
    public static void sendLeaderboardPage(ButtonInteractionEvent event, LeaderboardPagination pagination, int pageIndex) {
        List<UserVolunteerProfile> page = pagination.getPage(pageIndex);

        EmbedBuilder embed = buildLeaderboardEmbed(event, pagination, pageIndex, page);

        // Buttons
        ActionRow actionRow = buildLeaderboardActionRow(pagination, pageIndex);

        // Always edit the original message
        event.deferEdit().queue(); // Important to defer first
        event.getHook().editOriginalEmbeds(embed.build())
                .setComponents(actionRow)
                .queue();
    }

    // Helper method to build the embed
    private static EmbedBuilder buildLeaderboardEmbed(Interaction event, LeaderboardPagination pagination, int pageIndex, List<UserVolunteerProfile> page) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Volunteering Leaderboard")
                .setColor(Color.ORANGE);

        StringBuilder sb = new StringBuilder();
        int startRank = pageIndex * pagination.getItemsPerPage() + 1;

        for (int i = 0; i < page.size(); i++) {
            UserVolunteerProfile profile = page.get(i);
            sb.append(String.format(
                    "%d. <@%s> - %.1f hours\n",
                    startRank + i, profile.getUserId(), profile.getTotalHours()
            ));
        }
        embed.setDescription(sb.toString());

        // Get user info
        String userId = event.getUser().getId();
        List<UserVolunteerProfile> fullList = pagination.getFullList();
        int rank = getRank(fullList, userId);
        UserVolunteerProfile userProfile = volunteerManager.getProfile(userId, event.getMember().getEffectiveName());

        if (userProfile != null) {
            String footerText = String.format("Page %d/%d | Your rank: #%d | Your hours: %.1f",
                    pageIndex + 1, pagination.getTotalPages(), rank, userProfile.getTotalHours());
            embed.setFooter(footerText, event.getUser().getEffectiveAvatarUrl());
        } else {
            embed.setFooter(String.format("Page %d/%d", pageIndex + 1, pagination.getTotalPages()));
        }

        return embed;
    }

    // Helper method to build action row
    private static ActionRow buildLeaderboardActionRow(LeaderboardPagination pagination, int pageIndex) {
        return ActionRow.of(
                net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("leaderboard_prev:" + pageIndex, "◀ Previous")
                        .withDisabled(pageIndex == 0),
                Button.secondary("leaderboard_next:" + pageIndex, "Next ▶")
                        .withDisabled(pageIndex == pagination.getTotalPages() - 1)
        );
    }

    private static int getRank(List<UserVolunteerProfile> leaderboard, String userId) {
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getUserId().equals(userId)) {
                return i + 1;
            }
        }
        return -1;
    }

    public static void handleLeaderboardButton(ButtonInteractionEvent event, String buttonId) {
        String[] parts = buttonId.split(":");
        String direction = parts[0].substring("leaderboard_".length());
        int currentPage = Integer.parseInt(parts[1]);

        List<UserVolunteerProfile> leaderboard = volunteerManager.getLeaderboard();
        LeaderboardPagination pagination = new LeaderboardPagination(leaderboard, 10);

        int newPage = direction.equals("prev") ? currentPage - 1 : currentPage + 1;
        sendLeaderboardPage(event, pagination, newPage);
    }

}
