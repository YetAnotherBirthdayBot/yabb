package io.github.eggy03.yabb.bot.client.model;


import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.NonNull;

@RegisterForReflection
public record BirthdayDTO(
        @NonNull Long guildId,
        @NonNull Long userId,
        @NonNull Integer month,
        @NonNull Integer day,
        @NonNull String zoneId
) {}
