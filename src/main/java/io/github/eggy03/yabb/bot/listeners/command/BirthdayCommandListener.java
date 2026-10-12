package io.github.eggy03.yabb.bot.listeners.command;

import io.github.eggy03.yabb.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.yabb.bot.service.command.BirthdayCommandService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.ThreadFactory;

@ApplicationScoped
public class BirthdayCommandListener extends ListenerAdapter {

    private final @NonNull ThreadFactory virtualThreadFactory;
    private final @NonNull BirthdayCommandService birthdayCommandService;

    @Inject
    public BirthdayCommandListener(@NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory, @NonNull BirthdayCommandService birthdayCommandService) {
        this.virtualThreadFactory = virtualThreadFactory;
        this.birthdayCommandService = birthdayCommandService;
    }

    @Override
    public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("birthday")) {
            switch (event.getSubcommandName()) {
                case "save" -> virtualThreadFactory.newThread(()-> birthdayCommandService.handleSave(event)).start();
                case "view" -> virtualThreadFactory.newThread(()-> birthdayCommandService.handleView(event)).start();
                case "viewall" -> virtualThreadFactory.newThread(()-> birthdayCommandService.handleViewAll(event)).start();
                case "delete" -> virtualThreadFactory.newThread(()-> birthdayCommandService.handleDelete(event)).start();
                case null, default -> {
                    // do nothing
                }
            }
        }
    }
}
