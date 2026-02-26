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
 * Jira SIV vessel — fetches issues assigned to the user via the Jira REST API
 * v3.
 * Requires config: { "cloudId": "...", "email": "..." } stored in
 * UserSivConfig.
 * The encryptedToken field (API token) is decrypted upstream before being
 * passed here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JiraVessel implements SivVessel {

    private final WebClient.Builder webClientBuilder;

    @Override
    public String getType() {
        return "jira";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<?> fetchActivity(UserSivConfig config) {
        String apiToken = config.getEncryptedToken(); // decrypted upstream
        String cloudId = (String) config.getConfig().get("cloudId");
        String email = (String) config.getConfig().get("email");

        if (apiToken == null || apiToken.isBlank() || cloudId == null || email == null) {
            log.warn("JiraVessel: missing token, cloudId or email for userDid={}", config.getUserDid());
            return List.of();
        }

        // Jira Cloud REST API v3 — issues assigned to the authenticated user
        String url = "https://api.atlassian.com/ex/jira/" + cloudId
                + "/rest/api/3/search?jql=assignee%3DcurrentUser()%20ORDER%20BY%20updated%20DESC&maxResults=20";

        String basicAuth = java.util.Base64.getEncoder()
                .encodeToString((email + ":" + apiToken).getBytes());

        return webClientBuilder.build()
                .get()
                .uri(url)
                .header("Authorization", "Basic " + basicAuth)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .map(response -> {
                    Object issues = response.get("issues");
                    return issues instanceof List<?> list ? list : List.of();
                })
                .doOnError(e -> log.warn("JiraVessel: API call failed for userDid={}: {}", config.getUserDid(),
                        e.getMessage()))
                .onErrorReturn(List.of())
                .block();
    }
}
