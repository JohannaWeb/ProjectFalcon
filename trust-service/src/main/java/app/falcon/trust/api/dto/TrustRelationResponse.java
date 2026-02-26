package app.falcon.trust.api.dto;

/**
 * Response DTO for the POST /api/trust/relation endpoint.
 */
public record TrustRelationResponse(String status, String message) {
    public static TrustRelationResponse success(String sourceDid, String type, String targetDid) {
        return new TrustRelationResponse("success",
                "Relation %s -> %s -> %s recorded.".formatted(sourceDid, type, targetDid));
    }
}
