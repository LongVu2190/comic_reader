package com.api.comic_reader.dtos.requests;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRequest {
    private String email;

    private String password;
}