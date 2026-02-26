package app.falcon.siv.pulse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class SivPulseService {
    private final SivPulseRepository repository;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public SivPulseService(SivPulseRepository repository) {
        this.repository = repository;
    }

    public void recordPulse(String vesselType, String pulseType, String actorDid, Map<String, Object> payload) {
        // Fire and forget pulse recording on a virtual thread
        // Zero cost to the main request flow
        executor.submit(() -> {
            try {
                SivPulse pulse = SivPulse.builder()
                        .vesselType(vesselType)
                        .pulseType(pulseType)
                        .actorDid(actorDid)
                        .payload(payload)
                        .timestamp(java.time.Instant.now())
                        .build();
                repository.save(pulse);
                log.info("Recorded pulse {} for user {} on virtual thread", pulseType, actorDid);
            } catch (Exception e) {
                log.error("Failed to record pulse in background", e);
            }
        });
    }

    public List<SivPulse> getPulsesForUser(String actorDid) {
        return repository.findByActorDidOrderByTimestampDesc(actorDid);
    }
}
