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
 * Vercel SIV vessel — fetches recent deployments via the Vercel REST API.
 * Requires config: { "teamId": "...", "projectId": "..." } stored in
 * UserSivConfig.
 * The encryptedToken field is decrypted upstream before being passed here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VercelVessel implements SivVessel {

    private final WebClient.Builder webClientBuilder;

    @Override
    public String getType() {
        return "vercel";
    }

    @Override
    public List<?> fetchActivity(UserSivConfig config) {
        String token = config.getEncryptedToken(); // decrypted by SivIntelligenceService
        String projectId = (String) config.getConfig().get("projectId");

        if (token == null || token.isBlank()) {
            log.warn("VercelVessel: no token configured for userDid={}", config.getUserDid());
            return List.of();
        }

        String uri = projectId != null
                ? "https://api.vercel.com/v6/deployments?projectId=" + projectId + "&limit=10"
                : "https://api.vercel.com/v6/deployments?limit=10";

        return webClientBuilder.build()
                .get()
                .uri(uri)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .map(response -> {
                    Object deployments = response.get("deployments");
                    return deployments instanceof List<?> list ? list : List.of();
                })
                .block();
    }
}
