package io.github.eggy03.yabb.bot.scheduler;

import io.github.eggy03.yabb.client.ApiException;
import io.github.eggy03.yabb.client.api.BirthdayScheduleApi;
import io.github.eggy03.yabb.client.models.BirthdayScheduleDTO;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class ExpiredScheduleCheck {

    private final @NonNull BirthdayScheduleApi birthdayScheduleApi;
    private final @NonNull ShardManager shardManager;

    @Scheduled(every = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Blocking
    @RunOnVirtualThread // don't use my custom annotation here or else concurrent execution skip will be meaningless
    public void checkExpiredBirthdays() {

        // Get the guild IDs cached by this ShardManager instance.
        Set<Long> guildCacheSet = shardManager.getGuildCache()
                .stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toSet());

        // get all the expired birthday schedules
        // and filter the ones which have guild IDs not present in this ShardManager instance.
        // filtering makes sure that bot instances process only the guilds managed by their respective shards
        // update expired birthday schedules for next year
        getExpiredSchedules()
                .stream()
                .filter(dto -> guildCacheSet.contains(dto.getGuildId()))
                .forEach(this::advanceSchedule);
    }

    // checked api functions (ApiException is caught and sometimes logged)
    // it's hideous to try catch them in the scheduler
    private @NonNull @Unmodifiable List<@NonNull BirthdayScheduleDTO> getExpiredSchedules() {
        try {
            return birthdayScheduleApi.getExpiredSchedules()
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (ApiException e) {
            log.warn("Scheduler: Failed to retrieve expired birthday schedules", e);
            return List.of();
        }
    }

    private void advanceSchedule(@NonNull BirthdayScheduleDTO dto) {

        try {
            birthdayScheduleApi.advance(dto.getGuildId(), dto.getUserId());
        } catch (ApiException e) {
            log.warn("Scheduler: Failed to advance expired birthday schedules", e);
        }
    }
}
