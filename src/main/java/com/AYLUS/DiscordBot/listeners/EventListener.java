package com.AYLUS.DiscordBot.listeners;

import com.AYLUS.DiscordBot.HelpfulMethods.HourAndMoneyTracker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Objects;


public class EventListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        String message = event.getMessage().getContentRaw();

        if (message.equalsIgnoreCase(event.getJDA().getSelfUser().getAsMention())) {
            event.getChannel().sendMessage(String.format("Hello, %s, Please use **/volunteer-profile** or **/volunteer-leaderboard**" +
                    " to check your hours!", Objects.requireNonNull(event.getMember()).getAsMention())).queue();
        } else if (message.equalsIgnoreCase("!status")) {
            event.getChannel().sendMessage(event.getJDA().getPresence().getStatus().toString()).queue();
        } else if (message.equalsIgnoreCase("!activity")) {
            String m = event.getJDA().getPresence().getActivity().toString() == null ? "I am doing nothing" : event.getJDA().getPresence().getActivity().toString();
            event.getChannel().sendMessage(m).queue();
        }

        if (message.equalsIgnoreCase("!totals")) {
            event.getChannel().sendMessageEmbeds(
                    new EmbedBuilder()
                            .setTitle("📊 Totals so far")
                            .setColor(Color.BLUE)
                            .addField("Hours Logged", String.format("⏱️ %.1f", HourAndMoneyTracker.getTotalHours()), false)
                            .addField("Money Spent on AYLUS Events", String.format("💸 $%.2f", HourAndMoneyTracker.getTotalMoneyNeeded()), false)
                            .addField("Money Paid Back", String.format("💰 $%.2f", HourAndMoneyTracker.getTotalMoneyPaidBack()), false)
                            .setThumbnail(event.getJDA().getSelfUser().getAvatarUrl())
                            .build()
            ).queue();
        }

    }
}
