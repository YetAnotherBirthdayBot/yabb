package io.github.eggy03.yabb.bot.client.model;


import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

@RequiredArgsConstructor
@Getter
@RegisterForReflection
public class ConfigDTO {

    private final @NonNull Long guildId;
    private final @NonNull Long channelId;
    private final @Nullable Long roleId;
}