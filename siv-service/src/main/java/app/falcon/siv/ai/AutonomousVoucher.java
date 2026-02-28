package app.falcon.siv.ai;

import app.falcon.core.domain.AiFact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Logic for "Vouching" — auto-triggering on-chain attestations for
 * high-confidence AI findings.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AutonomousVoucher {

    private final WebClient.Builder webClientBuilder;
    private final SovereignAgentService agentService;

    @Value("${falcon.services.trust-url:http://localhost:8081}")
    private String trustServiceUrl;

    /**
     * Examines an AI fact and decides if it warrants an on-chain attestation.
     */
    public void evaluateVouch(AiFact fact) {
        // Condition: Confidence > 0.95 and HIGHLIGHT (positive behavioral signal)
        if (fact.getFactType() == AiFact.FactType.HIGHLIGHT && fact.getConfidence() >= 0.95) {
            vouchOnChain(fact.getSourceDid());
        }
    }

    private void vouchOnChain(String targetDid) {
        log.info("🛡️ Autonomous Voucher: High confidence signal for {}. Triggering on-chain attestation...",
                targetDid);

        webClientBuilder.build().post()
                .uri(trustServiceUrl + "/api/trust/attest/" + targetDid)
                .header("X-Falcon-Viewer-DID", agentService.getAgentDid())
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(res -> log.info("✅ Successfully vouched for {} on-chain. Attestation ID: {}", targetDid,
                        res.get("attestationId")))
                .doOnError(err -> log.error("❌ Failed to vouch for {} on-chain: {}", targetDid, err.getMessage()))
                .subscribe(); // Async fire-and-forget
    }
}
