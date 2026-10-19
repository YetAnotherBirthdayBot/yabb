package io.github.eggy03.yabb.bot.client.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
@Getter
@RegisterForReflection
public class BirthdayScheduleDTO {

    private final @NonNull Long guildId;
    private final @NonNull Long userId;
    private final @NonNull Instant upcomingBirthdayAt;
}
