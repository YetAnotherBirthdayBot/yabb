package io.github.eggy03.yabb.bot.service.command;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.github.eggy03.yabb.client.ApiException;
import io.github.eggy03.yabb.client.api.GuildApi;
import io.github.eggy03.yabb.client.models.ErrorResponse;
import io.github.eggy03.yabb.client.models.GuildDTO;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import java.util.Objects;

@ApplicationScoped
public class ConfigCommandService {

    private final @NonNull GuildApi guildApi;
    private final @NonNull YabbEnvironment yabbEnvironment;

    public ConfigCommandService(@NonNull GuildApi guildApi, @NonNull YabbEnvironment yabbEnvironment) {
        this.guildApi = guildApi;
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
        OptionMapping channelOption = Objects.requireNonNull(event.getOption("announcement_channel"), "announcement_channel cannot be null");
        TextChannel announcementChannel = channelOption.getAsChannel().asTextChannel();

        OptionMapping roleOption = event.getOption("announcement_role");
        Role announcementRole = roleOption == null ? null : roleOption.getAsRole();

        // build the dto
        GuildDTO guildDTO = new GuildDTO();
        guildDTO.setGuildId(event.getGuild().getIdLong());
        guildDTO.setChannelId(announcementChannel.getIdLong());
        guildDTO.setRoleId(announcementRole == null ? null : announcementRole.getIdLong());

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Save Configuration");
        embedBuilder.setDescription("Command to save configuration for this guild");
        embedBuilder.setThumbnail(guild.getIconUrl());

        try {
            guildApi.saveGuild(guildDTO);

            embedBuilder.setColor(yabbEnvironment.embedColor().successColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Save Success"),
                    MarkdownUtil.quoteBlock("Announcement Channel: %s%nAnnouncement Role: %s".formatted(announcementChannel.getAsMention(), announcementRole == null ? "N/A" : announcementRole.getAsMention())),
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
        embedBuilder.setTitle("View Configuration");
        embedBuilder.setDescription("Command to view saved configuration for this guild");
        embedBuilder.setThumbnail(guild.getIconUrl());

        try {
            GuildDTO guildDTO = guildApi.getGuild(guild.getIdLong());

            TextChannel announcementChanel = guild.getTextChannelById(guildDTO.getChannelId());
            Role announcementRole = guildDTO.getRoleId() == null ? null : guild.getRoleById(guildDTO.getRoleId());

            String announcementChannelMention = announcementChanel == null ? "N/A" : announcementChanel.getAsMention();
            String announcementRoleMention = announcementRole == null ? "N/A" : announcementRole.getAsMention();

            embedBuilder.setColor(yabbEnvironment.embedColor().successColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Configuration Info"),
                    MarkdownUtil.quoteBlock("Announcement Channel: %s%nAnnouncement Role: %s".formatted(announcementChannelMention, announcementRoleMention)),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ApiException e) {

            ErrorResponse errorResponse = e.getErrorObject();
            String errorMessage = errorResponse == null ? null : e.getMessage();

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Could not show configuration"),
                    MarkdownUtil.codeblock(errorMessage == null ? e.getMessage() : errorMessage),
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

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Delete Configuration");
        embedBuilder.setDescription("Command to delete configuration for this guild");
        embedBuilder.setThumbnail(guild.getIconUrl());

        try {
            guildApi.deleteGuild(guild.getIdLong());

            embedBuilder.setColor(yabbEnvironment.embedColor().destructiveColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Deletion Success"),
                    MarkdownUtil.quoteBlock("Configuration Deleted Successfully"),
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
