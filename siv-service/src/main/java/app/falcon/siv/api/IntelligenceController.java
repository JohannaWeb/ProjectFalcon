package app.falcon.siv.api;

import app.falcon.siv.service.IntelligenceVettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/intelligence")
@RequiredArgsConstructor
public class IntelligenceController {

    private final IntelligenceVettingService vettingService;

    @GetMapping("/bias/{did}")
    public Map<String, Object> getIntelligenceBias(@PathVariable String did) {
        double bias = vettingService.calculateBias(did);
        return Map.of(
                "did", did,
                "bias", bias,
                "vettedAt", java.time.Instant.now().toString());
    }
}
