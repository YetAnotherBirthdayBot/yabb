package io.github.eggy03.yabb.bot.client.interfaces;

import io.github.eggy03.yabb.bot.client.exception.ApiException;
import io.github.eggy03.yabb.bot.client.model.ApiError;
import io.github.eggy03.yabb.bot.client.model.ConfigDTO;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jspecify.annotations.NonNull;

import java.time.Instant;

@Path("api/v1/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "config-api")
public interface ConfigClient {

    @POST
    void saveConfig(ConfigDTO configDTO);

    @GET
    @Path("/{guildId}")
    ConfigDTO getConfig(@PathParam("guildId") long guildId);

    @PATCH
    void updateConfig(ConfigDTO updatedConfigDTO);

    @DELETE
    @Path("/{guildId}")
    void deleteConfig(@PathParam("guildId") long guildId);

    @ClientExceptionMapper
    @Blocking
    static @NonNull ApiException toException(Response response) {

        if (response == null)
            return new ApiException(999, new ApiError(999, "NullResponse", "jakarta.ws.rs.core.Response was null", Instant.now(), "N/A"));

        ApiError apiError = response.hasEntity()
                ? response.readEntity(ApiError.class)
                : new ApiError(response.getStatus(), "ApiErrorCaptureFailure", "API Error could not be deserialized", Instant.now(), "N/A");

        return new ApiException(response.getStatus(), apiError);
    }
}
