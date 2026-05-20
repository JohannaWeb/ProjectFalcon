package app.juntos.alpha.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates AT Protocol JWT verification:
 * extract claims → resolve candidate signing DIDs → resolve DID document → verify signature.
 *
 * Implements a permissive fallback for PDS audience tokens whose DID document cannot be
 * fetched — consistent with the original AT Protocol auth behaviour in this codebase.
 */
@ApplicationScoped
@Slf4j
public class AtprotoTokenVerifier implements TokenVerifier {

    @Inject JwtClaimsExtractor claimsExtractor;
    @Inject SigningDidResolver signingDidResolver;
    @Inject DidResolver didResolver;
    @Inject JwtSignatureVerifier signatureVerifier;

    @Override
    public String verify(String token) throws Exception {
        JwtClaims claims = claimsExtractor.extract(token);
        List<String> candidates = signingDidResolver.resolve(claims);

        Exception lastError = null;
        for (String candidate : candidates) {
            try {
                Map<String, Object> didDoc = didResolver.resolve(candidate);
                if (didDoc == null) {
                    if (isPdsAudience(candidate, claims)) {
                        log.warn("[VERIFIER] PDS DID doc null — permissive fallback for {}", candidate);
                        return claims.sub();
                    }
                    log.warn("[VERIFIER] DID doc null for {}, skipping", candidate);
                    continue;
                }
                signatureVerifier.verify(token, claims, didDoc);
                log.debug("[VERIFIER] verified via {}", candidate);
                return claims.sub();
            } catch (Exception e) {
                log.debug("[VERIFIER] candidate {} failed: {}", candidate, e.getMessage());
                if (isPdsAudience(candidate, claims) && isResolutionFailure(e)) {
                    log.warn("[VERIFIER] PDS resolution error — permissive fallback for {}", candidate);
                    return claims.sub();
                }
                lastError = e;
            }
        }

        throw new Exception("Authentication failed — no candidate DID verified. Last error: "
                + (lastError != null ? lastError.getMessage() : "none"));
    }

    private boolean isPdsAudience(String candidate, JwtClaims claims) {
        return candidate.equals(claims.aud());
    }

    private boolean isResolutionFailure(Exception e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("Cannot resolve") || msg.contains("404")
                || msg.contains("resolve") || msg.contains("timed out"));
    }
}
