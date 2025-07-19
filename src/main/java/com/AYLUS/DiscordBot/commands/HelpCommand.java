package com.AYLUS.DiscordBot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;

public class HelpCommand extends ListenerAdapter {

    public static CommandData getCommandData() {
        return Commands.slash("help", "View all available commands and their usage");
    }

    public static void execute(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📋 Volunteer Command List")
                .setColor(new Color(59, 152, 229))
                .setDescription("Here are all the available commands for the volunteer system:")
                .addField("🔹 **Log Volunteer Hours** (Admin only)",
                        "`/volunteer-log`\n" +
                                "• `user`: @User to log hours for\n" +
                                "• `event`: Name of the event\n" +
                                "• `hours`: Hours volunteered (0.1-24.0)\n" +
                                "• `date`: DD-MM-YYYY format\n" +
                                "• `money-owed`: Amount owed (xx.xx format, optional)", false)
                .addField("🔹 **View Volunteer Profile**",
                        "`/volunteer-profile` - Your profile\n" +
                                "`/volunteer-profile @User` - Another user's profile", false)
                .addField("🔹 **View Leaderboard**",
                        "`/volunteer-leaderboard` - Shows top volunteers", false)
                .addField("🔹 **Remove Volunteer Entry** (Admin only)",
                        "`/volunteer-remove`\n" +
                                "• `user`: @User whose entry to remove\n" +
                                "• `event`: Event name\n" +
                                "• `date`: DD-MM-YYYY format", false)
                .addField("🔹 **Record Payment** (Admin only)",
                        "`/pay`\n" +
                                "• `user`: @Volunteer who paid\n" +
                                "• `amount`: Payment amount\n" +
                                "• `notes`: Optional notes", false)
                .addField("🔹 **View Payment History**",
                        "`/payment-history` - Your history\n" +
                                "`/payment-history @User` - Another user's history", false)
                .addField("📌 **Notes:**",
                        "- Admin commands require special permissions\n" +
                                "- Money format: xx.xx (e.g., 10.99)\n" +
                                "- Date format: DD-MM-YYYY\n" +
                                "- Hours range: 0.1-24.0", false)
                .setFooter("Need more help? Use the buttons below to contact me");

        // Create a mention button that links to your user ID
        Button contactButton = Button.link("https://discord.com/users/840216337119969301", "My Discord")
                .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("❓"));
        Button emailButton = Button.link("https://mail.google.com/mail/?view=cm&fs=1&to=jyam478@gmail.com", "My Email")
                .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("✉️"));

        event.replyEmbeds(embed.build())
                .setEphemeral(false)
                .addActionRow(contactButton)
                .addActionRow(emailButton)
                .queue();
    }
}