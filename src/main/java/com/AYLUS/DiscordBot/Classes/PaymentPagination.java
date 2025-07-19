package com.AYLUS.DiscordBot.Classes;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.util.*;
import java.util.stream.Collectors;

public class PaymentPagination {
    private static final Map<String, List<List<PaymentEntry>>> PAGINATION_CACHE = new HashMap<>();

    public static void sendPaginatedPayments(SlashCommandInteractionEvent event,
                                             EmbedBuilder baseEmbed,
                                             List<PaymentEntry> payments,
                                             String displayName) {
        // Sort payments newest first
        List<PaymentEntry> sortedPayments = payments.stream()
                .sorted(Comparator.comparing(PaymentEntry::getDate).reversed())
                .collect(Collectors.toList());

        // Split into pages of 10
        List<List<PaymentEntry>> chunks = partitionPayments(sortedPayments, 10);

        // Store in cache
        String cacheKey = event.getUser().getId() + ":" + event.getChannel().getId();
        PAGINATION_CACHE.put(cacheKey, chunks);

        // Send first page
        sendPaymentPage(event, baseEmbed, chunks, displayName, 0, cacheKey);
    }

    private static void sendPaymentPage(Interaction event,
                                        EmbedBuilder baseEmbed,
                                        List<List<PaymentEntry>> chunks,
                                        String displayName,
                                        int pageIndex,
                                        String cacheKey) {
        List<PaymentEntry> page = chunks.get(pageIndex);

        // Build embed with current page
        EmbedBuilder embed = new EmbedBuilder(baseEmbed.build())
                .setFooter("Page " + (pageIndex + 1) + "/" + chunks.size());

        // Format payment list
        StringBuilder paymentList = new StringBuilder("```\n");
        paymentList.append("DATE       AMOUNT    DESCRIPTION\n");
        paymentList.append("------------------------------\n");

        page.forEach(p -> {
            String trimmedDesc = p.getDescription().length() > 20
                    ? p.getDescription().substring(0, 17) + "..."
                    : p.getDescription();

            paymentList.append(String.format("%-10s $%-7.2f %s\n",
                    p.getDate(),
                    p.getAmount(),
                    trimmedDesc));
        });

        paymentList.append("```");
        embed.addField("Payment History", paymentList.toString(), false);

        // Create buttons
        ActionRow buttons = ActionRow.of(
                Button.secondary("payments:prev:" + cacheKey + ":" + pageIndex, "◀")
                        .withDisabled(pageIndex == 0),
                Button.secondary("payments:next:" + cacheKey + ":" + pageIndex, "▶")
                        .withDisabled(pageIndex == chunks.size() - 1)
        );

        // Send or update message
        if (event instanceof SlashCommandInteractionEvent) {
            ((SlashCommandInteractionEvent)event).replyEmbeds(embed.build())
                    .addComponents(buttons)
                    .queue();
        } else {
            ((ButtonInteractionEvent)event).editMessageEmbeds(embed.build())
                    .setComponents(buttons)
                    .queue();
        }
    }

    public static void handleButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        String action = args[1]; // "prev" or "next"
        String cacheKey = args[2] + ":" + args[3];
        int currentPage = Integer.parseInt(args[4]);

        List<List<PaymentEntry>> chunks = PAGINATION_CACHE.get(cacheKey);
        if (chunks == null) {
            event.reply("This payment history has expired. Please run the command again.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        int newPage = action.equals("next") ? currentPage + 1 : currentPage - 1;
        MessageEmbed originalEmbed = event.getMessage().getEmbeds().get(0);

        // Rebuild embed with original info
        EmbedBuilder embed = new EmbedBuilder(originalEmbed);
        sendPaymentPage(event, embed, chunks, "", newPage, cacheKey);
    }

    private static List<List<PaymentEntry>> partitionPayments(List<PaymentEntry> payments, int chunkSize) {
        List<List<PaymentEntry>> chunks = new ArrayList<>();
        for (int i = 0; i < payments.size(); i += chunkSize) {
            chunks.add(payments.subList(i, Math.min(i + chunkSize, payments.size())));
        }
        return chunks;
    }
}