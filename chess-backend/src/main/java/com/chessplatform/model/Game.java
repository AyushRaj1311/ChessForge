package com.chessplatform.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 36)
    private String gameId; // UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_player_id")
    private User whitePlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_player_id")
    private User blackPlayer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameMode gameMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GameStatus status = GameStatus.WAITING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GameResult result = GameResult.IN_PROGRESS;

    @Column(columnDefinition = "TEXT")
    private String pgn; // Portable Game Notation

    @Column(columnDefinition = "TEXT")
    private String fen; // Final FEN position

    @Column(nullable = false)
    @Builder.Default
    private Integer whiteTimeRemaining = 0; // seconds

    @Column(nullable = false)
    @Builder.Default
    private Integer blackTimeRemaining = 0; // seconds

    @Column(nullable = false)
    @Builder.Default
    private Integer totalMoves = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean isAiGame = false;

    private String resultReason; // checkmate, timeout, resignation, draw

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
