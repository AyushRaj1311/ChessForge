/* ================================================================
   ChessForge – Fixed Board Renderer & Game Controller
================================================================ */

// ════════════════════════════════════════════════════
// BOARD RENDERER
// ════════════════════════════════════════════════════
const BoardRenderer = {
  boardEl: null,
  state: null,         // ChessBoardState instance

  init(boardElId, state) {
    this.boardEl = document.getElementById(boardElId);
    this.state   = state;
  },

  render(options = {}) {
    const { inCheckSq = null, animateMove = false } = options;
    if (!this.boardEl || !this.state) return;

    this.boardEl.innerHTML = '';

    for (let row = 7; row >= 0; row--) {
      for (let col = 0; col < 8; col++) {
        const sq      = sqName(row, col); 
        const isLight = (row + col) % 2 === 1;
        const piece   = this.state.board[sq];

        const div = document.createElement('div');
        
        let cls = 'sq ' + (isLight ? 'light' : 'dark');

        if (sq === this.state.selected)              cls += ' selected';
        if (this.state.lastMove?.from === sq)        cls += ' last-from';
        if (this.state.lastMove?.to   === sq)        cls += ' last-to';
        if (inCheckSq === sq)                         cls += ' in-check';
        
        if (this.state.isLegalHint(sq)) {
          cls += piece ? ' can-capture' : ' can-move';
        }

        div.className  = cls;
        div.dataset.sq = sq;
        div.onclick    = () => this._onSquareClick(sq);

        if (col === 0) {
          const span = document.createElement('span');
          span.className = 'coord coord-rank';
          span.textContent = row + 1;
          div.appendChild(span);
        }
        if (row === 0) {
          const span = document.createElement('span');
          span.className = 'coord coord-file';
          span.textContent = String.fromCharCode(97 + col);
          div.appendChild(span);
        }

        if (this.state.isLegalHint(sq)) {
          const dot = document.createElement('div');
          dot.className = 'move-dot';
          div.appendChild(dot);
        }

        if (piece) {
          const pEl = document.createElement('div');
          
          // FIX: Add the color class (white or black) to the piece element
          // This allows the CSS we wrote earlier to color the Knights and Pawns correctly.
          pEl.className = `piece ${piece.color.toLowerCase()}`;
          
          if (animateMove && this.state.lastMove?.to === sq) {
            pEl.classList.add('piece-just-moved');
          }
          
          // Use the solid Unicode set for better visibility with strokes
          pEl.textContent = PIECE_UNICODE[piece.color][piece.type];
          div.appendChild(pEl);
        }

        this.boardEl.appendChild(div);
      }
    }
  },

  _onSquareClick(sq) {
    if (this._clickHandler) this._clickHandler(sq);
  },

  onSquareClick(handler) {
    this._clickHandler = handler;
  }
};

// ════════════════════════════════════════════════════
// CLOCK MANAGER
// ════════════════════════════════════════════════════
const ClockManager = {
  whiteEl: null,
  blackEl: null,
  white: 0,
  black: 0,
  _interval: null,
  _whiteTurn: true,
  onChange: null,

  init(whiteElId, blackElId) {
    this.whiteEl = document.getElementById(whiteElId);
    this.blackEl = document.getElementById(blackElId);
  },

  format(seconds) {
    const s = Math.max(0, Math.floor(seconds));
    const m = Math.floor(s / 60);
    const secs = (s % 60).toString().padStart(2, '0');
    return `${m}:${secs}`;
  },

  set(whiteSecs, blackSecs) {
    this.white = whiteSecs;
    this.black = blackSecs;
    this._render();
  },

  setTurn(isWhiteTurn) {
    this._whiteTurn = isWhiteTurn;
    this._render();
  },

  start() {
    this.stop();
    this._interval = setInterval(() => {
      if (this._whiteTurn) this.white = Math.max(0, this.white - 1);
      else                 this.black = Math.max(0, this.black - 1);
      this._render();
      if (this.onChange) this.onChange(this.white, this.black);
      if (this.white === 0 || this.black === 0) this.stop();
    }, 1000);
  },

  stop() {
    clearInterval(this._interval);
    this._interval = null;
  },

  _render() {
    if (!this.whiteEl || !this.blackEl) return;
    this.whiteEl.textContent = this.format(this.white);
    this.blackEl.textContent = this.format(this.black);

    this.whiteEl.className = 'clock' +
      (this._whiteTurn ? ' active' : '') +
      (this.white <= 30 && this._whiteTurn ? ' low' : '');
      
    this.blackEl.className = 'clock' +
      (!this._whiteTurn ? ' active' : '') +
      (this.black <= 30 && !this._whiteTurn ? ' low' : '');
  }
};

// ════════════════════════════════════════════════════
// MOVE HISTORY PANEL
// ════════════════════════════════════════════════════
const MovePanel = {
  el: null,
  moves: [],

  init(elId) {
    this.el = document.getElementById(elId);
    this.clear();
  },

  clear() {
    this.moves = [];
    if (this.el) this.el.innerHTML = '';
  },

  add(san, color) {
    this.moves.push({ san, color });
    this._render();
  },

  _render() {
    if (!this.el) return;
    let html = '';
    for (let i = 0; i < this.moves.length; i += 2) {
      const num   = Math.floor(i / 2) + 1;
      const white = this.moves[i];
      const black = this.moves[i + 1];
      const wCls  = (i === this.moves.length - 1) ? 'mv-san current' : 'mv-san';
      const bCls  = black && (i + 1 === this.moves.length - 1) ? 'mv-san current' : 'mv-san';
      html += `
        <div class="move-pair">
          <span class="mv-num">${num}.</span>
          <span class="${wCls}">${white.san}</span>
          <span class="${black ? bCls : 'mv-san'}">${black ? black.san : ''}</span>
        </div>`;
    }
    this.el.innerHTML = html;
    this.el.scrollTo({ top: this.el.scrollHeight, behavior: 'smooth' });
  }
};

// ════════════════════════════════════════════════════
// CAPTURED PIECES
// ════════════════════════════════════════════════════
function renderCaptured(whiteElId, blackElId, state) {
  const captured = state.getCaptured();
  const wEl = document.getElementById(whiteElId);
  const bEl = document.getElementById(blackElId);
  if (wEl) wEl.textContent = captured.WHITE.join('');
  if (bEl) bEl.textContent = captured.BLACK.join('');
}

// ════════════════════════════════════════════════════
// GAME CONTROLLER
// ════════════════════════════════════════════════════
const GameController = {
  game:         null,  
  boardState:   null,  
  playerColor:  'WHITE',
  myTurn:       true,
  pendingPromo: null,  
  inCheckSq:    null,

  onGameOver: null,

  async startVsAI(gameMode) {
    const { ok, data } = await GameAPI.create(gameMode, true);
    if (!ok) { Toast.error(data.message || 'Failed to create game'); return false; }

    this.game        = data.data;
    this.playerColor = 'WHITE';
    this.myTurn      = true;
    this.inCheckSq   = null;

    this.boardState = new ChessBoardState();
    this.boardState.load(this.game.currentFen);

    ClockManager.set(this.game.whiteTimeRemaining, this.game.blackTimeRemaining);
    ClockManager.setTurn(true);
    ClockManager.start();
    
    ClockManager.onChange = (w, b) => {
      if (w === 0 || b === 0) {
        ClockManager.stop();
        const userWon = (w === 0 && this.playerColor === 'BLACK') ||
                        (b === 0 && this.playerColor === 'WHITE');
        if (this.onGameOver) this.onGameOver({
          title: userWon ? 'You win on time!' : 'Time out!',
          msg:   userWon ? 'Opponent ran out of time.' : 'You ran out of time.',
          gameId: this.game.gameId
        });
      }
    };

    MovePanel.clear();
    BoardRenderer.render({ inCheckSq: this.inCheckSq, animateMove: false });
    this._updateStatus('Your turn');
    return true;
  },

  handleSquareClick(sq) {
    if (!this.game || this.game.status === 'COMPLETED') return;
    if (!this.myTurn) return;

    const state = this.boardState;
    const piece = state.board[sq];

    if (state.selected && state.isLegalHint(sq)) {
      if (state.isPromotion(state.selected, sq)) {
        this.pendingPromo = { from: state.selected, to: sq };
        this._showPromoModal(state.board[state.selected].color);
        return;
      }
      this._doMove(state.selected, sq, null);
      return;
    }

    if (piece && piece.color === this.playerColor) {
      state.selectSquare(sq);
      BoardRenderer.render({ inCheckSq: this.inCheckSq, animateMove: false });
      return;
    }

    state.selected   = null;
    state.legalHints = [];
    BoardRenderer.render({ inCheckSq: this.inCheckSq, animateMove: false });
  },

  async _doMove(from, to, promotion) {
    this.myTurn = false;
    this.boardState.selected   = null;
    this.boardState.legalHints = [];
    this._updateStatus('<span class="spinner"></span> Thinking…');

    const body = { from, to }; 
    if (promotion) body.promotion = promotion;
    
    const { ok, data } = await GameAPI.move(this.game.gameId, from, to, promotion);

    if (!ok || !data.data?.valid) {
      this.myTurn = true;
      Toast.error(data.data?.errorMessage || data.message || 'Invalid move');
      BoardRenderer.render({ inCheckSq: this.inCheckSq, animateMove: false });
      return;
    }

    const result = data.data;
    this.boardState.load(result.fen);
    this.boardState.lastMove = { from, to };
    this.inCheckSq = null;

    if (result.whiteTimeRemaining != null) {
      ClockManager.set(result.whiteTimeRemaining, result.blackTimeRemaining);
    }
    ClockManager.setTurn(false);

    MovePanel.add(result.san, 'WHITE');
    renderCaptured('white-captured', 'black-captured', this.boardState);
    BoardRenderer.render({ inCheckSq: null, animateMove: true });

    if (result.isCheckmate || result.isStalemate || result.gameStatus === 'COMPLETED') {
      ClockManager.stop();
      this._triggerGameOver(result);
      return;
    }

    if (result.isCheck) {
      this.inCheckSq = this._findKing('BLACK');
      Toast.info('Check!');
    }

    if (result.aiMove) {
      await new Promise(r => setTimeout(r, 450));
      const ai = result.aiMove;
      this.boardState.load(ai.fen);
      this.boardState.lastMove = { from: ai.from, to: ai.to };

      if (ai.whiteTimeRemaining != null) {
        ClockManager.set(ai.whiteTimeRemaining, ai.blackTimeRemaining);
      }
      ClockManager.setTurn(true);

      MovePanel.add(ai.san, 'BLACK');
      renderCaptured('white-captured', 'black-captured', this.boardState);

      this.inCheckSq = null;
      if (ai.isCheck) this.inCheckSq = this._findKing('WHITE');

      BoardRenderer.render({ inCheckSq: this.inCheckSq, animateMove: true });

      if (ai.isCheckmate || ai.isStalemate || ai.gameStatus === 'COMPLETED') {
        ClockManager.stop();
        this._triggerGameOver(ai);
        return;
      }

      if (ai.isCheck) Toast.error('Check!', 4000);
    }

    this.myTurn = true;
    this._updateStatus(this.inCheckSq ? '<span style="color:var(--red)">⚠ Check!</span> Your move' : 'Your turn');
  },

  async resign() {
    if (!this.game) return;
    if (confirm("Are you sure you want to resign?")) {
      const { ok } = await GameAPI.resign(this.game.gameId);
      if (ok) {
        ClockManager.stop();
        if (this.onGameOver) this.onGameOver({
          title: 'Resignation',
          msg:   'You conceded the game.',
          gameId: this.game.gameId
        });
      }
    }
  },

  selectPromotion(piece) {
    Modal.close('promo-modal');
    if (this.pendingPromo) {
      this._doMove(this.pendingPromo.from, this.pendingPromo.to, piece);
      this.pendingPromo = null;
    }
  },

  _showPromoModal(color) {
    const pieces = ['QUEEN','ROOK','BISHOP','KNIGHT'];
    const grid   = document.getElementById('promo-grid');
    if (!grid) return;
    grid.innerHTML = pieces.map(p => `
      <div class="promo-piece-btn" onclick="GameController.selectPromotion('${p}')">
        ${PIECE_UNICODE[color][p]}
      </div>`).join('');
    Modal.open('promo-modal');
  },

  _findKing(color) {
    for (const [sq, p] of Object.entries(this.boardState.board)) {
      if (p.type === 'KING' && p.color === color) return sq;
    }
    return null;
  },

  _triggerGameOver(result) {
    this.game.status = 'COMPLETED';
    this.myTurn      = false;
    let title, msg;
    if (result.isCheckmate) {
      const winnerIsWhite = result.gameResult === 'WHITE_WIN';
      const iWon = (winnerIsWhite && this.playerColor === 'WHITE') || (!winnerIsWhite && this.playerColor === 'BLACK');
      title = iWon ? '🎉 Victory!' : 'Engine Wins';
      msg   = iWon ? 'Checkmate!' : 'The engine checkmated you.';
    } else if (result.isStalemate) {
      title = 'Draw';
      msg   = 'Stalemate.';
    } else {
      title = 'Game Over';
      msg   = result.resultReason || '';
    }
    this._updateStatus(title);
    if (this.onGameOver) this.onGameOver({ title, msg, gameId: this.game?.gameId });
  },

  _updateStatus(htmlContent) {
    const el = document.getElementById('status-text');
    if (el) el.innerHTML = htmlContent; 
  }
};