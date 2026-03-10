# Chess Platform - API & Deployment Guide

## Project Structure
```
chess-platform/
├── src/main/java/com/chessplatform/
│   ├── ChessPlatformApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java       # JWT + Spring Security
│   │   ├── WebSocketConfig.java      # STOMP WebSocket
│   │   └── RedisConfig.java          # Redis template
│   ├── controller/
│   │   ├── AuthController.java       # /api/auth/**
│   │   ├── GameController.java       # /api/games/**
│   │   ├── UserController.java       # /api/users/**, /api/leaderboard
│   │   └── MatchmakingController.java # /api/matchmaking/**
│   ├── dto/
│   │   ├── request/                  # RegisterRequest, LoginRequest, MakeMoveRequest...
│   │   └── response/                 # AuthResponse, GameResponse, MoveResponse...
│   ├── engine/
│   │   ├── ai/
│   │   │   ├── MinimaxAI.java        # Alpha-Beta Minimax
│   │   │   └── BoardEvaluator.java   # Piece-square tables
│   │   ├── model/
│   │   │   ├── Board.java            # Board representation + FEN
│   │   │   ├── Piece.java            # Chess piece
│   │   │   ├── Position.java         # Square coordinate
│   │   │   ├── ChessMove.java        # Move representation
│   │   │   └── MoveResult.java       # Move outcome
│   │   └── service/
│   │       ├── ChessEngine.java      # Move validation + SAN generation
│   │       ├── MoveGenerator.java    # Legal move generation
│   │       └── PgnService.java       # PGN export/import
│   ├── exception/                    # Global exception handling
│   ├── model/                        # JPA Entities + Enums
│   ├── repository/                   # Spring Data JPA repos
│   ├── security/                     # JwtUtil, JwtAuthFilter
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── GameService.java          # Core game logic
│   │   ├── GameCacheService.java     # Redis caching
│   │   ├── MatchmakingService.java   # Matchmaking queue
│   │   └── UserService.java          # ELO + profiles
│   └── websocket/
│       ├── handler/GameWebSocketHandler.java
│       └── message/WebSocketMessage.java
└── src/main/resources/
    └── application.yml
```

---

## REST API Reference

### Auth

#### POST /api/auth/register
```json
// Request
{
  "username": "magnus",
  "email": "magnus@chess.com",
  "password": "securePass123"
}

// Response 201
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGci...",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "magnus",
    "role": "USER",
    "rating": 1200
  }
}
```

#### POST /api/auth/login
```json
// Request
{ "username": "magnus", "password": "securePass123" }

// Response 200
{
  "success": true,
  "data": { "accessToken": "eyJhbGci...", "rating": 1200 }
}
```

---

### Games

#### POST /api/games  (Auth required)
```json
// Request
{ "gameMode": "BLITZ", "vsAi": false }

// Response 201
{
  "success": true,
  "data": {
    "gameId": "550e8400-e29b-41d4-a716-446655440000",
    "whitePlayer": "magnus",
    "blackPlayer": null,
    "gameMode": "BLITZ",
    "status": "WAITING",
    "currentFen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    "whiteTimeRemaining": 300,
    "blackTimeRemaining": 300,
    "isAiGame": false
  }
}
```

#### POST /api/games/{gameId}/moves  (Auth required)
```json
// Request
{ "from": "e2", "to": "e4" }

// Response 200
{
  "success": true,
  "data": {
    "valid": true,
    "from": "e2",
    "to": "e4",
    "san": "e4",
    "fen": "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
    "isCapture": false,
    "isCheck": false,
    "isCheckmate": false,
    "gameStatus": "IN_PROGRESS",
    "aiMove": null
  }
}
```

#### GET /api/games/{gameId}/pgn
```
[Event "Chess Platform Game"]
[Site "chess-platform.com"]
[Date "2025.03.03"]
[White "magnus"]
[Black "AI"]
[TimeControl "300"]
[Result "1-0"]

1. e4 e5 2. Nf3 Nc6 3. Bc4 ... 1-0
```

---

### Leaderboard

#### GET /api/leaderboard?page=0&size=10
```json
{
  "success": true,
  "data": [
    { "rank": 1, "userId": 42, "username": "magnus", "rating": 2847, "gamesPlayed": 312, "winRate": 78.2 },
    { "rank": 2, "userId": 7,  "username": "hikaru", "rating": 2780, "gamesPlayed": 450, "winRate": 72.1 }
  ]
}
```

---

## WebSocket API

### Connection
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);
stompClient.connect(
  { Authorization: 'Bearer eyJhbGci...' },
  () => {
    // Subscribe to game moves
    stompClient.subscribe(`/topic/game/${gameId}/move`, (msg) => {
      const move = JSON.parse(msg.body);
      updateBoard(move);
    });

    // Subscribe to game end
    stompClient.subscribe(`/topic/game/${gameId}/end`, (msg) => {
      showGameOver(JSON.parse(msg.body));
    });

    // Subscribe to matchmaking result (personal)
    stompClient.subscribe('/user/queue/matchmaking', (msg) => {
      const { gameId, color } = JSON.parse(msg.body);
      redirectToGame(gameId, color);
    });
  }
);
```

### Sending a move via WebSocket
```javascript
stompClient.send(`/app/game/${gameId}/move`, {}, JSON.stringify({
  from: 'e2',
  to: 'e4'
}));
```

### WebSocket Topics Summary

| Destination                          | Direction      | Description                 |
|--------------------------------------|----------------|-----------------------------|
| `/app/game/{id}/move`                | Client → Server | Send a move                 |
| `/app/game/{id}/join`                | Client → Server | Join game                   |
| `/topic/game/{id}/move`              | Server → Client | Move broadcast to all       |
| `/topic/game/{id}/end`              | Server → Client | Game over notification      |
| `/user/queue/matchmaking`            | Server → Client | Match found notification    |
| `/user/queue/game/{id}/error`       | Server → Client | Personal error message      |

---

## Deployment Guide

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6+

### Local Development

**1. Start MySQL and Redis with Docker:**
```bash
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=password -e MYSQL_DATABASE=chess_platform -p 3306:3306 mysql:8
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

**2. Build and Run:**
```bash
cd chess-platform
mvn clean install -DskipTests
mvn spring-boot:run
```

**3. Run Tests:**
```bash
mvn test
```

The API starts on http://localhost:8080

---

### Docker Compose (Full Stack)

Create `docker-compose.yml` in the project root:

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: chess_platform
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/chess_platform?createDatabaseIfNotExist=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: password
      SPRING_DATA_REDIS_HOST: redis
      JWT_SECRET: your-super-secret-key-change-in-production
    depends_on:
      - mysql
      - redis

volumes:
  mysql_data:
```

```bash
docker-compose up --build
```

---

### Production Checklist
- [ ] Change `jwt.secret` to a long random string (32+ chars)
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate` (use Flyway for migrations)
- [ ] Enable HTTPS / configure TLS
- [ ] Set proper CORS origins in SecurityConfig
- [ ] Configure Redis with AUTH password
- [ ] Set up connection pool (HikariCP tuning)
- [ ] Add rate limiting (e.g., Bucket4j)
- [ ] Configure logging to file/ELK
- [ ] Set up health checks (`/actuator/health`)

---

### IntelliJ IDEA Setup

1. **Open Project:** File → Open → select `chess-platform/pom.xml` → Open as Project
2. **Configure JDK:** File → Project Structure → SDK → Java 17
3. **Enable Annotation Processing:** Settings → Build → Compiler → Annotation Processors → Enable
4. **Add Lombok Plugin:** Settings → Plugins → search "Lombok" → Install → Restart
5. **Run:** Right-click `ChessPlatformApplication.java` → Run

> **Note:** For full functionality, ensure MySQL and Redis are running locally or via Docker before starting the application.

---

### AI Difficulty Levels

The Minimax AI depth is configured in `application.yml`:
```yaml
chess:
  ai:
    max-depth: 4   # Increase for stronger AI (exponentially slower)
```
| Depth | Strength    | Approx. time/move |
|-------|-------------|-------------------|
| 2     | Beginner    | < 50ms            |
| 3     | Intermediate| 100-300ms         |
| 4     | Strong      | 1-3s              |
| 5     | Very Strong | 5-20s             |
