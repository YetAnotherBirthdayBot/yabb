package io.github.eggy03.yabb.bot.listeners.misc;

import io.github.eggy03.yabb.bot.service.misc.SelfKickService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/*
 * This class will have methods that unregister the guild and all the birthdays after it has been kicked from it
 */
@ApplicationScoped
@RequiredArgsConstructor
public class SelfKickListener extends ListenerAdapter {

    private final @NonNull SelfKickService selfKickService;

    @Override
    public void onGuildLeave(@NonNull GuildLeaveEvent event) {
        selfKickService.removeConfigAndRelatedBirthdays(event);
    }
}
