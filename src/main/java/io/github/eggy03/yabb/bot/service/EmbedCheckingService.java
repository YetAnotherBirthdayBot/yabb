package io.github.eggy03.yabb.bot.service;

import io.github.eggy03.yabb.bot.environment.YabbEnvironment;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;

@ApplicationScoped
@Slf4j
@RequiredArgsConstructor
public final class EmbedCheckingService {

    private final @NonNull YabbEnvironment yabbEnvironment;

    public void checkAndSend(@NonNull SlashCommandInteractionEvent event, @NonNull EmbedBuilder embedBuilder) {

        if (!embedBuilder.isValidLength() || embedBuilder.isEmpty()) {
            log.warn("Embed containing content is either empty or too long (current length: {}).", embedBuilder.length());

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Embed Cannot Be Sent");
            eb.setDescription("Too Long or Empty");
            eb.addField("", MarkdownUtil.quoteBlock("Embed Length: %s%nValid: %s".formatted(embedBuilder.length(), embedBuilder.isValidLength())), false);
            eb.setColor(yabbEnvironment.embedColor().warningColor());

            event.getHook().editOriginalEmbeds(eb.build()).queue();
            return;
        }

        event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
    }
}
