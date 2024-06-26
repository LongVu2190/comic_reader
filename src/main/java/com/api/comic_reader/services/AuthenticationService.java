package com.api.comic_reader.services;

import java.util.Date;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.api.comic_reader.dtos.requests.LoginRequest;
import com.api.comic_reader.dtos.requests.RegisterRequest;
import com.api.comic_reader.dtos.requests.TokenRequest;
import com.api.comic_reader.dtos.responses.AuthResponse;
import com.api.comic_reader.dtos.responses.IntrospectResponse;
import com.api.comic_reader.entities.InvalidatedTokenEntity;
import com.api.comic_reader.entities.UserEntity;
import com.api.comic_reader.enums.Role;
import com.api.comic_reader.exception.AppException;
import com.api.comic_reader.exception.ErrorCode;
import com.api.comic_reader.repositories.InvalidatedTokenRepository;
import com.api.comic_reader.repositories.UserRepository;
import com.api.comic_reader.utils.DateUtil;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final InvalidatedTokenRepository invalidatedTokenRepository;

    // This method allows a new user to register.
    // It checks if the username or email is already taken.
    // It encodes the password before saving the user to the database.
    public void register(RegisterRequest newUser) throws AppException {
        Optional<UserEntity> userOptional =
                userRepository.findByUsernameOrEmail(newUser.getUsername(), newUser.getEmail());
        if (userOptional.isPresent()) {
            throw new AppException(ErrorCode.USERNAME_OR_EMAIL_TAKEN);
        }
        try {
            userRepository.save(UserEntity.builder()
                    .username(newUser.getUsername())
                    .email(newUser.getEmail())
                    .password(passwordEncoder.encode(newUser.getPassword()))
                    .fullName(newUser.getFullName())
                    .dateOfBirth(DateUtil.convertStringToDate(newUser.getDateOfBirth()))
                    .isMale(newUser.getIsMale())
                    .role(Role.USER)
                    .build());
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    // This method allows a user to log in.
    // It checks if the username and password are correct.
    // It generates a JWT token for the user and returns it in an AuthResponse object.
    public AuthResponse login(LoginRequest loginRequest) throws AppException {
        AuthResponse loginResponse = null;
        try {
            Optional<UserEntity> userOptional = userRepository.findByUsername(loginRequest.getUsername());
            if (userOptional.isPresent()) {
                UserEntity user = userOptional.get();
                if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                    String jwtToken;
                    jwtToken = jwtService.generateToken(user);
                    loginResponse = AuthResponse.builder()
                            .id(user.getId())
                            .token(jwtToken)
                            .authenticated(true)
                            .build();
                }
            }
        } catch (AppException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        if (loginResponse == null) {
            throw new AppException(ErrorCode.WRONG_USERNAME_OR_PASSWORD);
        }
        return loginResponse;
    }

    // This method allows a user to log out.
    // It invalidates the user's JWT token.
    public void logout(String token) throws AppException {
        try {
            var signedToken = jwtService.verifyToken(token);

            String jit = signedToken.getJWTClaimsSet().getJWTID();

            Date expirationTime = signedToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedTokenEntity invalidatedToken = InvalidatedTokenEntity.builder()
                    .token(jit)
                    .expirationTime(expirationTime)
                    .build();
            invalidatedTokenRepository.save(invalidatedToken);
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    // This method allows a user to refresh their JWT token.
    // It invalidates the old token and generates a new one.
    // It returns the new token in an AuthResponse object.
    public AuthResponse refreshToken(String token) throws AppException {
        try {
            var signedToken = jwtService.verifyToken(token);

            var jit = signedToken.getJWTClaimsSet().getJWTID();

            Date expirationTime = signedToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedTokenEntity invalidatedToken = InvalidatedTokenEntity.builder()
                    .token(jit)
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

    // This method introspects a JWT token.
    // It checks if the token is valid and not expired.
    // It returns the introspection result in an IntrospectResponse object.
    public IntrospectResponse introspect(TokenRequest request) throws AppException {
        var token = request.getToken();

        if (token == null) {
            throw new AppException(ErrorCode.TOKEN_IS_REQUIRED);
        }

        return jwtService.introspect(token);
    }
}
