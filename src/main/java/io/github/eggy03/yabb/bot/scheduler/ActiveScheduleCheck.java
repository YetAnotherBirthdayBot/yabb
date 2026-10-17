package io.github.eggy03.yabb.bot.scheduler;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.github.eggy03.yabb.client.ApiException;
import io.github.eggy03.yabb.client.api.BirthdayScheduleApi;
import io.github.eggy03.yabb.client.api.ConfigApi;
import io.github.eggy03.yabb.client.models.BirthdayScheduleDTO;
import io.github.eggy03.yabb.client.models.ConfigDTO;
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
public class ActiveScheduleCheck {

    private final @NonNull BirthdayScheduleApi birthdayScheduleApi;
    private final @NonNull ConfigApi configApi;
    private final @NonNull YabbEnvironment yabbEnvironment;
    private final @NonNull ShardManager shardManager;

    @Scheduled(every = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Blocking
    @RunOnVirtualThread // don't use my custom annotation here or else concurrent execution skip will be meaningless
    public void checkActiveSchedule() {

        // Get the guild IDs cached by this ShardManager instance.
        Set<Long> guildCacheSet = shardManager.getGuildCache()
                .stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toSet());

        // get all the active birthday schedules
        // and filter the ones which have guild IDs not present in this ShardManager instance.
        // filtering makes sure that bot instances process only the guilds managed by their respective shards

        // for each active schedule, check the registered guild, build the embed, send it
        // then advance the schedule
        getActiveSchedules()
                .stream()
                .filter(dto -> guildCacheSet.contains(dto.getGuildId()))
                .forEach(this::sendEmbedAndAdvanceSchedule);
    }

    // embed functions
    private void sendEmbedAndAdvanceSchedule(@NonNull BirthdayScheduleDTO birthdayScheduleDTO) {

        // only the guilds registered to receive birthday events should receive the notifications.
        // if config is null, it means the guild is not registered to receive birthday notifications
        // or the config could not be resolved due to API errors.
        // in either case, exit early
        ConfigDTO configDTO = getConfig(birthdayScheduleDTO.getGuildId());
        if (configDTO == null)
            return;

        // resolve the channel to which the notification will be sent
        TextChannel textChannel = shardManager.getTextChannelById(configDTO.getChannelId());
        if (textChannel == null)
            return;

        // skip if notification cant be sent to the channel
        if (!textChannel.canTalk()){
            log.warn("Scheduler: Cannot talk in the text channel. Skipping!");
            return;
        }

        // resolve the announcement role
        Role announcementRole = configDTO.getRoleId() == null ? null : shardManager.getRoleById(configDTO.getRoleId());
        String announcementRoleMention = announcementRole == null ? "Everyone" : announcementRole.getAsMention();

        // resolve the birthday user
        User user = shardManager.getUserById(birthdayScheduleDTO.getUserId());
        String userName = user == null ? "N/A" : user.getEffectiveName();
        String userMention = user == null ? "N/A" : user.getAsMention();

        // create the embed
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Birthday Event Notification");
        eb.setDescription("For %s".formatted(userName));
        eb.setColor(yabbEnvironment.embedColor().successColor());
        eb.setThumbnail(user == null ? null : user.getEffectiveAvatarUrl());

        eb.addField(MarkdownUtil.underline("Birthday Member"), MarkdownUtil.quoteBlock("%s (%s)").formatted(userMention, userName), false);
        eb.addField(MarkdownUtil.underline("Birthday Time"), MarkdownUtil.quoteBlock(TimeFormat.DATE_SHORT_TIME_SHORT.format(birthdayScheduleDTO.getUpcomingBirthdayAt())), false);

        eb.setFooter("Sent");
        eb.setTimestamp(Instant.now());

        // send the embed to the text channel
        try {
            textChannel.sendMessage("%s, let's celebrate %s's birthday!".formatted(announcementRoleMention, userName))
                    .addEmbeds(eb.build())
                    .complete(); // don't use async cause this needs to complete in the scheduler lifecycle itself

            // on success, advance the birthday schedule
            advanceSchedule(birthdayScheduleDTO);
        } catch (Exception e) {
            log.warn("Scheduler: Failed to send birthday notification for user:[{}], having userId:[{}], in guild: [{}]",
                    userName, birthdayScheduleDTO.getUserId(), birthdayScheduleDTO.getGuildId(), e
            );
        }
    }

    // checked api functions (ApiException is caught and sometimes logged)
    // it's hideous to try catch them in the scheduler

    private @NonNull @Unmodifiable List<@NonNull BirthdayScheduleDTO> getActiveSchedules() {
        try {
            return birthdayScheduleApi.getActiveSchedules()
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (ApiException e) {
            log.warn("Scheduler: Failed to retrieve active birthday schedules", e);
            return List.of();
        }
    }

    private void advanceSchedule(@NonNull BirthdayScheduleDTO birthdayScheduleDTO) {

        try {
            birthdayScheduleApi.advance(birthdayScheduleDTO.getGuildId(), birthdayScheduleDTO.getUserId());
        } catch (ApiException e) {
            log.warn("Scheduler: Failed to advance birthday schedule for guildId: [{}], userId: [{}]",
                    birthdayScheduleDTO.getGuildId(), birthdayScheduleDTO.getUserId(), e
            );
        }
    }

    private @Nullable ConfigDTO getConfig(long guildId) {
        try {
            return configApi.getConfig(guildId);
        } catch (ApiException e) {
            log.debug("Scheduler: Failed to retrieve configuration for guildId: [{}]", guildId, e);
            return null;
        }
    }
}
