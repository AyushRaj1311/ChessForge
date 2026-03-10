package com.chessplatform.model;

public enum GameMode {
    BULLET(60),
    BLITZ(300),
    RAPID(600);

    private final int timeSeconds;

    GameMode(int timeSeconds) {
        this.timeSeconds = timeSeconds;
    }

    public int getTimeSeconds() {
        return timeSeconds;
    }
}
