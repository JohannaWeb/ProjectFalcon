package app.falcon.trust.service;

import app.falcon.core.domain.Channel;
import app.falcon.core.domain.Member;
import app.falcon.core.domain.TrustRelation;
import app.falcon.trust.repository.ChannelRepository;
import app.falcon.trust.repository.MemberRepository;
import app.falcon.trust.repository.TrustRelationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipService {

    private final MemberRepository memberRepository;
    private final ChannelRepository channelRepository;
    private final TrustRelationRepository trustRelationRepository;

    private static final String AUTHORITY_DID = "did:plc:authority"; // Sovereign Authority DID

    public boolean verifyAccess(String userDid, Long serverId, Long channelId) {
        Optional<Channel> channelOpt = channelRepository.findById(channelId);
        if (channelOpt.isEmpty())
            return true; // Channel doesn't exist, ignore

        Channel channel = channelOpt.get();
        String requiredTier = channel.getRequiredTier();

        if (requiredTier == null || requiredTier.isBlank()) {
            return true; // Open channel
        }

        String userTier = calculateTier(userDid, serverId);

        // Simple hierarchy: ELITE > PRO > FREE
        if ("ELITE".equals(requiredTier)) {
            return "ELITE".equals(userTier);
        } else if ("PRO".equals(requiredTier)) {
            return "PRO".equals(userTier) || "ELITE".equals(userTier);
        }

        return true; // FREE or unknown tier
    }

    private String calculateTier(String userDid, Long serverId) {
        // 1. Check explicit tier in Member record
        Optional<Member> memberOpt = memberRepository.findByDidAndServerId(userDid, serverId);
        if (memberOpt.isPresent() && memberOpt.get().getMembershipTier() != null) {
            return memberOpt.get().getMembershipTier();
        }

        // 2. Logic: If Authority TRUSTS User with weight > 0.8 -> PRO
        Optional<TrustRelation> relation = trustRelationRepository.findBySourceDidAndTargetDid(AUTHORITY_DID, userDid);
        if (relation.isPresent() && relation.get().getType() == TrustRelation.TrustType.TRUST) {
            Double weight = relation.get().getWeight();
            if (weight != null && weight > 0.8) {
                return "PRO";
            }
        }

        return "FREE";
    }
}
