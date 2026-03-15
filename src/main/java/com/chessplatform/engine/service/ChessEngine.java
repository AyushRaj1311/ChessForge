package com.chessplatform.engine.service;

import com.chessplatform.engine.model.*;
import com.chessplatform.model.PieceColor;
import com.chessplatform.model.PieceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChessEngine {

    private final MoveGenerator moveGenerator;

    public MoveResult makeMove(Board board, String fromSq, String toSq, String promotionStr) {
        try {
            Position from = Position.fromAlgebraic(fromSq);
            Position to   = Position.fromAlgebraic(toSq);
            Piece piece   = board.getPiece(from);

            if (piece == null) {
                return MoveResult.invalid("No piece at " + fromSq);
            }
            if (piece.getColor() != board.getCurrentTurn()) {
                return MoveResult.invalid("Not your turn");
            }

            // FIX: Using a helper method makes this variable "final", solving the Lambda error!
            final PieceType promotion = parsePromotion(promotionStr);

            // 1. Find matching legal move
            List<ChessMove> legal = moveGenerator.generateLegalMoves(board, piece.getColor());
            ChessMove matched = legal.stream()
                    .filter(m -> m.getFrom().equals(from) && m.getTo().equals(to))
                    .filter(m -> promotion == null || promotion.equals(m.getPromotionPiece()))
                    .findFirst()
                    .orElse(null);

            if (matched == null) {
                return MoveResult.invalid("Illegal move: " + fromSq + " -> " + toSq);
            }

            boolean isCapture = board.getPiece(to) != null || matched.isEnPassant();

            // 2. Build the base SAN string BEFORE applying the move (needs old board state)
            String sanBase = buildBaseSan(board, matched, piece, isCapture);

            // 3. OPTIMIZATION: Apply the move directly to the board (No more board.copy!)
            board.makeMove(matched);

            // 4. Calculate Check/Checkmate/Stalemate on the NEW board state
            PieceColor nextTurn = board.getCurrentTurn();
            boolean isCheck = moveGenerator.isInCheck(board, nextTurn);

            // We only need to generate the opponent's moves ONCE to check for game over
            List<ChessMove> nextLegalMoves = moveGenerator.generateLegalMoves(board, nextTurn);
            boolean noMovesLeft = nextLegalMoves.isEmpty();

            boolean isCheckmate = isCheck && noMovesLeft;
            boolean isStalemate = !isCheck && noMovesLeft;
            boolean isDrawByInsufficientMaterial = isInsufficientMaterial(board);
            boolean isDrawByFiftyMoves = board.getHalfMoveClock() >= 100;

            // 5. Finalize the SAN notation with + or #
            String finalSan = sanBase + (isCheckmate ? "#" : (isCheck ? "+" : ""));

            return MoveResult.builder()
                    .valid(true)
                    .newBoard(board) // The board is now updated in place!
                    .san(finalSan)
                    .newFen(board.toFen())
                    .isCapture(isCapture)
                    .isCheck(isCheck)
                    .isCheckmate(isCheckmate)
                    .isStalemate(isStalemate || isDrawByInsufficientMaterial || isDrawByFiftyMoves)
                    .isCastle(matched.isCastle())
                    .isEnPassant(matched.isEnPassant())
                    .isPromotion(matched.getPromotionPiece() != null)
                    .build();

        } catch (Exception e) {
            return MoveResult.invalid("Error processing move: " + e.getMessage());
        }
    }

    // --- NEW HELPER METHOD ---
    private PieceType parsePromotion(String promotionStr) {
        if (promotionStr == null || promotionStr.isBlank()) {
            return null;
        }
        return switch (promotionStr.toUpperCase()) {
            case "Q" -> PieceType.QUEEN;
            case "R" -> PieceType.ROOK;
            case "B" -> PieceType.BISHOP;
            case "N" -> PieceType.KNIGHT;
            default  -> PieceType.QUEEN;
        };
    }

    // Extracted into a "Base" method so we don't have to pass isCheckmate and isCheck
    // before we've actually made the move.
    private String buildBaseSan(Board board, ChessMove move, Piece piece, boolean isCapture) {
        if (move.isCastle()) {
            return move.getTo().getCol() == 6 ? "O-O" : "O-O-O";
        }

        StringBuilder san = new StringBuilder();
        if (piece.getType() != PieceType.PAWN) {
            san.append(Character.toUpperCase(piece.toFenChar()));
        }

        // Disambiguation (e.g., distinguishing between two Knights that can jump to the same square)
        if (piece.getType() != PieceType.PAWN && piece.getType() != PieceType.KING) {
            List<ChessMove> ambiguous = moveGenerator.generateLegalMoves(board, piece.getColor())
                    .stream()
                    .filter(m -> !m.getFrom().equals(move.getFrom())
                            && m.getTo().equals(move.getTo())
                            && board.getPiece(m.getFrom()) != null
                            && board.getPiece(m.getFrom()).getType() == piece.getType())
                    .toList();

            if (!ambiguous.isEmpty()) {
                boolean sameFile = ambiguous.stream().anyMatch(m -> m.getFrom().getCol() == move.getFrom().getCol());
                boolean sameRank = ambiguous.stream().anyMatch(m -> m.getFrom().getRow() == move.getFrom().getRow());

                if (!sameFile) {
                    san.append((char) ('a' + move.getFrom().getCol()));
                } else if (!sameRank) {
                    san.append((char) ('1' + move.getFrom().getRow()));
                } else {
                    // Extremely rare case (e.g., 3 Queens on the board)
                    san.append((char) ('a' + move.getFrom().getCol()));
                    san.append((char) ('1' + move.getFrom().getRow()));
                }
            }
        }

        if (isCapture) {
            if (piece.getType() == PieceType.PAWN) {
                san.append((char) ('a' + move.getFrom().getCol()));
            }
            san.append('x');
        }

        san.append(move.getTo().toAlgebraic());

        if (move.getPromotionPiece() != null) {
            // Fix: Knight promotion notation is 'N', not 'K'
            char promoChar = move.getPromotionPiece() == PieceType.KNIGHT ? 'N' :
                    Character.toUpperCase(new Piece(move.getPromotionPiece(), piece.getColor(), false).toFenChar());
            san.append('=').append(promoChar);
        }

        return san.toString();
    }

    public boolean isGameOver(Board board) {
        List<ChessMove> legal = moveGenerator.generateLegalMoves(board, board.getCurrentTurn());
        return legal.isEmpty();
    }

    public boolean isInsufficientMaterial(Board board) {
        int whitePieces = 0, blackPieces = 0;
        int whiteBishops = 0, blackBishops = 0;
        int whiteKnights = 0, blackKnights = 0;
        boolean whiteBishopOnLight = false, whiteBishopOnDark = false;
        boolean blackBishopOnLight = false, blackBishopOnDark = false;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p == null || p.getType() == PieceType.KING) continue;

                // If a Pawn, Rook, or Queen is on the board, checkmate is still possible
                if (p.getType() == PieceType.PAWN || p.getType() == PieceType.ROOK || p.getType() == PieceType.QUEEN) {
                    return false;
                }

                if (p.getColor() == PieceColor.WHITE) {
                    whitePieces++;
                    if (p.getType() == PieceType.BISHOP) {
                        whiteBishops++;
                        if ((r + c) % 2 == 0) whiteBishopOnDark = true;
                        else whiteBishopOnLight = true;
                    } else if (p.getType() == PieceType.KNIGHT) {
                        whiteKnights++;
                    }
                } else {
                    blackPieces++;
                    if (p.getType() == PieceType.BISHOP) {
                        blackBishops++;
                        if ((r + c) % 2 == 0) blackBishopOnDark = true;
                        else blackBishopOnLight = true;
                    } else if (p.getType() == PieceType.KNIGHT) {
                        blackKnights++;
                    }
                }
            }
        }

        // 1. King vs King
        if (whitePieces == 0 && blackPieces == 0) return true;

        // 2. King + Bishop vs King
        if ((whiteBishops == 1 && whitePieces == 1 && blackPieces == 0) ||
            (blackBishops == 1 && blackPieces == 1 && whitePieces == 0)) return true;

        // 3. King + Knight vs King
        if ((whiteKnights == 1 && whitePieces == 1 && blackPieces == 0) ||
            (blackKnights == 1 && blackPieces == 1 && whitePieces == 0)) return true;

        // 4. King + Bishop vs King + Bishop (same color)
        if (whiteBishops == 1 && whitePieces == 1 && blackBishops == 1 && blackPieces == 1) {
            return (whiteBishopOnLight && blackBishopOnLight) || (whiteBishopOnDark && blackBishopOnDark);
        }

        return false;
    }
}