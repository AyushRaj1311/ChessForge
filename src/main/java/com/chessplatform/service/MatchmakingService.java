package com.chessplatform.service;

import com.chessplatform.dto.request.CreateGameRequest;
import com.chessplatform.dto.request.MatchmakingRequest;
import com.chessplatform.model.GameMode;
import com.chessplatform.model.MatchmakingEntry;
import com.chessplatform.model.User;
import com.chessplatform.repository.MatchmakingEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchmakingService {

    private final MatchmakingEntryRepository matchmakingRepository;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int RATING_RANGE = 200;

    @Transactional
    public String joinQueue(User user, MatchmakingRequest request) {
        // Remove old entry if exists
        matchmakingRepository.findByUserAndActiveTrue(user)
            .ifPresent(e -> { e.setActive(false); matchmakingRepository.save(e); });

        int ratingMin = Math.max(100, user.getRating() - RATING_RANGE);
        int ratingMax = user.getRating() + RATING_RANGE;

        MatchmakingEntry entry = MatchmakingEntry.builder()
            .user(user)
            .gameMode(request.getGameMode())
            .ratingMin(ratingMin)
            .ratingMax(ratingMax)
            .build();
        matchmakingRepository.save(entry);

        log.info("User {} joined {} queue", user.getUsername(), request.getGameMode());
        return "Joined matchmaking queue for " + request.getGameMode();
    }

    @Transactional
    public void leaveQueue(User user) {
        matchmakingRepository.findByUserAndActiveTrue(user)
            .ifPresent(e -> { e.setActive(false); matchmakingRepository.save(e); });
    }

    @Scheduled(fixedDelayString = "${chess.matchmaking.check-interval-seconds:5}000")
    @Transactional
    public void processMatchmaking() {
        for (GameMode mode : GameMode.values()) {
            processMode(mode);
        }
    }

    private void processMode(GameMode mode) {
        List<MatchmakingEntry> queue = matchmakingRepository.findActiveByMode(mode, null);
        // Simple greedy matching
        for (int i = 0; i < queue.size() - 1; i++) {
            MatchmakingEntry p1 = queue.get(i);
            if (!p1.isActive()) continue;
            for (int j = i + 1; j < queue.size(); j++) {
                MatchmakingEntry p2 = queue.get(j);
                if (!p2.isActive()) continue;
                if (isCompatible(p1, p2)) {
                    createMatchedGame(p1, p2, mode);
                    break;
                }
            }
        }
    }

    private boolean isCompatible(MatchmakingEntry a, MatchmakingEntry b) {
        return a.getGameMode() == b.getGameMode()
            && a.getUser().getRating() >= b.getRatingMin()
            && a.getUser().getRating() <= b.getRatingMax()
            && b.getUser().getRating() >= a.getRatingMin()
            && b.getUser().getRating() <= a.getRatingMax();
    }

    private void createMatchedGame(MatchmakingEntry p1, MatchmakingEntry p2, GameMode mode) {
        // Deactivate queue entries
        p1.setActive(false);
        p2.setActive(false);
        matchmakingRepository.save(p1);
        matchmakingRepository.save(p2);

        CreateGameRequest req = new CreateGameRequest();
        req.setGameMode(mode);
        req.setVsAi(false);

        var gameResp = gameService.createGame(p1.getUser(), req);
        gameService.joinGame(p2.getUser(), gameResp.getGameId());

        // Notify both players
        messagingTemplate.convertAndSendToUser(p1.getUser().getUsername(),
            "/queue/matchmaking", Map.of("gameId", gameResp.getGameId(), "color", "WHITE"));
        messagingTemplate.convertAndSendToUser(p2.getUser().getUsername(),
            "/queue/matchmaking", Map.of("gameId", gameResp.getGameId(), "color", "BLACK"));

        log.info("Matched {} vs {} in {} game {}", p1.getUser().getUsername(),
            p2.getUser().getUsername(), mode, gameResp.getGameId());
    }
}
