package app.falcon.api;

import app.falcon.domain.TrustRelation;
import app.falcon.trust.TrustGraphService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/trust")
@RequiredArgsConstructor
public class TrustController {

    private final TrustGraphService trustGraphService;

    @GetMapping("/score/{targetDid}")
    public ResponseEntity<Map<String, Object>> getTrustScore(
            HttpServletRequest request,
            @PathVariable String targetDid) {

        String viewerDid = (String) request.getAttribute(AtprotoAuthFilter.ATTR_USER_DID);
        double score = trustGraphService.calculateTrustScore(viewerDid, targetDid);

        return ResponseEntity.ok(Map.of(
                "targetDid", targetDid,
                "viewerDid", viewerDid,
                "score", score));
    }

    @PostMapping("/relation")
    public ResponseEntity<Void> setRelation(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {

        String sourceDid = (String) request.getAttribute(AtprotoAuthFilter.ATTR_USER_DID);
        String targetDid = body.get("targetDid");
        String typeStr = body.get("type");

        if (targetDid == null || typeStr == null) {
            return ResponseEntity.badRequest().build();
        }

        TrustRelation.TrustType type = TrustRelation.TrustType.valueOf(typeStr.toUpperCase());
        trustGraphService.addRelation(sourceDid, targetDid, type);

        return ResponseEntity.ok().build();
    }
}
