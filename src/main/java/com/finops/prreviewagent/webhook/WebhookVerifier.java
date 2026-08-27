package com.finops.prreviewagent.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Verifies that an incoming webhook genuinely came from GitHub.
 *
 * How it works:
 *  - You and GitHub share a secret (github.webhook.secret).
 *  - GitHub signs every payload by computing HMAC-SHA256(secret, payload)
 *    and sends the result in the "X-Hub-Signature-256" header,
 *    formatted as "sha256=<hex>".
 *  - We recompute the same HMAC on our side and compare. If they match,
 *    the request really came from GitHub and wasn't tampered with.
 */
@Component
public class WebhookVerifier {

    private final String secret;

    // Spring injects the secret from application.yml (github.webhook.secret).
    public WebhookVerifier(@Value("${github.webhook.secret}") String secret) {
        this.secret = secret;
    }

    public boolean isValid(String payload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String expected = "sha256=" + hmacSha256(payload);
        // Constant-time comparison to avoid timing attacks.
        return constantTimeEquals(expected, signatureHeader);
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : raw) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
