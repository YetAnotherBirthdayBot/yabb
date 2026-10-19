package io.github.eggy03.yabb.bot.environment;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import lombok.NonNull;

@ConfigMapping(prefix = "yabb")
public interface YabbEnvironment {

    General general();
    Discord discord();
    EmbedColor embedColor();

    interface General {

        @WithName("app.version")
        @NonNull
        String appVersion();
    }

    interface Discord {

        @WithName("token")
        @NonNull
        String token();
    }

    interface EmbedColor {

        @WithName("success.color.integer")
        int successColor();

        @WithName("warning.color.integer")
        int warningColor();

        @WithName("destructive.color.integer")
        int destructiveColor();
    }

}
