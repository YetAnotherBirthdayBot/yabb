package io.github.eggy03.yabb.bot.utils;

import io.github.eggy03.yabb.client.ApiException;
import io.github.eggy03.yabb.client.models.ErrorResponse;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

@Slf4j
@UtilityClass
public class ErrorResponseDeserializer {

    public @NonNull ErrorResponse fromApiException(@Nullable ApiException apiException) {

        if (apiException == null)
            return new ErrorResponse();

        try {
            return ErrorResponse.fromJson(apiException.getResponseBody());
        } catch (IOException e) {
            log.debug("Failed to deserialize ErrorResponse from JSON String", e);
            return new ErrorResponse();
        }
    }
}
