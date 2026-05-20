package app.juntos.alpha.auth;

/**
 * Verifies an AT Protocol bearer token and returns the authenticated subject DID.
 * Implementations may use different strategies (AT Protocol JWT, service auth, etc.).
 */
public interface TokenVerifier {
    /**
     * @param token raw JWT (without "Bearer " prefix)
     * @return the verified subject DID
     * @throws Exception if verification fails for any reason
     */
    String verify(String token) throws Exception;
}
