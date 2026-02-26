package app.falcon.siv.vessels;

import app.falcon.core.domain.UserSivConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubVessel implements SivVessel {

    private final WebClient.Builder webClientBuilder;

    @Override
    public String getType() {
        return "github";
    }

    @Override
    public List<?> fetchActivity(UserSivConfig config) {
        String username = (String) config.getConfig().get("username");
        if (username == null || username.isBlank()) {
            log.warn("GitHubVessel: no username configured for userDid={}", config.getUserDid());
            return List.of();
        }

        String url = "https://api.github.com/users/" + username + "/events";

        return webClientBuilder.build()
                .get()
                .uri(url)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                })
                .block();
    }
}
