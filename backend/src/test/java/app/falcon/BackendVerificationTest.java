package app.falcon;

import app.falcon.siv.pulse.SivPulse;
import app.falcon.siv.pulse.SivPulseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class BackendVerificationTest {

    @Autowired
    private SivPulseRepository repository;

    @Test
    void verifyVirtualThreadsEnabled() throws InterruptedException {
        AtomicBoolean isVirtual = new AtomicBoolean(false);
        Thread thread = Thread.ofVirtual().unstarted(() -> {
            isVirtual.set(Thread.currentThread().isVirtual());
        });
        thread.start();
        thread.join();
        assertThat(isVirtual.get()).isTrue();
    }

    @Test
    void testSivPulsePersistence() {
        SivPulse pulse = SivPulse.builder()
                .vesselType("github")
                .pulseType("COMMIT")
                .actorDid("did:plc:example")
                .payload(Map.of("message", "Win the next hour"))
                .timestamp(Instant.now())
                .build();

        SivPulse saved = repository.save(pulse);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPayload()).containsEntry("message", "Win the next hour");
    }
}
