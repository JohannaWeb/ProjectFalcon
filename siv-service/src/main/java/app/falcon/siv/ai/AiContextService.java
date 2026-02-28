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
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "falcon.ai.enabled", havingValue = "true", matchIfMissing = false)
public class AiContextService {

    private final FalconAiClient aiClient;
    private final AiFactRepository factRepository;
    private final SovereignAgentService agentService;
    private final AutonomousVoucher autonomousVoucher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final int contextWindowSize;
    private final long rateLimitSeconds;
    private final int maxTrackedDids;

    private final ConcurrentHashMap<String, ChannelMemory> memories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lastProcessed = new ConcurrentHashMap<>();
    private final java.util.concurrent.ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public AiContextService(
            FalconAiClient aiClient,
            AiFactRepository factRepository,
            SovereignAgentService agentService,
            AutonomousVoucher autonomousVoucher,
            @Value("${falcon.ai.context-window-size:50}") int contextWindowSize,
            @Value("${falcon.ai.rate-limit-seconds:60}") long rateLimitSeconds,
            @Value("${falcon.ai.max-tracked-dids:5000}") int maxTrackedDids) {
        this.aiClient = aiClient;
        this.factRepository = factRepository;
        this.agentService = agentService;
        this.autonomousVoucher = autonomousVoucher;
        this.contextWindowSize = contextWindowSize;
        this.rateLimitSeconds = rateLimitSeconds;
        this.maxTrackedDids = maxTrackedDids;
    }

    public void processPost(String did, String text, String eventId) {
        if (text == null || text.isBlank())
            return;

        executor.submit(() -> {
            try {
                analysePost(did, text, eventId);
            } catch (Exception e) {
                log.warn("AI analysis failed for DID {}: {}", did, e.getMessage());
            }
        });
    }

    private void analysePost(String did, String text, String eventId) {
        Instant last = lastProcessed.get(did);
        if (last != null && last.plusSeconds(rateLimitSeconds).isAfter(Instant.now())) {
            getOrCreateMemory(did).add(text);
            return;
        }

        if (memories.size() >= maxTrackedDids && !memories.containsKey(did)) {
            String evictKey = memories.keys().nextElement();
            memories.remove(evictKey);
            lastProcessed.remove(evictKey);
        }

        ChannelMemory memory = getOrCreateMemory(did);
        memory.add(text);
        lastProcessed.put(did, Instant.now());

        tagPost(did, text, memory, eventId);
        moderatePost(did, text, eventId);
    }

    private void tagPost(String did, String text, ChannelMemory memory, String eventId) {
        String userPrompt = "Analyse this AT Protocol post:\n\n%s\n\nConversation context:\n%s"
                .formatted(text, memory.toPrompt());

        String response = aiClient.complete(
                agentService.buildTaggingSystemPrompt(), userPrompt).block();

        if (response == null || response.isBlank())
            return;

        try {
            JsonNode json = objectMapper.readTree(response);
            String summary = json.path("summary").asText("");
            double confidence = json.path("confidence").asDouble(0.7);
            String reasoning = json.path("reasoning").asText("");

            json.path("tags").forEach(tagNode -> {
                String tag = tagNode.asText().toLowerCase().trim();
                if (!tag.isBlank()) {
                    saveAiFact(did, AiFact.FactType.TAG, tag, confidence, reasoning, eventId);
                }
            });

            if (!summary.isBlank()) {
                saveAiFact(did, AiFact.FactType.SUMMARY, summary, confidence, reasoning, eventId);
            }

            // Heuristic for AI-driven highlights
            if (confidence > 0.9
                    && (summary.toLowerCase().contains("contribution") || summary.toLowerCase().contains("standard"))) {
                saveAiFact(did, AiFact.FactType.HIGHLIGHT, summary, confidence,
                        "Autonomous high-value signal detection.", eventId);
            }

        } catch (Exception e) {
            log.warn("Failed to parse AI tagging response for {}: {}", did, e.getMessage());
        }
    }

    private void moderatePost(String did, String text, String eventId) {
        String response = aiClient.complete(
                agentService.buildModerationSystemPrompt(), text).block();

        if (response == null || response.isBlank())
            return;

        try {
            JsonNode json = objectMapper.readTree(response);
            boolean isHarmful = json.path("isHarmful").asBoolean(false);
            double confidence = json.path("confidence").asDouble(0.0);
            String reason = json.path("reason").asText("");
            String reasoning = json.path("reasoning").asText("");

            if (isHarmful && confidence >= 0.75) {
                saveAiFact(did, AiFact.FactType.WARNING, reason, confidence, reasoning, eventId);
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI moderation response for {}: {}", did, e.getMessage());
        }
    }

    private ChannelMemory getOrCreateMemory(String did) {
        return memories.computeIfAbsent(did, k -> new ChannelMemory(contextWindowSize));
    }

    private void saveAiFact(String did, AiFact.FactType factType, String content, double confidence, String reasoning,
            String eventId) {
        AiFact fact = AiFact.builder()
                .channelId(did)
                .sourceDid(did)
                .factType(factType)
                .content(content)
                .confidence(confidence)
                .reasoning(reasoning)
                .sourceEventId(eventId)
                .agentDid(agentService.getAgentDid())
                .build();
        factRepository.save(fact);

        autonomousVoucher.evaluateVouch(fact);
    }
}
