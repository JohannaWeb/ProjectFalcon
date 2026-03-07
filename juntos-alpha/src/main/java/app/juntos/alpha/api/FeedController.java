package app.juntos.alpha.api;

import app.juntos.alpha.auth.AtprotoAuthFilter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

@RestController
@RequestMapping("/xrpc")
@Slf4j
public class FeedController {

    private static final String BSKY = "https://bsky.social";

    private final RestTemplate http = new RestTemplate();

    // Raw bytes — avoids any charset or JSON serialization issues
    private final Cache<String, byte[]> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .maximumSize(10_000)
            .build();

    @GetMapping("/app.juntos.feed.getTimeline")
    public void getTimeline(
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(required = false) String cursor,
            HttpServletRequest req,
            HttpServletResponse resp) throws IOException {

        String did = (String) req.getAttribute(AtprotoAuthFilter.VIEWER_DID_ATTR);
        String cacheKey = did + ":" + limit + ":" + (cursor != null ? cursor : "");

        byte[] cached = cache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Feed cache HIT for {}", did);
            write(resp, cached);
            return;
        }

        log.debug("Feed cache MISS for {}", did);
        String url = BSKY + "/xrpc/app.bsky.feed.getTimeline?limit=" + limit;
        if (cursor != null) url += "&cursor=" + cursor;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", req.getHeader("Authorization"));
        try {
            ResponseEntity<byte[]> upstream = http.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
            byte[] body = upstream.getBody();
            if (body != null) {
                cache.put(cacheKey, body);
                write(resp, body);
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_GATEWAY);
            }
        } catch (Exception e) {
            log.warn("Timeline proxy failed for {}: {}", did, e.getMessage());
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY);
        }
    }

    private void write(HttpServletResponse resp, byte[] body) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentLength(body.length);
        resp.getOutputStream().write(body);
    }
}
