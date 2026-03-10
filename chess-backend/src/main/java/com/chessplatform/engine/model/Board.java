package com.chessplatform.engine.model;

import com.chessplatform.model.PieceColor;
import com.chessplatform.model.PieceType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

@Getter
@Setter
public class Board implements Serializable {

    public static final String STARTING_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private Piece[][] squares; // [row][col], row 0 = rank 1, row 7 = rank 8
    private PieceColor currentTurn;
    private boolean whiteKingsideCastle;
    private boolean whiteQueensideCastle;
    private boolean blackKingsideCastle;
    private boolean blackQueensideCastle;
    private Position enPassantTarget;
    private int halfMoveClock;
    private int fullMoveNumber;

    // --- OPTIMIZATION 1: King Position Caching ---
    private Position whiteKingPos;
    private Position blackKingPos;

    // --- OPTIMIZATION 2: Move History Stack for unmakeMove() ---
    // Marked transient so it doesn't break Serializable if you save games
    private transient Deque<SavedState> history = new ArrayDeque<>();

    public Board() {
        squares = new Piece[8][8];
    }

    public static Board fromFen(String fen) {
        Board board = new Board();
        String[] parts = fen.split(" ");

        // Parse piece placement
        String[] ranks = parts[0].split("/");
        for (int rank = 7; rank >= 0; rank--) {
            String rankStr = ranks[7 - rank];
            int col = 0;
            for (char c : rankStr.toCharArray()) {
                if (Character.isDigit(c)) {
                    col += c - '0';
                } else {
                    Piece p = Piece.fromFenChar(c);
                    board.squares[rank][col] = p;
                    // Cache Kings during setup
                    if (p.getType() == PieceType.KING) {
                        if (p.getColor() == PieceColor.WHITE) board.whiteKingPos = new Position(rank, col);
                        else board.blackKingPos = new Position(rank, col);
                    }
                    col++;
                }
            }
        }

        // Active color
        board.currentTurn = parts[1].equals("w") ? PieceColor.WHITE : PieceColor.BLACK;

        // Castling availability
        String castling = parts[2];
        board.whiteKingsideCastle  = castling.contains("K");
        board.whiteQueensideCastle = castling.contains("Q");
        board.blackKingsideCastle  = castling.contains("k");
        board.blackQueensideCastle = castling.contains("q");

        // En passant
        if (!parts[3].equals("-")) {
            board.enPassantTarget = Position.fromAlgebraic(parts[3]);
        }

        // Clocks
        if (parts.length > 4) board.halfMoveClock  = Integer.parseInt(parts[4]);
        if (parts.length > 5) board.fullMoveNumber = Integer.parseInt(parts[5]);

        return board;
    }

    public String toFen() {
        StringBuilder sb = new StringBuilder();

        // Piece placement
        for (int rank = 7; rank >= 0; rank--) {
            int empty = 0;
            for (int col = 0; col < 8; col++) {
                Piece piece = squares[rank][col];
                if (piece == null) {
                    empty++;
                } else {
                    if (empty > 0) { sb.append(empty); empty = 0; }
                    sb.append(piece.toFenChar());
                }
            }
            if (empty > 0) sb.append(empty);
            if (rank > 0) sb.append('/');
        }

        sb.append(' ').append(currentTurn == PieceColor.WHITE ? 'w' : 'b');

        // Castling
        StringBuilder castling = new StringBuilder();
        if (whiteKingsideCastle)  castling.append('K');
        if (whiteQueensideCastle) castling.append('Q');
        if (blackKingsideCastle)  castling.append('k');
        if (blackQueensideCastle) castling.append('q');
        sb.append(' ').append(castling.length() > 0 ? castling : "-");

        // En passant
        sb.append(' ').append(enPassantTarget != null ? enPassantTarget.toAlgebraic() : "-");
        sb.append(' ').append(halfMoveClock);
        sb.append(' ').append(fullMoveNumber);

        return sb.toString();
    }

    public Piece getPiece(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) return null;
        return squares[row][col];
    }

    public Piece getPiece(Position pos) {
        return getPiece(pos.getRow(), pos.getCol());
    }

    public void setPiece(Position pos, Piece piece) {
        squares[pos.getRow()][pos.getCol()] = piece;
    }

    public void removePiece(Position pos) {
        squares[pos.getRow()][pos.getCol()] = null;
    }

    // MASSIVE SPEED UP: No more looping 64 squares!
    public Position findKing(PieceColor color) {
        return color == PieceColor.WHITE ? whiteKingPos : blackKingPos;
    }

    // --- NEW: THE DO MOVE LOGIC ---
    public void makeMove(ChessMove move) {
        Position from = move.getFrom();
        Position to = move.getTo();
        Piece movingPiece = getPiece(from);

        // Ensure history stack exists (in case of deserialization)
        if (history == null) history = new ArrayDeque<>();

        // 1. Snapshot the current board state BEFORE we change anything
        SavedState state = new SavedState(
                move, getPiece(to), enPassantTarget,
                whiteKingsideCastle, whiteQueensideCastle,
                blackKingsideCastle, blackQueensideCastle,
                halfMoveClock, movingPiece.isHasMoved()
        );
        history.push(state);

        // Handle En Passant Capture
        if (move.isEnPassant()) {
            int capturedPawnRow = movingPiece.getColor() == PieceColor.WHITE ? to.getRow() - 1 : to.getRow() + 1;
            Position epPos = new Position(capturedPawnRow, to.getCol());
            state.capturedPiece = getPiece(epPos); // Update snapshot with the actual pawn we captured
            removePiece(epPos);
        }

        // Handle Castling (Move the Rook)
        if (move.isCastle()) {
            int rank = from.getRow();
            if (to.getCol() == 6) { // Kingside
                setPiece(new Position(rank, 5), getPiece(rank, 7));
                removePiece(new Position(rank, 7));
            } else { // Queenside
                setPiece(new Position(rank, 3), getPiece(rank, 0));
                removePiece(new Position(rank, 0));
            }
        }

        // Move the main piece
        removePiece(from);
        if (move.getPromotionPiece() != null) {
            setPiece(to, new Piece(move.getPromotionPiece(), movingPiece.getColor(), true));
        } else {
            movingPiece.setHasMoved(true);
            setPiece(to, movingPiece);
        }

        // Track King Position
        if (movingPiece.getType() == PieceType.KING) {
            if (movingPiece.getColor() == PieceColor.WHITE) whiteKingPos = to;
            else blackKingPos = to;
        }

        // Update En Passant Target
        enPassantTarget = null;
        if (movingPiece.getType() == PieceType.PAWN && Math.abs(to.getRow() - from.getRow()) == 2) {
            enPassantTarget = new Position((from.getRow() + to.getRow()) / 2, from.getCol());
        }

        // Update Castling Rights based on what moved (or what was captured)
        updateCastlingRights(from);
        updateCastlingRights(to); // In case a rook gets captured!

        // Update Clocks and Turn
        currentTurn = currentTurn.opposite();
        if (currentTurn == PieceColor.WHITE) { // Black just moved
            fullMoveNumber++;
        }

        if (movingPiece.getType() == PieceType.PAWN || state.capturedPiece != null) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }
    }

    // --- NEW: THE UNDO MOVE LOGIC ---
    public void unmakeMove() {
        if (history == null || history.isEmpty()) return;

        SavedState state = history.pop();
        ChessMove move = state.move;
        Position from = move.getFrom();
        Position to = move.getTo();

        Piece pieceOnTarget = getPiece(to); // Could be a promoted piece or regular piece

        // Restore turn & clocks
        currentTurn = currentTurn.opposite();
        if (currentTurn == PieceColor.BLACK) fullMoveNumber--;
        halfMoveClock = state.halfMoveClock;
        enPassantTarget = state.enPassantTarget;

        // Restore Castling Rights
        whiteKingsideCastle = state.whiteKingsideCastle;
        whiteQueensideCastle = state.whiteQueensideCastle;
        blackKingsideCastle = state.blackKingsideCastle;
        blackQueensideCastle = state.blackQueensideCastle;

        // Determine original piece
        Piece originalPiece = pieceOnTarget;
        if (move.getPromotionPiece() != null) {
            originalPiece = new Piece(PieceType.PAWN, currentTurn, state.movingPieceHadMoved);
        } else {
            originalPiece.setHasMoved(state.movingPieceHadMoved);
        }

        // Move the main piece back
        setPiece(from, originalPiece);
        removePiece(to);

        // Restore King Position
        if (originalPiece.getType() == PieceType.KING) {
            if (currentTurn == PieceColor.WHITE) whiteKingPos = from;
            else blackKingPos = from;
        }

        // Restore Captured Piece (including En Passant)
        if (state.capturedPiece != null) {
            if (move.isEnPassant()) {
                int capturedPawnRow = currentTurn == PieceColor.WHITE ? to.getRow() - 1 : to.getRow() + 1;
                setPiece(new Position(capturedPawnRow, to.getCol()), state.capturedPiece);
            } else {
                setPiece(to, state.capturedPiece);
            }
        }

        // Revert Castling (Move the Rook back)
        if (move.isCastle()) {
            int rank = from.getRow();
            if (to.getCol() == 6) { // Kingside
                Piece rook = getPiece(rank, 5);
                setPiece(new Position(rank, 7), rook);
                removePiece(new Position(rank, 5));
            } else { // Queenside
                Piece rook = getPiece(rank, 3);
                setPiece(new Position(rank, 0), rook);
                removePiece(new Position(rank, 3));
            }
        }
    }

    private void updateCastlingRights(Position pos) {
        if (pos.equals(new Position(0, 4))) { whiteKingsideCastle = false; whiteQueensideCastle = false; }
        if (pos.equals(new Position(7, 4))) { blackKingsideCastle = false; blackQueensideCastle = false; }
        if (pos.equals(new Position(0, 0))) whiteQueensideCastle = false;
        if (pos.equals(new Position(0, 7))) whiteKingsideCastle = false;
        if (pos.equals(new Position(7, 0))) blackQueensideCastle = false;
        if (pos.equals(new Position(7, 7))) blackKingsideCastle = false;
    }

    public Board copy() {
        Board copy = new Board();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (squares[r][c] != null) {
                    copy.squares[r][c] = squares[r][c].copy();
                }
            }
        }
        copy.currentTurn          = this.currentTurn;
        copy.whiteKingsideCastle  = this.whiteKingsideCastle;
        copy.whiteQueensideCastle = this.whiteQueensideCastle;
        copy.blackKingsideCastle  = this.blackKingsideCastle;
        copy.blackQueensideCastle = this.blackQueensideCastle;
        copy.enPassantTarget      = this.enPassantTarget;
        copy.halfMoveClock        = this.halfMoveClock;
        copy.fullMoveNumber       = this.fullMoveNumber;

        // Copy king positions
        copy.whiteKingPos         = this.whiteKingPos;
        copy.blackKingPos         = this.blackKingPos;

        return copy;
    }

    public static Board startingPosition() {
        return fromFen(STARTING_FEN);
    }

    // --- INNER CLASS: Snapshot of state needed to Undo ---
    private static class SavedState implements Serializable {
        ChessMove move;
        Piece capturedPiece;
        Position enPassantTarget;
        boolean whiteKingsideCastle;
        boolean whiteQueensideCastle;
        boolean blackKingsideCastle;
        boolean blackQueensideCastle;
        int halfMoveClock;
        boolean movingPieceHadMoved;

        public SavedState(ChessMove move, Piece capturedPiece, Position enPassantTarget,
                          boolean wKC, boolean wQC, boolean bKC, boolean bQC,
                          int halfMoveClock, boolean movingPieceHadMoved) {
            this.move = move;
            this.capturedPiece = capturedPiece;
            this.enPassantTarget = enPassantTarget;
            this.whiteKingsideCastle = wKC;
            this.whiteQueensideCastle = wQC;
            this.blackKingsideCastle = bKC;
            this.blackQueensideCastle = bQC;
            this.halfMoveClock = halfMoveClock;
            this.movingPieceHadMoved = movingPieceHadMoved;
        }
    }
}