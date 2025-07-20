package com.AYLUS.DiscordBot.Classes;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.util.Objects;

import static com.AYLUS.DiscordBot.Classes.VolunteerCommands.volunteerManager;

public class VolunteeringPayCommand {

    // REMOVE THIS LATER
    public static boolean AYLUSAdmin(SlashCommandInteractionEvent event) {
        final String kycheID = "840216337119969301";
        final String ALLOWED_SERVER_ID = "1119034327515287645"; // A server ID(for aylus)


        return event.getUser().getId().equals(kycheID) ||
                (event.getGuild().getId().equals(ALLOWED_SERVER_ID) &&
                        event.getMember().hasPermission(Permission.ADMINISTRATOR));

    }

    static void handlePayCommand(SlashCommandInteractionEvent event) {
        if (!AYLUSAdmin(event)) {
            event.reply("❌ You do not have permission to use this command.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Member targetMember = Objects.requireNonNull(event.getOption("user")).getAsMember();
        double paymentAmount = Objects.requireNonNull(event.getOption("amount")).getAsDouble();

        // I straight up pulled this from chatgpt cause it wasnt working lol
        String note = "No Notes"; // Default value
        OptionMapping noteOption = event.getOption("notes"); // Match the option name exactly
        if (noteOption != null) {
            String providedNote = noteOption.getAsString();
            if (!providedNote.trim().isEmpty()) {
                note = providedNote;
            }
        }

        if (paymentAmount <= 0) {
            event.reply("❌ Payment amount must be positive.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        UserVolunteerProfile profile = volunteerManager.getProfile(
                targetMember.getId(),
                targetMember.getEffectiveName()
        );

        // Get current balance before deduction
        double currentBalance = profile.getTotalMoneyOwed();

        // NO MORE DOUBLE SUBTRACTING PLEASE
        profile.setTotalMoneyOwed(currentBalance - paymentAmount);
        double newBalance = currentBalance - paymentAmount;
        profile.recordPayment(paymentAmount, note);
        volunteerManager.saveData(); // Explicit save

        // Build the response
        String paymentMessage;
        if (newBalance < 0) {
            paymentMessage = String.format(
                    "✅ Deducted $%.2f from %s\n" +
                            "Previous Amount Owed: $%.2f\n" +
                            "**We have to pay this guy $%.2f now lol**",
                    paymentAmount,
                    targetMember.getAsMention(),
                    currentBalance,
                    -newBalance
            );
        } else {
            paymentMessage = String.format(
                    "✅ Deducted $%.2f from %s\n" +
                            "Previous Amount Owed: $%.2f\n" +
                            "New Amount Owed: $%.2f",
                    paymentAmount,
                    targetMember.getAsMention(),
                    currentBalance,
                    newBalance
            );
        }

        event.replyEmbeds(
                new EmbedBuilder()
                        .setTitle("Payment Processed")
                        .setColor(newBalance < 0 ? Color.ORANGE : Color.GREEN)
                        .setDescription(paymentMessage)
                        .build()
        ).queue();
    }

}
