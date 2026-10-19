package io.github.eggy03.yabb.bot.client.interfaces;

import io.github.eggy03.yabb.bot.client.exception.ApiException;
import io.github.eggy03.yabb.bot.client.model.ApiError;
import io.github.eggy03.yabb.bot.client.model.BirthdayDTO;
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
import java.util.List;

@Path("api/v1/birthday")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "birthday-api")
public interface BirthdayClient {

    @POST
    void saveBirthday(BirthdayDTO birthdayDTO);

    @GET
    @Path("/{guildId}/{userId}")
    BirthdayDTO getBirthday(@PathParam("guildId") long guildId, @PathParam("userId") long userId);

    @GET
    @Path("/{guildId}")
    List<BirthdayDTO> getBirthdays(@PathParam("guildId") long guildId);

    @PATCH
    void updateBirthday(BirthdayDTO updatedBirthdayDTO);

    @DELETE
    @Path("/{guildId}")
    void deleteBirthdayByGuild(@PathParam("guildId") long guildId);

    @DELETE
    @Path("/{guildId}/{userId}")
    void deleteBirthday(@PathParam("guildId") long guildId, @PathParam("userId") long userId);

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
