package com.AYLUS.DiscordBot.Classes;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

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
        String displayName = member != null ? member.getEffectiveName() : target.getName();

        // Sort entries newest-first
        List<VolunteerEntry> sortedEntries = new ArrayList<>(profile.getEntries());
        sortedEntries.sort(Comparator.comparing(VolunteerEntry::getDate).reversed());

        // Create base embed from ProfilePagination
        EmbedBuilder baseEmbed = ProfilePagination.createBaseEmbed(
                displayName,
                target.getEffectiveAvatarUrl(),
                profile.getTotalHours(),
                profile.getTotalMoneyOwed()
        );

        if (sortedEntries.isEmpty()) {
            baseEmbed.addField("Events", "No events recorded yet!", false);
            event.replyEmbeds(baseEmbed.build()).queue();
        } else if (sortedEntries.size() <= 5) {
            // Single page
            baseEmbed.addField("Event Breakdown",
                    ProfilePagination.formatEventBreakdown(sortedEntries, 0, 5, sortedEntries.size()), false);
            event.replyEmbeds(baseEmbed.build()).queue();
        } else {
            // Pagination
            List<List<VolunteerEntry>> chunks = partitionEntries(sortedEntries, 5);
            ProfilePagination.sendPaginatedProfile(event, displayName, target.getEffectiveAvatarUrl(),
                    profile.getTotalHours(), profile.getTotalMoneyOwed(), chunks, 0);
        }
    }

    private static List<List<VolunteerEntry>> partitionEntries(List<VolunteerEntry> entries, int chunkSize) {
        List<List<VolunteerEntry>> chunks = new ArrayList<>();
        for (int i = 0; i < entries.size(); i += chunkSize) {
            chunks.add(entries.subList(i, Math.min(i + chunkSize, entries.size())));
        }
        return chunks;
    }
}
