package app.falcon.siv;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class GitHubVessel implements SivVessel {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getVesselType() {
        return "github";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchActivity(String token, Map<String, Object> config) {
        String repo = (String) config.get("repo"); // e.g., "owner/repo"
        if (repo == null) return Collections.emptyList();

        String url = "https://api.github.com/repos/" + repo + "/events?per_page=10";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + token);
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("User-Agent", "ProjectFalcon-SIV");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            return (List<Map<String, Object>>) response.getBody();
        } catch (Exception e) {
            // In a real app, we'd log this properly
            return Collections.emptyList();
        }
    }
}
