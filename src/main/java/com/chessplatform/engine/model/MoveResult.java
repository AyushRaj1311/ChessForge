package com.chessplatform.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString // Helps massively with debugging in your logs
public class MoveResult {

    // Core validity
    private final boolean valid;
    private final String errorMessage;

    // State representation
    @JsonIgnore // CRITICAL: Prevents Spring Boot from sending the massive Board object to the frontend
    private final Board newBoard;
    private final String newFen;
    private final String san; // Standard Algebraic Notation (e.g., "Nxf7+")

    // Move characteristics (Flags)
    private final boolean isCapture;
    private final boolean isCheck;
    private final boolean isCheckmate;
    private final boolean isStalemate;
    private final boolean isCastle;
    private final boolean isEnPassant;
    private final boolean isPromotion;

    // Helper method for failed moves
    public static MoveResult invalid(String reason) {
        return MoveResult.builder()
                .valid(false)
                .errorMessage(reason)
                .build();
    }

    // Optional: Helper method for a quick successful move if you don't want to use the builder everywhere
    public static MoveResult success(Board newBoard, String newFen, String san) {
        return MoveResult.builder()
                .valid(true)
                .newBoard(newBoard)
                .newFen(newFen)
                .san(san)
                .build();
    }
}