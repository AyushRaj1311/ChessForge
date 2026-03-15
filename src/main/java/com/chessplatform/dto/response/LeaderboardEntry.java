package com.chessplatform.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaderboardEntry {
    private int rank;
    private Long userId;
    private String username;
    private Integer rating;
    private Integer gamesPlayed;
    private Integer gamesWon;
    private double winRate;
}
