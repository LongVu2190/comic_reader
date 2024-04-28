package com.api.comic_reader.services;

import com.api.comic_reader.dtos.requests.IntrospectRequest;
import com.api.comic_reader.dtos.requests.LoginRequest;
import com.api.comic_reader.dtos.responses.IntrospectResponse;
import com.api.comic_reader.dtos.responses.AuthResponse;
import com.api.comic_reader.entities.UserEntity;
import com.api.comic_reader.entities.InvalidatedTokenEntity;
import com.api.comic_reader.exception.AppException;
import com.api.comic_reader.exception.ErrorCode;
import com.api.comic_reader.repositories.UserRepository;
import com.api.comic_reader.repositories.InvalidatedTokenRepository;

import lombok.AllArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final InvalidatedTokenRepository invalidatedTokenRepository;

    public AuthResponse login(LoginRequest loginRequest) throws AppException {
        AuthResponse loginResponse = null;
        try {
            Optional<UserEntity> comicUserOptional = userRepository.findByUsername(loginRequest.getUsername());
            if (comicUserOptional.isPresent()) {
                UserEntity comicUser = comicUserOptional.get();
                if (passwordEncoder.matches(loginRequest.getPassword(), comicUser.getPassword())) {
                    String jwtToken = "";
                    jwtToken = jwtService.generateToken(comicUser);
                    loginResponse = AuthResponse.builder()
                            .id(comicUser.getId())
                            .token(jwtToken)
                            .authenticated(true)
                            .build();
                }
            }
        } catch (AppException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        if (loginResponse == null) {
            throw new AppException(ErrorCode.WRONG_EMAIL_OR_PASSWORD);
        }
        return loginResponse;
    }

    public void logout(String token) throws Exception {
        var signedToken = jwtService.verifyToken(token);

        String jit = signedToken.getJWTClaimsSet().getJWTID();

        Date expirationTime = signedToken.getJWTClaimsSet().getExpirationTime();

        InvalidatedTokenEntity invalidatedToken = InvalidatedTokenEntity.builder()
                .id(jit)
                .expirationTime(expirationTime)
                .build();

        try {
            invalidatedTokenRepository.save(invalidatedToken);
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    public AuthResponse refreshToken(String token) throws AppException {
        try {
            var signedToken = jwtService.verifyToken(token);

            var jit = signedToken.getJWTClaimsSet().getJWTID();

            Date expirationTime = signedToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedTokenEntity invalidatedToken = InvalidatedTokenEntity.builder()
                    .id(jit)
                    .expirationTime(expirationTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);

            var username = signedToken.getJWTClaimsSet().getSubject();

            Optional<UserEntity> userOptional = userRepository.findByUsername(username);

            if (userOptional.isEmpty()) {
                throw new AppException(ErrorCode.USER_NOT_FOUND);
            }

            UserEntity currentUser = userOptional.get();
            
            var newToken = jwtService.generateToken(currentUser);

            return AuthResponse.builder()
                    .id(currentUser.getId())
                    .token(newToken)
                    .authenticated(true)
                    .build();
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }

    public IntrospectResponse introspect(IntrospectRequest request) throws AppException {
        var token = request.getToken();

        if (token == null) {
            throw new AppException(ErrorCode.TOKEN_IS_REQUIRED);
        }

        return jwtService.introspect(token);
    }
}
