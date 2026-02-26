package app.falcon.siv.vessels;

import app.falcon.core.domain.AiFact;
import app.falcon.core.domain.UserSivConfig;
import app.falcon.siv.repository.AiFactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI Sovereign Integration Vessel.
 *
 * <p>
 * Plugs into the existing {@link app.falcon.siv.service.SivIntelligenceService}
 * parallel fanout.
 * When called, surfaces the most recent AI-generated facts (tags, summaries,
 * warnings) for the
 * user's known activity DIDs. AI insights appear in the same intelligence panel
 * alongside
 * GitHub/Linear/Vercel activity — no special wiring needed.
 * </p>
 *
 * <p>
 * Only activated when {@code falcon.ai.enabled=true}.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "falcon.ai.enabled", havingValue = "true", matchIfMissing = false)
public class AiVessel implements SivVessel {

    private final AiFactRepository aiFactRepository;

    @Override
    public String getType() {
        return "ai";
    }

    @Override
    public List<?> fetchActivity(UserSivConfig config) {
        String userDid = config.getUserDid();

        List<AiFact> facts = aiFactRepository.findTop20BySourceDidOrderByCreatedAtDesc(userDid);

        log.debug("AiVessel: returning {} facts for userDid={}", facts.size(), userDid);

        return facts.stream()
                .map(f -> new AiFactView(
                        f.getFactType().name(),
                        f.getContent(),
                        f.getConfidence(),
                        f.getAgentDid(),
                        f.getCreatedAt().toString()))
                .toList();
    }

    /**
     * Lightweight view record safe to serialise across the service boundary.
     * Avoids leaking JPA managed entity references.
     */
    public record AiFactView(
            String factType,
            String content,
            Double confidence,
            String agentDid,
            String createdAt) {
    }
}
