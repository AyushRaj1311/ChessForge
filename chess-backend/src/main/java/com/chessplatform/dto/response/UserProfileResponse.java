package com.chessplatform.dto.response;

import com.chessplatform.model.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private Integer rating;
    private Integer gamesPlayed;
    private Integer gamesWon;
    private Integer gamesLost;
    private Integer gamesDraw;
    private double winRate;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
