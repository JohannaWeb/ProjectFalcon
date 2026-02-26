package app.falcon.siv.ai;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Represents the Falcon AI Agent — a sovereign participant in the social graph.
 *
 * <p>
 * Every {@link app.falcon.core.domain.AiFact} created by the AI will be stamped
 * with
 * this agent's DID, making every AI decision transparent and auditable rather
 * than
 * coming from an anonymous black-box filter.
 * </p>
 *
 * <p>
 * Phase 2 will have this agent register a real DID:PLC and cryptographically
 * sign AT
 * Protocol records into the Lexicon. Phase 1 stamps the DID on every local
 * {@code AiFact}.
 * </p>
 */
@Service
@Slf4j
public class SovereignAgentService {

    @Getter
    private final String agentDid;

    private final String agentName;

    public SovereignAgentService(
            @Value("${falcon.ai.agent-did:did:plc:falcon-ai-agent}") String agentDid,
            @Value("${falcon.ai.agent-name:Falcon Sovereign AI}") String agentName) {
        this.agentDid = agentDid;
        this.agentName = agentName;
        log.info("🤖 Sovereign AI Agent initialized: {} ({})", agentName, agentDid);
    }

    /**
     * Returns the system prompt that establishes this agent's persona and output
     * contract.
     * The format is strict JSON so downstream parsing is reliable.
     */
    public String buildTaggingSystemPrompt() {
        return """
                You are %s, a Sovereign AI agent for the Falcon decentralized communication platform.
                Your mission: analyse AT Protocol social posts and extract structured intelligence.

                Respond ONLY with compact JSON in exactly this format:
                {"tags":["tag1","tag2"],"summary":"one sentence","factType":"TAG","confidence":0.85}

                Rules:
                - tags: 1-5 lowercase single-word topic labels. No jargon, no duplicates.
                - summary: max 20 words describing the main idea.
                - factType: always "TAG" for tagging requests.
                - confidence: your certainty score 0.0–1.0.
                - No markdown, no explanation, just the JSON object.
                """.formatted(agentName);
    }

    public String buildSummarySystemPrompt() {
        return """
                You are %s, a Sovereign AI agent for the Falcon decentralized communication platform.
                Summarise the following conversation history in 1–2 sentences. Be neutral and factual.
                Respond ONLY with plain text — no JSON, no markdown.
                """.formatted(agentName);
    }

    public String buildModerationSystemPrompt() {
        return """
                You are %s, a safety-focused Sovereign AI agent.
                Analyse the following post and determine if it contains harmful content.
                Respond ONLY with JSON: {"isHarmful":false,"reason":"","confidence":0.0}
                isHarmful: true only for clear harassment, hate speech, or threats.
                confidence: 0.0–1.0. When uncertain, prefer false.
                """.formatted(agentName);
    }
}
