package com.AYLUS.DiscordBot.Classes;

import com.AYLUS.DiscordBot.HelpfulMethods.HourAndMoneyTracker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.AYLUS.DiscordBot.Classes.VolunteerCommands.getVolunteerManager;
import static com.AYLUS.DiscordBot.Classes.VolunteerCommands.volunteerManager;

public class VolunteeringLogCommand {

    // REMOVE THIS LATER
    public static boolean AYLUSAdmin(SlashCommandInteractionEvent event) {
        final String kycheID = "840216337119969301";
        final String ALLOWED_SERVER_ID = "1119034327515287645"; // A server ID(for aylus)


        return event.getUser().getId().equals(kycheID) ||
                (event.getGuild().getId().equals(ALLOWED_SERVER_ID) &&
                        event.getMember().hasPermission(Permission.ADMINISTRATOR));

    }

    public static void handleLogCommand(SlashCommandInteractionEvent event) {
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
        getVolunteerManager().logHours(
                targetUser.getId(),
                targetUser.getName(),
                eventName,
                hours,
                dateStr,
                moneyOwed
        );

        // Money check before response
        String temp;
        if (moneyOwed != 0) {
            temp = String.format("\nMoney Owed: $%.2f", moneyOwed);
        } else {
            temp = "\nMoney Owed: $0.00";
        }

        // Build response
        String response = String.format(
                "✅ Logged **%.1f hours** for %s for **%s** on %s" + temp,
                hours,
                targetUser.getAsMention(),
                eventName,
                dateStr
        );


        HourAndMoneyTracker.updateHoursAndMoney(hours, moneyOwed, 0); // hours volunteered, if spent any money, 0 money paid back from an event

        // Tiny spagetthi code for now
        response +=  "\n\n" + targetUser.getAsMention() + " now has " + volunteerManager.getProfile(targetUser.getId(), targetUser.getName()).getTotalHours() + " hours logged!";

        event.replyEmbeds(
                new EmbedBuilder()
                        .setTitle("Volunteer Hours Logged")
                        .setColor(Color.GREEN)
                        .setDescription(response)
                        .build()
        ).queue();
    }




}
