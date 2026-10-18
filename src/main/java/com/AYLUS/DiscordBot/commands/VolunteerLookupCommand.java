package com.AYLUS.DiscordBot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class VolunteerLookupCommand extends ListenerAdapter {

    public static CommandData getCommandData() {
        return Commands.slash("aihelp", "Ask AI SLM to look up volunteer hours or records")
                .addOption(OptionType.STRING, "prompt", "Natural language question (e.g., 'How many hours does Ellie have in Albany?')", true);
    }

    public static void execute(SlashCommandInteractionEvent event) {
        String prompt = event.getOption("prompt").getAsString();

        // Defer reply so Discord doesn't time out while Python SLM processes the request
        event.deferReply().queue();

        // Run Python SLM pipeline in a separate thread to keep the bot responsive
        new Thread(() -> {
            String slmResponse = queryVolunteerSLM(prompt);

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🤖 SLM Volunteer Lookup Result")
                    .setColor(new Color(59, 152, 229))
                    .addField("❓ **Query**", "`" + prompt + "`", false)
                    .addField("📊 **Result**", slmResponse, false)
                    .setFooter("Powered by local SLM Engine & json_data_storage");

            event.getHook().sendMessageEmbeds(embed.build()).queue();
        }).start();
    }

    private static String queryVolunteerSLM(String userPrompt) {
        StringBuilder output = new StringBuilder();
        try {
            // Path to infer.py relative to the IntelliJ project root
            String pythonScriptPath = Paths.get("src", "main", "slm_python_engine", "infer.py").toString();

            ProcessBuilder processBuilder = new ProcessBuilder("python", pythonScriptPath, userPrompt);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Ignore internal debug/stderr logs from PyTorch/Transformers loading
                    if (!line.startsWith("Loading") && !line.startsWith("Applying") && !line.startsWith("Warning")) {
                        output.append(line).append("\n");
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return "⚠️ Error executing SLM engine (Exit code: " + exitCode + ")";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Exception occurred while calling Python SLM engine: " + e.getMessage();
        }

        String result = output.toString().trim();
        return result.isEmpty() ? "No response received from SLM engine." : result;
    }
}