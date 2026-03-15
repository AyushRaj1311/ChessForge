package com.chessplatform.controller;

import com.chessplatform.dto.request.MatchmakingRequest;
import com.chessplatform.dto.response.ApiResponse;
import com.chessplatform.model.User;
import com.chessplatform.service.MatchmakingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matchmaking")
@RequiredArgsConstructor
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<String>> joinQueue(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody MatchmakingRequest request) {
        String message = matchmakingService.joinQueue(user, request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @DeleteMapping("/leave")
    public ResponseEntity<ApiResponse<String>> leaveQueue(@AuthenticationPrincipal User user) {
        matchmakingService.leaveQueue(user);
        return ResponseEntity.ok(ApiResponse.success("Left matchmaking queue"));
    }
}
