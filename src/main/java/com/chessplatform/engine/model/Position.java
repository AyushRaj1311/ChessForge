package com.chessplatform.engine.model;

import lombok.Getter;
import java.io.Serializable;

@Getter
// REMOVED: @Setter (Because fields are final, they cannot have setters)
public class Position implements Serializable {

    private final int row;
    private final int col;

    // --- PRO OPTIMIZATION: The 64-Square Cache ---
    private static final Position[][] CACHE = new Position[8][8];

    static {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                CACHE[r][c] = new Position(r, c);
            }
        }
    }

    // Keep constructor public so existing code doesn't immediately break,
    // but the goal is to transition to using Position.get()
    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    // --- NEW: Factory Method ---
    // Use this instead of 'new Position(r, c)' to eliminate RAM usage!
    public static Position get(int row, int col) {
        if (row >= 0 && row < 8 && col >= 0 && col < 8) {
            return CACHE[row][col];
        }
        // Fallback just in case your move generator temporarily
        // checks an out-of-bounds square before validating it
        return new Position(row, col);
    }

    public static Position fromAlgebraic(String algebraic) {
        if (algebraic == null || algebraic.length() != 2) {
            throw new IllegalArgumentException("Invalid algebraic notation: " + algebraic);
        }
        int col = algebraic.charAt(0) - 'a';
        int row = algebraic.charAt(1) - '1';

        // Grab from cache instead of making a new object!
        return Position.get(row, col);
    }

    public String toAlgebraic() {
        return "" + (char) ('a' + col) + (char) ('1' + row);
    }

    public boolean isValid() {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
        return row == p.row && col == p.col;
    }

    @Override
    public int hashCode() {
        // Optimized hash code. Since bounds are 0-7, this perfectly maps to 0-63
        return row * 8 + col;
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}