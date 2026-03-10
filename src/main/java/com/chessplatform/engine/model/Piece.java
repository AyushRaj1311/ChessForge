package com.chessplatform.engine.model;

import com.chessplatform.model.PieceColor;
import com.chessplatform.model.PieceType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode // CRITICAL: Allows the engine to check if two pieces are identical
public class Piece implements Serializable {

    private PieceType type;
    private PieceColor color;
    private boolean hasMoved;

    public Piece(PieceType type, PieceColor color) {
        this.type = type;
        this.color = color;
        this.hasMoved = false;
    }

    public Piece copy() {
        return new Piece(type, color, hasMoved);
    }

    public char toFenChar() {
        char c = switch (type) {
            case KING   -> 'k';
            case QUEEN  -> 'q';
            case ROOK   -> 'r';
            case BISHOP -> 'b';
            case KNIGHT -> 'n';
            case PAWN   -> 'p';
        };
        return color == PieceColor.WHITE ? Character.toUpperCase(c) : c;
    }

    public static Piece fromFenChar(char c) {
        PieceColor color = Character.isUpperCase(c) ? PieceColor.WHITE : PieceColor.BLACK;
        PieceType type = switch (Character.toLowerCase(c)) {
            case 'k' -> PieceType.KING;
            case 'q' -> PieceType.QUEEN;
            case 'r' -> PieceType.ROOK;
            case 'b' -> PieceType.BISHOP;
            case 'n' -> PieceType.KNIGHT;
            case 'p' -> PieceType.PAWN;
            default  -> throw new IllegalArgumentException("Invalid FEN char: " + c);
        };
        return new Piece(type, color);
    }

    @Override
    public String toString() {
        // Formatted to match standard enums for cleaner console logging
        // e.g., outputs "WHITE_PAWN" instead of "WHITE PAWN"
        return color + "_" + type;
    }
}