package com.chessplatform.service;

import com.chessplatform.dto.request.CreateGameRequest;
import com.chessplatform.dto.request.MakeMoveRequest;
import com.chessplatform.dto.response.GameResponse;
import com.chessplatform.dto.response.MoveResponse;
import com.chessplatform.engine.ai.MinimaxAI;
import com.chessplatform.engine.model.Board;
import com.chessplatform.engine.model.ChessMove;
import com.chessplatform.engine.model.MoveResult;
import com.chessplatform.engine.service.ChessEngine;
import com.chessplatform.engine.service.PgnService;
import com.chessplatform.exception.GameException;
import com.chessplatform.exception.ResourceNotFoundException;
import com.chessplatform.model.*;
import com.chessplatform.repository.GameRepository;
import com.chessplatform.repository.MoveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final ChessEngine chessEngine;
    private final MinimaxAI minimaxAI;
    private final PgnService pgnService;
    private final GameCacheService cacheService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public GameResponse createGame(User creator, CreateGameRequest request) {
        Game game = Game.builder()
            .gameId(UUID.randomUUID().toString())
            .whitePlayer(creator)
            .gameMode(request.getGameMode())
            .status(request.isVsAi() ? GameStatus.IN_PROGRESS : GameStatus.WAITING)
            .isAiGame(request.isVsAi())
            .whiteTimeRemaining(request.getGameMode().getTimeSeconds())
            .blackTimeRemaining(request.getGameMode().getTimeSeconds())
            .build();

        if (request.isVsAi()) {
            game.setStartedAt(LocalDateTime.now());
        }

        game = gameRepository.save(game);

        // Cache starting FEN
        cacheService.cacheFen(game.getGameId(), Board.STARTING_FEN);
        cacheService.cacheTimers(game.getGameId(),
            game.getWhiteTimeRemaining() * 1000,
            game.getBlackTimeRemaining() * 1000);

        log.info("Created game {} for user {}", game.getGameId(), creator.getUsername());
        return mapToResponse(game, Board.STARTING_FEN);
    }

    @Transactional
    public GameResponse joinGame(User joiner, String gameId) {
        Game game = findGame(gameId);
        if (game.getStatus() != GameStatus.WAITING) {
            throw new GameException("Game is not available to join");
        }
        if (game.getWhitePlayer().getId().equals(joiner.getId())) {
            throw new GameException("Cannot join your own game");
        }

        game.setBlackPlayer(joiner);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setStartedAt(LocalDateTime.now());
        game = gameRepository.save(game);

        String fen = getCurrentFen(game);
        // Notify via WebSocket
        messagingTemplate.convertAndSend("/topic/game/" + gameId, mapToResponse(game, fen));
        return mapToResponse(game, fen);
    }

    @Transactional
    public MoveResponse makeMove(User user, String gameId, MakeMoveRequest request) {
        Game game = findGame(gameId);

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            return MoveResponse.builder().valid(false).errorMessage("Game is not in progress").build();
        }

        // Validate it's the player's turn
        Board board = Board.fromFen(getCurrentFen(game));
        PieceColor playerColor = getPlayerColor(game, user);
        if (playerColor == null) {
            return MoveResponse.builder().valid(false).errorMessage("You are not in this game").build();
        }
        if (board.getCurrentTurn() != playerColor) {
            return MoveResponse.builder().valid(false).errorMessage("Not your turn").build();
        }

        MoveResult result = chessEngine.makeMove(board, request.getFrom(), request.getTo(), request.getPromotion());
        if (!result.isValid()) {
            return MoveResponse.builder().valid(false).errorMessage(result.getErrorMessage()).build();
        }

        // Save move
        int moveNumber = (int) moveRepository.countByGame(game) + 1;
        Move move = Move.builder()
            .game(game)
            .moveNumber(moveNumber)
            .playerColor(playerColor)
            .fromSquare(request.getFrom())
            .toSquare(request.getTo())
            .promotion(request.getPromotion())
            .san(result.getSan())
            .fen(result.getNewFen())
            .isCapture(result.isCapture())
            .isCheck(result.isCheck())
            .isCheckmate(result.isCheckmate())
            .isCastle(result.isCastle())
            .isEnPassant(result.isEnPassant())
            .build();
        moveRepository.save(move);

        // Update cache
        cacheService.cacheFen(gameId, result.getNewFen());
        game.setTotalMoves(moveNumber);

        MoveResponse playerMoveResponse = buildMoveResponse(result, request.getFrom(), request.getTo(), request.getPromotion(), game);

        // Check game over
        if (result.isCheckmate() || result.isStalemate()) {
            endGame(game, result, playerColor);
            gameRepository.save(game);
            broadcastMove(gameId, playerMoveResponse);
            return playerMoveResponse;
        }

        // Handle AI move
        if (game.isAiGame()) {
            PieceColor aiColor = playerColor.opposite();
            Board newBoard = result.getNewBoard();
            ChessMove aiChessMove = minimaxAI.getBestMove(newBoard, aiColor);
            if (aiChessMove != null) {
                MoveResult aiResult = chessEngine.makeMove(newBoard,
                    aiChessMove.getFrom().toAlgebraic(),
                    aiChessMove.getTo().toAlgebraic(),
                    aiChessMove.getPromotionPiece() != null ? aiChessMove.getPromotionPiece().name() : null);

                if (aiResult.isValid()) {
                    int aiMoveNumber = moveNumber + 1;
                    Move aiMove = Move.builder()
                        .game(game)
                        .moveNumber(aiMoveNumber)
                        .playerColor(aiColor)
                        .fromSquare(aiChessMove.getFrom().toAlgebraic())
                        .toSquare(aiChessMove.getTo().toAlgebraic())
                        .san(aiResult.getSan())
                        .fen(aiResult.getNewFen())
                        .isCapture(aiResult.isCapture())
                        .isCheck(aiResult.isCheck())
                        .isCheckmate(aiResult.isCheckmate())
                        .isCastle(aiResult.isCastle())
                        .build();
                    moveRepository.save(aiMove);
                    cacheService.cacheFen(gameId, aiResult.getNewFen());
                    game.setTotalMoves(aiMoveNumber);

                    MoveResponse aiMoveResp = buildMoveResponse(aiResult,
                        aiChessMove.getFrom().toAlgebraic(),
                        aiChessMove.getTo().toAlgebraic(), null, game);

                    if (aiResult.isCheckmate() || aiResult.isStalemate()) {
                        endGame(game, aiResult, aiColor);
                    }

                    gameRepository.save(game);
                    playerMoveResponse.setAiMove(aiMoveResp);
                }
            }
        }

        gameRepository.save(game);
        broadcastMove(gameId, playerMoveResponse);
        return playerMoveResponse;
    }

    @Transactional
    public GameResponse resignGame(User user, String gameId) {
        Game game = findGame(gameId);
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameException("Game is not in progress");
        }
        PieceColor playerColor = getPlayerColor(game, user);
        if (playerColor == null) throw new GameException("You are not in this game");

        game.setStatus(GameStatus.COMPLETED);
        game.setResult(playerColor == PieceColor.WHITE ? GameResult.BLACK_WIN : GameResult.WHITE_WIN);
        game.setResultReason("resignation");
        game.setCompletedAt(LocalDateTime.now());

        updatePgn(game);
        updateRatings(game);
        gameRepository.save(game);

        String fen = getCurrentFen(game);
        GameResponse response = mapToResponse(game, fen);
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/end", response);
        return response;
    }

    public GameResponse getGame(String gameId) {
        Game game = findGame(gameId);
        String fen = getCurrentFen(game);
        return mapToResponse(game, fen);
    }

    public List<Move> getGameMoves(String gameId) {
        Game game = findGame(gameId);
        return moveRepository.findByGameOrderByMoveNumberAsc(game);
    }

    public String getGamePgn(String gameId) {
        Game game = findGame(gameId);
        List<Move> moves = moveRepository.findByGameOrderByMoveNumberAsc(game);
        return pgnService.generatePgn(game, moves);
    }

    public Page<GameResponse> getPlayerGames(User user, int page, int size) {
        return gameRepository.findByPlayer(user, PageRequest.of(page, size, Sort.by("createdAt").descending()))
            .map(g -> mapToResponse(g, getCurrentFen(g)));
    }

    // ---- helpers ----

    private Game findGame(String gameId) {
        return gameRepository.findByGameId(gameId)
            .orElseThrow(() -> new ResourceNotFoundException("Game", "gameId", gameId));
    }

    private String getCurrentFen(Game game) {
        String cached = cacheService.getCachedFen(game.getGameId());
        if (cached != null) return cached;
        // Fall back to last move's FEN
        List<Move> moves = moveRepository.findByGameOrderByMoveNumberAsc(game);
        if (!moves.isEmpty()) return moves.get(moves.size() - 1).getFen();
        return Board.STARTING_FEN;
    }

    private PieceColor getPlayerColor(Game game, User user) {
        if (game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(user.getId()))
            return PieceColor.WHITE;
        if (!game.isAiGame() && game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(user.getId()))
            return PieceColor.BLACK;
        // In AI games, the human is always white
        if (game.isAiGame() && game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(user.getId()))
            return PieceColor.WHITE;
        return null;
    }

    private void endGame(Game game, MoveResult result, PieceColor lastMover) {
        game.setStatus(GameStatus.COMPLETED);
        game.setCompletedAt(LocalDateTime.now());
        if (result.isCheckmate()) {
            game.setResult(lastMover == PieceColor.WHITE ? GameResult.WHITE_WIN : GameResult.BLACK_WIN);
            game.setResultReason("checkmate");
        } else if (result.isStalemate()) {
            game.setResult(GameResult.DRAW);
            game.setResultReason("stalemate");
        }
        updatePgn(game);
        updateRatings(game);
    }

    private void updatePgn(Game game) {
        List<Move> moves = moveRepository.findByGameOrderByMoveNumberAsc(game);
        game.setPgn(pgnService.generatePgn(game, moves));
    }

    private void updateRatings(Game game) {
        if (game.isAiGame() || game.getWhitePlayer() == null || game.getBlackPlayer() == null) return;
        boolean isDraw = game.getResult() == GameResult.DRAW;
        User winner = game.getResult() == GameResult.WHITE_WIN ? game.getWhitePlayer() : game.getBlackPlayer();
        User loser  = game.getResult() == GameResult.WHITE_WIN ? game.getBlackPlayer() : game.getWhitePlayer();
        userService.updateRatingAfterGame(winner, loser, isDraw);
    }

    private MoveResponse buildMoveResponse(MoveResult result, String from, String to, String promotion, Game game) {
        return MoveResponse.builder()
            .valid(true)
            .from(from)
            .to(to)
            .san(result.getSan())
            .fen(result.getNewFen())
            .isCapture(result.isCapture())
            .isCheck(result.isCheck())
            .isCheckmate(result.isCheckmate())
            .isStalemate(result.isStalemate())
            .isCastle(result.isCastle())
            .promotion(promotion)
            .gameStatus(game.getStatus().name())
                .gameResult(game.getResult() != null ? game.getResult().name() : "IN_PROGRESS")
            .whiteTimeRemaining(game.getWhiteTimeRemaining())
            .blackTimeRemaining(game.getBlackTimeRemaining())
            .build();
    }

    private void broadcastMove(String gameId, MoveResponse response) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/move", response);
    }

    private GameResponse mapToResponse(Game game, String currentFen) {
        return GameResponse.builder()
            .gameId(game.getGameId())
            .whitePlayer(game.getWhitePlayer() != null ? game.getWhitePlayer().getUsername() : null)
            .blackPlayer(game.getBlackPlayer() != null ? game.getBlackPlayer().getUsername() : "AI")
            .gameMode(game.getGameMode())
            .status(game.getStatus())
            .result(game.getResult())
            .currentFen(currentFen)
            .whiteTimeRemaining(game.getWhiteTimeRemaining())
            .blackTimeRemaining(game.getBlackTimeRemaining())
            .totalMoves(game.getTotalMoves())
            .resultReason(game.getResultReason())
            .isAiGame(game.isAiGame())
            .createdAt(game.getCreatedAt())
            .startedAt(game.getStartedAt())
            .completedAt(game.getCompletedAt())
            .build();
    }
}
