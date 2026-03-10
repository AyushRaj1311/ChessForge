package com.chessplatform.dto.response;

import com.chessplatform.model.GameMode;
import com.chessplatform.model.GameResult;
import com.chessplatform.model.GameStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GameResponse {
    private String gameId;
    private String whitePlayer;
    private String blackPlayer;
    private GameMode gameMode;
    private GameStatus status;
    private GameResult result;
    private String currentFen;
    private Integer whiteTimeRemaining;
    private Integer blackTimeRemaining;
    private Integer totalMoves;
    private String resultReason;
    private boolean isAiGame;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
