package app.juntos.alpha.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Determines the ordered list of DIDs that may have signed a given JWT.
 * Encapsulates the AT Protocol rules for iss/aud/sub priority without
 * knowing anything about cryptography or HTTP.
 */
@ApplicationScoped
@Slf4j
public class SigningDidResolver {

    @Inject
    PdsDiscoveryService pdsDiscovery;

    /**
     * Returns candidate signing DIDs to try in priority order.
     *
     * <ul>
     *   <li>Service auth / session tokens: iss is the PDS URL → discover its DID via describeServer.</li>
     *   <li>Self-signed tokens (iss is a DID): use iss directly.</li>
     *   <li>No iss: try aud (if a DID) then sub.</li>
     * </ul>
     */
    public List<String> resolve(JwtClaims claims) {
        List<String> candidates = new ArrayList<>();
        String iss = claims.iss();
        String sub = claims.sub();
        String aud = claims.aud();

        if (iss != null) {
            candidates.add(issToSigningDid(iss));
        } else {
            if (aud != null && aud.startsWith("did:")) candidates.add(aud);
            if (!candidates.contains(sub)) candidates.add(sub);
        }

        log.debug("[SIGNING-DID] candidates for sub={} iss={}: {}", sub, iss, candidates);
        return candidates;
    }

    private String issToSigningDid(String iss) {
        if (iss.startsWith("did:")) return iss;
        String host = iss.replaceFirst("https?://", "").split("/")[0];
        return pdsDiscovery.discover(host);
    }
}
