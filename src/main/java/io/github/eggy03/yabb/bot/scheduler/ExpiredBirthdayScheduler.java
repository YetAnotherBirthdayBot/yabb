package io.github.eggy03.yabb.bot.scheduler;

import io.github.eggy03.yabb.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.yabb.client.ApiException;
import io.github.eggy03.yabb.client.api.BirthdayApi;
import io.github.eggy03.yabb.client.models.BirthdayRequestDTO;
import io.github.eggy03.yabb.client.models.BirthdayResponseDTO;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;

@ApplicationScoped
@Slf4j
public class ExpiredBirthdayScheduler {

    private final @NonNull BirthdayApi birthdayApi;
    private final @NonNull ThreadFactory virtualThreadFactory;

    @Inject
    public ExpiredBirthdayScheduler(@NonNull BirthdayApi birthdayApi, @NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory) {
        this.birthdayApi = birthdayApi;
        this.virtualThreadFactory = virtualThreadFactory;
    }

    @Scheduled(every = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void checkExpiredBirthdays() {
        virtualThreadFactory.newThread(this::orchestrate).start();
    }

    private void orchestrate() {

        // get all the expired birthdays
        // and update them for next year
        getExpiredBirthdaysChecked().forEach(this::updateBirthdayForNextYearChecked);
    }

    // checked api functions (ApiException is caught and sometimes logged)
    // it's hideous to try catch them in the scheduler

    private @NonNull @Unmodifiable List<@NonNull BirthdayResponseDTO> getExpiredBirthdaysChecked() {
        try {
            return birthdayApi.getExpiredBirthdays()
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();
        } catch (ApiException e) {
            log.warn("Scheduler: Failed to retrieve expired birthdays", e);
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
            log.warn("Scheduler: Failed to update expired birthday for next year", e);
        }
    }
}
