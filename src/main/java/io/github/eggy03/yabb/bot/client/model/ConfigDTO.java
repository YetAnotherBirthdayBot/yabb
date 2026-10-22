package io.github.eggy03.yabb.bot.client.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;

@RegisterForReflection
public record ConfigDTO(@NonNull Long guildId, @NonNull Long channelId, @Nullable Long roleId) {

}