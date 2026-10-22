package io.github.eggy03.yabb.bot.scheduler;

import io.github.eggy03.yabb.bot.client.interfaces.BirthdayScheduleClient;
import io.github.eggy03.yabb.bot.client.interfaces.ConfigClient;
import io.github.eggy03.yabb.bot.client.model.BirthdayScheduleDTO;
import io.github.eggy03.yabb.bot.client.model.ConfigDTO;
import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class ActiveScheduleCheck {

    private final @NonNull BirthdayScheduleClient birthdayScheduleClient;
    private final @NonNull ConfigClient configClient;
    private final @NonNull YabbEnvironment yabbEnvironment;
    private final @NonNull ShardManager shardManager;

    @Inject
    public ActiveScheduleCheck(@NonNull @RestClient BirthdayScheduleClient birthdayScheduleClient, @NonNull @RestClient ConfigClient configClient, @NonNull YabbEnvironment yabbEnvironment, @NonNull ShardManager shardManager) {
        this.birthdayScheduleClient = birthdayScheduleClient;
        this.configClient = configClient;
        this.yabbEnvironment = yabbEnvironment;
        this.shardManager = shardManager;
    }

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
                .filter(dto -> guildCacheSet.contains(dto.guildId()))
                .forEach(this::sendEmbedAndAdvanceSchedule);
    }

    // embed functions
    private void sendEmbedAndAdvanceSchedule(@NonNull BirthdayScheduleDTO birthdayScheduleDTO) {

        // only the guilds registered to receive birthday events should receive the notifications.
        // if config is null, it means the guild is not registered to receive birthday notifications
        // or the config could not be resolved due to API errors.
        // in either case, exit early
        ConfigDTO configDTO = getConfig(birthdayScheduleDTO.guildId());
        if (configDTO == null)
            return;

        // resolve the channel to which the notification will be sent
        TextChannel textChannel = shardManager.getTextChannelById(configDTO.channelId());
        if (textChannel == null)
            return;

        // skip if notification cant be sent to the channel
        if (!textChannel.canTalk()){
            log.warn("Scheduler: Cannot talk in the text channel. Skipping!");
            return;
        }

        // resolve the announcement role
        Role announcementRole = configDTO.roleId() == null ? null : shardManager.getRoleById(configDTO.roleId());
        String announcementRoleMention = announcementRole == null ? "Everyone" : announcementRole.getAsMention();

        // resolve the birthday user
        User user = shardManager.getUserById(birthdayScheduleDTO.userId());
        String userName = user == null ? "N/A" : user.getEffectiveName();
        String userMention = user == null ? "N/A" : user.getAsMention();

        // create the embed
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Birthday Event Notification");
        eb.setDescription("For %s".formatted(userName));
        eb.setColor(yabbEnvironment.embedColor().successColor());
        eb.setThumbnail(user == null ? null : user.getEffectiveAvatarUrl());

        eb.addField(MarkdownUtil.underline("Birthday Member"), MarkdownUtil.quoteBlock("%s (%s)").formatted(userMention, userName), false);
        eb.addField(MarkdownUtil.underline("Birthday Time"), MarkdownUtil.quoteBlock(TimeFormat.DATE_SHORT_TIME_SHORT.format(birthdayScheduleDTO.upcomingBirthdayAt())), false);

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
                    userName, birthdayScheduleDTO.userId(), birthdayScheduleDTO.guildId(), e
            );
        }
    }

    // checked api functions (All exceptions are caught and sometimes logged)
    // it's hideous to try catch them in the scheduler

    private @NonNull @Unmodifiable List<@NonNull BirthdayScheduleDTO> getActiveSchedules() {
        try {
            return birthdayScheduleClient.getActiveSchedules()
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("Scheduler: Failed to retrieve active birthday schedules", e);
            return List.of();
        }
    }

    private void advanceSchedule(@NonNull BirthdayScheduleDTO birthdayScheduleDTO) {

        try {
            birthdayScheduleClient.advanceSchedule(birthdayScheduleDTO.guildId(), birthdayScheduleDTO.userId());
        } catch (Exception e) {
            log.warn("Scheduler: Failed to advance birthday schedule for guildId: [{}], userId: [{}]",
                    birthdayScheduleDTO.guildId(), birthdayScheduleDTO.userId(), e
            );
        }
    }

    private @Nullable ConfigDTO getConfig(long guildId) {
        try {
            return configClient.getConfig(guildId);
        } catch (Exception e) {
            log.debug("Scheduler: Failed to retrieve configuration for guildId: [{}]", guildId, e);
            return null;
        }
    }
}
