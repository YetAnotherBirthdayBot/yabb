package io.github.eggy03.yabb.bot.environment;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import lombok.NonNull;

@ConfigMapping(prefix = "yabb")
public interface YabbEnvironment {

    General general();
    Discord discord();
    API api();

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

    interface API {

        @WithName("url")
        @NonNull
        String url();
    }

}
