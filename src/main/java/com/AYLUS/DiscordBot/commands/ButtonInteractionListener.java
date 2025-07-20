package com.AYLUS.DiscordBot.commands;

import com.AYLUS.DiscordBot.Classes.PaymentPagination;
import com.AYLUS.DiscordBot.Classes.ProfilePagination;
import com.AYLUS.DiscordBot.Classes.VolunteeringLeaderboardCommand;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ButtonInteractionListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        if (event.getComponentId().startsWith("profile:")) {
            ProfilePagination.handleButtonInteraction(event);
        } else if (event.getComponentId().startsWith("payment_history")) {
            PaymentPagination.handleButtonInteraction(event);
        }
        if (buttonId.startsWith("leaderboard_")) {
            // see this later if there are bugs(looks like none so far)
            VolunteeringLeaderboardCommand.handleLeaderboardButton(event, buttonId);
            return;
        }

    }

}
