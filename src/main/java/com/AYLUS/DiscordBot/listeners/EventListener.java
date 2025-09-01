package com.AYLUS.DiscordBot.listeners;

import com.AYLUS.DiscordBot.HelpfulMethods.HourAndMoneyTracker;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;


public class EventListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        String message = event.getMessage().getContentRaw();

        if (message.equalsIgnoreCase("!testServer")) {
            event.getChannel().sendMessage("https://discord.gg/jmGjCgGmJG").queue();
        } else if (message.equalsIgnoreCase("!status")) {
            event.getChannel().sendMessage(event.getJDA().getPresence().getStatus().toString()).queue();
        } else if (message.equalsIgnoreCase("!activity")) {
            String m = event.getJDA().getPresence().getActivity().toString() == null ? "I am doing nothing" : event.getJDA().getPresence().getActivity().toString();
            event.getChannel().sendMessage(m).queue();
        }

        if (message.equalsIgnoreCase("!totalhours")) {
            event.getChannel().sendMessage(String.format("I have logged a total of %s hours", HourAndMoneyTracker.getTotalHours())).queue();
        } else if (message.equalsIgnoreCase("!totalmoney")) {
            event.getChannel().sendMessage(String.format("I have logged $%s needed payments back to AYLUS and $%s payments to/from AYLUS", HourAndMoneyTracker.getTotalMoneyNeeded(), HourAndMoneyTracker.getTotalMoneyPaidBack())).queue();
        }

    }
}
