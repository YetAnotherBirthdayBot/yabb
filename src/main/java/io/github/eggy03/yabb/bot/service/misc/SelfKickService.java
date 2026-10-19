package io.github.eggy03.yabb.bot.service.misc;

import io.github.eggy03.yabb.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.yabb.bot.client.exception.ApiException;
import io.github.eggy03.yabb.bot.client.interfaces.BirthdayClient;
import io.github.eggy03.yabb.bot.client.interfaces.ConfigClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.concurrent.ThreadFactory;

@Slf4j
@ApplicationScoped
public class SelfKickService {

    private final @NonNull ConfigClient configClient;
    private final @NonNull BirthdayClient birthdayClient;
    private final @NonNull ThreadFactory virtualThreadFactory;

    @Inject
    public SelfKickService(@NonNull @RestClient ConfigClient configClient, @NonNull @RestClient BirthdayClient birthdayClient, @NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory) {
        this.configClient = configClient;
        this.birthdayClient = birthdayClient;
        this.virtualThreadFactory = virtualThreadFactory;
    }

    public void removeConfigAndRelatedBirthdays(@NonNull GuildLeaveEvent event) {
        Guild leftGuild = event.getGuild();

        virtualThreadFactory
                .newThread(() -> {
                    try {
                        configClient.deleteConfig(leftGuild.getIdLong());
                        birthdayClient.deleteBirthdayByGuild(leftGuild.getIdLong());
                    } catch (ApiException e) {
                        log.warn("Failed to unregister bot on self kick event", e);
                    }
                })
                .start();

    }
}
