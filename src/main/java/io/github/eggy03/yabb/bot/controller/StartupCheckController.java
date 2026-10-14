package io.github.eggy03.yabb.bot.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Startup;

import java.util.Map;

@Startup
@ApplicationScoped
public class StartupCheckController implements HealthCheck {

    private final @NonNull ShardManager shardManager;

    @Inject
    public StartupCheckController(@NonNull ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Override
    public HealthCheckResponse call() {

        Map<JDA, JDA.Status> statuses = shardManager.getStatuses();

        boolean ready = !statuses.isEmpty()
                && statuses.values().stream().allMatch(JDA.Status::isInit);

        return HealthCheckResponse.named("Readiness Check")
                .status(ready)
                .build();
    }
}
