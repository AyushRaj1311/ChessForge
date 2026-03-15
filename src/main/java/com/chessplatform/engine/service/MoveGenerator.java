package com.chessplatform.engine.service;

import com.chessplatform.engine.model.*;
import com.chessplatform.model.PieceColor;
import com.chessplatform.model.PieceType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MoveGenerator {

    public List<ChessMove> generateLegalMoves(Board board, PieceColor color) {
        List<ChessMove> pseudoLegal = generatePseudoLegalMoves(board, color);
        List<ChessMove> legal = new ArrayList<>();

        for (ChessMove move : pseudoLegal) {
            // OPTIMIZATION: Use the Board's ultra-fast make/unmake logic
            // instead of copying the board or running custom simulations.
            board.makeMove(move);
            if (!isInCheck(board, color)) {
                legal.add(move);
            }
            board.unmakeMove();
        }
        return legal;
    }

    public List<ChessMove> generatePseudoLegalMoves(Board board, PieceColor color) {
        List<ChessMove> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board.getPiece(r, c);
                if (piece != null && piece.getColor() == color) {
                    // OPTIMIZATION: Use cache instead of 'new Position'
                    Position from = Position.get(r, c);
                    moves.addAll(generatePieceMoves(board, from, piece));
                }
            }
        }
        return moves;
    }

    private List<ChessMove> generatePieceMoves(Board board, Position from, Piece piece) {
        return switch (piece.getType()) {
            case PAWN   -> generatePawnMoves(board, from, piece.getColor());
            case KNIGHT -> generateKnightMoves(board, from, piece.getColor());
            case BISHOP -> generateSlidingMoves(board, from, piece.getColor(), false, true);
            case ROOK   -> generateSlidingMoves(board, from, piece.getColor(), true, false);
            case QUEEN  -> generateSlidingMoves(board, from, piece.getColor(), true, true);
            case KING   -> generateKingMoves(board, from, piece.getColor());
        };
    }

    private List<ChessMove> generatePawnMoves(Board board, Position from, PieceColor color) {
        List<ChessMove> moves = new ArrayList<>();
        int dir      = color == PieceColor.WHITE ? 1 : -1;
        int startRow = color == PieceColor.WHITE ? 1 : 6;
        int promRow  = color == PieceColor.WHITE ? 7 : 0;

        // One square forward
        Position one = Position.get(from.getRow() + dir, from.getCol());
        if (one.isValid() && board.getPiece(one) == null) {
            if (one.getRow() == promRow) {
                addPromotionMoves(moves, from, one);
            } else {
                moves.add(new ChessMove(from, one));
            }
            // Two squares from start
            if (from.getRow() == startRow) {
                Position two = Position.get(from.getRow() + 2 * dir, from.getCol());
                if (board.getPiece(two) == null) {
                    moves.add(new ChessMove(from, two));
                }
            }
        }

        // Captures
        for (int dc : new int[]{-1, 1}) {
            Position cap = Position.get(from.getRow() + dir, from.getCol() + dc);
            if (!cap.isValid()) continue;
            Piece target = board.getPiece(cap);
            boolean isEnPassant = cap.equals(board.getEnPassantTarget());
            if ((target != null && target.getColor() != color) || isEnPassant) {
                ChessMove move = new ChessMove(from, cap);
                if (isEnPassant) move.setEnPassant(true);
                if (cap.getRow() == promRow) {
                    addPromotionMoves(moves, from, cap);
                } else {
                    moves.add(move);
                }
            }
        }
        return moves;
    }

    private void addPromotionMoves(List<ChessMove> moves, Position from, Position to) {
        for (PieceType pt : new PieceType[]{PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT}) {
            moves.add(new ChessMove(from, to, pt));
        }
    }

    private List<ChessMove> generateKnightMoves(Board board, Position from, PieceColor color) {
        List<ChessMove> moves = new ArrayList<>();
        int[][] offsets = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] off : offsets) {
            Position to = Position.get(from.getRow() + off[0], from.getCol() + off[1]);
            if (to.isValid()) {
                Piece target = board.getPiece(to);
                if (target == null || target.getColor() != color) {
                    moves.add(new ChessMove(from, to));
                }
            }
        }
        return moves;
    }

    private List<ChessMove> generateSlidingMoves(Board board, Position from, PieceColor color,
                                                 boolean orthogonal, boolean diagonal) {
        List<ChessMove> moves = new ArrayList<>();
        List<int[]> directions = new ArrayList<>();
        if (orthogonal) { directions.addAll(List.of(new int[][]{{1,0},{-1,0},{0,1},{0,-1}})); }
        if (diagonal)   { directions.addAll(List.of(new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}})); }

        for (int[] dir : directions) {
            Position cur = Position.get(from.getRow() + dir[0], from.getCol() + dir[1]);
            while (cur.isValid()) {
                Piece target = board.getPiece(cur);
                if (target == null) {
                    moves.add(new ChessMove(from, cur));
                } else {
                    if (target.getColor() != color) moves.add(new ChessMove(from, cur));
                    break;
                }
                cur = Position.get(cur.getRow() + dir[0], cur.getCol() + dir[1]);
            }
        }
        return moves;
    }

    private List<ChessMove> generateKingMoves(Board board, Position from, PieceColor color) {
        List<ChessMove> moves = new ArrayList<>();
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                Position to = Position.get(from.getRow() + dr, from.getCol() + dc);
                if (to.isValid()) {
                    Piece target = board.getPiece(to);
                    if (target == null || target.getColor() != color) {
                        moves.add(new ChessMove(from, to));
                    }
                }
            }
        }
        // Castling
        moves.addAll(generateCastlingMoves(board, from, color));
        return moves;
    }

    private List<ChessMove> generateCastlingMoves(Board board, Position from, PieceColor color) {
        List<ChessMove> moves = new ArrayList<>();
        int rank = color == PieceColor.WHITE ? 0 : 7;

        // Don't castle while in check
        if (isInCheck(board, color)) return moves;

        // Kingside
        boolean ksCastle = color == PieceColor.WHITE ? board.isWhiteKingsideCastle() : board.isBlackKingsideCastle();
        if (ksCastle) {
            boolean f1Empty = board.getPiece(rank, 5) == null;
            boolean g1Empty = board.getPiece(rank, 6) == null;
            if (f1Empty && g1Empty) {
                boolean f1Safe = !isSquareAttacked(board, Position.get(rank, 5), color.opposite());
                boolean g1Safe = !isSquareAttacked(board, Position.get(rank, 6), color.opposite());

                if (f1Safe && g1Safe) {
                    ChessMove castle = new ChessMove(from, Position.get(rank, 6));
                    castle.setCastle(true);
                    moves.add(castle);
                }
            }
        }

        // Queenside
        boolean qsCastle = color == PieceColor.WHITE ? board.isWhiteQueensideCastle() : board.isBlackQueensideCastle();
        if (qsCastle) {
            boolean d1Empty = board.getPiece(rank, 3) == null;
            boolean c1Empty = board.getPiece(rank, 2) == null;
            boolean b1Empty = board.getPiece(rank, 1) == null;
            if (d1Empty && c1Empty && b1Empty) {
                boolean d1Safe = !isSquareAttacked(board, Position.get(rank, 3), color.opposite());
                boolean c1Safe = !isSquareAttacked(board, Position.get(rank, 2), color.opposite());

                if (d1Safe && c1Safe) {
                    ChessMove castle = new ChessMove(from, Position.get(rank, 2));
                    castle.setCastle(true);
                    moves.add(castle);
                }
            }
        }
        return moves;
    }

    public boolean isInCheck(Board board, PieceColor color) {
        Position kingPos = board.findKing(color);
        if (kingPos == null) return false;
        return isSquareAttacked(board, kingPos, color.opposite());
    }

    public boolean isSquareAttacked(Board board, Position square, PieceColor attackerColor) {
        // 1. Check Knight attacks
        int[][] knightOffsets = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] off : knightOffsets) {
            Position pos = Position.get(square.getRow() + off[0], square.getCol() + off[1]);
            if (pos.isValid()) {
                Piece p = board.getPiece(pos);
                if (p != null && p.getColor() == attackerColor && p.getType() == PieceType.KNIGHT) {
                    return true;
                }
            }
        }

        // 2. Check King attacks
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                Position pos = Position.get(square.getRow() + dr, square.getCol() + dc);
                if (pos.isValid()) {
                    Piece p = board.getPiece(pos);
                    if (p != null && p.getColor() == attackerColor && p.getType() == PieceType.KING) {
                        return true;
                    }
                }
            }
        }

        // 3. Check Pawn attacks
        int pawnAttackDir = attackerColor == PieceColor.WHITE ? -1 : 1;
        for (int dc : new int[]{-1, 1}) {
            Position pos = Position.get(square.getRow() + pawnAttackDir, square.getCol() + dc);
            if (pos.isValid()) {
                Piece p = board.getPiece(pos);
                if (p != null && p.getColor() == attackerColor && p.getType() == PieceType.PAWN) {
                    return true;
                }
            }
        }

        // 4. Check Diagonal attacks (Bishop, Queen)
        int[][] diagonals = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
        for (int[] dir : diagonals) {
            Position cur = Position.get(square.getRow() + dir[0], square.getCol() + dir[1]);
            while (cur.isValid()) {
                Piece p = board.getPiece(cur);
                if (p != null) {
                    if (p.getColor() == attackerColor && (p.getType() == PieceType.BISHOP || p.getType() == PieceType.QUEEN)) {
                        return true;
                    }
                    break;
                }
                cur = Position.get(cur.getRow() + dir[0], cur.getCol() + dir[1]);
            }
        }

        // 5. Check Orthogonal attacks (Rook, Queen)
        int[][] orthogonals = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        for (int[] dir : orthogonals) {
            Position cur = Position.get(square.getRow() + dir[0], square.getCol() + dir[1]);
            while (cur.isValid()) {
                Piece p = board.getPiece(cur);
                if (p != null) {
                    if (p.getColor() == attackerColor && (p.getType() == PieceType.ROOK || p.getType() == PieceType.QUEEN)) {
                        return true;
                    }
                    break;
                }
                cur = Position.get(cur.getRow() + dir[0], cur.getCol() + dir[1]);
            }
        }

        return false;
    }
}