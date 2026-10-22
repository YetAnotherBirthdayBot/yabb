package io.github.eggy03.yabb.bot.listeners.misc;

import io.github.eggy03.yabb.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.yabb.bot.service.misc.AutoDeleteBirthdayOnGuildMemberRemoveService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.ThreadFactory;

@ApplicationScoped
public class AutoDeleteBirthdayOnGuildMemberRemoveListener extends ListenerAdapter {

    private final @NonNull AutoDeleteBirthdayOnGuildMemberRemoveService service;
    private final @NonNull ThreadFactory virtualThreadFactory;

    @Inject
    public AutoDeleteBirthdayOnGuildMemberRemoveListener(@NonNull AutoDeleteBirthdayOnGuildMemberRemoveService service, @NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory) {
        this.service = service;
        this.virtualThreadFactory = virtualThreadFactory;
    }

    @Override
    public void onGuildMemberRemove(@NonNull GuildMemberRemoveEvent event) {
        virtualThreadFactory
                .newThread(()-> service.handleOnMemberRemove(event))
                .start();
    }
}
