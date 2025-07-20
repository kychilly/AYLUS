package com.AYLUS.DiscordBot.Classes;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.AYLUS.DiscordBot.Classes.VolunteerCommands.volunteerManager;

public class VolunteeringProfileCommand {

    public static void handleProfileCommand(SlashCommandInteractionEvent event) {
        User target = event.getOption("user") != null
                ? event.getOption("user").getAsUser()
                : event.getUser();
        Member member = event.getOption("user") != null
                ? event.getOption("user").getAsMember()
                : event.getMember();

        UserVolunteerProfile profile = volunteerManager.getProfile(target.getId(), target.getName());
        String displayName = member.getNickname() != null ? member.getNickname() : target.getName();

        // Sort entries newest-first
        List<VolunteerEntry> sortedEntries = new ArrayList<>(profile.getEntries());
        sortedEntries.sort(Comparator.comparing(VolunteerEntry::getDate).reversed());

        // Build base embed
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(displayName + "'s Volunteer Profile")
                .setColor(Color.BLUE)
                .setThumbnail(target.getEffectiveAvatarUrl())
                .addField("Total Hours", String.format("⏱️ **%.1f hours**", profile.getTotalHours()), true)
                //check to see if we owe the guy money
                .addField(profile.getTotalMoneyOwed() > 0 ?
                        "Total Owed(w reimburse)" : "Total AYLUS Owes(w reimburse)", String.format("💰 **$%.2f**",
                        Math.abs(profile.getTotalMoneyOwed())), true);


        if (sortedEntries.isEmpty()) {
            embed.addField("Events", "No events recorded yet!", false);
            event.replyEmbeds(embed.build()).queue();
        } else if (sortedEntries.size() <= 10) {
            // Single page
            embed.addField("Event Breakdown", formatEventBreakdown(sortedEntries), false);
            event.replyEmbeds(embed.build()).queue();
        } else {
            // Pagination needed
            List<List<VolunteerEntry>> chunks = partitionEntries(sortedEntries, 10);
            ProfilePagination.sendPaginatedProfile(event, embed, chunks, displayName, 0);
        }
    }

    private static String formatEventBreakdown(List<VolunteerEntry> entries) {
        StringBuilder sb = new StringBuilder("```\n");
        // Header with proper spacing
        sb.append("EVENT               HOURS   DATE       OWED\n");
        sb.append("---------------------------------------------\n");

        for (VolunteerEntry entry : entries) {
            // Format money with $ included in the padding
            sb.append(String.format("%-18s %5.1f   %-10s $%6.2f\n",
                    entry.getEventName(),
                    entry.getHours(),
                    entry.getDate(),
                    entry.getMoney()));
        }
        sb.append("```");
        return sb.toString();
    }


    private static List<List<VolunteerEntry>> partitionEntries(List<VolunteerEntry> entries, int chunkSize) {
        List<List<VolunteerEntry>> chunks = new ArrayList<>();
        for (int i = 0; i < entries.size(); i += chunkSize) {
            chunks.add(entries.subList(i, Math.min(i + chunkSize, entries.size())));
        }
        return chunks;
    }


}
