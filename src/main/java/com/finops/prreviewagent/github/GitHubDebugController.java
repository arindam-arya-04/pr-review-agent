package com.finops.prreviewagent.github;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import io.jsonwebtoken.Jwts;

@RestController
public class GitHubDebugController {

    private final String appId;
    private final String installationId;
    private final String privateKeyPath;
    private final RestClient restClient = RestClient.create();

    public GitHubDebugController(
            @Value("${github.app.id}") String appId,
            @Value("${github.app.installation-id}") String installationId,
            @Value("${github.app.private-key-path}") String privateKeyPath) {
        this.appId = appId;
        this.installationId = installationId;
        this.privateKeyPath = privateKeyPath;
    }

    
    @GetMapping("/debug-token")
    public String debugToken() {
        String jwt = createJwt();
        return restClient.post()
                .uri("https://api.github.com/app/installations/{id}/access_tokens", installationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .retrieve()
                .body(String.class);
    }


    @GetMapping("/debug-app")
    public String debugApp() {
        String jwt = createJwt();
        return restClient.get()
                .uri("https://api.github.com/app")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .retrieve()
                .body(String.class);
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
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new RuntimeException("Key load failed: " + e.getMessage(), e);
        }
    }
}
