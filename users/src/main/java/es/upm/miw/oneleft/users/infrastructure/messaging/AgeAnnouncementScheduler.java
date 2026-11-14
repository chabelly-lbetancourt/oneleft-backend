package es.upm.miw.oneleft.users.infrastructure.messaging;

import es.upm.miw.oneleft.users.domain.port.in.AnnounceAgesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Every night, shortly after midnight, announces the new age of who has their birthday (HU-070). */
@Configuration
@EnableScheduling
class AgeAnnouncementScheduler {

    private static final Logger log = LoggerFactory.getLogger(AgeAnnouncementScheduler.class);

    private final AnnounceAgesUseCase ages;

    AgeAnnouncementScheduler(AnnounceAgesUseCase ages) {
        this.ages = ages;
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "UTC")
    void announceBirthdays() {
        try {
            log.info("Ages announced for {} birthdays", ages.announceBirthdays());
        } catch (RuntimeException error) {
            log.error("The birthdays were not announced", error);
        }
    }
}
