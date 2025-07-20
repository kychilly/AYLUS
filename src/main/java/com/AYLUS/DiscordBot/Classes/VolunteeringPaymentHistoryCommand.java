package com.AYLUS.DiscordBot.Classes;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import static com.AYLUS.DiscordBot.Classes.VolunteerCommands.volunteerManager;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class VolunteeringPaymentHistoryCommand {

    // The person's previous payments
    static void handlePaymentHistoryCommand(SlashCommandInteractionEvent event) {
        User target = event.getOption("user") != null
                ? event.getOption("user").getAsUser()
                : event.getUser();
        Member member = event.getOption("user") != null
                ? event.getOption("user").getAsMember()
                : event.getMember();

        UserVolunteerProfile profile = volunteerManager.getProfile(target.getId(), target.getName());
        String displayName = member.getNickname() != null ? member.getNickname() : target.getName();

        // Build base embed (title, color, thumbnail, totals)
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(displayName + "'s Payment History")
                .setColor(Color.GREEN)
                .setThumbnail(target.getEffectiveAvatarUrl())
                .addField("Total Paid", String.format("💰 **$%.2f**", profile.getTotalPaid()), true)
                .addField(profile.getTotalMoneyOwed() > 0 ?
                        "Amount Owe" : "Total AYLUS Owes", String.format("💰 **$%.2f**",
                        Math.abs(profile.getTotalMoneyOwed())), true);

        List<PaymentEntry> payments = profile.getPaymentHistory();

        if (payments.isEmpty()) {
            embed.addField("Payments", "No payments recorded yet!", false);
            event.replyEmbeds(embed.build()).queue();
        } else if (payments.size() <= 10) {
            // Single page
            StringBuilder paymentList = new StringBuilder("```\n");
            paymentList.append("DATE       AMOUNT    Notes\n");
            paymentList.append("----------------------------------\n");

            payments.stream()
                    .sorted(Comparator.comparing(PaymentEntry::getDate).reversed())
                    .forEach(p -> paymentList.append(String.format("%-10s $%-7.2f %s\n",
                            p.getDate(),
                            p.getAmount(),
                            p.getDescription())));

            paymentList.append("```");
            embed.addField("Payment History", paymentList.toString(), false);
            event.replyEmbeds(embed.build()).queue();
        } else {
            // Paginated version
            PaymentPagination.sendPaginatedPayments(event, embed, payments, displayName);
        }
    }

}
