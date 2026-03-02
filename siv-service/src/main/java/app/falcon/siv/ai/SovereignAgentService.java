package app.falcon.siv.ai;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents the Falcon AI Agent — a sovereign participant in the social graph.
 *
 * <p>
 * Every {@link app.falcon.core.domain.AiFact} created by the AI will be stamped
 * with this agent's DID, making every AI decision transparent and auditable rather
 * than coming from an anonymous black-box filter.
 * </p>
 *
 * <p>
 * The agent's identity is defined by three markdown files in the agent directory:
 * soul.md (core values), personality.md (behavioural style), persistence.md (memory
 * accumulated over time). These are loaded at startup so they can be edited and
 * redeployed without touching application code.
 * </p>
 */
@Service
@Slf4j
public class SovereignAgentService {

    public enum AgentMemoryState { CLEAN, PERSISTENT }

    @Getter
    private final String agentDid;

    private final String agentName;
    private final String soul;
    private final String personality;
    private final String persistence;

    private final AtomicReference<AgentMemoryState> memoryState =
            new AtomicReference<>(AgentMemoryState.PERSISTENT);

    public SovereignAgentService(
            @Value("${falcon.ai.agent-did:did:plc:falcon-ai-agent}") String agentDid,
            @Value("${falcon.ai.agent-name:Falcon Sovereign AI}") String agentName,
            @Value("${falcon.ai.agent-dir:agent}") String agentDir) {
        this.agentDid = agentDid;
        this.agentName = agentName;
        this.soul = loadFile(agentDir, "soul.md");
        this.personality = loadFile(agentDir, "personality.md");
        this.persistence = loadFile(agentDir, "persistence.md");
        log.info("Sovereign AI Agent initialized: {} ({})", agentName, agentDid);
        log.info("Agent identity loaded — soul: {}, personality: {}, persistence: {}",
                soul.isBlank() ? "not found" : "ok",
                personality.isBlank() ? "not found" : "ok",
                persistence.isBlank() ? "not found" : "ok");
    }

    private String loadFile(String dir, String filename) {
        Path path = Path.of(dir, filename);
        if (!Files.exists(path)) {
            log.warn("Agent file not found: {}", path.toAbsolutePath());
            return "";
        }
        try {
            String content = Files.readString(path).strip();
            log.debug("Loaded {}: {} chars", filename, content.length());
            return content;
        } catch (IOException e) {
            log.warn("Failed to read {}: {}", path.toAbsolutePath(), e.getMessage());
            return "";
        }
    }

    public AgentMemoryState getMemoryState() {
        return memoryState.get();
    }

    public AgentMemoryState setMemoryState(AgentMemoryState state) {
        AgentMemoryState previous = memoryState.getAndSet(state);
        log.info("Agent memory state changed: {} -> {}", previous, state);
        return state;
    }

    private String buildIdentityBlock() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are ").append(agentName).append(", a Sovereign AI agent for the Falcon decentralized communication platform.\n");
        if (!soul.isBlank()) {
            sb.append("\n## Soul\n").append(soul).append("\n");
        }
        if (!personality.isBlank()) {
            sb.append("\n## Personality\n").append(personality).append("\n");
        }
        if (memoryState.get() == AgentMemoryState.PERSISTENT && !persistence.isBlank()) {
            sb.append("\n## Memory\n").append(persistence).append("\n");
        }
        return sb.toString();
    }

    public String buildTaggingSystemPrompt() {
        return buildIdentityBlock() + """

                Your mission: analyse AT Protocol social posts and extract structured intelligence.

                Respond ONLY with compact JSON in exactly this format:
                {"tags":["tag1","tag2"],"summary":"one sentence","factType":"TAG","confidence":0.85,"reasoning":"why"}

                Rules:
                - tags: 1-5 lowercase single-word topic labels. No jargon, no duplicates.
                - summary: max 20 words describing the main idea.
                - factType: always "TAG" for tagging requests.
                - confidence: your certainty score 0.0–1.0.
                - reasoning: one sentence explaining your confidence level.
                - No markdown, no explanation, just the JSON object.
                """;
    }

    public String buildSummarySystemPrompt() {
        return buildIdentityBlock() + """

                Summarise the following conversation history in 1–2 sentences. Be neutral and factual.
                Respond ONLY with plain text — no JSON, no markdown.
                """;
    }

    public String buildModerationSystemPrompt() {
        return buildIdentityBlock() + """

                Analyse the following post and determine if it contains harmful content.
                Respond ONLY with JSON: {"isHarmful":false,"reason":"","confidence":0.0,"reasoning":""}
                isHarmful: true only for clear harassment, hate speech, or threats.
                confidence: 0.0–1.0. When uncertain, prefer false.
                """;
    }
}
