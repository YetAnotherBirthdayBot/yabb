package io.github.eggy03.yabb.bot.scheduler;

import io.github.eggy03.yabb.bot.annotations.VirtualThreadFactory;
import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.github.eggy03.yabb.client.YabbClient;
import io.github.eggy03.yabb.client.models.BirthdayRequestDTO;
import io.github.eggy03.yabb.client.models.BirthdayResponseDTO;
import io.github.eggy03.yabb.client.models.ErrorResponse;
import io.github.eggy03.yabb.client.models.GuildDTO;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class BirthdayNotificationScheduler {

    private final @NonNull YabbClient yabbClient;
    private final @NonNull YabbEnvironment yabbEnvironment;
    private final @NonNull ThreadFactory virtualThreadFactory;
    private final @NonNull JDA jda;

    @Inject
    public BirthdayNotificationScheduler(@NonNull YabbClient yabbClient, @NonNull YabbEnvironment yabbEnvironment, @NonNull @VirtualThreadFactory ThreadFactory virtualThreadFactory, @NonNull JDA jda) {
        this.yabbClient = yabbClient;
        this.yabbEnvironment = yabbEnvironment;
        this.virtualThreadFactory = virtualThreadFactory;
        this.jda = jda;
    }

    @Scheduled(every = "30s")
    public void checkPendingBirthdays() {

        virtualThreadFactory.newThread(()-> {
        try {
            orchestrate();
        } catch (ErrorResponse e) {
            log.warn("API Warning: {}\n Path: {}", e.getMessageEscaped(), e.getPath());
        }
        }).start();
    }

    private void orchestrate() {
        // Get the guild IDs cached by this JDA instance.
        Set<Long> guildCacheSet = jda.getGuildCache()
                .stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toSet());

        // get all the pending birthdays
        // and filter the ones which have guild IDs not present in this JDA
        List<BirthdayResponseDTO> pendingBirthdayList = Objects
                .requireNonNull(yabbClient.api().v1().birthday().pending().get(), "The API returned a null list")
                .stream()
                .filter(Objects::nonNull)
                .filter(dto -> dto.getGuildId() != null && guildCacheSet.contains(dto.getGuildId()))
                .toList();

        // for each pending birthday
        pendingBirthdayList.forEach(birthday -> {
            // get the registered guild
            GuildDTO guildDTO = Objects.requireNonNull(yabbClient.api()
                            .v1()
                            .guild()
                            .byGuildId(Objects.requireNonNull(birthday.getGuildId(), "guildId was null"))
                            .get(),
                    "guildDTO was null even though the API doesn't send nulls"
            );

            // send an announcement to the registered channel
            Role announcementRole = jda.getRoleById(Objects.requireNonNull(guildDTO.getRoleId(), "roleId was null"));
            Objects.requireNonNull(jda.getTextChannelById(Objects.requireNonNull(guildDTO.getChannelId(), "channelId was null")), "No text channel was found by JDA")
                    .sendMessage("%s, let's celebrate!".formatted(announcementRole == null ? "Everyone" : announcementRole.getAsMention()))
                    .addEmbeds(createEmbed(Objects.requireNonNull(birthday.getUserId(), "userId was null")))
                    .complete(); // don't use async cause this method is wrapped by virtual threads and yabb is sync

            // on success, update the birthday for next year
            BirthdayRequestDTO birthdayRequestDTO = new BirthdayRequestDTO();
            birthdayRequestDTO.setGuildId(birthday.getGuildId());
            birthdayRequestDTO.setUserId(birthday.getUserId());
            birthdayRequestDTO.setDay(birthday.getDay());
            birthdayRequestDTO.setMonth(birthday.getMonth());
            birthdayRequestDTO.setZoneId(birthday.getZoneId());

            yabbClient.api().v1().birthday().updateForNextYear().patch(birthdayRequestDTO);
        });
    }

    private @NonNull MessageEmbed createEmbed(long userId) {

        User user = jda.getUserById(userId);
        String userName = user == null ? null : user.getEffectiveName();
        String userMention = user == null ? null : user.getAsMention();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Happy Birthday!");
        eb.setDescription("Birthday Event");
        eb.setColor(yabbEnvironment.embedColor().successColor());

        eb.addField(MarkdownUtil.quote("Please Wish"), MarkdownUtil.quoteBlock("%s (%s), a very happy birthday").formatted(userMention, userName), false);

        return eb.build();
    }
}
