package io.github.eggy03.yabb.bot.client.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.NonNull;

import java.time.Instant;

@RegisterForReflection
public record BirthdayScheduleDTO(@NonNull Long guildId, @NonNull Long userId, @NonNull Instant upcomingBirthdayAt) {

}
