package com.AYLUS.DiscordBot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class ShutdownCommand {


    public static CommandData getCommandData() {
        return Commands.slash("shutdown", "Only use for bot issues + corruption");
    }

    public static void execute(SlashCommandInteractionEvent event) {
        if (event.getUser().getIdLong() == 840216337119969301L) {
            event.reply("Shutting down bot.").queue();
            event.getJDA().shutdown();
        } else {
            event.reply("You don't have permission to use this command!").setEphemeral(true).queue();
        }
    }
}