package com.api.comic_reader.services;

import com.api.comic_reader.dtos.requests.RegisterRequest;
import com.api.comic_reader.entities.UserEntity;
import com.api.comic_reader.enums.Role;
import com.api.comic_reader.exception.AppException;
import com.api.comic_reader.exception.ErrorCode;
import com.api.comic_reader.repositories.UserRepository;
import com.api.comic_reader.utils.DateUtil;

import lombok.AllArgsConstructor;

import java.util.Optional;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class UserService {
    @Autowired
    private UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public List<UserEntity> getAllUsers() throws AppException {
        List<UserEntity> users = null;
        try {
            users = userRepository.findAll();
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        return users;
    }

    public UserEntity register(RegisterRequest newUser) throws AppException {
        UserEntity user = null;
        Optional<UserEntity> userOptional = userRepository.findByUsernameOrEmail(newUser.getUsername(), newUser.getEmail());
        if (userOptional.isPresent()) {
            throw new AppException(ErrorCode.USERNAME_OR_EMAIL_TAKEN);
        }
        try {
            user = UserEntity.builder()
                    .username(newUser.getUsername())
                    .email(newUser.getEmail())
                    .password(passwordEncoder.encode(newUser.getPassword()))
                    .fullName(newUser.getFullName())
                    .dateOfBirth(DateUtil.convertStringToDate(newUser.getDateOfBirth()))
                    .isMale(newUser.getIsMale())
                    .role(Role.USER)
                    .build();
            userRepository.save(user);
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        return user;
    }

    @PostAuthorize("hasAuthority('SCOPE_ADMIN') or returnObject.username == authentication.name")
    public UserEntity getUserInformationById(Long id) throws AppException {
        Optional<UserEntity> userOptional = userRepository.findById(id);
        if (userOptional.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return userOptional.get();
    }

    public UserEntity getMyInformation() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        Optional<UserEntity> userOptional = userRepository.findByUsername(name);
        if (userOptional.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return userOptional.get();
    }

    public void changePassword(String oldPassword, String newPassword) throws AppException {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        Optional<UserEntity> userOptional = userRepository.findByUsername(name);
        if (userOptional.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        UserEntity user = userOptional.get();
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
