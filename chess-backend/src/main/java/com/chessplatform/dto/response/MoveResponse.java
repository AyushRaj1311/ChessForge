package com.chessplatform.dto.response;

import com.chessplatform.model.PieceColor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MoveResponse {
    private boolean valid;
    private String from;
    private String to;
    private String san;
    private String fen;
    private boolean isCapture;
    private boolean isCheck;
    private boolean isCheckmate;
    private boolean isStalemate;
    private boolean isCastle;
    private String promotion;
    private String gameStatus;
    private String gameResult;
    private Integer whiteTimeRemaining;
    private Integer blackTimeRemaining;
    private String errorMessage;
    // AI response move (if playing vs AI)
    private MoveResponse aiMove;
}
