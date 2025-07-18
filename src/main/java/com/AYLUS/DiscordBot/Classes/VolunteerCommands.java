package com.AYLUS.DiscordBot.Classes;

// VolunteerCommands.java
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.components.buttons.Button;


import net.dv8tion.jda.api.interactions.components.ActionRow;


import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class VolunteerCommands extends ListenerAdapter {
    private final VolunteerManager volunteerManager;

    public VolunteerCommands() {
        this.volunteerManager = new VolunteerManager();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "volunteer-log":
                handleLogCommand(event);
                break;
            case "volunteer-profile":
                handleProfileCommand(event);
                break;
            case "volunteer-leaderboard":
                handleLeaderboardCommand(event);
                break;
            case "volunteer-remove":
                handleRemoveCommand(event);
                break;
        }
    }

    private void handleLogCommand(SlashCommandInteractionEvent event) {
        if (!AYLUSAdmin(event)) {
            event.reply("❌ You do not have permission to use this command.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Get command options
        String eventName = event.getOption("event").getAsString();
        double hours = event.getOption("hours").getAsDouble();
        String dateStr = event.getOption("date") != null
                ? event.getOption("date").getAsString()
                : LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        double moneyOwed = event.getOption("money-owed") != null
                ?event.getOption("money-owed").getAsDouble()
                : 0;

        // Determine target user
        User targetUser;
        if (event.getOption("user") != null) {
            // Admin check when logging for others
            if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                event.reply("❌ You need administrator permissions to log hours for others")
                        .setEphemeral(true)
                        .queue();
                return;
            }
            targetUser = event.getOption("user").getAsUser();
        } else {
            targetUser = event.getUser(); // Default to command user
        }

        // Log hours
        volunteerManager.logHours(
                targetUser.getId(),
                targetUser.getName(),
                eventName,
                hours,
                dateStr,
                moneyOwed
        );

        //money check before response
        String temp = "";
        if (moneyOwed != 0) {
            temp = String.format("\nMoney Owed: $%.2f", moneyOwed);
        }

        // Build response
        String response = String.format(
                "✅ Logged **%.1f hours** for %s for **%s** on %s" + temp,
                hours,
                targetUser.getAsMention(),
                eventName,
                dateStr
        );

        event.replyEmbeds(
                new EmbedBuilder()
                        .setTitle("Volunteer Hours Logged")
                        .setColor(Color.GREEN)
                        .setDescription(response)
                        .build()
        ).queue();
    }

    private void handleProfileCommand(SlashCommandInteractionEvent event) {
        User target = event.getOption("user") != null
                ? event.getOption("user").getAsUser()
                : event.getUser();

        Member mention = event.getOption("user") != null
                ? event.getOption("user").getAsMember()
                : event.getMember();

        UserVolunteerProfile profile = volunteerManager.getProfile(target.getId(), target.getName());
        String displayName;
        displayName = mention.getNickname() == null ? target.getName() : mention.getNickname();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(displayName + "'s Volunteer Profile")
                .setColor(Color.BLUE)
                .setThumbnail(target.getEffectiveAvatarUrl())
                .addField("Total Hours", String.format("%.1f hours", profile.getTotalHours()), false)
                .addField("Total Money Owed(To AYLUS)", String.format("$%.2f owed", profile.getTotalMoneyOwed()), false);

        if (!profile.getEntries().isEmpty()) {
            StringBuilder breakdown = new StringBuilder();
            for (VolunteerEntry entry : profile.getEntries()) {
                breakdown.append(String.format(
                        "• **%s**: %.1f hours (%s)\n",
                        entry.getEventName(), entry.getHours(), entry.getDate()
                ));
            }
            embed.addField("Breakdown", breakdown.toString(), false);
        } else {
            embed.addField("Breakdown", "This user has no events!", false);
        }
        embed.setFooter("Format: **event**, hours, date");
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleLeaderboardCommand(SlashCommandInteractionEvent event) {
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
    public void sendLeaderboardPage(SlashCommandInteractionEvent event, LeaderboardPagination pagination, int pageIndex) {
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
    public void sendLeaderboardPage(ButtonInteractionEvent event, LeaderboardPagination pagination, int pageIndex) {
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

    private void handleRemoveCommand(SlashCommandInteractionEvent event) {
        if (!AYLUSAdmin(event)) {
            event.reply("❌ You do not have permission to use this command.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply().setEphemeral(true).queue(hook -> {
            try {
                User user = event.getOption("user").getAsUser();
                String eventName = event.getOption("event").getAsString();
                String date = event.getOption("date").getAsString();

                new Thread(() -> { // Process in background
                    boolean success = volunteerManager.removeEvent(user.getId(), eventName, date);
                    String response = success
                            ? "✅ Removed event from " + user.getAsMention()
                            : "❌ Event not found";
                    hook.sendMessage(response).queue();
                }).start();

            } catch (Exception e) {
                hook.sendMessage("⚠️ Error: " + e.getMessage()).queue();
            }
        });
    }

    // Helper method to build the embed
    private EmbedBuilder buildLeaderboardEmbed(Interaction event, LeaderboardPagination pagination, int pageIndex, List<UserVolunteerProfile> page) {
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
    private ActionRow buildLeaderboardActionRow(LeaderboardPagination pagination, int pageIndex) {
        return ActionRow.of(
                Button.secondary("leaderboard_prev:" + pageIndex, "◀ Previous")
                        .withDisabled(pageIndex == 0),
                Button.secondary("leaderboard_next:" + pageIndex, "Next ▶")
                        .withDisabled(pageIndex == pagination.getTotalPages() - 1)
        );
    }

    private int getRank(List<UserVolunteerProfile> leaderboard, String userId) {
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getUserId().equals(userId)) {
                return i + 1;
            }
        }
        return -1;
    }


    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        if (buttonId.startsWith("leaderboard_")) {
            handleLeaderboardButton(event, buttonId);
            return;
        }

    }

    private void handleLeaderboardButton(ButtonInteractionEvent event, String buttonId) {
        String[] parts = buttonId.split(":");
        String direction = parts[0].substring("leaderboard_".length());
        int currentPage = Integer.parseInt(parts[1]);

        List<UserVolunteerProfile> leaderboard = volunteerManager.getLeaderboard();
        LeaderboardPagination pagination = new LeaderboardPagination(leaderboard, 10);

        int newPage = direction.equals("prev") ? currentPage - 1 : currentPage + 1;
        sendLeaderboardPage(event, pagination, newPage);
    }

    public boolean AYLUSAdmin(SlashCommandInteractionEvent event) {
        final String kycheID = "840216337119969301";
        final String ALLOWED_SERVER_ID = "1119034327515287645"; // A server ID(for aylus)


        return event.getUser().getId().equals(kycheID) ||
                (event.getGuild().getId().equals(ALLOWED_SERVER_ID) &&
                        event.getMember().hasPermission(Permission.ADMINISTRATOR));

    }

}