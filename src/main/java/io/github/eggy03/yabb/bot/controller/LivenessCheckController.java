package io.github.eggy03.yabb.bot.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import net.dv8tion.jda.api.JDA;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import java.util.Set;

@Liveness
@ApplicationScoped
public class LivenessCheckController implements HealthCheck {

    private static final Set<JDA.Status> HEALTHY_STATUSES = Set.of(
            JDA.Status.CONNECTED,
            JDA.Status.ATTEMPTING_TO_RECONNECT,
            JDA.Status.RECONNECT_QUEUED,
            JDA.Status.WAITING_TO_RECONNECT
    );

    private final @NonNull JDA jda;

    @Inject
    public LivenessCheckController(@NonNull JDA jda) {
        this.jda = jda;
    }

    @Override
    public HealthCheckResponse call() {

        if (HEALTHY_STATUSES.contains(jda.getStatus())) {
            return HealthCheckResponse.named("Liveness Check")
                    .withData("Status", "Healthy")
                    .up()
                    .build();
        } else {
            return HealthCheckResponse.named("Liveness Check")
                    .withData("Status", jda.getStatus().name())
                    .down()
                    .build();
        }

    }
}
