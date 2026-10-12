package io.github.eggy03.yabb.bot.service.command;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.github.eggy03.yabb.bot.service.EmbedCheckingService;
import io.github.eggy03.yabb.client.ApiException;
import io.github.eggy03.yabb.client.api.BirthdayApi;
import io.github.eggy03.yabb.client.models.BirthdayRequestDTO;
import io.github.eggy03.yabb.client.models.BirthdayResponseDTO;
import io.github.eggy03.yabb.client.models.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class BirthdayCommandService {

    private final @NonNull BirthdayApi birthdayApi;
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
        embedBuilder.setDescription("Command to save your birthday");
        embedBuilder.setThumbnail(event.getUser().getEffectiveAvatarUrl());

        try {
            birthdayApi.saveBirthday(birthdayRequestDTO);

            embedBuilder.setColor(yabbEnvironment.embedColor().successColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Save Success"),
                    MarkdownUtil.quoteBlock(
                            "User: %s (%s)%nBirth Day: %s%nBirth Month: %s%nTime Zone: %s"
                                    .formatted(
                                            event.getUser().getAsMention(),
                                            event.getUser().getEffectiveName(),
                                            birthdayRequestDTO.getDay(),
                                            Month.of(birthdayRequestDTO.getMonth()).name(),
                                            birthdayRequestDTO.getZoneId()
                                    )
                    ),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ApiException e) {
            ErrorResponse errorResponse = e.getErrorObject();
            String errorMessage = errorResponse == null ? null : e.getMessage();

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Save Failure"),
                    MarkdownUtil.codeblock(errorMessage == null ? e.getMessage() : errorMessage),
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
        embedBuilder.setDescription("Command to view your birthday");
        embedBuilder.setThumbnail(event.getUser().getEffectiveAvatarUrl());

        try {
            BirthdayResponseDTO birthdayResponseDTO = birthdayApi.getBirthday(guild.getIdLong(), event.getUser().getIdLong());

            embedBuilder.setColor(yabbEnvironment.embedColor().successColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Birthday Info"),
                    MarkdownUtil.quoteBlock(
                            "User: %s (%s)%nBirth Day: %s%nBirth Month: %s%nTime Zone: %s%n Next Birthday: %s"
                                    .formatted(
                                            event.getUser().getAsMention(),
                                            event.getUser().getEffectiveName(),
                                            birthdayResponseDTO.getDay(),
                                            Month.of(birthdayResponseDTO.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                                            birthdayResponseDTO.getZoneId(),
                                            TimeFormat.DATE_SHORT_TIME_SHORT.format(birthdayResponseDTO.getNextBirthdayAt().toInstant())
                                    )
                    ),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ApiException _) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("View Failure"),
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

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("View Everyone's Birthday");
        embedBuilder.setDescription("Command to view everyone's birthday in this guild");
        embedBuilder.setColor(yabbEnvironment.embedColor().successColor());

        // get the dto list
        try {
            List<BirthdayResponseDTO> birthdayResponseDTOList = birthdayApi.getBirthdays(guild.getIdLong());

            // case where birthdays are not found
            if (birthdayResponseDTOList.isEmpty()) {
                embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());
                embedBuilder.addField(MarkdownUtil.quoteBlock("No birthdays found for this guild"),"", false);
                event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
            }

            // group birthdays by month
            Map<Integer, List<BirthdayResponseDTO>> birthdayResponseDTOMap = birthdayResponseDTOList
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(BirthdayResponseDTO::getMonth));

            // show the same grouping in the embed
            // each field represents a month
            birthdayResponseDTOMap.forEach((month, birthdayList) ->
                    embedBuilder.addField(
                            MarkdownUtil.underline(Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
                            birthdayList.stream()
                                    .map(dto -> {
                                        User user = event.getJDA().getUserById(dto.getUserId());
                                        return "User: %s | Next Birthday: %s".formatted(
                                                user == null ? "N/A" : user.getEffectiveName() + " | " + user.getAsMention(),
                                                TimeFormat.DATE_SHORT_TIME_SHORT.format(dto.getNextBirthdayAt().toInstant())
                                        );
                                    })
                                    .collect(Collectors.joining("/n")),
                            false
                    ));

            // check if the embed is too long
            embedCheckingService.checkAndSend(event, embedBuilder);

        } catch (ApiException e) {

            ErrorResponse errorResponse = e.getErrorObject();
            String errorMessage = errorResponse == null ? null : e.getMessage();

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());
            embedBuilder.addField("View Failure", MarkdownUtil.codeblock(errorMessage == null ? e.getMessage() : errorMessage), false);
            event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
        }

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
        embedBuilder.setDescription("Command to delete your birthday");
        embedBuilder.setThumbnail(event.getUser().getEffectiveAvatarUrl());

        try {
            birthdayApi.deleteBirthday(guild.getIdLong(), event.getUser().getIdLong());

            embedBuilder.setColor(yabbEnvironment.embedColor().destructiveColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Deletion Success"),
                    MarkdownUtil.quoteBlock("Your birthday has been deleted"),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ApiException e) {

            ErrorResponse errorResponse = e.getErrorObject();
            String errorMessage = errorResponse == null ? null : e.getMessage();

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Deletion Failure"),
                    MarkdownUtil.codeblock(errorMessage == null ? e.getMessage() : errorMessage),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        }
    }
}
