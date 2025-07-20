package com.AYLUS.DiscordBot.Classes;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import static com.AYLUS.DiscordBot.Classes.VolunteerCommands.volunteerManager;

public class VolunteeringRemoveCommand {

    // REMOVE THIS LATER
    public static boolean AYLUSAdmin(SlashCommandInteractionEvent event) {
        final String kycheID = "840216337119969301";
        final String ALLOWED_SERVER_ID = "1119034327515287645"; // A server ID(for aylus)


        return event.getUser().getId().equals(kycheID) ||
                (event.getGuild().getId().equals(ALLOWED_SERVER_ID) &&
                        event.getMember().hasPermission(Permission.ADMINISTRATOR));

    }

    static void handleRemoveCommand(SlashCommandInteractionEvent event) {
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


}
