package app.falcon.trust;

import app.falcon.domain.TrustRelation;
import app.falcon.repository.TrustRelationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustGraphService {

    private final TrustRelationRepository repository;

    /**
     * Calculates a trust score for a target user from the perspective of a viewer.
     * Uses a simple transitive trust model (depth 2 for now).
     * Score range: -1.0 (Distrusted) to 1.0 (Fully Trusted).
     */
    public double calculateTrustScore(String viewerDid, String targetDid) {
        if (viewerDid.equals(targetDid))
            return 1.0;

        // 1. Check direct relation
        Optional<TrustRelation> direct = repository.findBySourceDidAndTargetDid(viewerDid, targetDid);
        if (direct.isPresent()) {
            return scoreForType(direct.get().getType());
        }

        // 2. Check second-degree trust (Bridges)
        // Find everyone the viewer trusts
        List<TrustRelation> viewerTrusts = repository.findBySourceDidAndType(viewerDid, TrustRelation.TrustType.TRUST);

        double aggregateScore = 0.0;
        int bridgesFound = 0;

        for (TrustRelation bridge : viewerTrusts) {
            String bridgeDid = bridge.getTargetDid();
            Optional<TrustRelation> bridgeToTarget = repository.findBySourceDidAndTargetDid(bridgeDid, targetDid);

            if (bridgeToTarget.isPresent()) {
                double bridgeScore = scoreForType(bridgeToTarget.get().getType());
                // Decay factor for second degree trust (e.g., 0.5)
                aggregateScore += bridgeScore * 0.5;
                bridgesFound++;
            }
        }

        if (bridgesFound == 0)
            return 0.0; // Unknown

        // Normalize and cap
        return Math.max(-1.0, Math.min(1.0, aggregateScore / bridgesFound));
    }

    private double scoreForType(TrustRelation.TrustType type) {
        return switch (type) {
            case TRUST -> 1.0;
            case DISTRUST -> -0.8;
            case BLOCK -> -1.0;
            case MUTE -> -0.3;
        };
    }

    public void addRelation(String sourceDid, String targetDid, TrustRelation.TrustType type) {
        TrustRelation relation = repository.findBySourceDidAndTargetDid(sourceDid, targetDid)
                .orElse(TrustRelation.builder().sourceDid(sourceDid).targetDid(targetDid).build());

        relation.setType(type);
        relation.setTimestamp(java.time.Instant.now());
        repository.save(relation);
        log.info("Trust relation updated: {} {} {}", sourceDid, type, targetDid);
    }
}
