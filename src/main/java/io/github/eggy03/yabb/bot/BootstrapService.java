package io.github.eggy03.yabb.bot;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

@Slf4j
@ApplicationScoped
@Startup
public final class BootstrapService {

    private final @NonNull YabbEnvironment yabbEnvironment;
    private final @NonNull Instance<ListenerAdapter> listeners;
    private final @NonNull JDA jda;

    @Inject
    public BootstrapService(@NonNull YabbEnvironment yabbEnvironment, @NonNull Instance<ListenerAdapter> listeners) {
        this.yabbEnvironment = yabbEnvironment;
        this.listeners = listeners;
        this.jda = constructJDA();
    }

    @NonNull
    JDA constructJDA() {

        JDABuilder builder = JDABuilder.createDefault(yabbEnvironment.discord().token());

        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setChunkingFilter(ChunkingFilter.ALL);

        // set status
        builder.setStatus(OnlineStatus.ONLINE);

        // add listeners
        listeners.forEach(listener -> {
            log.debug("Registering Listener: {}", listener.getClass().getSimpleName());
            builder.addEventListeners(listener);
        });

        // build JDA and login
        return builder.build();
    }

    @Produces
    @ApplicationScoped
    @NonNull
    JDA getJda() {
        return jda;
    }
}
