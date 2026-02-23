package app.falcon.atproto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * HTTP client for AT Protocol XRPC endpoints.
 * No official Java SDK exists; we call the XRPC API directly.
 * @see <a href="https://atproto.com/specs/xrpc">XRPC spec</a>
 */
@Component
public class XrpcClient {

    private final WebClient webClient;
    private final String defaultService;

    public XrpcClient(
            WebClient.Builder builder,
            @Value("${atproto.service:https://bsky.social}") String defaultService) {
        this.defaultService = defaultService.endsWith("/") ? defaultService : defaultService + "/";
        this.webClient = builder
                .baseUrl(this.defaultService)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * GET /xrpc/{nsid}
     */
    public Mono<Map<String, Object>> get(String nsid, Map<String, String> queryParams, String accessJwt) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (queryParams != null) {
            queryParams.forEach(params::add);
        }
        String uri = UriComponentsBuilder.fromPath("xrpc/" + nsid)
                .queryParams(params)
                .toUriString();
        var spec = webClient.get()
                .uri(uri)
                .headers(h -> {
                    if (accessJwt != null && !accessJwt.isBlank()) {
                        h.setBearerAuth(accessJwt);
                    }
                });
        return spec.retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorMap(WebClientResponseException.class, e -> new AtprotoException(e.getStatusCode().value(), e.getResponseBodyAsString()));
    }

    /**
     * POST /xrpc/{nsid} with JSON body
     */
    public Mono<Map<String, Object>> post(String nsid, Object body, String accessJwt) {
        var spec = webClient.post()
                .uri("xrpc/" + nsid)
                .headers(h -> {
                    if (accessJwt != null && !accessJwt.isBlank()) {
                        h.setBearerAuth(accessJwt);
                    }
                })
                .bodyValue(body != null ? body : java.util.Collections.emptyMap());
        return spec.retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .onErrorMap(WebClientResponseException.class, e -> new AtprotoException(e.getStatusCode().value(), e.getResponseBodyAsString()));
    }

    public String getDefaultService() {
        return defaultService;
    }
}
