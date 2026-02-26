package app.falcon.siv;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class JiraVessel implements SivVessel {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getVesselType() {
        return "jira";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchActivity(String token, Map<String, Object> config) {
        String host = (String) config.get("host"); // e.g., "your-domain.atlassian.net"
        String project = (String) config.get("project");
        if (host == null || project == null)
            return Collections.emptyList();

        String url = "https://" + host + "/rest/api/3/search?jql=project=" + project
                + " ORDER BY updated DESC&maxResults=10";

        HttpHeaders headers = new HttpHeaders();
        // Jira usually requires Basic Auth with email:token base64 encoded, but we'll
        // assume the user provides the full header or token
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            List<Map<String, Object>> issues = (List<Map<String, Object>>) response.getBody().get("issues");
            return issues;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
