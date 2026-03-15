package com.chessplatform.websocket.handler;

import com.chessplatform.dto.request.MakeMoveRequest;
import com.chessplatform.dto.response.MoveResponse;
import com.chessplatform.model.User;
import com.chessplatform.security.JwtUtil;
import com.chessplatform.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;

//@Controller
@RequiredArgsConstructor
@Slf4j
public class GameWebSocketHandler {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    // Client sends: /app/game/{gameId}/move
    @MessageMapping("/game/{gameId}/move")
    public void handleMove(
            @DestinationVariable String gameId,
            @Payload MakeMoveRequest request,
            SimpMessageHeaderAccessor headerAccessor) {
        try {
            User user = extractUser(headerAccessor);
            if (user == null) {
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/error",
                    "Unauthorized: invalid or missing token");
                return;
            }
            MoveResponse response = gameService.makeMove(user, gameId, request);
            // Move is broadcast inside GameService
            if (!response.isValid()) {
                messagingTemplate.convertAndSendToUser(user.getUsername(),
                    "/queue/game/" + gameId + "/error", response.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("WebSocket move error for game {}: {}", gameId, e.getMessage());
        }
    }

    // Client sends: /app/game/{gameId}/join
    @MessageMapping("/game/{gameId}/join")
    public void handleJoin(
            @DestinationVariable String gameId,
            SimpMessageHeaderAccessor headerAccessor) {
        try {
            User user = extractUser(headerAccessor);
            if (user != null) {
                gameService.joinGame(user, gameId);
            }
        } catch (Exception e) {
            log.error("WebSocket join error for game {}: {}", gameId, e.getMessage());
        }
    }

    private User extractUser(SimpMessageHeaderAccessor headerAccessor) {
        try {
            String token = null;
            java.util.List<String> authHeaders = headerAccessor.getNativeHeader("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
            if (token == null) return null;

            String username = jwtUtil.extractUsername(token);
            if (username == null) return null;
            
            Object principal = userDetailsService.loadUserByUsername(username);
            if (!(principal instanceof User user)) return null;
            
            if (jwtUtil.validateToken(token, user)) return user;
        } catch (Exception e) {
            log.warn("Failed to extract user from WebSocket header: {}", e.getMessage());
        }
        return null;
    }
}
