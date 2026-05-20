package app.juntos.alpha.api;

import app.juntos.alpha.auth.PlcService;
import app.juntos.alpha.domain.GoogleUser;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Path("/api/auth/google")
@Slf4j
public class GoogleAuthController {

    @ConfigProperty(name = "google.oauth.client-id", defaultValue = "")
    String clientId;

    @ConfigProperty(name = "google.oauth.client-secret", defaultValue = "")
    String clientSecret;

    @ConfigProperty(name = "google.oauth.redirect-uri",
            defaultValue = "http://localhost:8080/api/auth/google/callback")
    String redirectUri;

    @ConfigProperty(name = "google.oauth.frontend-url", defaultValue = "http://localhost:5173")
    String frontendUrl;

    @Inject
    PlcService plcService;

    @GET
    public Response initiateOAuth() {
        if (clientId.isBlank()) {
            return Response.status(503).entity("Google OAuth not configured").build();
        }
        String state = generateState();
        String url = "https://accounts.google.com/o/oauth2/v2/auth?"
                + "client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&response_type=code"
                + "&scope=" + enc("openid email profile")
                + "&state=" + enc(state)
                + "&access_type=offline"
                + "&prompt=select_account";
        return Response.seeOther(URI.create(url))
                .cookie(new jakarta.ws.rs.core.NewCookie.Builder("oauth_state")
                        .value(state).path("/").maxAge(300).httpOnly(true).build())
                .build();
    }

    @GET
    @Path("/callback")
    @Transactional
    public Response handleCallback(
            @QueryParam("code") String code,
            @QueryParam("state") String state,
            @QueryParam("error") String oauthError,
            @CookieParam("oauth_state") String stateCookie
    ) {
        if (oauthError != null) {
            return redirectWithError("Google sign-in cancelled");
        }
        if (state == null || !state.equals(stateCookie)) {
            return redirectWithError("Invalid OAuth state");
        }
        if (code == null || code.isBlank()) {
            return redirectWithError("Missing authorization code");
        }

        try {
            // Exchange code for tokens
            String tokenJson = exchangeCodeForToken(code);
            String accessToken = extractField(tokenJson, "access_token");

            // Get user info
            String userJson = fetchUserInfo(accessToken);
            String googleSub = extractField(userJson, "sub");
            String email = extractField(userJson, "email");

            if (googleSub == null || googleSub.isBlank()) {
                return redirectWithError("Could not retrieve Google user info");
            }

            // Find or create Google user with DID:PLC
            GoogleUser user = GoogleUser.findBySub(googleSub);
            if (user == null) {
                log.info("[GOOGLE-AUTH] New user, creating DID:PLC for {}", email);
                KeyPair keyPair = plcService.generateP256KeyPair();
                String did = plcService.createPlcDid(keyPair);
                user = new GoogleUser();
                user.googleSub = googleSub;
                user.email = email != null ? email : googleSub;
                user.did = did;
                user.signingKeyPkcs8 = plcService.privateKeyToPkcs8Base64(keyPair.getPrivate());
                user.persist();
                log.info("[GOOGLE-AUTH] Created user {} with DID {}", email, did);
            } else {
                log.info("[GOOGLE-AUTH] Existing user {} with DID {}", email, user.did);
            }

            String jwt = plcService.issueJwt(user.did, user.signingKeyPkcs8);
            String redirectUrl = frontendUrl + "/?google_token=" + enc(jwt);
            return Response.seeOther(URI.create(redirectUrl)).build();

        } catch (Exception e) {
            log.error("[GOOGLE-AUTH] Callback failed", e);
            return redirectWithError("Authentication failed: " + e.getMessage());
        }
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private String exchangeCodeForToken(String code) throws Exception {
        String body = "code=" + enc(code)
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret)
                + "&redirect_uri=" + enc(redirectUri)
                + "&grant_type=authorization_code";
        HttpClient http = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("Token exchange failed: " + resp.body());
        }
        return resp.body();
    }

    private String fetchUserInfo(String accessToken) throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://openidconnect.googleapis.com/v1/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("Userinfo failed: " + resp.body());
        }
        return resp.body();
    }

    /** Minimal JSON field extractor (avoids adding a JSON dep for two fields). */
    private String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }

    private String generateState() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private Response redirectWithError(String msg) {
        String url = frontendUrl + "/?auth_error=" + enc(msg);
        return Response.seeOther(URI.create(url)).build();
    }
}
