package io.github.eggy03.yabb.bot.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.Permission;

import java.util.EnumSet;
import java.util.Set;

@UtilityClass
public final class PermissionUtils {

    @NonNull
    public Set<Permission> necessaryPermissions() {
        return EnumSet.of(
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_SEND_IN_THREADS,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_HISTORY
        );
    }
}
