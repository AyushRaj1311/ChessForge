# Chess Platform – MySQL Only Setup Guide

## What You Need (Install These First)

| Tool | Download |
|------|----------|
| Java 17 (JDK) | https://adoptium.net |
| Maven | https://maven.apache.org/download.cgi |
| MySQL + MySQL Workbench | https://dev.mysql.com/downloads/workbench/ |
| IntelliJ IDEA (Community is free) | https://www.jetbrains.com/idea/download/ |

---

## Step 1 – Set Up MySQL via Workbench

1. Open **MySQL Workbench**
2. Connect to your local MySQL server (usually `localhost:3306`, user `root`)
3. Open the file **`chess_platform_schema.sql`** from this project folder
   - File → Open SQL Script → select `chess_platform_schema.sql`
4. Click the **⚡ lightning bolt** (Execute All) button
5. You should see `Schema created successfully!` at the bottom

---

## Step 2 – Update Database Credentials

Open `src/main/resources/application.yml` and change these two lines to match your MySQL login:

```yaml
spring:
  datasource:
    username: root        # ← your MySQL username
    password: password    # ← your MySQL password
```

The database URL is already set to `localhost:3306/chess_platform` which matches what the SQL script creates.

---

## Step 3 – Open in IntelliJ IDEA

1. **File → Open** → select the `pom.xml` file in this folder → **Open as Project**
2. Wait for Maven to download all dependencies (watch the bottom progress bar)
3. Install **Lombok plugin**:
   - Settings → Plugins → search **Lombok** → Install → Restart IntelliJ
4. Enable **Annotation Processing**:
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Tick ✅ **Enable annotation processing** → OK

---

## Step 4 – Run the Application

- Find `ChessPlatformApplication.java` in the Project panel
- Right-click → **Run 'ChessPlatformApplication'**
- Watch the console – you should see:

```
Started ChessPlatformApplication on port 1111
```

Spring Boot will automatically create/update all database tables on startup.

---

## Step 5 – Test the API

Open your browser or Postman:

### Register a user
```
POST http://localhost:1111/api/auth/register
Content-Type: application/json

{
  "username": "player1",
  "email": "player1@chess.com",
  "password": "password123"
}
```

### Login
```
POST http://localhost:1111/api/auth/login
Content-Type: application/json

{
  "username": "player1",
  "password": "password123"
}
```
Copy the `accessToken` from the response.

### Create a game vs AI
```
POST http://localhost:1111/api/games
Authorization: Bearer <your_token>
Content-Type: application/json

{ "gameMode": "BLITZ", "vsAi": true }
```

### Make a move
```
POST http://localhost:1111/api/games/{gameId}/moves
Authorization: Bearer <your_token>
Content-Type: application/json

{ "from": "e2", "to": "e4" }
```

### View leaderboard (no login needed)
```
GET http://localhost:1111/api/leaderboard
```

---

## Verify Data in MySQL Workbench

After running the app and registering a user, go back to Workbench and run:

```sql
USE chess_platform;
SELECT * FROM users;
SELECT * FROM games;
SELECT * FROM moves;
```

You should see your data there.

---

## Common Errors & Fixes

| Error | Fix |
|-------|-----|
| `Access denied for user 'root'` | Wrong password in `application.yml` – update it |
| `Unknown database 'chess_platform'` | Run the `chess_platform_schema.sql` script in Workbench first |
| `Port 3306 already in use` | MySQL is already running – that's fine, just connect normally |
| Red underlines in IntelliJ | Install Lombok plugin + enable annotation processing (Step 3) |
| `java: cannot find symbol` on Lombok classes | Rebuild: Build → Rebuild Project |

---

## Game Modes

| Mode | Time per player |
|------|----------------|
| BULLET | 1 minute |
| BLITZ | 5 minutes |
| RAPID | 10 minutes |
