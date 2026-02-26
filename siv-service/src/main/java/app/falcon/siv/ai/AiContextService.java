package app.falcon.siv.ai;

import app.falcon.core.domain.AiFact;
import app.falcon.siv.repository.AiFactRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * The core AI engine — processes AT Protocol Jetstream events through the LLM.
 *
 * <h2>Design</h2>
 * <ul>
 * <li>Every call to {@link #processPost} is non-blocking from the caller's
 * perspective.
 * A new <b>virtual thread</b> is spun up per event via
 * {@code VirtualThreadPerTaskExecutor}.</li>
 * <li>A bounded {@link ChannelMemory} keeps the last N messages per DID (used
 * as a channel proxy
 * on the public firehose) so the LLM has rolling conversational context.</li>
 * <li>A <b>rate limiter</b> (last-processed timestamp per DID) caps AI calls at
 * one per DID per
 * {@code falcon.ai.rate-limit-seconds} to avoid hammering the model on
 * high-traffic accounts.</li>
 * <li>The memory map is bounded to {@code falcon.ai.max-tracked-dids} entries
 * to prevent OOM
 * on the global firehose.</li>
 * </ul>
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "falcon.ai.enabled", havingValue = "true", matchIfMissing = false)
public class AiContextService {

    private final FalconAiClient aiClient;
    private final AiFactRepository factRepository;
    private final SovereignAgentService agentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final int contextWindowSize;
    private final long rateLimitSeconds;
    private final int maxTrackedDids;

    // DID → sliding window of recent messages
    private final ConcurrentHashMap<String, ChannelMemory> memories = new ConcurrentHashMap<>();
    // DID → last time we called the AI for this DID
    private final ConcurrentHashMap<String, Instant> lastProcessed = new ConcurrentHashMap<>();

    // Virtual thread executor — one thread per Jetstream event, zero blocking
    // overhead
    private final java.util.concurrent.ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public AiContextService(
            FalconAiClient aiClient,
            AiFactRepository factRepository,
            SovereignAgentService agentService,
            @Value("${falcon.ai.context-window-size:50}") int contextWindowSize,
            @Value("${falcon.ai.rate-limit-seconds:60}") long rateLimitSeconds,
            @Value("${falcon.ai.max-tracked-dids:5000}") int maxTrackedDids) {
        this.aiClient = aiClient;
        this.factRepository = factRepository;
        this.agentService = agentService;
        this.contextWindowSize = contextWindowSize;
        this.rateLimitSeconds = rateLimitSeconds;
        this.maxTrackedDids = maxTrackedDids;
    }

    /**
     * Accepts a Jetstream post event and dispatches AI analysis to a virtual
     * thread.
     * Returns immediately — the caller (JetstreamHandler) is never blocked.
     *
     * @param did  the AT Protocol DID of the post author
     * @param text the post content
     */
    public void processPost(String did, String text) {
        if (text == null || text.isBlank())
            return;

        executor.submit(() -> {
            try {
                analysePost(did, text);
            } catch (Exception e) {
                log.warn("AI analysis failed for DID {}: {}", did, e.getMessage());
            }
        });
    }

    private void analysePost(String did, String text) {
        // Enforce rate limit
        Instant last = lastProcessed.get(did);
        if (last != null && last.plusSeconds(rateLimitSeconds).isAfter(Instant.now())) {
            // Still in cooldown — just update memory and return
            getOrCreateMemory(did).add(text);
            return;
        }

        // Enforce DID map bounds — evict a random entry when full
        if (memories.size() >= maxTrackedDids && !memories.containsKey(did)) {
            String evictKey = memories.keys().nextElement();
            memories.remove(evictKey);
            lastProcessed.remove(evictKey);
            log.debug("Evicted DID {} from AI context to stay within bounds", evictKey);
        }

        ChannelMemory memory = getOrCreateMemory(did);
        memory.add(text);
        lastProcessed.put(did, Instant.now());

        // 1. Tag the post and generate a summary using the rolling window
        tagPost(did, text, memory);

        // 2. Moderation check — only on the new post, not the full history
        moderatePost(did, text);
    }

    private void tagPost(String did, String text, ChannelMemory memory) {
        String userPrompt = "Analyse this AT Protocol post:\n\n%s\n\nConversation context:\n%s"
                .formatted(text, memory.toPrompt());

        // .block() is safe here — we are on a virtual thread, not a reactive thread
        String response = aiClient.complete(
                agentService.buildTaggingSystemPrompt(), userPrompt).block();

        if (response == null || response.isBlank())
            return;

        try {
            JsonNode json = objectMapper.readTree(response);
            String summary = json.path("summary").asText("");
            double confidence = json.path("confidence").asDouble(0.7);

            // Persist each tag as a separate AiFact
            json.path("tags").forEach(tagNode -> {
                String tag = tagNode.asText().toLowerCase().trim();
                if (!tag.isBlank()) {
                    saveAiFact(did, AiFact.FactType.TAG, tag, confidence);
                }
            });

            // Persist the summary
            if (!summary.isBlank()) {
                saveAiFact(did, AiFact.FactType.SUMMARY, summary, confidence);
            }

            log.info("🧠 AI [{}]: tags={}, summary=\"{}\"",
                    did.substring(0, Math.min(did.length(), 20)),
                    json.path("tags"),
                    summary);

        } catch (Exception e) {
            log.warn("Failed to parse AI tagging response for {}: {} | raw={}",
                    did, e.getMessage(), response);
        }
    }

    private void moderatePost(String did, String text) {
        String response = aiClient.complete(
                agentService.buildModerationSystemPrompt(), text).block();

        if (response == null || response.isBlank())
            return;

        try {
            JsonNode json = objectMapper.readTree(response);
            boolean isHarmful = json.path("isHarmful").asBoolean(false);
            double confidence = json.path("confidence").asDouble(0.0);
            String reason = json.path("reason").asText("");

            if (isHarmful && confidence >= 0.75) {
                saveAiFact(did, AiFact.FactType.WARNING, reason, confidence);
                log.warn("⚠️ AI moderation: potential harmful content from {} (confidence={}) — {}",
                        did, confidence, reason);
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI moderation response for {}: {}", did, e.getMessage());
        }
    }

    private ChannelMemory getOrCreateMemory(String did) {
        return memories.computeIfAbsent(did, k -> new ChannelMemory(contextWindowSize));
    }

    private void saveAiFact(String did, AiFact.FactType factType, String content, double confidence) {
        AiFact fact = AiFact.builder()
                .channelId(did) // On the public firehose, DID acts as channel proxy
                .sourceDid(did)
                .factType(factType)
                .content(content)
                .confidence(confidence)
                .agentDid(agentService.getAgentDid())
                .build();
        factRepository.save(fact);
    }
}
