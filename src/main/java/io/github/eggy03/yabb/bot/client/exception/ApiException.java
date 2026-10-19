package io.github.eggy03.yabb.bot.client.exception;

import io.github.eggy03.yabb.bot.client.model.ApiError;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class ApiException extends RuntimeException {

    private final @NonNull Integer statusCode;
    @NonNull
    private final transient ApiError apiError;

    public ApiException(@NonNull Integer statusCode, @NonNull ApiError apiError) {
        super("API returned HTTP " + statusCode);
        this.statusCode = Objects.requireNonNull(statusCode, "statusCode cannot be null");
        this.apiError = Objects.requireNonNull(apiError, "apiError cannot be null");
    }

    public @NonNull Integer getStatusCode() {
        return statusCode;
    }

    public @NonNull ApiError getError() {
        return apiError;
    }

    @Override
    public String toString() {
        return "ApiException{" +
                "statusCode=" + statusCode +
                ", apiError=" + apiError +
                "} " + super.toString();
    }
}
