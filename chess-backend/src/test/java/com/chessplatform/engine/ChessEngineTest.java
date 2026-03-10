package com.chessplatform.engine;

import com.chessplatform.engine.model.Board;
import com.chessplatform.engine.model.ChessMove;
import com.chessplatform.engine.model.MoveResult;
import com.chessplatform.engine.model.Position;
import com.chessplatform.engine.service.ChessEngine;
import com.chessplatform.engine.service.MoveGenerator;
import com.chessplatform.model.PieceColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChessEngineTest {

    private MoveGenerator moveGenerator;
    private ChessEngine chessEngine;

    @BeforeEach
    void setUp() {
        moveGenerator = new MoveGenerator();
        chessEngine   = new ChessEngine(moveGenerator);
    }

    @Test
    void testStartingPositionHas20Moves() {
        Board board = Board.startingPosition();
        List<ChessMove> moves = moveGenerator.generateLegalMoves(board, PieceColor.WHITE);
        assertThat(moves).hasSize(20); // 16 pawn + 4 knight moves
    }

    @Test
    void testValidPawnMove() {
        Board board = Board.startingPosition();
        MoveResult result = chessEngine.makeMove(board, "e2", "e4", null);
        assertThat(result.isValid()).isTrue();
        assertThat(result.getSan()).isEqualTo("e4");
    }

    @Test
    void testInvalidMove() {
        Board board = Board.startingPosition();
        MoveResult result = chessEngine.makeMove(board, "e2", "e5", null); // too far
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testScholarsMate() {
        Board board = Board.startingPosition();
        // e4
        board = chessEngine.makeMove(board, "e2", "e4", null).getNewBoard();
        // e5
        board = chessEngine.makeMove(board, "e7", "e5", null).getNewBoard();
        // Bc4
        board = chessEngine.makeMove(board, "f1", "c4", null).getNewBoard();
        // Nc6
        board = chessEngine.makeMove(board, "b8", "c6", null).getNewBoard();
        // Qh5
        board = chessEngine.makeMove(board, "d1", "h5", null).getNewBoard();
        // Nf6?
        board = chessEngine.makeMove(board, "g8", "f6", null).getNewBoard();
        // Qxf7# Scholar's Mate
        MoveResult result = chessEngine.makeMove(board, "h5", "f7", null);
        assertThat(result.isValid()).isTrue();
        assertThat(result.isCheckmate()).isTrue();
    }

    @Test
    void testFenRoundTrip() {
        String fen = Board.STARTING_FEN;
        Board board = Board.fromFen(fen);
        assertThat(board.toFen()).isEqualTo(fen);
    }

    @Test
    void testCastling() {
        // Position where white can castle kingside
        String fen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4";
        Board board = Board.fromFen(fen);
        MoveResult result = chessEngine.makeMove(board, "e1", "g1", null);
        assertThat(result.isValid()).isTrue();
        assertThat(result.isCastle()).isTrue();
        assertThat(result.getSan()).isEqualTo("O-O");
    }

    @Test
    void testNotYourTurn() {
        Board board = Board.startingPosition();
        MoveResult result = chessEngine.makeMove(board, "e7", "e5", null); // Black's turn would be first
        assertThat(result.isValid()).isFalse();
    }
}
