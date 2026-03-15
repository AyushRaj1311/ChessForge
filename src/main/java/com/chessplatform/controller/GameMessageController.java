package com.chessplatform.controller;

import com.chessplatform.dto.request.MakeMoveRequest;
import com.chessplatform.dto.response.MoveResponse;
import com.chessplatform.model.User;
import com.chessplatform.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GameMessageController {

    private final GameService gameService;

    @MessageMapping("/game/{gameId}/move")
    public void handleMove(@DestinationVariable String gameId, 
                           MakeMoveRequest request, 
                           SimpMessageHeaderAccessor headerAccessor) {
        
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) headerAccessor.getUser();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            log.warn("Unauthorized WebSocket move attempt for game {}", gameId);
            return;
        }

        log.info("Received WebSocket move for game {}: {} to {}", gameId, request.getFrom(), request.getTo());
        try {
            gameService.makeMove(user, gameId, request);
        } catch (Exception e) {
            log.error("Error processing WebSocket move: {}", e.getMessage());
        }
    }
}
