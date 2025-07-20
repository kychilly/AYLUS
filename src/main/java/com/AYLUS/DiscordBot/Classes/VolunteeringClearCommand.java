package com.AYLUS.DiscordBot.Classes;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

import static com.AYLUS.DiscordBot.Classes.VolunteerCommands.volunteerManager;

public class VolunteeringClearCommand {

    // REMOVE THIS LATER
    public boolean AYLUSAdmin(SlashCommandInteractionEvent event) {
        final String kycheID = "840216337119969301";
        final String ALLOWED_SERVER_ID = "1119034327515287645"; // A server ID(for aylus)


        return event.getUser().getId().equals(kycheID) ||
                (event.getGuild().getId().equals(ALLOWED_SERVER_ID) &&
                        event.getMember().hasPermission(Permission.ADMINISTRATOR));

    }

    static void handleClearCommand(SlashCommandInteractionEvent event) {

        // Hardcoded sure check
        if (!event.getOption("positive").getAsBoolean()) {
            event.reply("\uD83E\uDEE2 Whew! Crisis averted!")
                    .setEphemeral(true)
                    .queue();
            return;
        }
        // Hardcoded admin check
        if (!event.getUser().getId().equals("840216337119969301")) {
            event.reply("❌ Only Kyche can use this command! Please contact if you want to use this because it is very dangerous :gasp:")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        User target = event.getOption("user").getAsUser();
        UserVolunteerProfile profile = volunteerManager.getProfile(target.getId(), target.getName());

        // Clear all data
        volunteerManager.clearProfile(target.getId());

        event.replyEmbeds(
                new EmbedBuilder()
                        .setTitle("Profile Cleared")
                        .setColor(Color.RED)
                        .setDescription("♻️ Cleared all volunteer data for " + target.getAsMention())
                        .build()
        ).queue();
    }

}
