/* ================================================================
   ChessForge – Premium Client-Side Chess Logic
   Handles FEN parsing, lightweight UI state, and UX move hints
================================================================ */

// ── CONSTANTS & LOOKUPS ──────────────────────────────
const PIECE_UNICODE = {
  WHITE: { KING: '♔', QUEEN: '♕', ROOK: '♖', BISHOP: '♗', KNIGHT: '♘', PAWN: '♙' },
  BLACK: { KING: '♚', QUEEN: '♛', ROOK: '♜', BISHOP: '♝', KNIGHT: '♞', PAWN: '♟' }
};

const STARTING_FEN = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';

// ── UTILITIES ────────────────────────────────────────
const sqName  = (row, col) => String.fromCharCode(97 + col) + (row + 1);
const sqRow   = (sq) => parseInt(sq[1]) - 1;
const sqCol   = (sq) => sq.charCodeAt(0) - 97;
const sqValid = (r, c) => r >= 0 && r <= 7 && c >= 0 && c <= 7;

// ════════════════════════════════════════════════════
// CORE: FEN PARSER
// ════════════════════════════════════════════════════
function parseFen(fen) {
  const board = {};
  const [pos, turn, castling, ep] = fen.split(' ');

  const TYPE_MAP = { 
    k: 'KING', q: 'QUEEN', r: 'ROOK', 
    b: 'BISHOP', n: 'KNIGHT', p: 'PAWN' 
  };
  
  const rows = pos.split('/');

  for (let r = 0; r < 8; r++) {
    let c = 0;
    for (const ch of rows[r]) {
      if (/\d/.test(ch)) {
        c += parseInt(ch);
      } else {
        const color = ch === ch.toUpperCase() ? 'WHITE' : 'BLACK';
        const type  = TYPE_MAP[ch.toLowerCase()];
        // rows[0] in FEN is Rank 8 (row index 7 in our system)
        board[sqName(7 - r, c)] = { type, color };
        c++;
      }
    }
  }

  return {
    board,
    turn: turn === 'w' ? 'WHITE' : 'BLACK',
    castling: castling === '-' ? '' : castling,
    enPassant: ep !== '-' ? ep : null
  };
}

// ════════════════════════════════════════════════════
// CORE: PSEUDO-LEGAL MOVE HINTS
// ════════════════════════════════════════════════════
function getPseudoLegalMoves(sq, state) {
  const { board, enPassant, castling } = state;
  const piece = board[sq];
  if (!piece) return [];

  const row = sqRow(sq);
  const col = sqCol(sq);
  const moves = [];

  // Helper: returns true if square is empty (so sliding pieces can continue)
  const addSq = (r, c) => {
    if (!sqValid(r, c)) return false;
    const targetSq = sqName(r, c);
    const targetPiece = board[targetSq];
    
    // Blocked by friendly piece
    if (targetPiece && targetPiece.color === piece.color) return false;
    
    moves.push(targetSq);
    
    // If it's an enemy piece, we can capture it, but we can't slide past it
    return !targetPiece; 
  };

  const slide = (dr, dc) => {
    for (let i = 1; i < 8; i++) {
      if (!addSq(row + dr * i, col + dc * i)) break;
    }
  };

  switch (piece.type) {
    case 'PAWN': {
      const dir   = piece.color === 'WHITE' ? 1 : -1;
      const start = piece.color === 'WHITE' ? 1 : 6;
      
      // Forward moves
      const fwd1 = sqName(row + dir, col);
      if (sqValid(row + dir, col) && !board[fwd1]) {
        moves.push(fwd1);
        const fwd2 = sqName(row + 2 * dir, col);
        if (row === start && !board[fwd2]) {
          moves.push(fwd2);
        }
      }
      
      // Diagonal Captures
      for (const dc of [-1, 1]) {
        if (!sqValid(row + dir, col + dc)) continue;
        const capSq = sqName(row + dir, col + dc);
        const targetPiece = board[capSq];
        
        if ((targetPiece && targetPiece.color !== piece.color) || capSq === enPassant) {
          moves.push(capSq);
        }
      }
      break;
    }
    case 'KNIGHT':
      [[-2,-1], [-2,1], [-1,-2], [-1,2], [1,-2], [1,2], [2,-1], [2,1]].forEach(([dr, dc]) => {
        addSq(row + dr, col + dc);
      });
      break;
    case 'BISHOP':
      slide(1,1); slide(1,-1); slide(-1,1); slide(-1,-1);
      break;
    case 'ROOK':
      slide(1,0); slide(-1,0); slide(0,1); slide(0,-1);
      break;
    case 'QUEEN':
      slide(1,1); slide(1,-1); slide(-1,1); slide(-1,-1);
      slide(1,0); slide(-1,0); slide(0,1); slide(0,-1);
      break;
    case 'KING':
      [[-1,-1], [-1,0], [-1,1], [0,-1], [0,1], [1,-1], [1,0], [1,1]].forEach(([dr, dc]) => {
        addSq(row + dr, col + dc);
      });
      
      // Castling Hints (Only checks rights and empty squares. Server validates check/attacks)
      if (piece.color === 'WHITE' && row === 0 && col === 4) {
        if (castling.includes('K') && !board['f1'] && !board['g1']) moves.push('g1');
        if (castling.includes('Q') && !board['d1'] && !board['c1'] && !board['b1']) moves.push('c1');
      } else if (piece.color === 'BLACK' && row === 7 && col === 4) {
        if (castling.includes('k') && !board['f8'] && !board['g8']) moves.push('g8');
        if (castling.includes('q') && !board['d8'] && !board['c8'] && !board['b8']) moves.push('c8');
      }
      break;
  }

  return moves;
}

// ════════════════════════════════════════════════════
// STATE: CHESS BOARD CLASS
// ════════════════════════════════════════════════════
class ChessBoardState {
  constructor() {
    this.reset();
  }

  reset() {
    this.fen        = STARTING_FEN;
    this.board      = {};
    this.turn       = 'WHITE';
    this.enPassant  = null;
    this.castling   = 'KQkq';
    
    // UI tracking state
    this.selected   = null;
    this.legalHints = [];
    this.lastMove   = null; // { from: 'e2', to: 'e4' }
    
    this.load(this.fen);
  }

  // Applies a new FEN from the server
  load(fen) {
    if (!fen) return;
    this.fen = fen;
    
    const parsed    = parseFen(fen);
    this.board      = parsed.board;
    this.turn       = parsed.turn;
    this.enPassant  = parsed.enPassant;
    this.castling   = parsed.castling;
    
    // Instantly clear selection to prevent graphical ghosting
    this.selected   = null;
    this.legalHints = [];
  }

  // Triggered when user clicks one of their own pieces
  selectSquare(sq) {
    const piece = this.board[sq];
    if (!piece) {
      this.selected   = null;
      this.legalHints = [];
      return false;
    }
    this.selected   = sq;
    // Pass 'this' so the generator has access to board, enPassant, and castling
    this.legalHints = getPseudoLegalMoves(sq, this);
    return true;
  }

  isLegalHint(sq) { 
    return this.legalHints.includes(sq); 
  }

  // Checks if a planned pawn move ends on the promotion rank
  isPromotion(from, to) {
    const piece = this.board[from];
    if (!piece || piece.type !== 'PAWN') return false;
    
    const toRow = sqRow(to);
    return (piece.color === 'WHITE' && toRow === 7) ||
           (piece.color === 'BLACK' && toRow === 0);
  }

  // Calculates the material difference between the current board and the starting position
  getCaptured() {
    const startCounts = { PAWN: 8, KNIGHT: 2, BISHOP: 2, ROOK: 2, QUEEN: 1 };
    const currentCounts = { WHITE: {}, BLACK: {} };

    // Count what is currently on the board
    for (const sq of Object.keys(this.board)) {
      const p = this.board[sq];
      if (p.type !== 'KING') {
        currentCounts[p.color][p.type] = (currentCounts[p.color][p.type] || 0) + 1;
      }
    }

    const captured = { WHITE: [], BLACK: [] };

    // Compare against starting counts
    for (const color of ['WHITE', 'BLACK']) {
      for (const [type, maxCount] of Object.entries(startCounts)) {
        const remaining = currentCounts[color][type] || 0;
        const lost = maxCount - remaining;
        
        for (let i = 0; i < lost; i++) {
          captured[color].push(PIECE_UNICODE[color][type]);
        }
      }
    }

    return captured;
  }
}