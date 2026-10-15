package io.github.eggy03.yabb.bot.configuration;

import io.github.eggy03.yabb.client.ApiClient;
import io.github.eggy03.yabb.client.api.BirthdayApi;
import io.github.eggy03.yabb.client.api.BirthdayScheduleApi;
import io.github.eggy03.yabb.client.api.GuildApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

@ApplicationScoped
public class YabbApiProducer {

    private final @NonNull ApiClient apiClient;

    @Inject
    public YabbApiProducer(@NonNull ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Contract(" -> new")
    @Produces
    @ApplicationScoped
    public @NonNull BirthdayApi birthdayApi() {
        return new BirthdayApi(apiClient);
    }

    @Contract(" -> new")
    @Produces
    @ApplicationScoped
    public @NonNull BirthdayScheduleApi birthdayScheduleApi() {
        return new BirthdayScheduleApi(apiClient);
    }

    @Contract(" -> new")
    @Produces
    @ApplicationScoped
    public @NonNull GuildApi guildApi() {
        return new GuildApi(apiClient);
    }
}
