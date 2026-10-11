package io.github.eggy03.yabb.bot.configuration;

import com.microsoft.kiota.RequestAdapter;
import com.microsoft.kiota.authentication.AnonymousAuthenticationProvider;
import com.microsoft.kiota.http.OkHttpRequestAdapter;
import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import io.github.eggy03.yabb.client.YabbClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

@ApplicationScoped
public final class YabbClientBeanProducer {

    private final @NonNull YabbEnvironment yabbEnvironment;

    public YabbClientBeanProducer(@NonNull YabbEnvironment yabbEnvironment) {
        this.yabbEnvironment = yabbEnvironment;
    }

    @Contract(" -> new")
    @Produces
    @Singleton // AppScoped doesnt work on Kiota
    public @NonNull YabbClient yabbClient() {

        RequestAdapter adapter = new OkHttpRequestAdapter(new AnonymousAuthenticationProvider());
        adapter.setBaseUrl(yabbEnvironment.api().url());

        return new YabbClient(adapter);
    }
}
