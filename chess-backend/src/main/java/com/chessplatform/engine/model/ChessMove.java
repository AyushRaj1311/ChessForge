package com.chessplatform.engine.model;

import com.chessplatform.model.PieceType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode // CRITICAL: Allows you to compare moves (e.g., move1.equals(move2))
public class ChessMove implements Serializable {

    private Position from;
    private Position to;
    private PieceType promotionPiece; // null unless promotion
    private boolean isCastle;
    private boolean isEnPassant;

    public ChessMove(Position from, Position to) {
        this.from = from;
        this.to = to;
    }

    public ChessMove(Position from, Position to, PieceType promotionPiece) {
        this.from = from;
        this.to = to;
        this.promotionPiece = promotionPiece;
    }

    @Override
    public String toString() {
        String moveStr = from.toAlgebraic() + to.toAlgebraic();

        if (promotionPiece != null) {
            // FIX: Knight promotion must be 'n', not 'k' (which is King)
            if (promotionPiece == PieceType.KNIGHT) {
                moveStr += "n";
            } else {
                moveStr += promotionPiece.name().toLowerCase().charAt(0);
            }
        }

        return moveStr;
    }
}