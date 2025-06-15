package com.AYLUS.DiscordBot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class ShutdownCommand {


    public static CommandData getCommandData() {
        return Commands.slash("shutdown", "(Owner only) Shuts bot down");
    }

    public static void execute(SlashCommandInteractionEvent event) {
        if (event.getUser().getIdLong() == 840216337119969301L) {
            event.reply("\uD83D\uDEA8 I am shutting down \uD83D\uDEA8  - Bye bye D:").queue();
            event.getJDA().shutdown();
        } else {
            event.reply("You don't have permission to use this command!").setEphemeral(true).queue();
        }
    }
}