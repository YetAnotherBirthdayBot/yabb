package io.github.eggy03.yabb.bot.service.command;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.github.eggy03.yabb.bot.service.EmbedCheckingService;
import io.github.eggy03.yabb.bot.utils.ErrorResponseDeserializer;
import io.github.eggy03.yabb.client.ApiException;
import io.github.eggy03.yabb.client.api.BirthdayApi;
import io.github.eggy03.yabb.client.api.BirthdayScheduleApi;
import io.github.eggy03.yabb.client.models.BirthdayDTO;
import io.github.eggy03.yabb.client.models.BirthdayScheduleDTO;
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
    private final @NonNull BirthdayScheduleApi birthdayScheduleApi;
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
        BirthdayDTO birthdayDTO = new BirthdayDTO();
        birthdayDTO.setGuildId(guild.getIdLong());
        birthdayDTO.setUserId(event.getUser().getIdLong());
        birthdayDTO.setMonth(birthMonthOption.getAsInt());
        birthdayDTO.setDay(birthDayOption.getAsInt());
        birthdayDTO.setZoneId(timeZoneOption.getAsString());

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Save Birthday");
        embedBuilder.setDescription("This action is constructive");
        embedBuilder.setThumbnail(event.getUser().getEffectiveAvatarUrl());

        try {
            birthdayApi.saveBirthday(birthdayDTO);

            embedBuilder.setColor(yabbEnvironment.embedColor().successColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Save Success"),
                    MarkdownUtil.quoteBlock(
                            "User: %s (%s)%nBirth Day: %s%nBirth Month: %s%nTime Zone: %s"
                                    .formatted(
                                            event.getUser().getAsMention(),
                                            event.getUser().getEffectiveName(),
                                            birthdayDTO.getDay(),
                                            Month.of(birthdayDTO.getMonth()).name(),
                                            birthdayDTO.getZoneId()
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

        // get the options
        OptionMapping birthUserOption = event.getOption("birth_user");
        User birthUser = birthUserOption == null ? event.getUser() : birthUserOption.getAsUser();

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("View Birthday");
        embedBuilder.setDescription("This action is read-only");
        embedBuilder.setThumbnail(birthUser.getEffectiveAvatarUrl());

        try {
            BirthdayDTO birthdayDTO = birthdayApi.getBirthday(guild.getIdLong(), birthUser.getIdLong());
            BirthdayScheduleDTO birthdayScheduleDTO = birthdayScheduleApi.getSchedule(guild.getIdLong(), birthUser.getIdLong());

            embedBuilder.setColor(yabbEnvironment.embedColor().successColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Birthday Info"),
                    MarkdownUtil.quoteBlock(
                            "User: %s (%s)%nBirth Day: %s%nBirth Month: %s%nTime Zone: %s%n Next Birthday: %s"
                                    .formatted(
                                            birthUser.getAsMention(),
                                            birthUser.getEffectiveName(),
                                            birthdayDTO.getDay(),
                                            Month.of(birthdayDTO.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                                            birthdayDTO.getZoneId(),
                                            TimeFormat.DATE_SHORT_TIME_SHORT.format(birthdayScheduleDTO.getUpcomingBirthdayAt())
                                    )
                    ),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ApiException e) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());

            if (e.getCode() == 404) {
                embedBuilder.addField("Birthday Does Not Exist", MarkdownUtil.codeblock("Birthday not found for (%s)".formatted(birthUser.getEffectiveName())), false);
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
        embedBuilder.setTitle("View Birthdays");
        embedBuilder.setDescription("This action is read-only");
        embedBuilder.setColor(yabbEnvironment.embedColor().successColor());

        // get the dto list
        try {
            List<BirthdayDTO> birthdayDTOList = birthdayApi.getBirthdays(guild.getIdLong());

            // case where birthdays are not found
            if (birthdayDTOList.isEmpty()) {
                embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());
                embedBuilder.addField("Uh Oh!","No birthdays found for this guild", false);
                event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
                return;
            }

            // group birthdays by month
            Map<Integer, List<BirthdayDTO>> birthdayDTOMap = birthdayDTOList
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(BirthdayDTO::getMonth));

            // show the same grouping in the embed
            // each field represents a month
            birthdayDTOMap.forEach((month, birthdayList) ->
                    embedBuilder.addField(
                            MarkdownUtil.underline(Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
                            birthdayList.stream()
                                    .map(dto -> {
                                        User user = event.getJDA().getUserById(dto.getUserId());
                                        String userName = user == null ? "N/A" : user.getEffectiveName();
                                        String userMention = user == null ? "N/A" : user.getAsMention();

                                        return MarkdownUtil.quote("[%s]-[%s]-[%s]".formatted(userMention, userName, dto.getDay()));
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
        embedBuilder.setDescription("This action is destructive");
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
