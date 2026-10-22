package io.github.eggy03.yabb.bot.scheduler;

import io.github.eggy03.yabb.bot.client.interfaces.BirthdayScheduleClient;
import io.github.eggy03.yabb.bot.client.model.BirthdayScheduleDTO;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class ExpiredScheduleCheck {

    private final @NonNull BirthdayScheduleClient birthdayScheduleClient;
    private final @NonNull ShardManager shardManager;

    @Inject
    public ExpiredScheduleCheck(@NonNull @RestClient BirthdayScheduleClient birthdayScheduleClient, @NonNull ShardManager shardManager) {
        this.birthdayScheduleClient = birthdayScheduleClient;
        this.shardManager = shardManager;
    }

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
                .filter(dto -> guildCacheSet.contains(dto.guildId()))
                .forEach(this::advanceSchedule);
    }

    // checked api functions (ApiException is caught and sometimes logged)
    // it's hideous to try catch them in the scheduler
    private @NonNull @Unmodifiable List<@NonNull BirthdayScheduleDTO> getExpiredSchedules() {
        try {
            return birthdayScheduleClient.getExpiredSchedules()
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("Scheduler: Failed to retrieve expired birthday schedules", e);
            return List.of();
        }
    }

    private void advanceSchedule(@NonNull BirthdayScheduleDTO dto) {

        try {
            birthdayScheduleClient.advanceSchedule(dto.guildId(), dto.userId());
        } catch (Exception e) {
            log.warn("Scheduler: Failed to advance expired birthday schedules", e);
        }
    }
}
