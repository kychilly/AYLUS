package com.AYLUS.DiscordBot.Classes;

import com.AYLUS.DiscordBot.HelpfulMethods.HourAndMoneyTracker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.AYLUS.DiscordBot.Classes.VolunteerCommands.volunteerManager;

public class VolunteeringRemovePaymentCommand {

    public static CommandData getCommandData() {
        return Commands.slash("volunteering-remove-payment", "Remove a volunteer's payment")
                .addOption(OptionType.USER, "user", "The user whose payment to remove", true)
                .addOption(OptionType.STRING, "date", "Date of the payment (YYYY-MM-DD)", true)
                .addOption(OptionType.NUMBER, "amount", "Amount of the payment", true);
    }

    public static void execute(SlashCommandInteractionEvent event) {
        // Permission check
        if (!AYLUSAdmin(event)) {
            event.reply("❌ You do not have permission to use this command.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Pull required options
        Member targetMember = Objects.requireNonNull(event.getOption("user")).getAsMember();
        String date = Objects.requireNonNull(event.getOption("date")).getAsString();
        double amount = Objects.requireNonNull(event.getOption("amount")).getAsDouble();

        // Load profile
        UserVolunteerProfile profile = volunteerManager.getProfile(
                targetMember.getId(),
                targetMember.getEffectiveName()
        );

        List<PaymentEntry> history = profile.getPaymentHistory();
        boolean removed = false;

        // Find and remove the matching entry
        Iterator<PaymentEntry> iterator = history.iterator();
        while (iterator.hasNext()) {
            PaymentEntry entry = iterator.next();
            if (entry.getAmount() == amount && entry.getDate().equals(date)) {
                iterator.remove(); // remove from history
                profile.setTotalMoneyOwed(profile.getTotalMoneyOwed() + amount); // undo deduction
                HourAndMoneyTracker.updateHoursAndMoney(0, 0, -amount); // reverse global stats
                removed = true;
                break;
            }
        }

        if (removed) {
            volunteerManager.saveData();
            event.replyEmbeds(
                    new EmbedBuilder()
                            .setTitle("Payment Removed")
                            .setColor(Color.RED)
                            .setDescription(String.format(
                                    "Removed payment of $%.2f on %s for %s.",
                                    amount,
                                    date,
                                    targetMember.getAsMention()
                            ))
                            .build()
            ).queue();
        } else {
            event.reply("❌ No matching payment found for that date and amount.")
                    .setEphemeral(true)
                    .queue();
        }
    }

    // Keep consistent with your PayCommand
    public static boolean AYLUSAdmin(SlashCommandInteractionEvent event) {
        final String kycheID = "840216337119969301";
        final String ALLOWED_SERVER_ID = "1119034327515287645"; // AYLUS server ID

        return event.getUser().getId().equals(kycheID) ||
                (event.getGuild().getId().equals(ALLOWED_SERVER_ID) &&
                        event.getMember().hasPermission(Permission.ADMINISTRATOR));
    }
}
