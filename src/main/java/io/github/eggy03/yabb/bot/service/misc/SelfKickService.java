package io.github.eggy03.yabb.bot.service.misc;

import io.github.eggy03.yabb.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.yabb.client.ApiException;
import io.github.eggy03.yabb.client.api.BirthdayApi;
import io.github.eggy03.yabb.client.api.ConfigApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;

import java.util.concurrent.ThreadFactory;

@Slf4j
@ApplicationScoped
public class SelfKickService {

    private final @NonNull ConfigApi configApi;
    private final @NonNull BirthdayApi birthdayApi;
    private final @NonNull ThreadFactory virtualThreadFactory;

    @Inject
    public SelfKickService(@NonNull ConfigApi configApi, @NonNull BirthdayApi birthdayApi, @NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory) {
        this.configApi = configApi;
        this.birthdayApi = birthdayApi;
        this.virtualThreadFactory = virtualThreadFactory;
    }

    public void removeConfigAndRelatedBirthdays(@NonNull GuildLeaveEvent event) {
        Guild leftGuild = event.getGuild();

        virtualThreadFactory
                .newThread(() -> {
                    try {
                        configApi.deleteConfig(leftGuild.getIdLong());
                        birthdayApi.deleteBirthdays(leftGuild.getIdLong());
                    } catch (ApiException e) {
                        log.warn("Failed to unregister bot on self kick event\n{}", e.getResponseBody());
                    }
                })
                .start();

    }
}
