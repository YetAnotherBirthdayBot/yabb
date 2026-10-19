package io.github.eggy03.yabb.bot.client.interfaces;

import io.github.eggy03.yabb.bot.client.exception.ApiException;
import io.github.eggy03.yabb.bot.client.model.ApiError;
import io.github.eggy03.yabb.bot.client.model.BirthdayScheduleDTO;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.List;

@Path("api/v1/birthday/schedule")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "birthday-schedule-api")
public interface BirthdayScheduleClient {

    @GET
    @Path("/{guildId}/{userId}")
    BirthdayScheduleDTO getSchedule(@PathParam("guildId") long guildId, @PathParam("userId") long userId);

    @GET
    @Path("/upcoming")
    List<BirthdayScheduleDTO> getUpcomingSchedules();

    @GET
    @Path("/active")
    List<BirthdayScheduleDTO> getActiveSchedules();

    @GET
    @Path("/expired")
    List<BirthdayScheduleDTO> getExpiredSchedules();

    @PATCH
    @Path("/advance/{guildId}/{userId}")
    void advanceSchedule(@PathParam("guildId") long guildId, @PathParam("userId") long userId);

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
