package io.github.eggy03.yabb.bot.client.model;


import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.NonNull;

import java.time.Instant;

@RegisterForReflection
public record ApiError(
        @NonNull Integer status,
        @NonNull String error,
        @NonNull String message,
        @NonNull Instant timeStamp,
        @NonNull String path
) {
    @Override
    public @NonNull String toString() {
        return "ApiError{" +
                "status=" + status +
                ", error='" + error + '\'' +
                ", message='" + message + '\'' +
                ", timeStamp=" + timeStamp +
                ", path='" + path + '\'' +
                '}';
    }
}