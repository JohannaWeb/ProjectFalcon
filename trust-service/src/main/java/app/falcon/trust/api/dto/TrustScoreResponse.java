package app.falcon.trust.api.dto;

/**
 * Response DTO for the GET /api/trust/score/{targetDid} endpoint.
 */
public record TrustScoreResponse(
        String targetDid,
        double score,
        String status) {
    public static TrustScoreResponse of(String targetDid, double score) {
        String status = score > 0.5 ? "Trusted" : score < -0.5 ? "Distrusted" : "Neutral";
        return new TrustScoreResponse(targetDid, score, status);
    }
}
