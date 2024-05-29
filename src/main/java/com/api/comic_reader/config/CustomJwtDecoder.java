package com.api.comic_reader.config;

import java.util.Objects;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.api.comic_reader.dtos.requests.TokenRequest;
import com.api.comic_reader.services.AuthenticationService;

@Component
public class CustomJwtDecoder implements JwtDecoder {
    @Value("${jwt.signer-key}")
    private String signerKey;

    @Autowired
    private AuthenticationService authenticationService;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    // This method decodes the JWT token
    @Override
    public Jwt decode(String token) {
        try {
            // Introspect the token to check its validity
            var response = authenticationService.introspect(
                    TokenRequest.builder().token(token).build());

            // If the token is not valid, return null
            if (!response.isValid()) return null;
        } catch (JwtException e) {
            // If there is an exception during introspection, return null
            return null;
        }

        // If the NimbusJwtDecoder is not initialized, initialize it
        if (Objects.isNull(nimbusJwtDecoder)) {
            SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS256");
            nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
        }

        // Decode the token using the NimbusJwtDecoder and return the result
        return nimbusJwtDecoder.decode(token);
    }
}
