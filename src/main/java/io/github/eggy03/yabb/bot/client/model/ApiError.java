package io.github.eggy03.yabb.bot.client.model;


import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
@Getter
@RegisterForReflection
public class ApiError {

    private final @NonNull Integer status;
    private final @NonNull String error;
    private final @NonNull String message;
    private final @NonNull Instant timeStamp;
    private final @NonNull String path;

    @Override
    public String toString() {
        return "ApiError{" +
                "status=" + status +
                ", error='" + error + '\'' +
                ", message='" + message + '\'' +
                ", timeStamp=" + timeStamp +
                ", path='" + path + '\'' +
                '}';
    }
}
