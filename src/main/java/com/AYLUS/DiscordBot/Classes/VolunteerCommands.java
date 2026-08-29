package com.AYLUS.DiscordBot.Classes;

import com.AYLUS.DiscordBot.HelpfulMethods.HourAndMoneyTracker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.Permission;

import java.awt.*;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;


public class VolunteerCommands extends ListenerAdapter {
    static VolunteerManager volunteerManager;

    public VolunteerCommands() {
        this.volunteerManager = new VolunteerManager();
    }

    public static VolunteerManager getVolunteerManager() { return volunteerManager; }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "volunteer-log":
                VolunteeringLogCommand.handleLogCommand(event);
                break;
            case "volunteer-profile":
                VolunteeringProfileCommand.handleProfileCommand(event);
                break;
            case "volunteer-leaderboard":
                VolunteeringLeaderboardCommand.handleLeaderboardCommand(event);
                break;
            case "volunteer-remove":
                VolunteeringRemoveCommand.handleRemoveCommand(event);
                break;
            case "process-payment":
                VolunteeringPayCommand.handlePayCommand(event);
                break;
            case "volunteer-clear":
                VolunteeringClearCommand.handleClearCommand(event);
                break;
            case "payment-history":
                VolunteeringPaymentHistoryCommand.handlePaymentHistoryCommand(event);
                break;
            case "totalhours":
                handleTotalHoursCommand(event);
                break;
        }
    }


    private void handleTotalHoursCommand(SlashCommandInteractionEvent event) {
        // 1. Instantly acknowledge interaction to prevent Discord 3-second timeout
        event.deferReply().queue(hook -> {
            // 2. Aggregate statistics from json_data_storage asynchronously
            CompletableFuture.runAsync(() -> {
                DirectoryStatsCalculator.BranchStats stats =
                        DirectoryStatsCalculator.calculateDirectoryStats("json_data_storage");

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("AYLUS National Statistics")
                        .setColor(Color.BLUE)
                        .addField("Total Hours Tracked", String.format("%,.1f hrs", Math.ceil(stats.totalHours)), true)
                        .addField("Total Volunteers", String.format("%,d", stats.totalVolunteers), true)
                        .addField("Total Events (minimum)", String.format("%,d", stats.totalEvents), true)
                        .addField("Branches Tracked", String.format("%,d", stats.totalBranches), true)
                        .setFooter("Calculated from saved branch JSON records", null);

                // 3. Edit deferred response with final results
                hook.sendMessageEmbeds(embed.build()).queue();
            });
        });
    }

}