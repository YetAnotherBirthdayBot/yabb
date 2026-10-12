package io.github.eggy03.yabb.bot.listeners.command;

import io.github.eggy03.yabb.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.yabb.bot.service.command.AdminCommandService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.ThreadFactory;

@ApplicationScoped
public class AdminCommandListener extends ListenerAdapter {

    private final @NonNull ThreadFactory virtualThreadFactory;
    private final @NonNull AdminCommandService adminCommandService;

    @Inject
    public AdminCommandListener(@NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory, @NonNull AdminCommandService adminCommandService) {
        this.virtualThreadFactory = virtualThreadFactory;
        this.adminCommandService = adminCommandService;
    }

    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("admin")) {
            switch (event.getSubcommandName()) {
                case "savebirthday" -> virtualThreadFactory.newThread(()-> adminCommandService.handleSave(event)).start();
                case "deletebirthday" -> virtualThreadFactory.newThread(()-> adminCommandService.handleDelete(event)).start();
                case null, default -> {
                    // do nothing
                }
            }
        }
    }
}
