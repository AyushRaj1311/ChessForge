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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void checkTimeouts() {
        List<Game> activeGames = gameRepository.findAllByStatus(GameStatus.IN_PROGRESS);
        LocalDateTime now = LocalDateTime.now();

        for (Game game : activeGames) {
            try {
                processGameTimeout(game, now);
            } catch (Exception e) {
                log.error("Error processing timeout for game {}: {}", game.getGameId(), e.getMessage());
            }
        }
    }

    private void processGameTimeout(Game game, LocalDateTime now) {
        Board board = Board.fromFen(getCurrentFen(game));
        PieceColor turn = board.getCurrentTurn();
        
        // Find last move time
        LocalDateTime lastMoveTime = game.getStartedAt();
        List<Move> moves = moveRepository.findByGameOrderByMoveNumberAsc(game);
        if (!moves.isEmpty()) {
            lastMoveTime = moves.get(moves.size() - 1).getCreatedAt();
        }

        long secondsElapsed = Duration.between(lastMoveTime, now).getSeconds();
        
        if (turn == PieceColor.WHITE) {
            int remaining = game.getWhiteTimeRemaining() - (int) secondsElapsed;
            if (remaining <= 0) {
                game.setWhiteTimeRemaining(0);
                handleTimeout(game, PieceColor.WHITE);
            }
        } else {
            int remaining = game.getBlackTimeRemaining() - (int) secondsElapsed;
            if (remaining <= 0) {
                game.setBlackTimeRemaining(0);
                handleTimeout(game, PieceColor.BLACK);
            }
        }
    }

    private void handleTimeout(Game game, PieceColor timedOutColor) {
        game.setStatus(GameStatus.COMPLETED);
        game.setResult(timedOutColor == PieceColor.WHITE ? GameResult.BLACK_WIN : GameResult.WHITE_WIN);
        game.setResultReason("timeout");
        game.setCompletedAt(LocalDateTime.now());
        
        updatePgn(game);
        updateRatings(game);
        gameRepository.save(game);
        
        messagingTemplate.convertAndSend("/topic/game/" + game.getGameId() + "/end", mapToResponse(game, getCurrentFen(game)));
    }

    @Transactional
    public GameResponse createGame(User creator, CreateGameRequest request) {
        Game game = Game.builder()
            .gameId(UUID.randomUUID().toString())
            .whitePlayer(creator)
            .gameMode(request.getGameMode())
            .status(request.isVsAi() ? GameStatus.IN_PROGRESS : GameStatus.WAITING)
            .isAiGame(request.isVsAi())
            .aiLevel(request.isVsAi() ? request.getAiLevel() : null)
            .whiteTimeRemaining(request.getGameMode().getTimeSeconds())
            .blackTimeRemaining(request.getGameMode().getTimeSeconds())
            .build();

        if (request.isVsAi()) {
            game.setStartedAt(LocalDateTime.now());
        }

        Game savedGame = gameRepository.save(game);

        // Cache starting FEN
        cacheService.cacheFen(savedGame.getGameId(), Board.STARTING_FEN);
        cacheService.cacheTimers(savedGame.getGameId(),
            savedGame.getWhiteTimeRemaining() * 1000,
            savedGame.getBlackTimeRemaining() * 1000);

        log.info("Created game {} for user {}", savedGame.getGameId(), creator.getUsername());
        return mapToResponse(savedGame, Board.STARTING_FEN);
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
        Game savedGame = gameRepository.save(game);

        String fen = getCurrentFen(savedGame);
        // Notify via WebSocket
        messagingTemplate.convertAndSend("/topic/game/" + gameId, mapToResponse(savedGame, fen));
        return mapToResponse(savedGame, fen);
    }

    @Transactional
    public MoveResponse makeMove(User user, String gameId, MakeMoveRequest request) {
        Game game = findGame(gameId);
        log.info("Processing move for game {}: {} to {} by user {}", gameId, request.getFrom(), request.getTo(), user.getUsername());

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            log.warn("Move rejected: Game {} is in status {}", gameId, game.getStatus());
            return MoveResponse.builder().valid(false).errorMessage("Game is not in progress").build();
        }

        // Validate it's the player's turn
        String currentFen = getCurrentFen(game);
        Board board = Board.fromFen(currentFen);
        PieceColor playerColor = getPlayerColor(game, user);
        
        log.info("Game turn: {}, Player color: {}, FEN: {}", board.getCurrentTurn(), playerColor, currentFen);

        if (playerColor == null) {
            log.warn("Move rejected: User {} is not part of game {}", user.getUsername(), gameId);
            return MoveResponse.builder().valid(false).errorMessage("You are not in this game").build();
        }
        if (board.getCurrentTurn() != playerColor) {
            log.warn("Move rejected: Not {}'s turn in game {}", playerColor, gameId);
            return MoveResponse.builder().valid(false).errorMessage("Not your turn").build();
        }

        MoveResult result = chessEngine.makeMove(board, request.getFrom(), request.getTo(), request.getPromotion());
        if (!result.isValid()) {
            log.warn("Move rejected: Engine says invalid move ({} to {}) - {}", request.getFrom(), request.getTo(), result.getErrorMessage());
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

        // Check game over
        if (result.isCheckmate() || result.isStalemate()) {
            endGame(game, result, playerColor);
        }

        Game savedGame = gameRepository.save(game);
        MoveResponse playerMoveResponse = buildMoveResponse(result, request.getFrom(), request.getTo(), request.getPromotion(), savedGame);

        // Broadcast move before handling AI
        broadcastMove(gameId, playerMoveResponse);

        // Handle AI move
        if (savedGame.getStatus() == GameStatus.IN_PROGRESS && savedGame.isAiGame()) {
            PieceColor aiColor = playerColor.opposite();
            Board newBoard = result.getNewBoard();
            
            // Map AI level (1-10) to depth (1-5)
            int depth = Math.max(1, (savedGame.getAiLevel() != null ? savedGame.getAiLevel() : 1) / 2);
            ChessMove aiChessMove = minimaxAI.getBestMove(newBoard, aiColor, depth);
            if (aiChessMove != null) {
                MoveResult aiResult = chessEngine.makeMove(newBoard,
                    aiChessMove.getFrom().toAlgebraic(),
                    aiChessMove.getTo().toAlgebraic(),
                    aiChessMove.getPromotionPiece() != null ? aiChessMove.getPromotionPiece().name() : null);

                if (aiResult.isValid()) {
                    int aiMoveNumber = moveNumber + 1;
                    Move aiMove = Move.builder()
                        .game(savedGame)
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
                    savedGame.setTotalMoves(aiMoveNumber);

                    if (aiResult.isCheckmate() || aiResult.isStalemate()) {
                        endGame(savedGame, aiResult, aiColor);
                    }

                    gameRepository.save(savedGame);
                    MoveResponse aiMoveResp = buildMoveResponse(aiResult,
                        aiChessMove.getFrom().toAlgebraic(),
                        aiChessMove.getTo().toAlgebraic(), null, savedGame);
                    
                    broadcastMove(gameId, aiMoveResp);
                    playerMoveResponse.setAiMove(aiMoveResp);
                }
            }
        }

        gameRepository.save(savedGame);
        // broadcastMove(gameId, playerMoveResponse); // Removed, now called above
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

    @Transactional
    public GameResponse offerDraw(User user, String gameId) {
        Game game = findGame(gameId);
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameException("Game is not in progress");
        }
        PieceColor playerColor = getPlayerColor(game, user);
        if (playerColor == null) throw new GameException("You are not in this game");

        game.setDrawOffer(playerColor);
        gameRepository.save(game);

        GameResponse response = mapToResponse(game, getCurrentFen(game));
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/draw-offer", response);
        return response;
    }

    @Transactional
    public GameResponse acceptDraw(User user, String gameId) {
        Game game = findGame(gameId);
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameException("Game is not in progress");
        }
        PieceColor playerColor = getPlayerColor(game, user);
        if (playerColor == null) throw new GameException("You are not in this game");

        if (game.getDrawOffer() == null || game.getDrawOffer() == playerColor) {
            throw new GameException("No draw offer to accept");
        }

        game.setStatus(GameStatus.COMPLETED);
        game.setResult(GameResult.DRAW);
        game.setResultReason("draw by agreement");
        game.setDrawOffer(null);
        game.setCompletedAt(LocalDateTime.now());

        updatePgn(game);
        updateRatings(game);
        gameRepository.save(game);

        GameResponse response = mapToResponse(game, getCurrentFen(game));
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/end", response);
        return response;
    }

    @Transactional
    public GameResponse declineDraw(User user, String gameId) {
        Game game = findGame(gameId);
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameException("Game is not in progress");
        }
        PieceColor playerColor = getPlayerColor(game, user);
        if (playerColor == null) throw new GameException("You are not in this game");

        game.setDrawOffer(null);
        gameRepository.save(game);

        GameResponse response = mapToResponse(game, getCurrentFen(game));
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/draw-decline", response);
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
        } else if (result.getNewFen().contains("insufficient")) { // This is a bit hacky, but the engine already flags it in isStalemate now
            game.setResult(GameResult.DRAW);
            game.setResultReason("insufficient material");
        } else {
            game.setResult(GameResult.DRAW);
            game.setResultReason("draw");
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
        User winner, loser;
        if (isDraw) {
            winner = game.getWhitePlayer();
            loser = game.getBlackPlayer();
        } else {
            winner = game.getResult() == GameResult.WHITE_WIN ? game.getWhitePlayer() : game.getBlackPlayer();
            loser  = game.getResult() == GameResult.WHITE_WIN ? game.getBlackPlayer() : game.getWhitePlayer();
        }

        UserService.RatingChange change = userService.updateRatingAfterGame(winner, loser, isDraw);

        if (winner.getId().equals(game.getWhitePlayer().getId())) {
            game.setWhiteRatingChange(change.winnerChange());
            game.setBlackRatingChange(change.loserChange());
        } else {
            game.setWhiteRatingChange(change.loserChange());
            game.setBlackRatingChange(change.winnerChange());
        }
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
            .resultReason(game.getResultReason())
            .whiteTimeRemaining(game.getWhiteTimeRemaining())
            .blackTimeRemaining(game.getBlackTimeRemaining())
            .whiteRating(game.getWhitePlayer() != null ? game.getWhitePlayer().getRating() : null)
            .blackRating(game.getBlackPlayer() != null ? game.getBlackPlayer().getRating() : null)
            .whiteRatingChange(game.getWhiteRatingChange())
            .blackRatingChange(game.getBlackRatingChange())
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
            .whiteRating(game.getWhitePlayer() != null ? game.getWhitePlayer().getRating() : null)
            .blackRating(game.getBlackPlayer() != null ? game.getBlackPlayer().getRating() : null)
            .whiteRatingChange(game.getWhiteRatingChange())
            .blackRatingChange(game.getBlackRatingChange())
            .gameMode(game.getGameMode())
            .status(game.getStatus())
            .result(game.getResult())
            .currentFen(currentFen)
            .whiteTimeRemaining(game.getWhiteTimeRemaining())
            .blackTimeRemaining(game.getBlackTimeRemaining())
            .totalMoves(game.getTotalMoves())
            .resultReason(game.getResultReason())
            .drawOffer(game.getDrawOffer() != null ? game.getDrawOffer().name() : null)
            .isAiGame(game.isAiGame())
            .createdAt(game.getCreatedAt())
            .startedAt(game.getStartedAt())
            .completedAt(game.getCompletedAt())
            .build();
    }
}
