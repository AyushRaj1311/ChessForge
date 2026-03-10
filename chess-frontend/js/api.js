/* ================================================================
   ChessForge – API Client
   All communication with Spring Boot backend
================================================================ */

const API_BASE = 'http://localhost:1111';

// ── Token management ─────────────────────────────────
const Auth = {
  getToken: ()  => localStorage.getItem('cf_token'),
  setToken: (t) => localStorage.setItem('cf_token', t),
  clearToken: () => localStorage.removeItem('cf_token'),

  getUser: ()   => {
    const u = localStorage.getItem('cf_user');
    return u ? JSON.parse(u) : null;
  },
  setUser: (u)  => localStorage.setItem('cf_user', JSON.stringify(u)),
  clearUser: () => localStorage.removeItem('cf_user'),

  isLoggedIn: () => !!localStorage.getItem('cf_token'),

  clear: () => {
    Auth.clearToken();
    Auth.clearUser();
  }
};

// ── Base fetch wrapper ───────────────────────────────
async function apiFetch(path, method = 'GET', body = null) {
  const headers = { 'Content-Type': 'application/json' };
  const token = Auth.getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const config = { method, headers };
  if (body) config.body = JSON.stringify(body);

  try {
    const res  = await fetch(API_BASE + path, config);
    const data = await res.json();
    return { ok: res.ok, status: res.status, data };
  } catch (err) {
    console.error('API fetch error:', err);
    return { ok: false, status: 0, data: { message: 'Network error — is the server running?' } };
  }
}

// ════════════════════════════════════════════════════
// AUTH
// ════════════════════════════════════════════════════
const AuthAPI = {
  async register(username, email, password) {
    return apiFetch('/api/auth/register', 'POST', { username, email, password });
  },
  async login(username, password) {
    return apiFetch('/api/auth/login', 'POST', { username, password });
  }
};

// ════════════════════════════════════════════════════
// GAMES
// ════════════════════════════════════════════════════
const GameAPI = {
  async create(gameMode, vsAi = true) {
    return apiFetch('/api/games', 'POST', { gameMode, vsAi });
  },
  async get(gameId) {
    return apiFetch(`/api/games/${gameId}`);
  },
  async join(gameId) {
    return apiFetch(`/api/games/${gameId}/join`, 'POST');
  },
  async move(gameId, from, to, promotion = null) {
    const body = { from, to };
    if (promotion) body.promotion = promotion;
    return apiFetch(`/api/games/${gameId}/moves`, 'POST', body);
  },
  async resign(gameId) {
    return apiFetch(`/api/games/${gameId}/resign`, 'POST');
  },
  async getMoves(gameId) {
    return apiFetch(`/api/games/${gameId}/moves`);
  },
  async getPgn(gameId) {
    const token = Auth.getToken();
    const headers = {};
    if (token) headers['Authorization'] = 'Bearer ' + token;
    const res = await fetch(API_BASE + `/api/games/${gameId}/pgn`, { headers });
    return res.text();
  },
  async getMyGames(page = 0, size = 10) {
    return apiFetch(`/api/games/my-games?page=${page}&size=${size}`);
  }
};

// ════════════════════════════════════════════════════
// USERS
// ════════════════════════════════════════════════════
const UserAPI = {
  async getMe() {
    return apiFetch('/api/users/me');
  },
  async getProfile(username) {
    return apiFetch(`/api/users/${username}`);
  },
  async getLeaderboard(page = 0, size = 10) {
    return apiFetch(`/api/leaderboard?page=${page}&size=${size}`);
  }
};

// ════════════════════════════════════════════════════
// MATCHMAKING
// ════════════════════════════════════════════════════
const MatchmakingAPI = {
  async join(gameMode) {
    return apiFetch('/api/matchmaking/join', 'POST', { gameMode });
  },
  async leave() {
    return apiFetch('/api/matchmaking/leave', 'DELETE');
  }
};
