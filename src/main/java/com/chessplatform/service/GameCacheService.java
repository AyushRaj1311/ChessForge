package com.chessplatform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for active game state (FEN + timers).
 * No Redis required – uses ConcurrentHashMap.
 */
@Service
@Slf4j
public class GameCacheService {

    private final Map<String, String>   fenCache   = new ConcurrentHashMap<>();
    private final Map<String, int[]>    timerCache = new ConcurrentHashMap<>();

    // ── FEN ──────────────────────────────────────────────────────────────────

    public void cacheFen(String gameId, String fen) {
        fenCache.put(gameId, fen);
    }

    public String getCachedFen(String gameId) {
        return fenCache.get(gameId);
    }

    // ── Timers (milliseconds) ─────────────────────────────────────────────────

    public void cacheTimers(String gameId, int whiteMs, int blackMs) {
        timerCache.put(gameId, new int[]{whiteMs, blackMs});
    }

    /** @return int[]{whiteMs, blackMs} or null if not cached */
    public int[] getCachedTimers(String gameId) {
        return timerCache.get(gameId);
    }

    // ── Eviction ─────────────────────────────────────────────────────────────

    public void evictGame(String gameId) {
        fenCache.remove(gameId);
        timerCache.remove(gameId);
        log.debug("Evicted cache for game {}", gameId);
    }
}
