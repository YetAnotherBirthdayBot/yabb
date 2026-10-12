package io.github.eggy03.yabb.bot.configuration;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.github.eggy03.yabb.client.ApiClient;
import io.github.eggy03.yabb.client.Configuration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

@ApplicationScoped
public final class YabbApiClientProducer {

    private final @NonNull YabbEnvironment yabbEnvironment;

    public YabbApiClientProducer(@NonNull YabbEnvironment yabbEnvironment) {
        this.yabbEnvironment = yabbEnvironment;
    }

    @Contract(" -> new")
    @Produces
    @ApplicationScoped
    public @NonNull ApiClient apiClient() {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(yabbEnvironment.api().url());

        return defaultClient;
    }
}
