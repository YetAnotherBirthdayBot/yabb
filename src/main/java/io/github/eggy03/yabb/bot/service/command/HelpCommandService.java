package io.github.eggy03.yabb.bot.service.command;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import java.time.Instant;

@ApplicationScoped
@RequiredArgsConstructor
public class HelpCommandService {

    private final @NonNull YabbEnvironment yabbEnvironment;

    public void handleCommand(@NonNull SlashCommandInteractionEvent event) {

        event.deferReply().queue();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Setup Guide for YABB");
        eb.setColor(yabbEnvironment.embedColor().successColor());

        eb.addField(
                MarkdownUtil.underline("1. Config Commands"),
                """
                        Use `/config save [announcement_channel] [announcement_role]` to save your server configuration (
                        announcement_role is optional).
                        
                        Use `/config view` to view your server configuration.
                        Use `/config delete` to delete your server configuration.
                        """,
                false
        );

        eb.addField(
                MarkdownUtil.underline("2. Birthday Commands"),
                """
                        Use `/birthday save [birth_day] [birth_month] [time_zone]` to save your birthday.
                        
                        `time_zone` accepts a wide variety of formats. The following patterns are supported:
                        
                        Region Based: `America/Montreal`, `Australia/Perth`, `Europe/Paris`
                        GMT Offset: `GMT+0530`, `GMT+0400`
                        UTC Offset: `UTC+0530`, `UTC+0400`
                        UT  Offset:  `UT+0530`,  `UT+0400`
                        
                        Use `/birthday view [birth_user]` to view your or someone else's birthday.
                        Leave `birth_user` blank to view your own birthday.
                        
                        Use `/birthday viewall` to view everyone's birthday in a guild.
                        Use `/birthday delete` to delete your birthday.
                        """,
                false
        );

        eb.addField(
                MarkdownUtil.underline("3. Admin Commands"),
                """
                        Use `/admin savebirthday [birth_user] [birth_day] [birth_month] [time_zone]` to save someone's birthday.

                        Use `/admin deletebirthday [birth_user]` to delete someone's birthday.
                        """,
                false
        );


        eb.addBlankField(false);
        eb.addField(
                MarkdownUtil.underline("Permissions"),
                """
                        `/config` requires `MANAGE_SERVER` and above.
                        `/birthday` requires no special permissions.
                        `/admin` requires `ADMINISTRATOR` and above.
                        """,
                false
        );

        eb.setFooter("Thank you for using YABB " + yabbEnvironment.general().appVersion());
        eb.setTimestamp(Instant.now());

        MessageEmbed mb = eb.build();
        event.getHook().editOriginalEmbeds(mb).queue();
    }
}
