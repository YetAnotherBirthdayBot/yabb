package io.github.eggy03.yabb.bot.listeners.misc;

import io.github.eggy03.yabb.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.yabb.client.YabbClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.ThreadFactory;

/*
 * This class will have methods that unregister the guild and all the birthdays after it has been kicked from it
 */
@ApplicationScoped
public final class SelfKickListener extends ListenerAdapter {

    @NonNull
    private final YabbClient yabbClient;

    private final @NonNull ThreadFactory virtualThreadFactory;

    @Inject
    public SelfKickListener(@NonNull YabbClient yabbClient, @NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory) {
        this.yabbClient = yabbClient;
        this.virtualThreadFactory = virtualThreadFactory;
    }

    @Override
    public void onGuildLeave(@NonNull GuildLeaveEvent event) {
        Guild leftGuild = event.getGuild();

        virtualThreadFactory
                .newThread(() -> {
                    yabbClient.api().v1().guild().byGuildId(leftGuild.getIdLong()).delete();
                    yabbClient.api().v1().birthday().byGuildId(leftGuild.getIdLong()).delete();
                })
                .start();

    }
}
