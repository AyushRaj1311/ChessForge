package com.chessplatform.dto.response;

import com.chessplatform.model.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private Long userId;
    private String username;
    private Role role;
    private Integer rating;
}
