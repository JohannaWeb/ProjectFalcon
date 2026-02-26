package app.falcon.siv.vessels;

import app.falcon.core.domain.UserSivConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GitHubVessel implements SivVessel {
    private final RestTemplate restTemplate;

    @Override
    public String getType() {
        return "github";
    }

    @Override
    public List<?> fetchActivity(UserSivConfig config) {
        String url = "https://api.github.com/users/" + config.getConfig().get("username") + "/events";
        return restTemplate.getForObject(url, List.class);
    }
}
