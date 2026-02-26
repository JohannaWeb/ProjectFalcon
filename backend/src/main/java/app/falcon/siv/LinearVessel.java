package app.falcon.siv;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class LinearVessel implements SivVessel {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getVesselType() {
        return "linear";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchActivity(String token, Map<String, Object> config) {
        String url = "https://api.linear.app/graphql";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Simple GraphQL query to get recently updated issues
        String query = "{\"query\": \"{ issues(first: 10, filter: { team: { name: { eq: \\\"" + config.get("team")
                + "\\\" } } }) { nodes { id title updatedAt state { name } } } }\"}";

        HttpEntity<String> entity = new HttpEntity<>(query, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            Map<String, Object> issues = (Map<String, Object>) data.get("issues");
            return (List<Map<String, Object>>) issues.get("nodes");
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
