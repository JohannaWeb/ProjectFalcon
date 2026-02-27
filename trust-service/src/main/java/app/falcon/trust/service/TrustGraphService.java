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

    private static final double DECAY_LAMBDA = 0.005; // ~0.5% decay per day
    private final TrustRelationRepository repository;

    public double calculateTrustScore(String viewerDid, String targetDid) {
        if (viewerDid == null || viewerDid.equals(targetDid))
            return 1.0;

        Optional<TrustRelation> direct = repository.findBySourceDidAndTargetDid(viewerDid, targetDid);
        if (direct.isPresent()) {
            return calculateWeightedScore(direct.get());
        }

        List<TrustRelation> viewerTrusts = repository.findBySourceDidAndType(viewerDid, TrustRelation.TrustType.TRUST);
        if (viewerTrusts.isEmpty())
            return 0.0;

        double sumWeightedScores = 0.0;
        double sumViewerTrust = 0.0;

        for (TrustRelation bridge : viewerTrusts) {
            String bridgeDid = bridge.getTargetDid();
            Optional<TrustRelation> bridgeToTarget = repository.findBySourceDidAndTargetDid(bridgeDid, targetDid);

            if (bridgeToTarget.isPresent()) {
                double viewerToBridge = calculateWeightedScore(bridge);
                double bridgeToTargetScore = calculateWeightedScore(bridgeToTarget.get());

                // Formula: S = T(u,w) * T(w,v) * Decay
                sumWeightedScores += viewerToBridge * bridgeToTargetScore;
                sumViewerTrust += Math.abs(viewerToBridge);
            }
        }

        if (sumViewerTrust == 0)
            return 0.0;

        // Apply non-linear squashing (tanh) for Sybil resistance and normalization
        double aggregate = sumWeightedScores / sumViewerTrust;
        return Math.tanh(aggregate);
    }

    private double calculateWeightedScore(TrustRelation relation) {
        double baseScore = scoreForType(relation.getType());
        long daysOld = java.time.Duration.between(relation.getTimestamp(), java.time.Instant.now()).toDays();
        double temporalWeight = Math.exp(-DECAY_LAMBDA * daysOld);
        return baseScore * temporalWeight;
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
