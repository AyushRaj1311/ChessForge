-- ============================================================
--  Chess Platform – MySQL Schema
--  Run this in MySQL Workbench to set up the database.
--  Spring Boot (ddl-auto=update) will also auto-create tables,
--  but you can run this manually first if you prefer.
-- ============================================================

CREATE DATABASE IF NOT EXISTS chess_platform
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chess_platform;

-- ── Users ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(100) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    rating          INT          NOT NULL DEFAULT 1200,
    games_played    INT          NOT NULL DEFAULT 0,
    games_won       INT          NOT NULL DEFAULT 0,
    games_lost      INT          NOT NULL DEFAULT 0,
    games_draw      INT          NOT NULL DEFAULT 0,
    enabled         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL,
    last_login_at   DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_username (username),
    UNIQUE KEY uq_email    (email),
    KEY idx_rating (rating DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Games ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS games (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    game_id              VARCHAR(36)  NOT NULL,
    white_player_id      BIGINT,
    black_player_id      BIGINT,
    game_mode            VARCHAR(20)  NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'WAITING',
    result               VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    pgn                  TEXT,
    fen                  VARCHAR(120),
    white_time_remaining INT          NOT NULL DEFAULT 0,
    black_time_remaining INT          NOT NULL DEFAULT 0,
    total_moves          INT          NOT NULL DEFAULT 0,
    is_ai_game           TINYINT(1)   NOT NULL DEFAULT 0,
    result_reason        VARCHAR(50),
    created_at           DATETIME     NOT NULL,
    started_at           DATETIME,
    completed_at         DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_game_id (game_id),
    KEY idx_status        (status),
    KEY idx_white_player  (white_player_id),
    KEY idx_black_player  (black_player_id),
    CONSTRAINT fk_game_white FOREIGN KEY (white_player_id) REFERENCES users (id),
    CONSTRAINT fk_game_black FOREIGN KEY (black_player_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Moves ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS moves (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    game_id            BIGINT      NOT NULL,
    move_number        INT         NOT NULL,
    player_color       VARCHAR(10) NOT NULL,
    from_square        VARCHAR(4)  NOT NULL,
    to_square          VARCHAR(4)  NOT NULL,
    promotion          VARCHAR(10),
    san                VARCHAR(10) NOT NULL,
    fen                VARCHAR(120) NOT NULL,
    is_capture         TINYINT(1),
    is_check           TINYINT(1),
    is_checkmate       TINYINT(1),
    is_castle          TINYINT(1),
    is_en_passant      TINYINT(1),
    time_remaining_ms  INT,
    created_at         DATETIME    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_game_moves (game_id, move_number),
    CONSTRAINT fk_move_game FOREIGN KEY (game_id) REFERENCES games (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Matchmaking Queue ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS matchmaking_queue (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    game_mode    VARCHAR(20) NOT NULL,
    rating_min   INT         NOT NULL,
    rating_max   INT         NOT NULL,
    active       TINYINT(1)  NOT NULL DEFAULT 1,
    enqueued_at  DATETIME    NOT NULL,
    PRIMARY KEY (id),
    KEY idx_active_mode (active, game_mode),
    KEY idx_user_active (user_id, active),
    CONSTRAINT fk_queue_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Optional: seed an admin user (password = "admin123") ─────────────────────
-- INSERT INTO users (username, email, password, role, created_at)
-- VALUES ('admin', 'admin@chess.com',
--   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
--   'ADMIN', NOW());

SELECT 'Schema created successfully!' AS status;
