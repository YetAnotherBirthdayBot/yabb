package io.github.eggy03.yabb.bot.listeners.misc;

import io.github.eggy03.yabb.bot.service.misc.SlashCommandRegistrationService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/*
 * Registers all the slash commands places throughout the code on startup
 */
@ApplicationScoped
@RequiredArgsConstructor
public class SlashCommandRegistrationListener extends ListenerAdapter {

    private final @NonNull SlashCommandRegistrationService slashCommandRegistrationService;

    @Override
    public void onReady(@NonNull ReadyEvent event) {
        slashCommandRegistrationService.registerCommands(event);
    }

}
