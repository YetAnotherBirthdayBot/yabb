package io.github.eggy03.yabb.bot.service.misc;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;

@ApplicationScoped
@RequiredArgsConstructor
public class ActivityUpdateService {

    private final @NonNull YabbEnvironment yabbEnvironment;

    public void updateActivity(@NonNull ReadyEvent event) {
        event.getJDA()
                .getPresence()
                .setActivity(Activity.customStatus("/help | v" + yabbEnvironment.general().appVersion()));
    }
}
