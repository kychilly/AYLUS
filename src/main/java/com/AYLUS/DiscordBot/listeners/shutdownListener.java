package com.AYLUS.DiscordBot.listeners;

import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import org.jetbrains.annotations.NotNull;

public class shutdownListener extends ListenerAdapter {

    private static final long CHANNEL_ID = 1186115783013711894L;

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        try {
            TextChannel channel = event.getJDA().getTextChannelById(CHANNEL_ID);
            if (channel == null) {
                System.out.println("Shutdown channel not found!");
                return;
            }

            channel.sendMessage("Bot is shutting down. Bye byes!").queue(
                    success -> System.out.println("Sent shutdown message"),
                    error -> System.err.println("Failed to send shutdown message: " + error)
            );
        } catch (Exception e) {
            System.err.println("Error in shutdown handler: " + e);
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        if (event.getAuthor().isBot()) {
            return;
        }
        if (message.equals("!shutdown")) {
            if (event.getMember().getUser().getIdLong() == 840216337119969301L) {
                event.getChannel().sendMessage("\uD83D\uDEA8 I am shutting down \uD83D\uDEA8  - Bye bye D:").queue(response -> {
                    event.getJDA().shutdown();
                });
                return;
            }
        }
    }

}
