package com.AYLUS.DiscordBot.Classes;

// VolunteerCommands.java
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;



import com.AYLUS.DiscordBot.Classes.VolunteerCommands;



import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class VolunteerCommands extends ListenerAdapter {
    private final VolunteerManager volunteerManager;

    public VolunteerCommands() {
        this.volunteerManager = new VolunteerManager();
    }

    public static List<CommandData> getCommandData() {
        return List.of(
                Commands.slash("volunteer-log", "Log volunteer hours")
                        .addOptions(
                                new OptionData(OptionType.USER, "user", "User to log hours for", true),
                                new OptionData(OptionType.STRING, "event", "Name of the event", true),
                                new OptionData(OptionType.NUMBER, "hours", "Hours volunteered", true)
                                        .setMinValue(0.1)
                                        .setMaxValue(24.0),
                                new OptionData(OptionType.STRING, "date", "Date (DD-MM-YY)", true)
                        ),
                Commands.slash("volunteer-profile", "View volunteer profile")
                        .addOptions(
                                new OptionData(OptionType.USER, "user", "User to view", false)
                        ),
                Commands.slash("volunteer-leaderboard", "View volunteer leaderboard"),
                Commands.slash("volunteer-remove", "Remove a volunteer event")
                        .addOptions(
                                new OptionData(OptionType.USER, "user", "User whose event to remove", true),
                                new OptionData(OptionType.STRING, "event", "Name of the event to remove", true),
                                new OptionData(OptionType.STRING, "date", "Date of the event (DD-MM-YY)", true)
                        )
        );
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
                dateStr
        );

        // Build response
        String response = String.format(
                "✅ Logged **%.1f hours** for %s for **%s** on %s",
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

        UserVolunteerProfile profile = volunteerManager.getProfile(target.getId(), target.getName());

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(target.getName() + "'s Volunteer Profile")
                .setColor(Color.BLUE)
                .setThumbnail(target.getEffectiveAvatarUrl())
                .addField("Total Hours", String.format("%.1f hours", profile.getTotalHours()), false);

        if (!profile.getEntries().isEmpty()) {
            StringBuilder breakdown = new StringBuilder();
            for (VolunteerEntry entry : profile.getEntries()) {
                breakdown.append(String.format(
                        "• **%s**: %.1f hours (%s)\n",
                        entry.getEventName(), entry.getHours(), entry.getDate()
                ));
            }
            embed.addField("Breakdown", breakdown.toString(), false);
        }

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleLeaderboardCommand(SlashCommandInteractionEvent event) {
        List<UserVolunteerProfile> leaderboard = volunteerManager.getLeaderboard();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Volunteer Leaderboard")
                .setColor(Color.ORANGE);

        if (leaderboard.isEmpty()) {
            embed.setDescription("No volunteer hours logged yet!");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(10, leaderboard.size()); i++) {
                UserVolunteerProfile profile = leaderboard.get(i);
                sb.append(String.format(
                        "%d. <@%s> - %.1f hours\n",  // Changed to use mention format
                        i + 1, profile.getUserId(), profile.getTotalHours()
                ));
            }
            embed.setDescription(sb.toString());
        }

        event.replyEmbeds(embed.build()).queue();
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

    public boolean AYLUSAdmin(SlashCommandInteractionEvent event) {
        final String kycheID = "840216337119969301";
        final String ALLOWED_SERVER_ID = "1119034327515287645"; // A server ID(for aylus)


        return event.getUser().getId().equals(kycheID) ||
                (event.getGuild().getId().equals(ALLOWED_SERVER_ID) &&
                        event.getMember().hasPermission(Permission.ADMINISTRATOR));

    }

}