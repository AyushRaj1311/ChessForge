# Chess Platform - Database Design

## Schema Overview

### users
```sql
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  UNIQUE NOT NULL,
    email           VARCHAR(100) UNIQUE NOT NULL,
    password        VARCHAR(255) NOT NULL,
    role            ENUM('USER','ADMIN') DEFAULT 'USER',
    rating          INT DEFAULT 1200,
    games_played    INT DEFAULT 0,
    games_won       INT DEFAULT 0,
    games_lost      INT DEFAULT 0,
    games_draw      INT DEFAULT 0,
    enabled         BOOLEAN DEFAULT TRUE,
    created_at      DATETIME NOT NULL,
    last_login_at   DATETIME,
    INDEX idx_rating (rating DESC),
    INDEX idx_username (username)
);
```

### games
```sql
CREATE TABLE games (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id              VARCHAR(36) UNIQUE NOT NULL,  -- UUID
    white_player_id      BIGINT REFERENCES users(id),
    black_player_id      BIGINT REFERENCES users(id),
    game_mode            ENUM('BULLET','BLITZ','RAPID') NOT NULL,
    status               ENUM('WAITING','IN_PROGRESS','COMPLETED','ABANDONED') DEFAULT 'WAITING',
    result               ENUM('WHITE_WIN','BLACK_WIN','DRAW','IN_PROGRESS') DEFAULT 'IN_PROGRESS',
    pgn                  TEXT,
    fen                  VARCHAR(100),
    white_time_remaining INT DEFAULT 0,
    black_time_remaining INT DEFAULT 0,
    total_moves          INT DEFAULT 0,
    is_ai_game           BOOLEAN DEFAULT FALSE,
    result_reason        VARCHAR(50),
    created_at           DATETIME NOT NULL,
    started_at           DATETIME,
    completed_at         DATETIME,
    INDEX idx_game_id (game_id),
    INDEX idx_status (status),
    INDEX idx_white_player (white_player_id),
    INDEX idx_black_player (black_player_id)
);
```

### moves
```sql
CREATE TABLE moves (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id             BIGINT NOT NULL REFERENCES games(id),
    move_number         INT NOT NULL,
    player_color        ENUM('WHITE','BLACK') NOT NULL,
    from_square         VARCHAR(4) NOT NULL,
    to_square           VARCHAR(4) NOT NULL,
    promotion           VARCHAR(10),
    san                 VARCHAR(10) NOT NULL,
    fen                 VARCHAR(100) NOT NULL,
    is_capture          BOOLEAN,
    is_check            BOOLEAN,
    is_checkmate        BOOLEAN,
    is_castle           BOOLEAN,
    is_en_passant       BOOLEAN,
    time_remaining_ms   INT,
    created_at          DATETIME NOT NULL,
    INDEX idx_game_moves (game_id, move_number)
);
```

### matchmaking_queue
```sql
CREATE TABLE matchmaking_queue (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    game_mode   ENUM('BULLET','BLITZ','RAPID') NOT NULL,
    rating_min  INT NOT NULL,
    rating_max  INT NOT NULL,
    active      BOOLEAN DEFAULT TRUE,
    enqueued_at DATETIME NOT NULL,
    INDEX idx_active_mode (active, game_mode),
    INDEX idx_user_active (user_id, active)
);
```

## Redis Cache Keys

| Key Pattern             | Value              | TTL    | Description              |
|-------------------------|--------------------|--------|--------------------------|
| `game:fen:{gameId}`     | FEN string         | 2 hrs  | Current board position   |
| `game:timer:{gameId}`   | Hash {white,black} | 2 hrs  | Time remaining in ms     |
