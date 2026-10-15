package io.github.eggy03.yabb.bot.service.misc;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

@ApplicationScoped
@SuppressWarnings("java:S1192")
public class SlashCommandRegistrationService {

    public void registerCommands(@NonNull ReadyEvent event) {

        //config commands
        CommandData config = Commands
                .slash("config", "Configures the bot in your guild")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                .addSubcommands(
                        new SubcommandData("save", "Saves configuration for this guild")
                                .addOption(OptionType.CHANNEL, "announcement_channel", "Announcements will be sent to this channel", true)
                                .addOption(OptionType.ROLE, "announcement_role", "This role will be pinged during announcements", false),

                        new SubcommandData("view", "Shows your saved configuration for this guild"),
                        new SubcommandData("delete", "Deletes your saved configuration for this guild")
                );

        // birthday commands
        CommandData birthday = Commands
                .slash("birthday", "Set your birthday")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                .addSubcommands(
                        new SubcommandData("save", "Saves your birthday for this guild")
                                .addOption(OptionType.INTEGER, "birth_day", "Set your birth day", true)
                                .addOption(OptionType.INTEGER, "birth_month", "Set your birth month", true)
                                .addOption(OptionType.STRING, "time_zone", "IANA Time Zone", true),

                        new SubcommandData("view", "Shows your or someone else' saved birthday for this guild")
                                .addOption(OptionType.USER, "birth_user", "Leave this blank to view your birthday", false),

                        new SubcommandData("viewall", "Shows everyone's saved birthdays for this guild"),
                        new SubcommandData("delete", "Deletes your saved birthday for this guild")
                );

        // admin commands
        CommandData admin = Commands
                .slash("admin", "Admin commands")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
                .addSubcommands(
                        new SubcommandData("savebirthday", "Save someone's birthday in this guild")
                                .addOption(OptionType.USER, "birth_user", "User whose birthday will be saved", true)
                                .addOption(OptionType.INTEGER, "birth_day", "Set birth day", true)
                                .addOption(OptionType.INTEGER, "birth_month", "Set birth month", true)
                                .addOption(OptionType.STRING, "time_zone", "IANA Time Zone", true),


                        new SubcommandData("deletebirthday", "Delete someone's birthday from this guild")
                                .addOption(OptionType.USER, "birth_user", "User whose birthday will be removed", true)
                );

        // general commands
        CommandData help = Commands
                .slash("help", "Get setup and usage instructions")
                .setContexts(InteractionContextType.GUILD)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED);

        event.getJDA()
                .updateCommands()
                .addCommands(config, birthday, admin, help)
                .queue();
    }
}
