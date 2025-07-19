package com.AYLUS.DiscordBot.commands;

import com.AYLUS.DiscordBot.Classes.PaymentPagination;
import com.AYLUS.DiscordBot.Classes.ProfilePagination;
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

    }

}
