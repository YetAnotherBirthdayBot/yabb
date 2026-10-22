package io.github.eggy03.yabb.bot.service.command;

import io.github.eggy03.yabb.bot.client.exception.ApiException;
import io.github.eggy03.yabb.bot.client.interfaces.BirthdayClient;
import io.github.eggy03.yabb.bot.client.model.ApiError;
import io.github.eggy03.yabb.bot.client.model.BirthdayDTO;
import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@ApplicationScoped
public class AdminCommandService {

    private final @NonNull BirthdayClient birthdayClient;
    private final @NonNull YabbEnvironment yabbEnvironment;

    @Inject
    public AdminCommandService(@NonNull @RestClient BirthdayClient birthdayClient, @NonNull YabbEnvironment yabbEnvironment) {
        this.birthdayClient = birthdayClient;
        this.yabbEnvironment = yabbEnvironment;
    }

    public void handleSave(@NonNull SlashCommandInteractionEvent event) {

        // null guild means the command was from a DM
        // ignore (cause it shouldn't happen)
        Guild guild = event.getGuild();
        if (guild == null) return;

        // defer reply
        event.deferReply().queue();

        // get the options
        OptionMapping birthUserOption = Objects.requireNonNull(event.getOption("birth_user"), "birth_user cannot be null");
        OptionMapping birthMonthOption = Objects.requireNonNull(event.getOption("birth_month"), "birth_month cannot be null");
        OptionMapping birthDayOption = Objects.requireNonNull(event.getOption("birth_day"), "birth_month cannot be null");
        OptionMapping timeZoneOption = Objects.requireNonNull(event.getOption("time_zone"), "time_zone cannot be null");

        // build the dto
        BirthdayDTO birthdayDTO = new BirthdayDTO(
                guild.getIdLong(),
                birthUserOption.getAsUser().getIdLong(),
                birthMonthOption.getAsInt(),
                birthDayOption.getAsInt(),
                timeZoneOption.getAsString()
        );

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Admin Save Birthday");
        embedBuilder.setDescription("This action is constructive");
        embedBuilder.setThumbnail(birthUserOption.getAsUser().getEffectiveAvatarUrl());

        try {
            birthdayClient.saveBirthday(birthdayDTO);

            embedBuilder.setColor(yabbEnvironment.embedColor().successColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Save Success"),
                    MarkdownUtil.quoteBlock(
                            "User: %s (%s)%nBirth Day: %s%nBirth Month: %s%nTime Zone: %s"
                                    .formatted(
                                            birthUserOption.getAsUser().getAsMention(),
                                            birthUserOption.getAsUser().getEffectiveName(),
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
                case 409 -> embedBuilder.addField("Birthday Already Exists", MarkdownUtil.codeblock("Birthday for user (%s) already exists".formatted(birthUserOption.getAsUser().getEffectiveName())), false);
                default -> {
                    embedBuilder.addField("Internal Server Error", MarkdownUtil.codeblock("Something went wrong. Please try again later"), false);
                    log.warn("API Error during admin save birthday", e);
                }
            }

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        }
    }

    public void handleDelete(@NonNull SlashCommandInteractionEvent event) {

        // null guild means the command was from a DM
        // ignore (cause it shouldn't happen)
        Guild guild = event.getGuild();
        if (guild == null) return;

        // defer reply
        event.deferReply().queue();

        // get the options
        OptionMapping birthUserOption = Objects.requireNonNull(event.getOption("birth_user"), "birth_user cannot be null");

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Admin Delete Birthday");
        embedBuilder.setDescription("This action is destructive");
        embedBuilder.setThumbnail(birthUserOption.getAsUser().getEffectiveAvatarUrl());

        try {
            birthdayClient.deleteBirthday(guild.getIdLong(), birthUserOption.getAsUser().getIdLong());

            embedBuilder.setColor(yabbEnvironment.embedColor().destructiveColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Deletion Success"),
                    MarkdownUtil.quoteBlock("%s (%s)'s birthday has been deleted".formatted(birthUserOption.getAsUser().getAsMention(), birthUserOption.getAsUser().getEffectiveName())),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ApiException e) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());

            ApiError apiError = e.getError();
            switch (apiError.status()) {
                case 400 -> embedBuilder.addField("Invalid Parameters", MarkdownUtil.codeblock(apiError.message()), false);
                case 404 -> embedBuilder.addField("Birthday Does Not Exist", MarkdownUtil.codeblock("Birthday not found for (%s)".formatted(birthUserOption.getAsUser().getEffectiveName())), false);
                default -> {
                    embedBuilder.addField("Internal Server Error", MarkdownUtil.codeblock("Something went wrong. Please try again later"), false);
                    log.warn("API Error during admin delete birthday", e);
                }
            }

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        }
    }
}
