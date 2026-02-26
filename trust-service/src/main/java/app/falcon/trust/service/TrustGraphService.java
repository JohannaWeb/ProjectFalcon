package app.falcon.trust.service;

import app.falcon.core.domain.TrustRelation;
import app.falcon.trust.repository.TrustRelationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustGraphService {

    private final TrustRelationRepository repository;

    public double calculateTrustScore(String viewerDid, String targetDid) {
        if (viewerDid == null || viewerDid.equals(targetDid))
            return 1.0;

        Optional<TrustRelation> direct = repository.findBySourceDidAndTargetDid(viewerDid, targetDid);
        if (direct.isPresent()) {
            return scoreForType(direct.get().getType());
        }

        List<TrustRelation> viewerTrusts = repository.findBySourceDidAndType(viewerDid, TrustRelation.TrustType.TRUST);

        double aggregateScore = 0.0;
        int bridgesFound = 0;

        for (TrustRelation bridge : viewerTrusts) {
            String bridgeDid = bridge.getTargetDid();
            Optional<TrustRelation> bridgeToTarget = repository.findBySourceDidAndTargetDid(bridgeDid, targetDid);

            if (bridgeToTarget.isPresent()) {
                double bridgeScore = scoreForType(bridgeToTarget.get().getType());
                aggregateScore += bridgeScore * 0.5;
                bridgesFound++;
            }
        }

        if (bridgesFound == 0)
            return 0.0;
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
