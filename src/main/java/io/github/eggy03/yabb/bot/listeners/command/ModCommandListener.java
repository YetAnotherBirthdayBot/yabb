package io.github.eggy03.yabb.bot.listeners.command;

import io.github.eggy03.yabb.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.yabb.bot.service.command.ModCommandService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.ThreadFactory;

@ApplicationScoped
public class ModCommandListener extends ListenerAdapter {

    private final @NonNull ThreadFactory virtualThreadFactory;
    private final @NonNull ModCommandService modCommandService;

    @Inject
    public ModCommandListener(@NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory, @NonNull ModCommandService modCommandService) {
        this.virtualThreadFactory = virtualThreadFactory;
        this.modCommandService = modCommandService;
    }

    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("mod")) {
            switch (event.getSubcommandName()) {
                case "savebirthday" -> virtualThreadFactory.newThread(()-> modCommandService.handleSave(event)).start();
                case "deletebirthday" -> virtualThreadFactory.newThread(()-> modCommandService.handleDelete(event)).start();
                case null, default -> {
                    // do nothing
                }
            }
        }
    }
}
