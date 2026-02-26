package app.falcon.siv.service;

import app.falcon.core.domain.UserSivConfig;
import app.falcon.siv.api.dto.IntelligenceResponse;
import app.falcon.siv.repository.UserSivConfigRepository;
import app.falcon.siv.vessels.SivVessel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SivIntelligenceService {

    private final List<SivVessel> vessels;
    private final UserSivConfigRepository configRepository;

    public IntelligenceResponse fetchIntelligence(String userDid) {
        List<UserSivConfig> configs = configRepository.findByUserDid(userDid);
        Map<String, Object> activities = new ConcurrentHashMap<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = configs.stream()
                    .map(config -> CompletableFuture.runAsync(() -> {
                        vessels.stream()
                                .filter(v -> v.getType().equals(config.getVesselType()))
                                .findFirst()
                                .ifPresent(vessel -> {
                                    List<?> data = vessel.fetchActivity(config);
                                    activities.put(config.getVesselType(), data);
                                });
                    }, executor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        return new IntelligenceResponse("success", activities, java.time.Instant.now().toString());
    }
}
