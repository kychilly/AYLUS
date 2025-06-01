package com;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.OptionType;


import javax.security.auth.login.LoginException;

public class AYLUSBot {

    private final ShardManager shardManager;
    private final Dotenv config;

    public AYLUSBot() throws LoginException {
        config = Dotenv.configure().ignoreIfMissing().load();
        String token = config.get("TOKEN");

        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.watching("the sunflowers"));
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setChunkingFilter(ChunkingFilter.ALL);

        shardManager = builder.build();

        // Register event listeners
        shardManager.addEventListener(new CommandManager());
        shardManager.addEventListener(new VolunteerCommands());

        // Add listener for command registration
        shardManager.addEventListener(new ListenerAdapter() {
            @Override
            public void onReady(ReadyEvent event) {
                // Only register commands once (using shard 0)
                if (event.getJDA().getShardInfo().getShardId() == 0) {
                    registerCommands(event.getJDA());
                }
            }
        });
    }

    private void registerCommands(JDA jda) {
        // 1. First check if guild exists
        String TEST_GUILD_ID = "1186115782313267321";
        Guild testGuild = jda.getGuildById(TEST_GUILD_ID);

        if (testGuild == null) {
            System.err.println("❌ Test guild not found! Using global commands instead");
            registerGlobalCommands(jda);
            return;
        }

        // 2. Register guild-specific commands
        testGuild.updateCommands()
                .addCommands(
                        // Include ALL your commands
                        Commands.slash("volunteer-log", "Log volunteer hours")
                                .addOptions(/* your options */),
                        Commands.slash("volunteer-profile", "View volunteer profile")
                                .addOptions(/* your options */),
                        Commands.slash("volunteer-leaderboard", "View leaderboard"),
                        Commands.slash("volunteer-remove", "Remove a volunteer entry")
                                .addOption(OptionType.USER, "user", "User to remove from", true)
                                .addOption(OptionType.STRING, "event", "Event name", true)
                                .addOption(OptionType.STRING, "date", "Date (YYYY-MM-DD)", true)
                )
                .queue(
                        success -> {
                            System.out.println("✅ Commands registered in test guild:");
                            // Print all registered commands
                            testGuild.retrieveCommands().queue(commands ->
                                    commands.forEach(cmd ->
                                            System.out.println("- " + cmd.getName())
                                    )
                            );
                        },
                        error -> {
                            System.err.println("❌ Guild command error: " + error.getMessage());
                            // Fallback to global registration
                            registerGlobalCommands(jda);
                        }
                );
    }

    private void registerGlobalCommands(JDA jda) {
        jda.updateCommands()
                .addCommands(VolunteerCommands.getCommandData())
                .queue(
                        success -> System.out.println("✅ Global commands registered"),
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
            AYLUSBot bot = new AYLUSBot();
        } catch (LoginException e) {
            System.out.println("Error: Invalid bot token - check your .env file");
        } catch (Exception e) {
            System.out.println("Error: Bot failed to start");
            e.printStackTrace();
        }
    }
}