package io.github.eggy03.yabb.bot.listeners.command;

import io.github.eggy03.yabb.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.yabb.bot.service.command.ConfigCommandService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.ThreadFactory;

@ApplicationScoped
public class ConfigCommandListener extends ListenerAdapter {

    private final @NonNull ThreadFactory virtualThreadFactory;
    private final @NonNull ConfigCommandService configCommandService;

    @Inject
    public ConfigCommandListener(@NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory, @NonNull ConfigCommandService configCommandService) {
        this.virtualThreadFactory = virtualThreadFactory;
        this.configCommandService = configCommandService;
    }

    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("config")) {
            switch (event.getSubcommandName()) {
                case "save" -> virtualThreadFactory.newThread(()-> configCommandService.handleSave(event)).start();
                case "view" -> virtualThreadFactory.newThread(()-> configCommandService.handleView(event)).start();
                case "delete" -> virtualThreadFactory.newThread(()-> configCommandService.handleDelete(event)).start();
                case null, default -> {
                    // do nothing
                }
            }
        }
    }
}
