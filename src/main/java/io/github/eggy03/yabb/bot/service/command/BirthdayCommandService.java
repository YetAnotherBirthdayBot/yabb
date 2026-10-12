package io.github.eggy03.yabb.bot.service.command;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.github.eggy03.yabb.bot.service.EmbedCheckingService;
import io.github.eggy03.yabb.client.YabbClient;
import io.github.eggy03.yabb.client.models.BirthdayRequestDTO;
import io.github.eggy03.yabb.client.models.BirthdayResponseDTO;
import io.github.eggy03.yabb.client.models.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
@RequiredArgsConstructor
public class BirthdayCommandService {

    private final @NonNull YabbClient yabbClient;
    private final @NonNull YabbEnvironment yabbEnvironment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    public void handleSave(@NonNull SlashCommandInteractionEvent event) {
        // null guild means the command was from a DM
        // ignore (cause it shouldn't happen)
        Guild guild = event.getGuild();
        if (guild == null) return;

        // defer reply
        event.deferReply().queue();

        // get the options
        OptionMapping birthMonthOption = Objects.requireNonNull(event.getOption("birth_month"), "birth_month cannot be null");
        OptionMapping birthDayOption = Objects.requireNonNull(event.getOption("birth_day"), "birth_month cannot be null");
        OptionMapping timeZoneOption = Objects.requireNonNull(event.getOption("time_zone"), "time_zone cannot be null");

        // build the dto
        BirthdayRequestDTO birthdayRequestDTO = new BirthdayRequestDTO();
        birthdayRequestDTO.setGuildId(guild.getIdLong());
        birthdayRequestDTO.setUserId(event.getUser().getIdLong());
        birthdayRequestDTO.setMonth(birthMonthOption.getAsInt());
        birthdayRequestDTO.setDay(birthDayOption.getAsInt());
        birthdayRequestDTO.setZoneId(timeZoneOption.getAsString());

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Save Birthday");

        try {
            yabbClient.api().v1().birthday().post(birthdayRequestDTO);

            embedBuilder.setColor(yabbEnvironment.embedColor().successColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Save Success"),
                    MarkdownUtil.quoteBlock(
                            "User: %s%nBirth Day: %s%nBirth Month: %s%nTime Zone: %s"
                                    .formatted(
                                            event.getUser().getEffectiveName(),
                                            birthdayRequestDTO.getDay(),
                                            birthdayRequestDTO.getMonth() == null ? "N/A" : Month.of(birthdayRequestDTO.getMonth()).name(),
                                            birthdayRequestDTO.getZoneId()
                                    )
                    ),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ErrorResponse e) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Save Failure"),
                    MarkdownUtil.quoteBlock(e.getMessageEscaped() == null ? "" : e.getMessageEscaped()),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        }
    }

    public void handleView(@NonNull SlashCommandInteractionEvent event) {
        // null guild means the command was from a DM
        // ignore (cause it shouldn't happen)
        Guild guild = event.getGuild();
        if (guild == null) return;

        // defer reply
        event.deferReply().queue();

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("View Birthday");

        try {
            BirthdayResponseDTO birthdayResponseDTO = Objects.requireNonNull(
                    yabbClient.api().v1().birthday()
                            .byGuildId(guild.getIdLong())
                            .byUserId(event.getUser().getIdLong())
                            .get(),
                    "birthdayResponseDTO should never be null cause the API throws 404 instead"
            );

            embedBuilder.setColor(yabbEnvironment.embedColor().successColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Birthday Info"),
                    MarkdownUtil.quoteBlock(
                            "User: %s%nBirth Day: %s%nBirth Month: %s%nTime Zone: %s%n Next Birthday: %s"
                                    .formatted(
                                            event.getUser().getEffectiveName(),
                                            birthdayResponseDTO.getDay(),
                                            birthdayResponseDTO.getMonth() == null ? "N/A" : Month.of(birthdayResponseDTO.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                                            birthdayResponseDTO.getZoneId(),
                                            birthdayResponseDTO.getNextBirthdayAt() == null ? "N/A" : TimeFormat.DATE_SHORT_TIME_SHORT.format(birthdayResponseDTO.getNextBirthdayAt().toInstant())
                                    )
                    ),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ErrorResponse _) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Not Found"),
                    MarkdownUtil.quoteBlock("Birthday not found for %s".formatted(event.getUser().getEffectiveName())),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        }
    }

    public void handleViewAll(@NonNull SlashCommandInteractionEvent event) {

        // null guild means the command was from a DM
        // ignore (cause it shouldn't happen)
        Guild guild = event.getGuild();
        if (guild == null) return;

        // defer reply
        event.deferReply().queue();

        // get the dto list
        List<BirthdayResponseDTO> birthdayResponseDTOList = yabbClient.api().v1().birthday().byGuildId(guild.getIdLong()).get();

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("View Everyone's Birthday");
        embedBuilder.setDescription("");
        embedBuilder.setColor(yabbEnvironment.embedColor().successColor());

        // case where birthdays are not found
        if (birthdayResponseDTOList == null || birthdayResponseDTOList.isEmpty()) {
            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());
            embedBuilder.addField("", MarkdownUtil.quoteBlock("No birthdays found for this guild"), false);
            event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
            return;
        }

        // group birthdays by month
        Map<Integer, List<BirthdayResponseDTO>> birthdayResponseDTOMap = birthdayResponseDTOList
                .stream()
                .filter(Objects::nonNull)
                .filter(dto -> dto.getMonth() != null)
                .collect(Collectors.groupingBy(BirthdayResponseDTO::getMonth));

        // show the same grouping in the embed
        // each field represents a month
        birthdayResponseDTOMap.forEach((month, birthdayList) ->
                embedBuilder.addField(
                        MarkdownUtil.underline(Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
                        birthdayList.stream()
                                .filter(dto -> dto.getUserId() != null)
                                .map(dto -> {
                                    User user = event.getJDA().getUserById(dto.getUserId());
                                    return "User: %s | Next Birthday: %s".formatted(
                                            user == null ? "N/A" : user.getEffectiveName() + " | " + user.getAsMention(),
                                            dto.getNextBirthdayAt() == null ? "N/A" : TimeFormat.DATE_SHORT_TIME_SHORT.format(dto.getNextBirthdayAt().toInstant())
                                    );
                                })
                                .collect(Collectors.joining("/n")),
                        false
                ));

        // check if the embed is too long
        embedCheckingService.checkAndSend(event, embedBuilder);

    }

    public void handleDelete(@NonNull SlashCommandInteractionEvent event) {

        // null guild means the command was from a DM
        // ignore (cause it shouldn't happen)
        Guild guild = event.getGuild();
        if (guild == null) return;

        // defer reply
        event.deferReply().queue();

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Delete Birthday");

        try {
            yabbClient.api().v1().birthday().byGuildId(guild.getIdLong()).byUserId(event.getUser().getIdLong()).delete();

            embedBuilder.setColor(yabbEnvironment.embedColor().destructiveColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Deletion Success"),
                    MarkdownUtil.quoteBlock("Your birthday has been deleted"),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ErrorResponse e) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Deletion Failure"),
                    MarkdownUtil.quoteBlock(e.getMessageEscaped() == null ? "" : e.getMessageEscaped()),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        }
    }
}
