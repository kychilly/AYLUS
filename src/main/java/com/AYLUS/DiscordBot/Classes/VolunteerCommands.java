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
        // 1. Instantly acknowledge the interaction to prevent the 3-second timeout
        event.deferReply().queue(hook -> {
            // 2. Execute scraping / JSON calculation asynchronously off the main thread
            CompletableFuture.runAsync(() -> {
                double totalHours = HourAndMoneyTracker.getTotalHours();
                int totalVolunteers = HourAndMoneyTracker.getTotalVolunteersCount();
                long totalEvents = HourAndMoneyTracker.getTotalEventsCount();
                double totalFunds = HourAndMoneyTracker.fetchTotalNationwideFunds(); // Slow web scraper call

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("AYLUS National Statistics")
                        .setColor(Color.BLUE)
                        .addField("Total Hours Tracked", String.format("%.1f hrs", totalHours), true)
                        .addField("Total Volunteers", String.valueOf(totalVolunteers), true)
                        .addField("Total Events(minimum)", String.valueOf(totalEvents), true)
                        .addField("Total Funds Raised", String.format("$%.2f", totalFunds), false);

                // 3. Edit the original deferred response via InteractionHook
                hook.sendMessageEmbeds(embed.build()).queue();
            });
        });
    }

}