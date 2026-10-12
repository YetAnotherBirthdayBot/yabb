package io.github.eggy03.yabb.bot.service.command;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.github.eggy03.yabb.client.ApiException;
import io.github.eggy03.yabb.client.api.BirthdayApi;
import io.github.eggy03.yabb.client.models.BirthdayRequestDTO;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import java.time.Month;
import java.util.Objects;

@ApplicationScoped
@RequiredArgsConstructor
public class AdminCommandService {

    private final @NonNull BirthdayApi birthdayApi;
    private final @NonNull YabbEnvironment yabbEnvironment;

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
        BirthdayRequestDTO birthdayRequestDTO = new BirthdayRequestDTO();
        birthdayRequestDTO.setGuildId(guild.getIdLong());
        birthdayRequestDTO.setUserId(birthUserOption.getAsUser().getIdLong());
        birthdayRequestDTO.setMonth(birthMonthOption.getAsInt());
        birthdayRequestDTO.setDay(birthDayOption.getAsInt());
        birthdayRequestDTO.setZoneId(timeZoneOption.getAsString());

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Admin Save Birthday");

        try {
            birthdayApi.saveBirthday(birthdayRequestDTO);

            embedBuilder.setColor(yabbEnvironment.embedColor().successColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Save Success"),
                    MarkdownUtil.quoteBlock(
                            "User: %s%nBirth Day: %s%nBirth Month: %s%nTime Zone: %s"
                                    .formatted(
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
            embedBuilder.addField(
                    MarkdownUtil.underline("Save Failure"),
                    MarkdownUtil.codeblock(e.getResponseBody()),
                    false
            );

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

        try {
            birthdayApi.deleteBirthday(guild.getIdLong(), birthUserOption.getAsUser().getIdLong());

            embedBuilder.setColor(yabbEnvironment.embedColor().destructiveColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Deletion Success"),
                    MarkdownUtil.quoteBlock("%s's birthday has been deleted".formatted(birthUserOption.getAsUser().getEffectiveName())),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ApiException e) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Deletion Failure"),
                    MarkdownUtil.codeblock(e.getResponseBody()),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        }
    }
}
