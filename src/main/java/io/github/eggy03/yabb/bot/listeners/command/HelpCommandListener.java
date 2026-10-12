package io.github.eggy03.yabb.bot.listeners.command;

import io.github.eggy03.yabb.bot.service.command.HelpCommandService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@ApplicationScoped
@RequiredArgsConstructor
public class HelpCommandListener extends ListenerAdapter {

    private final @NonNull HelpCommandService helpCommandService;

    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("help")) {
            helpCommandService.handleCommand(event);
        }
    }
}


