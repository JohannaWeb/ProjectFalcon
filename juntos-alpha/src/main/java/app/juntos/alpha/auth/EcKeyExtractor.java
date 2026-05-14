package app.juntos.alpha.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Extracts an EC public key from an AT Protocol DID document's verificationMethod array.
 * Supports both JWK (publicKeyJwk) and Multibase (publicKeyMultibase) encoding.
 */
@ApplicationScoped
@Slf4j
public class EcKeyExtractor {

    @Inject
    MultibaseKeyDecoder multibaseDecoder;

    @SuppressWarnings("unchecked")
    public ECPublicKey extract(Map<String, Object> didDoc, String jwtCurve, String jcaCurve, String kid)
            throws Exception {
        List<Map<String, Object>> vms = (List<Map<String, Object>>) didDoc.get("verificationMethod");
        if (vms == null || vms.isEmpty())
            throw new IllegalArgumentException("No verificationMethod in DID document");

        String did = (String) didDoc.get("id");

        // Kid-targeted lookup first
        if (kid != null && !kid.isBlank()) {
            for (Map<String, Object> vm : vms) {
                if (kidMatches(kid, (String) vm.get("id"), did)) {
                    ECPublicKey key = extractFromVm(vm, jwtCurve, jcaCurve);
                    if (key != null) return key;
                    throw new IllegalArgumentException("kid matched but curve mismatch: expected " + jwtCurve);
                }
            }
            log.warn("[KEY] kid '{}' not found — scanning all verificationMethods", kid);
        }

        // Curve-scan fallback
        for (Map<String, Object> vm : vms) {
            ECPublicKey key = extractFromVm(vm, jwtCurve, jcaCurve);
            if (key != null) return key;
        }

        throw new IllegalArgumentException("No " + jwtCurve + " key found in DID document");
    }

    @SuppressWarnings("unchecked")
    private ECPublicKey extractFromVm(Map<String, Object> vm, String jwtCurve, String jcaCurve)
            throws Exception {
        // JWK path
        Map<String, Object> jwk = (Map<String, Object>) vm.get("publicKeyJwk");
        if (jwk != null && jwtCurve.equals(jwk.get("crv"))) {
            return buildFromJwk((String) jwk.get("x"), (String) jwk.get("y"), jcaCurve);
        }

        // Multibase path
        String multibase = (String) vm.get("publicKeyMultibase");
        if (multibase != null) {
            return multibaseDecoder.decode(multibase, jwtCurve, jcaCurve);
        }

        return null;
    }

    private ECPublicKey buildFromJwk(String x, String y, String curve) throws Exception {
        BigInteger bx = new BigInteger(1, Base64.getUrlDecoder().decode(x));
        BigInteger by = new BigInteger(1, Base64.getUrlDecoder().decode(y));
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(curve);
        ECNamedCurveSpec jcaSpec = new ECNamedCurveSpec(curve, spec.getCurve(), spec.getG(), spec.getN(), spec.getH());
        return (ECPublicKey) KeyFactory.getInstance("EC", "BC")
                .generatePublic(new ECPublicKeySpec(new ECPoint(bx, by), jcaSpec));
    }

    private boolean kidMatches(String kid, String vmId, String did) {
        if (vmId == null) return false;
        if (kid.equals(vmId)) return true;
        if (kid.startsWith("#") && vmId.endsWith(kid)) return true;
        if (vmId.startsWith("#") && did != null && kid.equals(did + vmId)) return true;
        return false;
    }
}
