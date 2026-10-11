package io.github.eggy03.yabb.bot.listeners.misc;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@ApplicationScoped
@Slf4j
public final class ActivityUpdateListener extends ListenerAdapter {

    private final @NonNull JDA jda;
    private final @NonNull YabbEnvironment yabbEnvironment;

    @Inject
    public ActivityUpdateListener(@NonNull JDA jda, @NonNull YabbEnvironment yabbEnvironment) {
        this.jda = jda;
        this.yabbEnvironment = yabbEnvironment;
    }

    @Override
    public void onReady(@NonNull ReadyEvent event) { // update on cold start
        jda.getPresence()
                .setActivity(Activity.customStatus("/help | v" + yabbEnvironment.general().appVersion()));

    }

    @Override
    public void onGuildJoin(@NonNull GuildJoinEvent event) { // update on guild join
        log.info("Bot Added To [Guild={}, ID={}]", event.getGuild().getName(), event.getGuild().getId());
    }

    @Override
    public void onGuildLeave(@NonNull GuildLeaveEvent event) { // update on guild leave
        log.info("Bot Removed From [Guild={}, ID={}]", event.getGuild().getName(), event.getGuild().getId());
    }
}