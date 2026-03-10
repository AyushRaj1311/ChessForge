/* ================================================================
   ChessForge – Premium UI Utilities
   Toast notifications, modals, loading states, and formatters
================================================================ */

// ════════════════════════════════════════════════════
// TOAST NOTIFICATIONS (Now with Icons)
// ════════════════════════════════════════════════════
const Toast = {
  _container: null,

  init() {
    this._container = document.getElementById('toasts') || document.getElementById('toast-container');
  },

  show(message, type = 'info', duration = 3000) {
    if (!this._container) this.init();
    
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    
    // Premium Touch: Add tiny icons based on the toast type
    const icons = {
      success: '<span style="color: var(--green); margin-right: 6px;">✓</span>',
      error:   '<span style="color: var(--red); margin-right: 6px;">✕</span>',
      info:    '<span style="color: var(--accent); margin-right: 6px;">ℹ</span>'
    };

    el.innerHTML = `${icons[type] || ''} ${message}`;
    this._container.appendChild(el);

    // Smooth exit after duration
    setTimeout(() => {
      el.classList.add('out');
      // Wait for CSS animation to finish before removing from DOM
      setTimeout(() => el.remove(), 300); 
    }, duration);
  },

  success: (msg, dur) => Toast.show(msg, 'success', dur),
  error:   (msg, dur) => Toast.show(msg, 'error',   dur),
  info:    (msg, dur) => Toast.show(msg, 'info',    dur),
};

// ════════════════════════════════════════════════════
// MODAL MANAGER (With Smooth Fade-Out Lifecycle)
// ════════════════════════════════════════════════════
const Modal = {
  open(id) { 
    const el = document.getElementById(id);
    if (!el) return;
    
    // Step 1: Make visible but transparent
    el.style.display = 'flex';
    
    // Step 2: Trigger CSS transition on the next animation frame
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        el.classList.add('open');
      });
    });
  },
  
  close(id) { 
    const el = document.getElementById(id);
    if (!el) return;
    
    // Step 1: Start fade out
    el.classList.remove('open');
    
    // Step 2: Wait for transition to finish, then hide from layout
    setTimeout(() => {
      if (!el.classList.contains('open')) {
        el.style.display = 'none';
      }
    }, 250); // Matches the 0.2s ease in CSS + slight buffer
  },
  
  closeAll() {
    document.querySelectorAll('.overlay.open, .modal-overlay.open').forEach(el => {
      el.classList.remove('open');
      setTimeout(() => { el.style.display = 'none'; }, 250);
    });
  }
};

// Close modal on overlay background click
document.addEventListener('click', e => {
  if (e.target.classList.contains('modal-overlay') || e.target.classList.contains('overlay')) {
    e.target.classList.remove('open');
    setTimeout(() => { e.target.style.display = 'none'; }, 250);
  }
});

// Close on ESC key
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') Modal.closeAll();
});

// ════════════════════════════════════════════════════
// LOADING BUTTON STATE
// ════════════════════════════════════════════════════
function setButtonLoading(btnId, isLoading, originalText = 'Submit') {
  const btn = typeof btnId === 'string' ? document.getElementById(btnId) : btnId;
  if (!btn) return;
  
  if (isLoading) {
    btn.disabled = true;
    // Inject the CSS spinner we created in main.css
    btn.innerHTML = `<span class="spinner"></span> &nbsp; ${originalText}...`;
  } else {
    btn.disabled = false;
    btn.textContent = originalText;
  }
}

// ════════════════════════════════════════════════════
// PAGE ROUTER (Single-Page Navigation)
// ════════════════════════════════════════════════════
const Router = {
  current: null,

  go(pageId) {
    // Hide all pages
    document.querySelectorAll('.page').forEach(p => {
      p.classList.remove('active');
      p.style.display = 'none';
    });
    
    // Show target page
    const page = document.getElementById(pageId);
    if (page) {
      page.style.display = 'flex';
      
      // Request animation frame ensures the fade-in animation triggers properly
      requestAnimationFrame(() => {
        page.classList.add('active');
      });
      this.current = pageId;
    }

    // Automatically update Navbar active states
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
    if (pageId === 'game-page') {
      document.querySelector('.nav-link[onclick="showLobby()"]')?.classList.add('active');
    } else if (pageId === 'profile-page') {
      document.querySelector('.nav-link[onclick="showProfile()"]')?.classList.add('active');
    }
  }
};

// ════════════════════════════════════════════════════
// FORMATTERS
// ════════════════════════════════════════════════════
function formatClock(totalSeconds) {
  const s = Math.max(0, Math.floor(totalSeconds));
  const m = Math.floor(s / 60);
  const sec = s % 60;
  return `${m}:${String(sec).padStart(2, '0')}`;
}

function formatDate(isoString) {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric'
  });
}

// ════════════════════════════════════════════════════
// ERROR BOX MANAGER
// ════════════════════════════════════════════════════
function showError(containerId, message) {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.textContent = message;
  el.style.display = 'block';
  
  // Premium Touch: Brief shake animation class to draw attention if it updates
  el.classList.remove('shake');
  void el.offsetWidth; // Trigger reflow
  el.classList.add('shake');
}

function hideError(containerId) {
  const el = document.getElementById(containerId);
  if (el) el.style.display = 'none';
}

// ════════════════════════════════════════════════════
// LEADERBOARD RENDERER (Cascading Animation)
// ════════════════════════════════════════════════════
function renderLeaderboard(containerId, entries) {
  const el = document.getElementById(containerId);
  if (!el) return;
  
  if (!entries || !entries.length) {
    el.innerHTML = `<div class="text-dim text-xs" style="padding:0.5rem; text-align:center;">No players yet</div>`;
    return;
  }

  // Create rows with inline animation delays for a cascading slide-in effect
  el.innerHTML = entries.map((e, index) => `
    <div class="lb-item anim-fade-in" style="animation-delay: ${index * 0.05}s">
      <span class="lb-rank">#${e.rank}</span>
      <span class="lb-name">${e.username}</span>
      <span class="lb-elo">${e.rating}</span>
    </div>
  `).join('');
}

// ════════════════════════════════════════════════════
// RESULT BADGE RENDERER
// ════════════════════════════════════════════════════
function getResultBadge(result, userIsWhite) {
  if (result === 'IN_PROGRESS') return `<span class="badge badge-gold">Live</span>`;
  if (result === 'DRAW')        return `<span class="badge badge-gold">Draw</span>`;
  
  const userWon = (result === 'WHITE_WIN' && userIsWhite) ||
                  (result === 'BLACK_WIN' && !userIsWhite);
                  
  return userWon
    ? `<span class="badge badge-green">Victory</span>`
    : `<span class="badge badge-red">Defeat</span>`;
}