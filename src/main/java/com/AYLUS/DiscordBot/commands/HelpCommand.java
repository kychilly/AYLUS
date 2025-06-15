package com.AYLUS.DiscordBot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class HelpCommand extends ListenerAdapter {

    private static final String commandsList = """
**📋 Volunteer Command List** 📋

🔹 **Log Hours** (Admin only)
`/volunteer-log @User, "Event Name", hours, date(DD/MM/YYYY)

🔹 **View Profile**
`/volunteer-profile` - Shows your profile
`/volunteer-profile @User` - Shows another user's profile

🔹 **Leaderboard**
`/volunteer-leaderboard` - Shows top volunteers with pagination

🔹 **Remove Entry** (Admin only)
`/volunteer-remove @User , "Event Name", hours, date(DD/MM/YYYY)
Removes the specified event entry

📌 **Notes:**
- Dates should be in DD-MM-YY format
- Hours must be between 0.1 and 24.0
- Some commands require admin permissions
""";

    public static CommandData getCommandData() {
        return Commands.slash("help", "commands information");
    }

    public static void execute(SlashCommandInteractionEvent event) {
        event.reply(commandsList).setEphemeral(true).queue();
    }

}
