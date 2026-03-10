package com.chessplatform.engine.ai;

import com.chessplatform.engine.model.Board;
import com.chessplatform.engine.model.ChessMove;
import com.chessplatform.engine.model.Piece;
import com.chessplatform.engine.service.MoveGenerator;
import com.chessplatform.model.PieceColor;
import com.chessplatform.model.PieceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinimaxAI {

    private final MoveGenerator moveGenerator;
    private final BoardEvaluator evaluator;

    @Value("${chess.ai.max-depth:4}")
    private int maxDepth;

    public ChessMove getBestMove(Board board, PieceColor aiColor) {
        long startTime = System.currentTimeMillis();
        ChessMove bestMove = null;
        int bestScore = aiColor == PieceColor.WHITE ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        List<ChessMove> moves = moveGenerator.generateLegalMoves(board, aiColor);
        if (moves.isEmpty()) return null;

        // OPTIMIZATION: Order moves before searching to maximize Alpha-Beta pruning efficiency
        orderMoves(board, moves);

        for (ChessMove move : moves) {
            // FIX: Use the new in-place board modification!
            board.makeMove(move);

            // Note: we pass 'board' directly now, not 'newBoard'
            int score = minimax(board, maxDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, aiColor == PieceColor.BLACK);

            // FIX: Undo the move immediately after the evaluation finishes
            board.unmakeMove();

            if (aiColor == PieceColor.WHITE) {
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("AI computed move {} (score={}) in {}ms", bestMove, bestScore, elapsed);
        return bestMove;
    }

    private int minimax(Board board, int depth, int alpha, int beta, boolean isMaximizing) {
        // Base case: max depth reached
        if (depth == 0) {
            return evaluator.evaluate(board);
        }

        List<ChessMove> moves = moveGenerator.generateLegalMoves(board, board.getCurrentTurn());

        // Base case: game over (Checkmate or Stalemate)
        if (moves.isEmpty()) {
            if (moveGenerator.isInCheck(board, board.getCurrentTurn())) {
                // Checkmate - prefer faster mates by adding/subtracting the depth
                return isMaximizing ? -(100000 + depth) : (100000 + depth);
            }
            return 0; // Stalemate
        }

        // OPTIMIZATION: Order moves to find cutoffs faster
        orderMoves(board, moves);

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (ChessMove move : moves) {
                board.makeMove(move); // Apply directly!
                int eval = minimax(board, depth - 1, alpha, beta, false);
                board.unmakeMove();   // Revert immediately!

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break; // Alpha-Beta pruning
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (ChessMove move : moves) {
                // FIX: Removed applyMoveToBoard here as well!
                board.makeMove(move); // Apply directly!
                int eval = minimax(board, depth - 1, alpha, beta, true);
                board.unmakeMove();   // Revert immediately!

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break; // Alpha-Beta pruning
            }
            return minEval;
        }
    }

    // --- NEW: MOVE ORDERING LOGIC ---
    private void orderMoves(Board board, List<ChessMove> moves) {
        moves.sort((m1, m2) -> {
            int score1 = guessMoveScore(board, m1);
            int score2 = guessMoveScore(board, m2);
            return Integer.compare(score2, score1); // Sort descending (highest score first)
        });
    }

    private int guessMoveScore(Board board, ChessMove move) {
        int score = 0;
        Piece movingPiece = board.getPiece(move.getFrom());
        Piece capturedPiece = board.getPiece(move.getTo());

        // 1. MVV-LVA (Most Valuable Victim - Least Valuable Attacker)
        if (capturedPiece != null) {
            // A pawn capturing a Queen is a great move to check first.
            // A Queen capturing a protected Pawn might be a terrible move.
            score = 10 * getPieceValue(capturedPiece.getType()) - getPieceValue(movingPiece.getType());
        }

        // 2. Prioritize Pawn Promotions
        if (move.getPromotionPiece() != null) {
            score += getPieceValue(move.getPromotionPiece());
        }

        return score;
    }

    private int getPieceValue(PieceType type) {
        return switch (type) {
            case PAWN   -> 100;
            case KNIGHT -> 320;
            case BISHOP -> 330;
            case ROOK   -> 500;
            case QUEEN  -> 900;
            case KING   -> 20000;
        };
    }
}