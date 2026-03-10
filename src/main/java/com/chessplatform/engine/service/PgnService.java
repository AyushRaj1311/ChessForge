package com.chessplatform.engine.service;

import com.chessplatform.model.Game;
import com.chessplatform.model.Move;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PgnService {

    public String generatePgn(Game game, List<Move> moves) {
        StringBuilder pgn = new StringBuilder();

        // PGN Headers
        pgn.append("[Event \"Chess Platform Game\"]\n");
        pgn.append("[Site \"chess-platform.com\"]\n");

        // Null safety for date
        String dateStr = game.getCreatedAt() != null
                ? game.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                : "????.??.??";
        pgn.append("[Date \"").append(dateStr).append("\"]\n");

        pgn.append("[Round \"-\"]\n");

        String whiteName = game.getWhitePlayer() != null ? game.getWhitePlayer().getUsername() : "AI";
        String blackName = game.getBlackPlayer() != null ? game.getBlackPlayer().getUsername() : "AI";
        pgn.append("[White \"").append(whiteName).append("\"]\n");
        pgn.append("[Black \"").append(blackName).append("\"]\n");

        if (game.getGameMode() != null) {
            pgn.append("[TimeControl \"").append(game.getGameMode().getTimeSeconds()).append("\"]\n");
        }

        String resultStr = switch (game.getResult()) {
            case WHITE_WIN -> "1-0";
            case BLACK_WIN -> "0-1";
            case DRAW      -> "1/2-1/2";
            default        -> "*";
        };
        pgn.append("[Result \"").append(resultStr).append("\"]\n");

        // Standard PGN requires exactly one empty line between headers and moves
        pgn.append("\n");

        // Move text
        int moveNum = 1;
        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            if (i % 2 == 0) {
                pgn.append(moveNum).append(". ");
                moveNum++;
            }
            pgn.append(move.getSan()).append(" ");
        }

        pgn.append(resultStr).append("\n");
        return pgn.toString();
    }

    public List<String> parsePgn(String pgn) {
        if (pgn == null || pgn.isBlank()) {
            return List.of();
        }

        // 1. Remove Headers (e.g., [Event "Tournament"])
        // (?m) enables multiline mode so ^ matches the start of each line
        String body = pgn.replaceAll("(?m)^\\[.*\\]$", "");

        // 2. Remove standard comments { ... } and inline comments ; ...
        body = body.replaceAll("\\{.*?\\}", "");
        body = body.replaceAll(";.*$", "");

        // 3. Remove variations ( ... )
        // Note: This regex handles single-level variations. Nested variations
        // are rare but require a true parser rather than Regex if encountered.
        body = body.replaceAll("\\([^()]*\\)", "");

        // 4. Remove Numeric Annotation Glyphs like $1, $3
        body = body.replaceAll("\\$\\d+", "");

        // 5. Remove Move Numbers (e.g., "1.", "12...", "24 .")
        body = body.replaceAll("\\d+\\s*\\.+", "");

        // 6. Remove Game Results from text
        body = body.replaceAll("1-0|0-1|1/2-1/2|\\*", "");

        // 7. Normalize whitespace, remove standard check/mate annotations if desired
        // (Though usually you want to keep + and #, so we just trim spaces)
        body = body.trim();

        // Split by whitespace and filter out any empty strings that got left behind
        return Arrays.stream(body.split("\\s+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}