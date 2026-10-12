package io.github.eggy03.yabb.bot.listeners.misc;

import io.github.eggy03.yabb.bot.service.misc.ActivityUpdateService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@ApplicationScoped
@RequiredArgsConstructor
public class ActivityUpdateListener extends ListenerAdapter {

    private final @NonNull ActivityUpdateService activityUpdateService;

    @Override
    public void onReady(@NonNull ReadyEvent event) { // update activity on cold start
        activityUpdateService.updateActivity(event);
    }
}