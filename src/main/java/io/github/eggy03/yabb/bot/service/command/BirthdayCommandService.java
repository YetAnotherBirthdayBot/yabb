package io.github.eggy03.yabb.bot.service.command;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.github.eggy03.yabb.bot.service.EmbedCheckingService;
import io.github.eggy03.yabb.bot.utils.ErrorResponseDeserializer;
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
        embedBuilder.setDescription("Saving your birthday in this guild");
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

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());

            ErrorResponse errorResponse = ErrorResponseDeserializer.fromApiException(e);
            switch (e.getCode()) {
                case 400 -> embedBuilder.addField("Invalid Parameters", MarkdownUtil.codeblock(errorResponse.getMessage()), false);
                case 409 -> embedBuilder.addField("Birthday Already Exists", MarkdownUtil.codeblock("Birthday for (%s) already exists".formatted(event.getUser().getEffectiveName())), false);
                default -> {
                    embedBuilder.addField("Internal Server Error", MarkdownUtil.codeblock("Something went wrong. Please try again later"), false);
                    log.warn("API Error during user save birthday", e);
                }
            }

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
        embedBuilder.setDescription("Viewing your birthday in this guild");
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
                                            TimeFormat.DATE_SHORT_TIME_SHORT.format(birthdayResponseDTO.getUpcomingBirthday())
                                    )
                    ),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ApiException e) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());

            if (e.getCode() == 404) {
                embedBuilder.addField("Birthday Does Not Exist", MarkdownUtil.codeblock("Birthday not found for (%s)".formatted(event.getUser().getEffectiveName())), false);
            } else {
                embedBuilder.addField("Internal Server Error", MarkdownUtil.codeblock("Something went wrong. Please try again later"), false);
                log.warn("API Error during user view birthday", e);
            }

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
        embedBuilder.setDescription("Viewing everyone's birthday in this guild");
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
                                        String userName = user == null ? "N/A" : user.getEffectiveName();
                                        String userMention = user == null ? "N/A" : user.getAsMention();

                                        return "User: %s (%s) | Next Birthday: %s".formatted(
                                                userMention, userName,
                                                TimeFormat.DATE_SHORT_TIME_SHORT.format(dto.getUpcomingBirthday())
                                        );
                                    })
                                    .collect(Collectors.joining(System.lineSeparator())),
                            false
                    ));

            // check if the embed is too long
            embedCheckingService.checkAndSend(event, embedBuilder);

        } catch (ApiException e) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());

            ErrorResponse errorResponse = ErrorResponseDeserializer.fromApiException(e);
            if (e.getCode() == 400) {
                embedBuilder.addField("Invalid Parameters", MarkdownUtil.codeblock(errorResponse.getMessage()), false);
            } else {
                embedBuilder.addField("Internal Server Error", MarkdownUtil.codeblock("Something went wrong. Please try again later"), false);
                log.warn("API Error during user view all birthdays", e);
            }

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
        embedBuilder.setDescription("Deleting your birthday from this guild");
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

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());

            ErrorResponse errorResponse = ErrorResponseDeserializer.fromApiException(e);
            switch (e.getCode()) {
                case 400 -> embedBuilder.addField("Invalid Parameters", MarkdownUtil.codeblock(errorResponse.getMessage()), false);
                case 404 -> embedBuilder.addField("Birthday Does Not Exist", MarkdownUtil.codeblock("Birthday not found for (%s)".formatted(event.getUser().getEffectiveName())), false);
                default -> {
                    embedBuilder.addField("Internal Server Error", MarkdownUtil.codeblock("Something went wrong. Please try again later"), false);
                    log.warn("API Error during user delete birthday", e);
                }
            }

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        }
    }
}
