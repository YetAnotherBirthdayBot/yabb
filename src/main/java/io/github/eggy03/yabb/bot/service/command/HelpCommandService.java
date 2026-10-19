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
                MarkdownUtil.quoteBlock("""
                        Configuration allows the bot to send birthday notifications to your guild.
                        - Use `/config save` to save your server configuration.
                        - Use `/config view` to view your server configuration.
                        - Use `/config delete` to delete your server configuration.
                        """),
                false
        );

        eb.addField(
                MarkdownUtil.underline("2. Birthday Commands"),
                MarkdownUtil.quoteBlock("""
                        - Use `/birthday save` to save your birthday.
                        - Use `/birthday view` to view your or someone else's birthday.
                        - Use `/birthday viewall` to view everyone's birthday in a guild.
                        - Use `/birthday delete` to delete your birthday.
                        """),
                false
        );

        eb.addField(
                MarkdownUtil.underline("3. Admin Commands"),
                MarkdownUtil.quoteBlock("""
                        - Use `/admin savebirthday` to save someone's birthday.
                        - Use `/admin deletebirthday` to delete someone's birthday.
                        """),
                false
        );

        eb.addField(
                MarkdownUtil.underline("4. Timezones"),
                MarkdownUtil.quoteBlock("""
                        `/birthday save` and `/admin savebirthday` accept time zone in one of the following patterns:
                        - Region Based : `America/Montreal`, `Australia/Perth`, `Europe/Paris`
                        - GMT Offset   : `GMT+0530`, `GMT+0400`
                        - UTC Offset   : `UTC+0530`, `UTC+0400`
                        - UT  Offset   : `UT+0530`,  `UT+0400`
                        
                        Refer to the [IANA TimeZone Database](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones) to find your zone IDs and offsets.
                        """),
                false
        );

        eb.addField(
                MarkdownUtil.underline("5. Permissions"),
                MarkdownUtil.quoteBlock("""
                        - `/config` requires `MANAGE_SERVER` and above.
                        - `/birthday` requires no special permissions.
                        - `/admin` requires `ADMINISTRATOR` and above.
                        """),
                false
        );

        eb.setFooter("Thank you for using YABB " + yabbEnvironment.general().appVersion());
        eb.setTimestamp(Instant.now());

        MessageEmbed mb = eb.build();
        event.getHook().editOriginalEmbeds(mb).queue();
    }
}
