package com.chessplatform.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "moves")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Move {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private Integer moveNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PieceColor playerColor;

    @Column(nullable = false, length = 4)
    private String fromSquare; // e.g. "e2"

    @Column(nullable = false, length = 4)
    private String toSquare;   // e.g. "e4"

    @Column(length = 10)
    private String promotion;  // piece type if pawn promotion

    @Column(nullable = false, length = 10)
    private String san; // Standard Algebraic Notation e.g. "e4", "Nf3", "O-O"

    @Column(nullable = false, length = 100)
    private String fen; // board FEN after this move

    private Boolean isCapture;
    private Boolean isCheck;
    private Boolean isCheckmate;
    private Boolean isCastle;
    private Boolean isEnPassant;

    private Integer timeRemainingMs; // time remaining after this move

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
