package app.falcon.siv.api.dto;

import java.util.Map;

/**
 * Response DTO for the GET /api/siv/intelligence endpoint.
 */
public record IntelligenceResponse(
        String status,
        Map<String, Object> activities,
        String timestamp) {
}
