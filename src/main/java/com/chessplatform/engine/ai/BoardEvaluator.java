package com.chessplatform.engine.ai;

import com.chessplatform.engine.model.Board;
import com.chessplatform.engine.model.Piece;
import com.chessplatform.model.PieceColor;
import com.chessplatform.model.PieceType;
import org.springframework.stereotype.Component;

@Component
public class BoardEvaluator {

    // Piece values in centipawns
    private static final int PAWN_VALUE   = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE   = 500;
    private static final int QUEEN_VALUE  = 900;
    private static final int KING_VALUE   = 20000;

    // Threshold to determine if we are in the endgame (e.g., no Queens and few minor pieces)
    // Total starting non-pawn material per side is 3170. We transition around 1500.
    private static final int ENDGAME_MATERIAL_THRESHOLD = 1500;

    // Piece-square tables (from White's perspective)
    private static final int[] PAWN_TABLE = {
            0,  0,  0,  0,  0,  0,  0,  0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5,  5, 10, 25, 25, 10,  5,  5,
            0,  0,  0, 20, 20,  0,  0,  0,
            5, -5,-10,  0,  0,-10, -5,  5,
            5, 10, 10,-20,-20, 10, 10,  5,
            0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] KNIGHT_TABLE = {
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50
    };

    private static final int[] BISHOP_TABLE = {
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] ROOK_TABLE = {
            0,  0,  0,  0,  0,  0,  0,  0,
            5, 10, 10, 10, 10, 10, 10,  5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            0,  0,  0,  5,  5,  0,  0,  0
    };

    private static final int[] QUEEN_TABLE = {
            -20,-10,-10, -5, -5,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5,  5,  5,  5,  0,-10,
            -5,  0,  5,  5,  5,  5,  0, -5,
            0,  0,  5,  5,  5,  5,  0, -5,
            -10,  5,  5,  5,  5,  5,  0,-10,
            -10,  0,  5,  0,  0,  0,  0,-10,
            -20,-10,-10, -5, -5,-10,-10,-20
    };

    // King Table for the Midgame (Hides in the corners)
    private static final int[] KING_TABLE_MG = {
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -20,-30,-30,-40,-40,-30,-30,-20,
            -10,-20,-20,-20,-20,-20,-20,-10,
            20, 20,  0,  0,  0,  0, 20, 20,
            20, 30, 10,  0,  0, 10, 30, 20
    };

    // --- NEW: King Table for the Endgame (Rushes to the center) ---
    private static final int[] KING_TABLE_EG = {
            -50,-40,-30,-20,-20,-30,-40,-50,
            -30,-20,-10,  0,  0,-10,-20,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-30,  0,  0,  0,  0,-30,-30,
            -50,-30,-30,-30,-30,-30,-30,-50
    };

    public int evaluate(Board board) {
        int score = 0;
        int nonPawnMaterialWhite = 0;
        int nonPawnMaterialBlack = 0;

        // Pass 1: We evaluate the whole board, but skip the Kings temporarily.
        // We also track how much material is left to determine the game phase.
        int whiteKingRow = -1, whiteKingCol = -1;
        int blackKingRow = -1, blackKingCol = -1;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board.getPiece(r, c);
                if (piece == null) continue;

                PieceType type = piece.getType();
                PieceColor color = piece.getColor();

                // Save kings for the second step
                if (type == PieceType.KING) {
                    if (color == PieceColor.WHITE) {
                        whiteKingRow = r; whiteKingCol = c;
                    } else {
                        blackKingRow = r; blackKingCol = c;
                    }
                    continue;
                }

                int value = getPieceValue(type);
                int pstValue = getPstValue(type, color, r, c, false); // Kings handled later

                if (color == PieceColor.WHITE) {
                    score += value + pstValue;
                    if (type != PieceType.PAWN) nonPawnMaterialWhite += value;
                } else {
                    score -= value + pstValue;
                    if (type != PieceType.PAWN) nonPawnMaterialBlack += value;
                }
            }
        }

        // Pass 2: Evaluate the Kings based on the detected Game Phase
        boolean whiteInEndgame = nonPawnMaterialBlack <= ENDGAME_MATERIAL_THRESHOLD;
        boolean blackInEndgame = nonPawnMaterialWhite <= ENDGAME_MATERIAL_THRESHOLD;

        if (whiteKingRow != -1) {
            score += KING_VALUE + getPstValue(PieceType.KING, PieceColor.WHITE, whiteKingRow, whiteKingCol, whiteInEndgame);
        }
        if (blackKingRow != -1) {
            score -= KING_VALUE + getPstValue(PieceType.KING, PieceColor.BLACK, blackKingRow, blackKingCol, blackInEndgame);
        }

        return score;
    }

    private int getPieceValue(PieceType type) {
        return switch (type) {
            case PAWN   -> PAWN_VALUE;
            case KNIGHT -> KNIGHT_VALUE;
            case BISHOP -> BISHOP_VALUE;
            case ROOK   -> ROOK_VALUE;
            case QUEEN  -> QUEEN_VALUE;
            case KING   -> KING_VALUE;
        };
    }

    private int getPstValue(PieceType type, PieceColor color, int row, int col, boolean isEndgame) {
        int[] table = switch (type) {
            case PAWN   -> PAWN_TABLE;
            case KNIGHT -> KNIGHT_TABLE;
            case BISHOP -> BISHOP_TABLE;
            case ROOK   -> ROOK_TABLE;
            case QUEEN  -> QUEEN_TABLE;
            case KING   -> isEndgame ? KING_TABLE_EG : KING_TABLE_MG;
        };

        // If White, flip the row index (assuming row 0 is the top/Black's side of the board)
        int index = (color == PieceColor.WHITE) ? (7 - row) * 8 + col : row * 8 + col;

        return table[index];
    }
}