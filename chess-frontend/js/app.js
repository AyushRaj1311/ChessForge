/* ================================================================
   ChessForge – Premium Main Application Controller
   Handles auth flow, silky navigation, and page initialization
================================================================ */

// ════════════════════════════════════════════════════
// APP INIT
// ════════════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', () => {
  // Resume session if token exists
  if (Store.isAuth()) {
    enterApp();
  } else {
    showPage('auth-page');
    switchTab('login');
  }

  // Enter key support for auth forms
  document.addEventListener('keydown', e => {
    if (e.key !== 'Enter') return;
    const authPage = document.getElementById('auth-page');
    if (authPage && authPage.classList.contains('active')) {
      const loginForm = document.getElementById('login-form');
      if (loginForm && !loginForm.classList.contains('hidden')) {
        doLogin();
      } else {
        doRegister();
      }
    }
  });
});

// ════════════════════════════════════════════════════
// UI UTILITIES
// ════════════════════════════════════════════════════
function setButtonLoading(btnId, isLoading, defaultText) {
  const btn = document.getElementById(btnId);
  if (!btn) return;
  
  if (isLoading) {
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner"></span> &nbsp; ${defaultText}...`;
  } else {
    btn.disabled = false;
    btn.innerHTML = defaultText;
  }
}

// ════════════════════════════════════════════════════
// AUTH FLOW
// ════════════════════════════════════════════════════
async function doLogin() {
  const user = document.getElementById('l-user').value.trim();
  const pass = document.getElementById('l-pass').value;
  
  if (!user || !pass) { 
    setErr('Please fill in all fields'); 
    return; 
  }

  setButtonLoading('btn-login', true, 'Logging in');

  const { ok, data } = await api('/api/auth/login', 'POST', { username: user, password: pass });

  setButtonLoading('btn-login', false, 'Login');

  if (!ok) { 
    setErr(data.message || 'Invalid username or password'); 
    return; 
  }

  Store.setToken(data.data.accessToken);
  Store.setUser(data.data);
  setErr('');
  Toast.success(`Welcome back, ${data.data.username}!`);
  enterApp();
}

async function doRegister() {
  const user  = document.getElementById('r-user').value.trim();
  const email = document.getElementById('r-email').value.trim();
  const pass  = document.getElementById('r-pass').value;

  if (!user || !email || !pass) { setErr('Please fill in all fields'); return; }
  if (pass.length < 6) { setErr('Password must be at least 6 characters'); return; }
  if (!email.includes('@')) { setErr('Please enter a valid email address'); return; }

  setButtonLoading('btn-register', true, 'Creating account');

  const { ok, data } = await api('/api/auth/register', 'POST', { username: user, email, password: pass });

  setButtonLoading('btn-register', false, 'Create Account');

  if (!ok) { 
    setErr(data.message || 'Registration failed'); 
    return; 
  }

  Store.setToken(data.data.accessToken);
  Store.setUser(data.data);
  setErr('');
  Toast.success('Account created! Welcome to ChessForge!');
  enterApp();
}

// ════════════════════════════════════════════════════
// APP ENTRY & NAVIGATION
// ════════════════════════════════════════════════════
function enterApp() {
  const user = Store.getUser();
  if (!user) { showPage('auth-page'); return; }

  // Update navbar globally
  document.getElementById('nav-uname').textContent = user.username;
  document.getElementById('nav-urat').textContent  = user.rating;
  document.getElementById('prof-nav-uname').textContent = user.username;
  document.getElementById('prof-nav-urat').textContent  = user.rating;

  // Initialize Game Controllers
  BoardRenderer.init('chess-board', null);
  BoardRenderer.onSquareClick(sq => GameController.handleSquareClick(sq));
  ClockManager.init('w-clock', 'b-clock');
  MovePanel.init('move-history');

  showLobby();
}

function doLogout() {
  ClockManager.stop();
  Store.clear();
  showPage('auth-page');
  switchTab('login');
  Toast.info('Logged out successfully');
}

// ════════════════════════════════════════════════════
// LOBBY & GAME SETUP
// ════════════════════════════════════════════════════
function showLobby() {
  ClockManager.stop();
  GameController.game = null;

  showPage('game-page');
  document.getElementById('lobby').style.display = 'flex';
  document.getElementById('board-view').classList.add('hidden');
  
  const statusEl = document.getElementById('status-text');
  if(statusEl) statusEl.textContent = 'Awaiting Game';
  
  MovePanel.clear();
  loadLeaderboard();
}

// Note: startGame logic was partially handled in the index.html script tag previously.
// Moving it fully into app.js here ensures clean architecture.
window.startGame = async function(mode) {
  const statusEl = document.getElementById('status-text');
  const icon = mode === 'BULLET' ? '⚡' : mode === 'BLITZ' ? '🔥' : '🕐';
  
  if(statusEl) statusEl.innerHTML = `${icon} Starting Engine...`;

  const ok = await GameController.startVsAI(mode);
  if (!ok) {
    showLobby();
    return;
  }

  // Hide Lobby, Show Board
  document.getElementById('lobby').style.display = 'none';
  document.getElementById('board-view').classList.remove('hidden');

  // Update Player Bars
  const u = Store.getUser();
  document.getElementById('w-name').textContent = u?.username || 'You';
  document.getElementById('w-sub').textContent  = 'Rating ' + (u?.rating || 1200) + ' · White';
  document.getElementById('b-name').textContent = 'Stockfish AI';
  document.getElementById('b-sub').textContent  = 'Level 4 Engine · Black';

  document.getElementById('w-captured').textContent = '';
  document.getElementById('b-captured').textContent = '';

  Toast.info('Game started! Good luck.');
};

// ════════════════════════════════════════════════════
// LEADERBOARD
// ════════════════════════════════════════════════════
async function loadLeaderboard() {
  const el = document.getElementById('sidebar-lb');
  if(!el) return;
  
  el.innerHTML = '<span class="spinner" style="border-width: 2px;"></span>';

  const { ok, data } = await api('/api/leaderboard?size=8');
  
  if (ok && data.data && data.data.length > 0) {
    el.innerHTML = data.data.map(e => `
      <div class="lb-row">
        <span class="lb-rank">#${e.rank}</span>
        <span class="lb-name">${e.username}</span>
        <span class="lb-elo">${e.rating}</span>
      </div>`).join('');
  } else {
    el.innerHTML = '<span style="color:var(--text-muted);font-size:0.8rem">No players yet</span>';
  }
}

// ════════════════════════════════════════════════════
// PROFILE PAGE
// ════════════════════════════════════════════════════
window.showProfile = async function() {
  showPage('profile-page');

  const user = Store.getUser();
  if (!user) return;

  // Add loading state to history table
  const tbody = document.getElementById('history-body');
  tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:2rem"><span class="spinner"></span></td></tr>';

  // Fetch fresh profile data
  const { ok, data } = await api('/api/users/me');
  if (!ok) { 
    Toast.error('Failed to load profile'); 
    return; 
  }

  const u = data.data;
  document.getElementById('prof-avatar').textContent = u.username[0].toUpperCase();
  document.getElementById('prof-username').textContent = u.username;
  document.getElementById('prof-since').textContent  = 'Member since ' + formatDate(u.createdAt);
  
  // Animate numbers (Optional premium touch)
  document.getElementById('s-rating').textContent = u.rating;
  document.getElementById('s-played').textContent = u.gamesPlayed;
  document.getElementById('s-won').textContent    = u.gamesWon;
  document.getElementById('s-rate').textContent   = u.gamesPlayed > 0 
    ? Math.round((u.gamesWon / u.gamesPlayed) * 100) + '%' 
    : '0%';

  // Fetch Game History
  const ghRes = await api('/api/games/my-games?page=0&size=15');
  
  if (ghRes.ok && ghRes.data.data?.content?.length) {
    tbody.innerHTML = ghRes.data.data.content.map(g => {
      const iAmWhite = g.whitePlayer === u.username;
      const opp      = iAmWhite ? (g.blackPlayer || 'Engine') : (g.whitePlayer || 'Engine');
      return `
        <tr>
          <td><span style="font-weight:600;color:var(--accent)">${g.gameMode}</span></td>
          <td>${opp}</td>
          <td>${getResultBadge(g.result, iAmWhite)}</td>
          <td style="font-family:'JetBrains Mono',monospace">${g.totalMoves || 0}</td>
          <td style="color:var(--text-dim)">${formatDate(g.completedAt || g.createdAt)}</td>
        </tr>`;
    }).join('');
  } else {
    tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:var(--text-muted);padding:2rem">No games played yet</td></tr>';
  }
};

// ════════════════════════════════════════════════════
// UTILITIES & FORMATTING
// ════════════════════════════════════════════════════
function formatDate(dateString) {
  if (!dateString) return '—';
  const opts = { year: 'numeric', month: 'short', day: 'numeric' };
  return new Date(dateString).toLocaleDateString('en-US', opts);
}

function getResultBadge(result, isUserWhite) {
  if (result === 'IN_PROGRESS') return '<span class="badge badge-draw">Live</span>';
  if (result === 'DRAW')        return '<span class="badge badge-draw">Draw</span>';
  
  const userWon = (result === 'WHITE_WIN' && isUserWhite) || (result === 'BLACK_WIN' && !isUserWhite);
  return userWon 
    ? '<span class="badge badge-green">Victory</span>' 
    : '<span class="badge badge-red">Defeat</span>';
}