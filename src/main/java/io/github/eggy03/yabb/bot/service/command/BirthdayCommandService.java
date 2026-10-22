package io.github.eggy03.yabb.bot.service.command;

import io.github.eggy03.yabb.bot.client.exception.ApiException;
import io.github.eggy03.yabb.bot.client.interfaces.BirthdayClient;
import io.github.eggy03.yabb.bot.client.interfaces.BirthdayScheduleClient;
import io.github.eggy03.yabb.bot.client.model.ApiError;
import io.github.eggy03.yabb.bot.client.model.BirthdayDTO;
import io.github.eggy03.yabb.bot.client.model.BirthdayScheduleDTO;
import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.github.eggy03.yabb.bot.service.EmbedCheckingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class BirthdayCommandService {

    private final @NonNull BirthdayClient birthdayClient;
    private final @NonNull BirthdayScheduleClient birthdayScheduleClient;
    private final @NonNull YabbEnvironment yabbEnvironment;
    private final @NonNull EmbedCheckingService embedCheckingService;

    @Inject
    public BirthdayCommandService(@NonNull @RestClient BirthdayClient birthdayClient, @NonNull @RestClient BirthdayScheduleClient birthdayScheduleClient, @NonNull YabbEnvironment yabbEnvironment, @NonNull EmbedCheckingService embedCheckingService) {
        this.birthdayClient = birthdayClient;
        this.birthdayScheduleClient = birthdayScheduleClient;
        this.yabbEnvironment = yabbEnvironment;
        this.embedCheckingService = embedCheckingService;
    }

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
        BirthdayDTO birthdayDTO = new BirthdayDTO(
                guild.getIdLong(),
                event.getUser().getIdLong(),
                birthMonthOption.getAsInt(),
                birthDayOption.getAsInt(),
                timeZoneOption.getAsString()
        );

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Save Birthday");
        embedBuilder.setDescription("This action is constructive");
        embedBuilder.setThumbnail(event.getUser().getEffectiveAvatarUrl());

        try {
            birthdayClient.saveBirthday(birthdayDTO);

            embedBuilder.setColor(yabbEnvironment.embedColor().successColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Save Success"),
                    MarkdownUtil.quoteBlock(
                            "User: %s (%s)%nBirth Day: %s%nBirth Month: %s%nTime Zone: %s"
                                    .formatted(
                                            event.getUser().getAsMention(),
                                            event.getUser().getEffectiveName(),
                                            birthdayDTO.day(),
                                            Month.of(birthdayDTO.month()).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                                            birthdayDTO.zoneId()
                                    )
                    ),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ApiException e) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());

            ApiError apiError = e.getError();
            switch (apiError.status()) {
                case 400 -> embedBuilder.addField("Invalid Parameters", MarkdownUtil.codeblock(apiError.message()), false);
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
            BirthdayDTO birthdayDTO = birthdayClient.getBirthday(guild.getIdLong(), birthUser.getIdLong());
            BirthdayScheduleDTO birthdayScheduleDTO = birthdayScheduleClient.getSchedule(guild.getIdLong(), birthUser.getIdLong());

            embedBuilder.setColor(yabbEnvironment.embedColor().successColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Birthday Info"),
                    MarkdownUtil.quoteBlock(
                            "User: %s (%s)%nBirth Day: %s%nBirth Month: %s%nTime Zone: %s%n Next Birthday: %s"
                                    .formatted(
                                            birthUser.getAsMention(),
                                            birthUser.getEffectiveName(),
                                            birthdayDTO.day(),
                                            Month.of(birthdayDTO.month()).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                                            birthdayDTO.zoneId(),
                                            TimeFormat.DATE_SHORT_TIME_SHORT.format(birthdayScheduleDTO.upcomingBirthdayAt())
                                    )
                    ),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ApiException e) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());

            if (e.getStatusCode() == 404) {
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
            List<BirthdayDTO> birthdayDTOList = birthdayClient.getBirthdays(guild.getIdLong());

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
                    .collect(Collectors.groupingBy(BirthdayDTO::month));

            // show the same grouping in the embed
            // each field represents a month
            birthdayDTOMap.forEach((month, birthdayList) ->
                    embedBuilder.addField(
                            MarkdownUtil.underline(Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH)),
                            birthdayList.stream()
                                    .map(dto -> {
                                        User user = event.getJDA().getUserById(dto.userId());
                                        String userName = user == null ? "N/A" : user.getEffectiveName();
                                        String userMention = user == null ? String.valueOf(dto.userId()) : user.getAsMention();

                                        return MarkdownUtil.quote("[%s]-[%s]-[%s]".formatted(userMention, userName, dto.day()));
                                    })
                                    .collect(Collectors.joining(System.lineSeparator())),
                            false
                    ));

            // check if the embed is too long
            embedCheckingService.checkAndSend(event, embedBuilder);

        } catch (ApiException e) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());

            ApiError apiError = e.getError();
            if (apiError.status() == 400) {
                embedBuilder.addField("Invalid Parameters", MarkdownUtil.codeblock(apiError.message()), false);
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
            birthdayClient.deleteBirthday(guild.getIdLong(), event.getUser().getIdLong());

            embedBuilder.setColor(yabbEnvironment.embedColor().destructiveColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Deletion Success"),
                    MarkdownUtil.quoteBlock("Your birthday has been deleted"),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ApiException e) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());

            ApiError apiError = e.getError();
            switch (apiError.status()) {
                case 400 -> embedBuilder.addField("Invalid Parameters", MarkdownUtil.codeblock(apiError.message()), false);
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
