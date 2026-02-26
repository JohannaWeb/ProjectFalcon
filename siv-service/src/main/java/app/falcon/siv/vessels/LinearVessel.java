package app.falcon.siv.vessels;

import app.falcon.core.domain.UserSivConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Linear SIV vessel — fetches recent issues assigned to the user via the Linear
 * GraphQL API.
 * Requires config: { "teamId": "..." } stored in UserSivConfig.
 * The encryptedToken field is decrypted upstream before being passed here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LinearVessel implements SivVessel {

    private final WebClient.Builder webClientBuilder;

    @Override
    public String getType() {
        return "linear";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<?> fetchActivity(UserSivConfig config) {
        String token = config.getEncryptedToken(); // decrypted upstream

        if (token == null || token.isBlank()) {
            log.warn("LinearVessel: no token configured for userDid={}", config.getUserDid());
            return List.of();
        }

        String query = """
                {
                  "query": "{ viewer { assignedIssues(first: 20, orderBy: updatedAt) { nodes { id title state { name } updatedAt url } } } }"
                }
                """;

        return webClientBuilder.build()
                .post()
                .uri("https://api.linear.app/graphql")
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .bodyValue(query)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .map(response -> {
                    try {
                        Map<String, Object> data = (Map<String, Object>) response.get("data");
                        Map<String, Object> viewer = (Map<String, Object>) data.get("viewer");
                        Map<String, Object> assignedIssues = (Map<String, Object>) viewer.get("assignedIssues");
                        Object nodes = assignedIssues.get("nodes");
                        return nodes instanceof List<?> list ? list : List.of();
                    } catch (Exception e) {
                        log.warn("LinearVessel: failed to parse response for userDid={}: {}", config.getUserDid(),
                                e.getMessage());
                        return List.of();
                    }
                })
                .block();
    }
}
