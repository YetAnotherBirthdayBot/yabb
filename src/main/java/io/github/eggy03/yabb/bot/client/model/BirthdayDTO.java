package io.github.eggy03.yabb.bot.client.model;


import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@RegisterForReflection
public class BirthdayDTO {

    private final @NonNull Long guildId;
    private final @NonNull Long userId;
    private final @NonNull Integer month;
    private final @NonNull Integer day;
    private final @NonNull String zoneId;
}
