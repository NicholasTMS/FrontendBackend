package com.kallavaninc.backend.Services;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class GoogleTokenVerifier {
    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}";

    private final RestTemplate restTemplate;
    private final String clientId;

    public GoogleTokenVerifier(@Value("${google.oauth.client-id}") String clientId) {
        this.restTemplate = new RestTemplate();
        this.clientId = clientId;
    }

    public GoogleTokenInfo verify(String idToken) {
        try {
            ResponseEntity<GoogleTokenInfo> response = restTemplate.getForEntity(
                TOKEN_INFO_URL,
                GoogleTokenInfo.class,
                idToken
            );
            GoogleTokenInfo body = response.getBody();
            if (body == null || body.sub() == null || body.sub().isBlank()) {
                throw new IllegalArgumentException("Invalid token payload");
            }
            boolean enforceAudience = clientId != null
                && !clientId.isBlank()
                && !"YOUR_GOOGLE_CLIENT_ID".equals(clientId);
            if (enforceAudience && body.audience() != null && !clientId.equals(body.audience())) {
                throw new IllegalArgumentException("Token audience mismatch");
            }
            return body;
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Failed to verify Google ID token", ex);
        }
    }

    public record GoogleTokenInfo(
        @JsonProperty("sub") String sub,
        @JsonProperty("email") String email,
        @JsonProperty("email_verified") boolean emailVerified,
        @JsonProperty("name") String name,
        @JsonProperty("given_name") String givenName,
        @JsonProperty("family_name") String familyName,
        @JsonProperty("aud") String audience,
        @JsonProperty("iss") String issuer,
        @JsonProperty("exp") String exp
    ) {
    }
}



