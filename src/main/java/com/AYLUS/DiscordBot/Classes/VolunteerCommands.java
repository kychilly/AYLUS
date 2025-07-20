package com.AYLUS.DiscordBot.Classes;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.Permission;


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
            case "pay":
                VolunteeringPayCommand.handlePayCommand(event);
                break;
            case "volunteer-clear":
                VolunteeringClearCommand.handleClearCommand(event);
                break;
            case "payment-history":
                VolunteeringPaymentHistoryCommand.handlePaymentHistoryCommand(event);
                break;
        }
    }

    public boolean AYLUSAdmin(SlashCommandInteractionEvent event) {
        final String kycheID = "840216337119969301";
        final String ALLOWED_SERVER_ID = "1119034327515287645"; // A server ID(for aylus)


        return event.getUser().getId().equals(kycheID) ||
                (event.getGuild().getId().equals(ALLOWED_SERVER_ID) &&
                        event.getMember().hasPermission(Permission.ADMINISTRATOR));

    }

}