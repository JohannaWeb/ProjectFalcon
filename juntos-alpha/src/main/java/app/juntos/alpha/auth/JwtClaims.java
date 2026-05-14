package app.juntos.alpha.auth;

import java.util.Date;

/**
 * Immutable value object holding the decoded claims from an AT Protocol JWT.
 */
public record JwtClaims(
        String sub,
        String iss,
        String aud,
        String alg,
        String kid,
        Date exp,
        Date nbf
) {}
