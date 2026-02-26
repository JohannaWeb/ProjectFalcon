package app.falcon.siv;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class VercelVessel implements SivVessel {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getVesselType() {
        return "vercel";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchActivity(String token, Map<String, Object> config) {
        String projectId = (String) config.get("projectId");
        String teamId = (String) config.get("teamId"); // Optional
        if (projectId == null)
            return Collections.emptyList();

        String url = "https://api.vercel.com/v6/deployments?projectId=" + projectId
                + (teamId != null ? "&teamId=" + teamId : "") + "&limit=10";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return (List<Map<String, Object>>) response.getBody().get("deployments");
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
