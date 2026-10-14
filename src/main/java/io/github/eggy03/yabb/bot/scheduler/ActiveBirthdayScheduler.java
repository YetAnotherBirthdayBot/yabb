package io.github.eggy03.yabb.bot.scheduler;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.github.eggy03.yabb.client.ApiException;
import io.github.eggy03.yabb.client.api.BirthdayApi;
import io.github.eggy03.yabb.client.api.GuildApi;
import io.github.eggy03.yabb.client.models.BirthdayRequestDTO;
import io.github.eggy03.yabb.client.models.BirthdayResponseDTO;
import io.github.eggy03.yabb.client.models.GuildDTO;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ActiveBirthdayScheduler {

    private final @NonNull BirthdayApi birthdayApi;
    private final @NonNull GuildApi guildApi;
    private final @NonNull YabbEnvironment yabbEnvironment;
    private final @NonNull ShardManager shardManager;

    @Scheduled(every = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Blocking
    @RunOnVirtualThread // don't use my custom annotation here or else concurrent execution skip will be meaningless
    public void checkActiveBirthdays() {
        orchestrate();
    }

    private void orchestrate() {

        // Get the guild IDs cached by this ShardManager instance.
        Set<Long> guildCacheSet = shardManager.getGuildCache()
                .stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toSet());

        // get all the active birthdays
        // and filter the ones which have guild IDs not present in this ShardManager instance.
        // filtering makes sure that bot instances process only the guilds managed by their respective shards

        // for each active birthday, check the registered guild, build the embed, send it
        // then update birthday for next year
        getActiveBirthdaysChecked()
                .stream()
                .filter(dto -> guildCacheSet.contains(dto.getGuildId()))
                .forEach(this::sendEmbedAndUpdateBirthday);
    }

    // embed functions
    private void sendEmbedAndUpdateBirthday(@NonNull BirthdayResponseDTO birthdayResponseDTO) {

        // only the guilds registered to receive birthday events should receive the notifications.
        // get the registered guild for the given birthday
        // if guild is null, it means the guild is not registered to receive birthday notifications
        // or the guild could not be resolved due to API errors.
        // in either case, exit early
        GuildDTO guildDTO = getGuildChecked(birthdayResponseDTO.getGuildId());
        if (guildDTO == null)
            return;

        // resolve the channel to which the notification will be sent
        TextChannel textChannel = shardManager.getTextChannelById(guildDTO.getChannelId());
        if (textChannel == null)
            return;

        // skip if notification cant be sent to the channel
        if (!textChannel.canTalk()){
            log.warn("Scheduler: Cannot talk in the text channel. Skipping!");
            return;
        }

        // resolve the birthday user
        User user = shardManager.getUserById(birthdayResponseDTO.getUserId());
        String userName = user == null ? "N/A" : user.getEffectiveName();
        String userMention = user == null ? "N/A" : user.getAsMention();

        // resolve the announcement role
        Role announcementRole = guildDTO.getRoleId() == null ? null : shardManager.getRoleById(guildDTO.getRoleId());
        String announcementRoleMention = announcementRole == null ? "Everyone" : announcementRole.getAsMention();

        // create the embed
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Birthday Event Notification");
        eb.setDescription("For %s".formatted(userName));
        eb.setColor(yabbEnvironment.embedColor().successColor());
        eb.setThumbnail(user == null ? null : user.getEffectiveAvatarUrl());

        eb.addField(MarkdownUtil.underline("Birthday Member"), MarkdownUtil.quoteBlock("%s (%s)").formatted(userMention, userName), false);
        eb.addField(MarkdownUtil.underline("Birthday Time"), MarkdownUtil.quoteBlock(TimeFormat.DATE_SHORT_TIME_SHORT.format(birthdayResponseDTO.getUpcomingBirthday())), false);

        eb.setFooter("Sent");
        eb.setTimestamp(Instant.now());

        // send the embed to the text channel
        try {
            textChannel.sendMessage("%s, let's celebrate %s's birthday!".formatted(announcementRoleMention, userName))
                    .addEmbeds(eb.build())
                    .complete(); // don't use async cause this needs to complete in the scheduler lifecycle itself

            // on success, update the birthday for next year
            updateBirthdayForNextYearChecked(birthdayResponseDTO);
        } catch (Exception e) {
            log.warn("Scheduler: Failed to send birthday notification", e);
        }
    }

    // checked api functions (ApiException is caught and sometimes logged)
    // it's hideous to try catch them in the scheduler

    private @NonNull @Unmodifiable List<@NonNull BirthdayResponseDTO> getActiveBirthdaysChecked() {
        try {
            return birthdayApi.getActiveBirthdays()
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (ApiException e) {
            log.warn("Scheduler: Failed to retrieve active birthdays", e);
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
            log.debug("Scheduler: Failed to retrieve guild", e);
            return null;
        }
    }
}
