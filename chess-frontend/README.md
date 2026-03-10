# ChessForge Frontend

A complete chess UI — one single HTML file, no build tools needed.

---

## How to Run

### Step 1 — Fix CORS in Your Backend (Do this once)

Copy the file `BACKEND_CORS_FIX/CorsConfig.java` into your Spring Boot project at:

```
src/main/java/com/chessplatform/config/CorsConfig.java
```

Then **restart** the Spring Boot app in IntelliJ.

> Without this step the browser will block API calls with a CORS error.

---

### Step 2 — Make Sure Your Backend Is Running

Open IntelliJ and confirm the app is running. You should see:
```
Tomcat started on port 1111
```

---

### Step 3 — Open the Frontend

Simply **double-click** `index.html` to open it in your browser.

Or right-click → Open With → Chrome / Firefox / Safari

---

## What's Inside

| File | Purpose |
|------|---------|
| `index.html` | Complete frontend — all HTML, CSS, JavaScript in one file |
| `BACKEND_CORS_FIX/CorsConfig.java` | Add this to your backend to fix CORS |

---

## Features

| Feature | Details |
|---------|---------|
| ✅ Login / Register | Connects to your Spring Boot API |
| ✅ Chess Board | Full interactive board with piece moves |
| ✅ Legal Move Hints | Green dots show where you can move |
| ✅ Play vs AI | Bullet / Blitz / Rapid modes |
| ✅ Live Clocks | Countdown timers for both players |
| ✅ Move History | All moves listed in algebraic notation |
| ✅ Captured Pieces | Shown below each player |
| ✅ Pawn Promotion | Popup to choose Queen / Rook / Bishop / Knight |
| ✅ Resign | End the game early |
| ✅ Download PGN | Export your game in standard chess format |
| ✅ Leaderboard | Top players by rating |
| ✅ Profile Page | Your stats and game history |
| ✅ Check Highlight | King square turns red when in check |
| ✅ Checkmate Detection | Game over screen with result |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| "Network error — is the server running?" | Start your Spring Boot app in IntelliJ |
| Moves return errors | Make sure you're logged in first |
| CORS error in browser console | Add CorsConfig.java to backend and restart |
| App running on different port | Open `index.html`, find `const API = 'http://localhost:1111'` at top of script and change 1111 to your port |

---

## Changing the Backend Port

If your Spring Boot app runs on a port other than 1111, open `index.html` in a text editor and find this line near the top of the `<script>` section:

```javascript
const API = 'http://localhost:1111';
```

Change `1111` to your actual port number and save.
