package com.AYLUS.DiscordBot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class MainPageCommand extends ListenerAdapter {


    public static CommandData getCommandData() {
        return Commands.slash("aylus", "AYLUS webpage")
                .addOption(OptionType.BOOLEAN, "cheshire", "Cheshire AYLUS webpage", false);

    }

    public static void HandleEvent(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("cheshire");
        boolean cheshire = option != null && option.getAsBoolean();


        if (cheshire) {
            event.reply("https://aylus.org/cheshire-ct/").queue();
            return;
        }
        event.reply("https://aylus.org").queue();

    }

}
