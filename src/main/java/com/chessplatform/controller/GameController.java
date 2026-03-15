package com.chessplatform.controller;

import com.chessplatform.dto.request.CreateGameRequest;
import com.chessplatform.dto.request.MakeMoveRequest;
import com.chessplatform.dto.response.ApiResponse;
import com.chessplatform.dto.response.GameResponse;
import com.chessplatform.dto.response.MoveResponse;
import com.chessplatform.model.Move;
import com.chessplatform.model.User;
import com.chessplatform.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping
    public ResponseEntity<ApiResponse<GameResponse>> createGame(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateGameRequest request) {
        GameResponse response = gameService.createGame(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Game created", response));
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<ApiResponse<GameResponse>> joinGame(
            @AuthenticationPrincipal User user,
            @PathVariable String gameId) {
        GameResponse response = gameService.joinGame(user, gameId);
        return ResponseEntity.ok(ApiResponse.success("Joined game", response));
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<ApiResponse<GameResponse>> getGame(@PathVariable String gameId) {
        return ResponseEntity.ok(ApiResponse.success(gameService.getGame(gameId)));
    }

    @PostMapping("/{gameId}/moves")
    public ResponseEntity<ApiResponse<MoveResponse>> makeMove(
            @AuthenticationPrincipal User user,
            @PathVariable String gameId,
            @Valid @RequestBody MakeMoveRequest request) {
        MoveResponse response = gameService.makeMove(user, gameId, request);
        if (!response.isValid()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(response.getErrorMessage()));
        }
        return ResponseEntity.ok(ApiResponse.success("Move made", response));
    }

    @GetMapping("/{gameId}/moves")
    public ResponseEntity<ApiResponse<List<Move>>> getGameMoves(@PathVariable String gameId) {
        return ResponseEntity.ok(ApiResponse.success(gameService.getGameMoves(gameId)));
    }

    @GetMapping("/{gameId}/pgn")
    public ResponseEntity<String> getGamePgn(@PathVariable String gameId) {
        return ResponseEntity.ok()
            .header("Content-Type", "text/plain")
            .body(gameService.getGamePgn(gameId));
    }

    @PostMapping("/{gameId}/resign")
    public ResponseEntity<ApiResponse<GameResponse>> resignGame(
            @AuthenticationPrincipal User user,
            @PathVariable String gameId) {
        GameResponse response = gameService.resignGame(user, gameId);
        return ResponseEntity.ok(ApiResponse.success("Game resigned", response));
    }

    @PostMapping("/{gameId}/draw/offer")
    public ResponseEntity<ApiResponse<GameResponse>> offerDraw(
            @AuthenticationPrincipal User user,
            @PathVariable String gameId) {
        GameResponse response = gameService.offerDraw(user, gameId);
        return ResponseEntity.ok(ApiResponse.success("Draw offered", response));
    }

    @PostMapping("/{gameId}/draw/accept")
    public ResponseEntity<ApiResponse<GameResponse>> acceptDraw(
            @AuthenticationPrincipal User user,
            @PathVariable String gameId) {
        GameResponse response = gameService.acceptDraw(user, gameId);
        return ResponseEntity.ok(ApiResponse.success("Draw accepted", response));
    }

    @PostMapping("/{gameId}/draw/decline")
    public ResponseEntity<ApiResponse<GameResponse>> declineDraw(
            @AuthenticationPrincipal User user,
            @PathVariable String gameId) {
        GameResponse response = gameService.declineDraw(user, gameId);
        return ResponseEntity.ok(ApiResponse.success("Draw declined", response));
    }

    @GetMapping("/{gameId}/spectate")
    public ResponseEntity<ApiResponse<GameResponse>> spectateGame(@PathVariable String gameId) {
        return ResponseEntity.ok(ApiResponse.success(gameService.getGame(gameId)));
    }

    @GetMapping("/my-games")
    public ResponseEntity<ApiResponse<Page<GameResponse>>> getMyGames(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(gameService.getPlayerGames(user, page, size)));
    }
}
