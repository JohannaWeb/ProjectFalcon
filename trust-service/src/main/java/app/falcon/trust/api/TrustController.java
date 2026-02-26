package app.falcon.trust.api;

import app.falcon.core.domain.TrustRelation;
import app.falcon.trust.service.TrustGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/trust")
@RequiredArgsConstructor
public class TrustController {

    private final TrustGraphService trustGraphService;

    @GetMapping("/score/{targetDid}")
    public Map<String, Object> getTrustScore(
            @PathVariable String targetDid,
            @RequestHeader(value = "X-Falcon-Viewer-DID", required = false) String viewerDid) {

        double score = trustGraphService.calculateTrustScore(viewerDid, targetDid);
        return Map.of(
                "targetDid", targetDid,
                "score", score,
                "status", score > 0.5 ? "Trusted" : score < -0.5 ? "Distrusted" : "Neutral");
    }

    @PostMapping("/relation")
    public Map<String, String> setRelation(
            @RequestBody Map<String, String> request,
            @RequestHeader("X-Falcon-Viewer-DID") String viewerDid) {

        String targetDid = request.get("targetDid");
        TrustRelation.TrustType type = TrustRelation.TrustType.valueOf(request.get("type").toUpperCase());

        trustGraphService.addRelation(viewerDid, targetDid, type);
        return Map.of("status", "success");
    }
}
