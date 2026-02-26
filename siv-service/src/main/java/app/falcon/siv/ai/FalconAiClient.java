package app.falcon.siv.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible REST client for Falcon's AI integration.
 *
 * <p>
 * Supports any provider that speaks the OpenAI Chat Completions API:
 * <ul>
 * <li><b>Ollama (local/sovereign)</b> — {@code http://localhost:11434/v1} with
 * api-key {@code "local"}</li>
 * <li><b>Gemini</b> —
 * {@code https://generativelanguage.googleapis.com/v1beta/openai/} with a
 * Gemini API key</li>
 * <li><b>OpenAI</b> — {@code https://api.openai.com/v1} with an OpenAI API
 * key</li>
 * </ul>
 * Switch between them with a single change to {@code falcon.ai.base-url} and
 * {@code falcon.ai.api-key}.
 * </p>
 */
@Component
@Slf4j
public class FalconAiClient {

    private final WebClient webClient;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FalconAiClient(
            WebClient.Builder webClientBuilder,
            @Value("${falcon.ai.base-url:http://localhost:11434/v1}") String baseUrl,
            @Value("${falcon.ai.api-key:local}") String apiKey,
            @Value("${falcon.ai.model:llama3.2}") String model) {
        this.model = model;
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Sends a chat completion request.
     *
     * @param systemPrompt instructions to the model (persona, output format)
     * @param userPrompt   the actual content to analyse
     * @return the model's text response, or empty on error
     */
    public Mono<String> complete(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "max_tokens", 256,
                "temperature", 0.3);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode root = objectMapper.readTree(response);
                        return root.path("choices").get(0)
                                .path("message").path("content").asText();
                    } catch (Exception e) {
                        log.warn("Failed to parse AI response: {}", e.getMessage());
                        return "";
                    }
                })
                .onErrorResume(e -> {
                    log.error("AI API call failed: {}", e.getMessage());
                    return Mono.just("");
                });
    }
}
