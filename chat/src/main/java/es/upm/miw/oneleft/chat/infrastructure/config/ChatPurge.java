package es.upm.miw.oneleft.chat.infrastructure.config;

import es.upm.miw.oneleft.chat.domain.port.in.ChatUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Every hour, the messages of the plans that ended a while ago are deleted (HU-008). Every replica may run it: deleting
 * twice is harmless.
 */
@Configuration
@EnableScheduling
public class ChatPurge {

    private static final Logger log = LoggerFactory.getLogger(ChatPurge.class);

    private final ChatUseCase chat;

    public ChatPurge(ChatUseCase chat) {
        this.chat = chat;
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1M")
    public void purge() {
        var deleted = chat.purge();
        if (deleted > 0) {
            log.info("Deleted {} chat messages of plans that are over", deleted);
        }
    }
}
