package app.falcon.siv;

import app.falcon.domain.UserSivConfig;
import app.falcon.repository.UserSivConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SivIntelligenceService {

    private final UserSivConfigRepository repository;
    private final List<SivVessel> vessels;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public Map<String, Object> fetchAllActivity(String userDid) {
        List<UserSivConfig> configs = repository.findByUserDid(userDid);

        // Spawn a virtual thread for each integration to fetch activities in parallel
        // This leverages Loom's full potential for massive I/O concurrency
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Map<String, Object> activities = Collections.synchronizedMap(new HashMap<>());

        for (UserSivConfig config : configs) {
            SivVessel vessel = findVessel(config.getVesselType());
            if (vessel != null) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        log.info("Fetching {} activity for user {} on virtual thread",
                                config.getVesselType(), userDid);
                        List<Map<String, Object>> result = vessel.fetchActivity(config.getEncryptedToken(),
                                config.getConfig());
                        activities.put(config.getVesselType(), result);
                    } catch (Exception e) {
                        log.error("Failed to fetch {} activity", config.getVesselType(), e);
                    }
                }, executor));
            }
        }

        // Wait for all fetches to complete (or time out)
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Intelligence aggregation partially failed or timed out for user {}", userDid);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("userDid", userDid);
        report.put("timestamp", java.time.Instant.now().toString());
        report.put("activities", activities);

        return report;
    }

    private SivVessel findVessel(String type) {
        return vessels.stream()
                .filter(v -> v.getVesselType().equals(type))
                .findFirst()
                .orElse(null);
    }
}
