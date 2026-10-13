package io.github.eggy03.yabb.bot.scheduler;

import io.github.eggy03.yabb.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.github.eggy03.yabb.client.ApiException;
import io.github.eggy03.yabb.client.api.BirthdayApi;
import io.github.eggy03.yabb.client.api.GuildApi;
import io.github.eggy03.yabb.client.models.BirthdayRequestDTO;
import io.github.eggy03.yabb.client.models.BirthdayResponseDTO;
import io.github.eggy03.yabb.client.models.GuildDTO;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class BirthdayNotificationScheduler {

    private final @NonNull BirthdayApi birthdayApi;
    private final @NonNull GuildApi guildApi;
    private final @NonNull YabbEnvironment yabbEnvironment;
    private final @NonNull ThreadFactory virtualThreadFactory;
    private final @NonNull JDA jda;

    @Inject
    public BirthdayNotificationScheduler(@NonNull BirthdayApi birthdayApi, @NonNull GuildApi guildApi, @NonNull YabbEnvironment yabbEnvironment, @NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory, @NonNull JDA jda) {
        this.birthdayApi = birthdayApi;
        this.guildApi = guildApi;
        this.yabbEnvironment = yabbEnvironment;
        this.virtualThreadFactory = virtualThreadFactory;
        this.jda = jda;
    }

    @Scheduled(every = "360s")
    public void checkPendingBirthdays() {
        virtualThreadFactory.newThread(this::orchestrate).start();
    }

    private void orchestrate() {

        // Get the guild IDs cached by this JDA instance.
        Set<Long> guildCacheSet = jda.getGuildCache()
                .stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toSet());

        // get all the pending birthdays
        // and filter the ones which have guild IDs not present in this JDA
        List<BirthdayResponseDTO> pendingBirthdayList = getPendingBirthdaysChecked()
                .stream()
                .filter(dto -> guildCacheSet.contains(dto.getGuildId()))
                .toList();

        // for each pending birthday, check the registered guild, build the embed, send it
        // then update birthday for next year
        pendingBirthdayList.forEach(birthday -> {

            // get the registered guild
            GuildDTO guildDTO = getGuildChecked(birthday.getGuildId());

            // guildDTO being null could mean that the guild containing birthdays has been unregistered
            // or there were other API errors.
            // For the former, we could potentially silently update the birthday for next year to prevent
            // accumulation of notifications, but it's a low priority
            if (guildDTO != null)
                sendEmbedAndUpdateBirthday(guildDTO, birthday);
        });
    }

    // embed functions
    private void sendEmbedAndUpdateBirthday(@NonNull GuildDTO guildDTO, @NonNull BirthdayResponseDTO birthdayResponseDTO) {

        // resolve the birthday user
        User user = jda.getUserById(birthdayResponseDTO.getUserId());
        String userName = user == null ? null : user.getEffectiveName();
        String userMention = user == null ? null : user.getAsMention();

        // resolve the announcement role
        Role announcementRole = guildDTO.getRoleId() == null ? null : jda.getRoleById(guildDTO.getRoleId());
        String announcementRoleMention = announcementRole == null ? "Everyone" : announcementRole.getAsMention();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Scheduled Birthday Event Notification");
        eb.setDescription("For %s (%s)".formatted(userMention, userName));
        eb.setColor(yabbEnvironment.embedColor().successColor());

        eb.addField(MarkdownUtil.quote("Please Wish"), MarkdownUtil.quoteBlock("%s (%s), a very happy birthday").formatted(userMention, userName), false);

        eb.setFooter("This notification was scheduled for");
        eb.setTimestamp(birthdayResponseDTO.getNextBirthdayAt());

        TextChannel textChannel = jda.getTextChannelById(guildDTO.getChannelId());
        if (textChannel != null && textChannel.canTalk()) {
            textChannel.sendMessage("%s, let's celebrate!".formatted(announcementRoleMention))
                    .addEmbeds(eb.build())
                    .complete(); // don't use async cause this method is wrapped by virtual threads

            // on success, update the birthday for next year
            updateBirthdayForNextYearChecked(birthdayResponseDTO);
        }

    }

    // checked api functions (ApiException is caught and sometimes logged)
    // it's hideous to try catch them in the scheduler

    private @NonNull @Unmodifiable List<@NonNull BirthdayResponseDTO> getPendingBirthdaysChecked() {
        try {
            return birthdayApi.getPendingBirthdays()
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (ApiException e) {
            log.debug("Scheduler: No pending birthdays found", e);
            return List.of();
        }
    }

    private void updateBirthdayForNextYearChecked(@NonNull BirthdayResponseDTO birthday) {

        BirthdayRequestDTO birthdayRequestDTO = new BirthdayRequestDTO();
        birthdayRequestDTO.setGuildId(birthday.getGuildId());
        birthdayRequestDTO.setUserId(birthday.getUserId());
        birthdayRequestDTO.setDay(birthday.getDay());
        birthdayRequestDTO.setMonth(birthday.getMonth());
        birthdayRequestDTO.setZoneId(birthday.getZoneId());

        try {
            birthdayApi.updateBirthdayForNextYear(birthdayRequestDTO);
        } catch (ApiException e) {
            log.warn("Scheduler: Failed to update birthday for next year", e);
        }
    }

    private @Nullable GuildDTO getGuildChecked(long guildId) {
        try {
            return guildApi.getGuild(guildId);
        } catch (ApiException e) {
            log.debug("Scheduler: Guild to send notification not found", e);
            return null;
        }
    }
}
