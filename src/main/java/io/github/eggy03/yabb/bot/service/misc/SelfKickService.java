package io.github.eggy03.yabb.bot.service.misc;

import io.github.eggy03.yabb.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.yabb.client.YabbClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;

import java.util.concurrent.ThreadFactory;

@ApplicationScoped
public class SelfKickService {

    private final @NonNull YabbClient yabbClient;
    private final @NonNull ThreadFactory virtualThreadFactory;

    @Inject
    public SelfKickService(@NonNull YabbClient yabbClient, @NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory) {
        this.yabbClient = yabbClient;
        this.virtualThreadFactory = virtualThreadFactory;
    }

    public void removeGuildAndRelatedBirthdays(@NonNull GuildLeaveEvent event) {
        Guild leftGuild = event.getGuild();

        virtualThreadFactory
                .newThread(() -> {
                    yabbClient.api().v1().guild().byGuildId(leftGuild.getIdLong()).delete();
                    yabbClient.api().v1().birthday().byGuildId(leftGuild.getIdLong()).delete();
                })
                .start();

    }
}
