package io.github.eggy03.yabb.bot.service.command;

import io.github.eggy03.yabb.bot.client.exception.ApiException;
import io.github.eggy03.yabb.bot.client.interfaces.ConfigClient;
import io.github.eggy03.yabb.bot.client.model.ApiError;
import io.github.eggy03.yabb.bot.client.model.ConfigDTO;
import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Objects;

@Slf4j
@ApplicationScoped
public class ConfigCommandService {

    private final @NonNull ConfigClient configClient;
    private final @NonNull YabbEnvironment yabbEnvironment;

    @Inject
    public ConfigCommandService(@NonNull @RestClient ConfigClient configClient, @NonNull YabbEnvironment yabbEnvironment) {
        this.configClient = configClient;
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
        ConfigDTO configDTO = new ConfigDTO(
                event.getGuild().getIdLong(),
                announcementChannel.getIdLong(),
                announcementRole == null ? null : announcementRole.getIdLong()
        );

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Save Configuration");
        embedBuilder.setDescription("This action is constructive");
        embedBuilder.setThumbnail(guild.getIconUrl());

        try {
            configClient.saveConfig(configDTO);

            embedBuilder.setColor(yabbEnvironment.embedColor().successColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Save Success"),
                    MarkdownUtil.quoteBlock("Announcement Channel: %s%nAnnouncement Role: %s".formatted(announcementChannel.getAsMention(), announcementRole == null ? "N/A" : announcementRole.getAsMention())),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ApiException e) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());

            ApiError apiError = e.getError();
            switch (apiError.status()) {
                case 400 -> embedBuilder.addField("Invalid Parameters", MarkdownUtil.codeblock(apiError.message()), false);
                case 409 -> embedBuilder.addField("Configuration Already Exists", MarkdownUtil.codeblock("Guild (%s) is already configured".formatted(guild.getName())), false);
                default -> {
                    embedBuilder.addField("Internal Server Error", MarkdownUtil.codeblock("Something went wrong. Please try again later"), false);
                    log.warn("API Error during configuration save", e);
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
        embedBuilder.setTitle("View Configuration");
        embedBuilder.setDescription("This action is read-only");
        embedBuilder.setThumbnail(guild.getIconUrl());

        try {
            ConfigDTO configDTO = configClient.getConfig(guild.getIdLong());

            TextChannel announcementChanel = guild.getTextChannelById(configDTO.channelId());
            Role announcementRole = configDTO.roleId() == null ? null : guild.getRoleById(configDTO.roleId());

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

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());

            ApiError apiError = e.getError();
            switch (apiError.status()) {
                case 400 -> embedBuilder.addField("Invalid Parameters", MarkdownUtil.codeblock(apiError.message()), false);
                case 404 -> embedBuilder.addField("Configuration Does Not Exist", MarkdownUtil.codeblock("Guild (%s) is not configured".formatted(guild.getName())), false);
                default -> {
                    embedBuilder.addField("Internal Server Error", MarkdownUtil.codeblock("Something went wrong. Please try again later"), false);
                    log.warn("API Error during configuration view", e);
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

        // build the embed
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Delete Configuration");
        embedBuilder.setDescription("This action is destructive");
        embedBuilder.setThumbnail(guild.getIconUrl());

        try {
            configClient.deleteConfig(guild.getIdLong());

            embedBuilder.setColor(yabbEnvironment.embedColor().destructiveColor());
            embedBuilder.addField(
                    MarkdownUtil.underline("Deletion Success"),
                    MarkdownUtil.quoteBlock("Configuration Deleted Successfully"),
                    false
            );

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        } catch (ApiException e) {

            embedBuilder.setColor(yabbEnvironment.embedColor().warningColor());

            ApiError apiError = e.getError();
            switch (apiError.status()) {
                case 400 -> embedBuilder.addField("Invalid Parameters", MarkdownUtil.codeblock(apiError.message()), false);
                case 404 -> embedBuilder.addField("Configuration Does Not Exist", MarkdownUtil.codeblock("Guild (%s) is not configured".formatted(guild.getName())), false);
                default -> {
                    embedBuilder.addField("Internal Server Error", MarkdownUtil.codeblock("Something went wrong. Please try again later"), false);
                    log.warn("API Error during configuration delete", e);
                }
            }

            event.getHook().editOriginalEmbeds((embedBuilder.build())).queue();
        }
    }
}
