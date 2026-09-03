package com.finops.prreviewagent.github;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import io.jsonwebtoken.Jwts;


@Service
public class GitHubAuthService {

    private final String appId;
    private final String defaultInstallationId;
    private final String privateKeyPath;
    private final RestClient restClient = RestClient.create();

    // One cache entry per installation id.
    private record CachedToken(String token, Instant expiry) {}
    private final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    private static final long REFRESH_MARGIN_SECONDS = 300; // refresh 5 min early

    public GitHubAuthService(
            @Value("${github.app.id}") String appId,
            @Value("${github.app.installation-id}") String defaultInstallationId,
            @Value("${github.app.private-key-path}") String privateKeyPath) {
        this.appId = appId;
        this.defaultInstallationId = defaultInstallationId;
        this.privateKeyPath = privateKeyPath;
    }

   
    public String getInstallationToken() {
        return getInstallationToken(defaultInstallationId);
    }

    
    public String getInstallationToken(String installationId) {
        Instant now = Instant.now();
        CachedToken cached = tokenCache.get(installationId);
        if (cached != null && now.isBefore(cached.expiry().minusSeconds(REFRESH_MARGIN_SECONDS))) {
            return cached.token();
        }
        return refreshToken(installationId);
    }

    @SuppressWarnings("unchecked")
    private synchronized String refreshToken(String installationId) {
        // Double-check inside the lock in case another thread just refreshed.
        CachedToken cached = tokenCache.get(installationId);
        if (cached != null && Instant.now().isBefore(cached.expiry().minusSeconds(REFRESH_MARGIN_SECONDS))) {
            return cached.token();
        }

        String jwt = createJwt();
        Map<String, Object> response = restClient.post()
                .uri("https://api.github.com/app/installations/{id}/access_tokens", installationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("token") == null) {
            throw new RuntimeException("Failed to obtain installation token for id " + installationId);
        }

        String token = (String) response.get("token");
        Object expiresAt = response.get("expires_at");
        Instant expiry = expiresAt != null
                ? Instant.parse(expiresAt.toString())
                : Instant.now().plusSeconds(3600);
        tokenCache.put(installationId, new CachedToken(token, expiry));
        System.out.println(">>> Minted token for installation " + installationId + " (expires " + expiry + ")");
        return token;
    }

    private String createJwt() {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(appId)
                .issuedAt(Date.from(now.minusSeconds(60)))
                .expiration(Date.from(now.plusSeconds(9 * 60)))
                .signWith(loadPrivateKey())
                .compact();
    }

    private PrivateKey loadPrivateKey() {
        try {
            String pem = Files.readString(Path.of(privateKeyPath));
            String base64 = pem
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load GitHub private key: " + e.getMessage(), e);
        }
    }
}
