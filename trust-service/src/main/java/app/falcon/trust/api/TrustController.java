package app.falcon.trust.api;

import app.falcon.core.domain.TrustRelation;
import app.falcon.trust.api.dto.TrustRelationResponse;
import app.falcon.trust.api.dto.TrustScoreResponse;
import app.falcon.trust.service.EasService;
import app.falcon.trust.service.TrustGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/trust")
@RequiredArgsConstructor
public class TrustController {

    private final TrustGraphService trustGraphService;
    private final EasService easService;

    @GetMapping("/score/{targetDid}")
    public TrustScoreResponse getTrustScore(
            @PathVariable String targetDid,
            @RequestHeader(value = "X-Falcon-Viewer-DID", required = false) String viewerDid) {

        double score = trustGraphService.calculateTrustScore(viewerDid, targetDid);
        return TrustScoreResponse.of(targetDid, score);
    }

    @PostMapping("/relation")
    public TrustRelationResponse setRelation(
            @RequestBody Map<String, String> request,
            @RequestHeader("X-Falcon-Viewer-DID") String viewerDid) {

        String targetDid = request.get("targetDid");
        TrustRelation.TrustType type = TrustRelation.TrustType.valueOf(request.get("type").toUpperCase());

        trustGraphService.addRelation(viewerDid, targetDid, type);
        return TrustRelationResponse.success(viewerDid, type.name(), targetDid);
    }

    @PostMapping("/attest/{targetDid}")
    public Map<String, String> attestOnChain(
            @PathVariable String targetDid,
            @RequestHeader("X-Falcon-Viewer-DID") String viewerDid) throws Exception {

        double score = trustGraphService.calculateTrustScore(viewerDid, targetDid);
        String attestationId = easService.attest(score, targetDid, viewerDid);

        return Map.of(
                "status", "success",
                "attestationId", attestationId,
                "targetDid", targetDid,
                "score", String.valueOf(score));
    }
}
