package io.github.eggy03.yabb.bot.service.misc;

import io.github.eggy03.yabb.bot.client.exception.ApiException;
import io.github.eggy03.yabb.bot.client.interfaces.BirthdayClient;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
@Slf4j
public class AutoDeleteBirthdayOnGuildMemberRemoveService {

    private final @NonNull BirthdayClient birthdayClient;

    public AutoDeleteBirthdayOnGuildMemberRemoveService(@NonNull @RestClient BirthdayClient birthdayClient) {
        this.birthdayClient = birthdayClient;
    }

    public void handleOnMemberRemove(@NonNull GuildMemberRemoveEvent event) {

        long guildId = event.getGuild().getIdLong();
        long userId = event.getUser().getIdLong();

        try {
            birthdayClient.deleteBirthday(guildId, userId);
        } catch (ApiException e) {
            if (e.getStatusCode() >= 500)
                log.warn("API Error while deleting a birthday [guildId=[{}], userId=[{}]] on guild member remove", guildId, userId, e);
        }
    }
}
