package com;

import com.AYLUS.DiscordBot.Classes.ProfilePagination;
import com.AYLUS.DiscordBot.commands.ButtonInteractionListener;
import com.AYLUS.DiscordBot.listeners.shutdownListener;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import com.AYLUS.DiscordBot.commands.CommandManager;
import com.AYLUS.DiscordBot.Classes.VolunteerCommands;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import com.AYLUS.DiscordBot.listeners.EventListener;

import javax.security.auth.login.LoginException;

public class AYLUSBot {
    private final ShardManager shardManager;
    private final Dotenv config;

    public AYLUSBot() throws LoginException {
        config = Dotenv.configure().ignoreIfMissing().load();
        String token = config.get("TOKEN");

        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.customStatus("Online and running \uD83D\uDFE2"));
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setChunkingFilter(ChunkingFilter.ALL);

        shardManager = builder.build();

        // Register event listeners
        shardManager.addEventListener(new CommandManager());
        shardManager.addEventListener(new VolunteerCommands());
        shardManager.addEventListener(new EventListener());
        shardManager.addEventListener(new shutdownListener());

        // Add listener for command registration
        shardManager.addEventListener(new ListenerAdapter() {
            @Override
            public void onReady(ReadyEvent event) {
                if (event.getJDA().getShardInfo().getShardId() == 0) {
                    registerCommands(event.getJDA());
                }
            }
        });
        shardManager.addEventListener(new ButtonInteractionListener());
    }

    private void registerCommands(JDA jda) {
        registerGlobalCommands(jda);
    }

    private void registerGlobalCommands(JDA jda) {
        jda.updateCommands()
                .addCommands(
                        Commands.slash("volunteer-log", "Log volunteer hours")
                                .addOptions(
                                        new OptionData(OptionType.USER, "user", "User to log hours for", true),
                                        new OptionData(OptionType.STRING, "event", "Name of the event", true),
                                        new OptionData(OptionType.NUMBER, "hours", "Hours volunteered", true)
                                                .setMinValue(0.1)
                                                .setMaxValue(24.0),
                                        new OptionData(OptionType.STRING, "date", "Date (DD-MM-YYYY)", true),
                                        new OptionData(OptionType.INTEGER, "money-owed", "Money volunteer OWES US (format: xx.xx  ex: 10.99). LEAVE BLANK IF NO MONEY OWED!!!", false)
                                ),
                        Commands.slash("volunteer-profile", "View volunteer profile")
                                .addOptions(
                                        new OptionData(OptionType.USER, "user", "User to view", false)
                                ),
                        Commands.slash("volunteer-leaderboard", "View leaderboard"),
                        Commands.slash("volunteer-remove", "Remove a volunteer entry")
                                .addOptions(
                                        new OptionData(OptionType.USER, "user", "User whose event to remove", true),
                                        new OptionData(OptionType.STRING, "event", "Name of the event to remove", true),
                                        new OptionData(OptionType.STRING, "date", "Date of the event (DD-MM-YYYY)", true)
                                ),
                        Commands.slash("pay", "Record a payment from a volunteer")
                                .addOption(OptionType.USER, "user", "The volunteer who paid", true)
                                .addOption(OptionType.NUMBER, "amount", "Payment amount", true)
                                .addOption(OptionType.STRING, "notes", "Any additional notes? Leave blank if none.", false)
                                .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                        Commands.slash("volunteer-clear", "Only kyche(jeffrey) is allowed to use this command(deletes a person's volunteer data)")
                                .addOption(OptionType.USER, "user", "User to clear", true)
                                .addOption(OptionType.BOOLEAN, "positive", "Be extra careful! Did you select the right person?", true)
                                .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                        Commands.slash("payment-history", "View payment history")
                                .addOption(OptionType.USER, "user", "The user to check", false)
                )
                .queue(
                        success -> {
                            System.out.println("✅ Global commands registered:");
                            jda.retrieveCommands().queue(commands ->
                                    commands.forEach(cmd ->
                                            System.out.println("- " + cmd.getName())
                                    )
                            );
                        },
                        error -> System.err.println("❌ Global command error: " + error.getMessage())
                );
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    public Dotenv getConfig() {
        return config;
    }

    public static void main(String[] args) {
        try {
            new AYLUSBot();
        } catch (LoginException e) {
            System.out.println("Error: Invalid bot token - check your .env file");
        } catch (Exception e) {
            System.out.println("Error: Bot failed to start");
            e.printStackTrace();
        }
    }
}