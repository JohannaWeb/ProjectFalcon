package app.falcon.atproto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.Map;

/**
 * HTTP client for AT Protocol XRPC endpoints.
 * No official Java SDK exists; we call the XRPC API directly.
 */
@Component
public class XrpcClient {

    private final RestClient restClient;
    private final String defaultService;

    public XrpcClient(@Value("${atproto.service:https://bsky.social}") String defaultService) {
        this.defaultService = defaultService.endsWith("/") ? defaultService : defaultService + "/";
        this.restClient = RestClient.builder()
                .baseUrl(this.defaultService)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * GET /xrpc/{nsid}
     */
    public Map<String, Object> get(String nsid, Map<String, String> queryParams, String accessJwt) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("xrpc/" + nsid);
                        if (queryParams != null) {
                            queryParams.forEach(builder::queryParam);
                        }
                        return builder.build();
                    })
                    .headers(h -> {
                        if (accessJwt != null && !accessJwt.isBlank()) {
                            h.setBearerAuth(accessJwt);
                        }
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException e) {
            throw new AtprotoException(e.getStatusCode().value(), e.getResponseBodyAsString());
        }
    }

    /**
     * POST /xrpc/{nsid} with JSON body
     */
    public Map<String, Object> post(String nsid, Object body, String accessJwt) {
        try {
            return restClient.post()
                    .uri("xrpc/" + nsid)
                    .headers(h -> {
                        if (accessJwt != null && !accessJwt.isBlank()) {
                            h.setBearerAuth(accessJwt);
                        }
                    })
                    .body(body != null ? body : Collections.emptyMap())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException e) {
            throw new AtprotoException(e.getStatusCode().value(), e.getResponseBodyAsString());
        }
    }

    public String getDefaultService() {
        return defaultService;
    }
}