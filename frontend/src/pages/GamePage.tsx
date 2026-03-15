import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Chessboard } from 'react-chessboard';
import { Chess, Move } from 'chess.js';
import axios from 'axios';
import { motion, AnimatePresence } from 'framer-motion';
import { Timer, Trophy, RotateCcw, ChevronLeft, ChevronRight, Settings, Info, MessageCircle, MoreHorizontal } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useChessSocket } from '../hooks/useChessSocket';

const GamePage: React.FC = () => {
  const { gameId } = useParams<{ gameId: string }>();
  const [game, setGame] = useState(new Chess());
  const [gameData, setGameData] = useState<any>(null);
  const [fen, setFen] = useState(game.fen());
  const [moveHistory, setMoveHistory] = useState<string[]>([]);
  const [whiteTimer, setWhiteTimer] = useState(0);
  const [blackTimer, setBlackTimer] = useState(0);
  const [lastMove, setLastMove] = useState<any>(null);
  const [gameOver, setGameOver] = useState<any>(null);
  const { user } = useAuth();
  const navigate = useNavigate();

  const isWhite = useMemo(() => {
    if (!gameData || !user) return true;
    return gameData.whitePlayer === user.username;
  }, [gameData, user]);

  const onMoveReceived = useCallback((moveResp: any) => {
    if (moveResp.valid === false) return;
    
    const newGame = new Chess(moveResp.fen);
    setGame(newGame);
    setFen(moveResp.fen);
    setLastMove(moveResp);
    setWhiteTimer(moveResp.whiteTimeRemaining);
    setBlackTimer(moveResp.blackTimeRemaining);

    if (moveResp.gameStatus === 'COMPLETED') {
      setGameOver({
        result: moveResp.gameResult,
        reason: moveResp.resultReason,
        whiteRating: moveResp.whiteRating,
        blackRating: moveResp.blackRating,
        whiteRatingChange: moveResp.whiteRatingChange,
        blackRatingChange: moveResp.blackRatingChange,
      });
    }

    if (moveResp.san) {
      setMoveHistory(prev => [...prev, moveResp.san]);
    }

    if (moveResp.aiMove) {
      setTimeout(() => onMoveReceived(moveResp.aiMove), 500);
    }
  }, []);

  const { sendMove } = useChessSocket(gameId, onMoveReceived);

  useEffect(() => {
    const fetchGame = async () => {
      try {
        const response = await axios.get(`/api/games/${gameId}`);
        const data = response.data.data;
        setGameData(data);
        const newGame = new Chess(data.currentFen);
        setGame(newGame);
        setFen(data.currentFen);
        setWhiteTimer(data.whiteTimeRemaining);
        setBlackTimer(data.blackTimeRemaining);
        
        if (data.status === 'COMPLETED') {
           setGameOver({
              result: data.result,
              reason: data.resultReason,
              whiteRating: data.whiteRating,
              blackRating: data.blackRating,
              whiteRatingChange: data.whiteRatingChange,
              blackRatingChange: data.blackRatingChange,
           });
        }
      } catch (err) {
        console.error('Failed to fetch game', err);
      }
    };
    fetchGame();
  }, [gameId]);

  // Timer countdown
  useEffect(() => {
    if (!gameData || gameData.status !== 'IN_PROGRESS' || gameOver) return;

    const interval = setInterval(() => {
      const turn = game.turn();
      if (turn === 'w') {
        setWhiteTimer(prev => Math.max(0, prev - 1));
      } else {
        setBlackTimer(prev => Math.max(0, prev - 1));
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [gameData, game, gameOver]);

  const onDrop = (sourceSquare: string, targetSquare: string) => {
    if (gameOver) return false;

    // Check if it's the player's turn
    const turn = game.turn();
    if ((turn === 'w' && !isWhite) || (turn === 'b' && isWhite)) return false;

    try {
      console.log('Attempting move:', { from: sourceSquare, to: targetSquare });
      const move = game.move({
        from: sourceSquare,
        to: targetSquare,
        promotion: 'q',
      });

      if (move) {
        console.log('Local move valid, sending to backend...');
        setFen(game.fen()); // Update board immediately for responsiveness
        
        // Fallback: If WebSocket is slow/failed, send via HTTP as well
        sendMove(sourceSquare, targetSquare, 'q');
        
        // Try HTTP move if WebSocket isn't confirming
        axios.post(`/api/games/${gameId}/moves`, {
          from: sourceSquare,
          to: targetSquare,
          promotion: 'q'
        }).catch(err => console.error('HTTP Move Fallback failed:', err));

        return true;
      } else {
        console.warn('Local move invalid according to chess.js');
      }
    } catch (e) {
      console.error('Error in onDrop:', e);
      return false;
    }
    return false;
  };

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="max-w-[1400px] mx-auto px-4 py-8 grid lg:grid-cols-[1fr_400px] gap-8 h-[calc(100vh-140px)]">
      {/* Left: Board & Players */}
      <div className="flex flex-col space-y-4">
        {/* Opponent Info */}
        <div className="flex items-center justify-between bg-[#262421] p-4 rounded-xl border border-white/5">
          <div className="flex items-center space-x-4">
            <div className="w-12 h-12 bg-[#312e2b] rounded-lg flex items-center justify-center">
              <span className="text-gray-400 text-xl font-bold">
                {isWhite ? (gameData?.blackPlayer?.[0] || 'A') : (gameData?.whitePlayer?.[0] || 'A')}
              </span>
            </div>
            <div>
              <div className="font-bold text-lg">{isWhite ? (gameData?.blackPlayer || 'AI') : (gameData?.whitePlayer || 'AI')}</div>
              <div className="text-chess-green text-sm font-bold">{isWhite ? (gameData?.blackRating || 1200) : (gameData?.whiteRating || 1200)} ELO</div>
            </div>
          </div>
          <div className={`text-4xl font-mono font-black px-6 py-2 rounded-lg ${
            game.turn() === (isWhite ? 'b' : 'w') ? 'bg-white text-black' : 'bg-[#161512] text-gray-500'
          }`}>
            {formatTime(isWhite ? blackTimer : whiteTimer)}
          </div>
        </div>

        {/* The Board */}
        <div className="flex-grow flex items-center justify-center bg-[#262421] rounded-2xl p-4 shadow-2xl relative overflow-hidden">
          <div className="w-full max-w-[650px] aspect-square">
            <Chessboard 
              position={fen} 
              onPieceDrop={onDrop} 
              boardOrientation={isWhite ? 'white' : 'black'}
              customDarkSquareStyle={{ backgroundColor: '#779556' }}
              customLightSquareStyle={{ backgroundColor: '#ebecd0' }}
              animationDuration={200}
            />
          </div>
        </div>

        {/* Player Info */}
        <div className="flex items-center justify-between bg-[#262421] p-4 rounded-xl border border-white/5">
          <div className="flex items-center space-x-4">
            <div className="w-12 h-12 bg-chess-green rounded-lg flex items-center justify-center shadow-lg">
              <span className="text-white text-xl font-bold">{user?.username[0].toUpperCase()}</span>
            </div>
            <div>
              <div className="font-bold text-lg">{user?.username} (You)</div>
              <div className="text-chess-green text-sm font-bold">{user?.rating} ELO</div>
            </div>
          </div>
          <div className={`text-4xl font-mono font-black px-6 py-2 rounded-lg ${
            game.turn() === (isWhite ? 'w' : 'b') ? 'bg-white text-black' : 'bg-[#161512] text-gray-500'
          }`}>
            {formatTime(isWhite ? whiteTimer : blackTimer)}
          </div>
        </div>
      </div>

      {/* Right: Sidebar */}
      <div className="flex flex-col space-y-4 h-full">
        {/* Moves & History */}
        <div className="glass-card bg-[#262421] border-white/5 flex flex-col h-[50%] overflow-hidden">
          <div className="flex items-center justify-between mb-4 border-b border-white/5 pb-2">
            <div className="flex items-center space-x-2 text-gray-400">
              <RotateCcw size={18} />
              <span className="font-bold text-sm tracking-widest uppercase">Move History</span>
            </div>
            <div className="flex space-x-2">
              <button className="p-1 hover:bg-white/5 rounded"><ChevronLeft size={20} /></button>
              <button className="p-1 hover:bg-white/5 rounded"><ChevronRight size={20} /></button>
            </div>
          </div>
          
          <div className="grid grid-cols-2 gap-2 overflow-y-auto pr-2 custom-scrollbar">
            {moveHistory.reduce((acc: any[], move, i) => {
              if (i % 2 === 0) acc.push([move]);
              else acc[acc.length - 1].push(move);
              return acc;
            }, []).map((pair, i) => (
              <React.Fragment key={i}>
                <div className="flex items-center space-x-2 p-2 bg-white/5 rounded-lg border border-white/5">
                  <span className="text-gray-600 text-xs font-bold w-4">{i + 1}.</span>
                  <span className="font-bold text-sm text-gray-200">{pair[0]}</span>
                </div>
                {pair[1] && (
                  <div className="flex items-center space-x-2 p-2 bg-white/5 rounded-lg border border-white/5">
                    <span className="text-gray-600 text-xs font-bold w-4"></span>
                    <span className="font-bold text-sm text-gray-200">{pair[1]}</span>
                  </div>
                )}
              </React.Fragment>
            ))}
          </div>
        </div>

        {/* Chat & Analysis */}
        <div className="glass-card bg-[#262421] border-white/5 flex flex-col h-[50%] overflow-hidden p-0">
          <div className="flex border-b border-white/5">
            <button className="flex-1 py-3 font-bold text-sm bg-white/5 text-white border-b-2 border-chess-green">ANALYSIS</button>
            <button className="flex-1 py-3 font-bold text-sm text-gray-500 hover:text-gray-300">CHAT</button>
          </div>
          <div className="p-6 flex flex-col items-center justify-center flex-grow text-center">
            <div className="w-16 h-16 bg-white/5 rounded-full flex items-center justify-center mb-4">
              <MoreHorizontal className="text-gray-500" />
            </div>
            <h4 className="text-white font-bold mb-2">Game Analysis</h4>
            <p className="text-gray-500 text-sm">Post-game analysis will be available once the game ends.</p>
          </div>
          <div className="p-4 bg-[#161512] flex space-x-2">
             <button className="flex-1 btn-secondary text-sm py-2 px-0 flex items-center justify-center space-x-2">
                <RotateCcw size={16} />
                <span>Draw</span>
             </button>
             <button onClick={() => navigate('/play')} className="flex-1 bg-red-500/10 hover:bg-red-500/20 text-red-500 font-bold text-sm py-2 rounded-lg transition-all border border-red-500/20">
                Resign
             </button>
          </div>
        </div>
      </div>

      {/* Game Over Modal */}
      <AnimatePresence>
        {gameOver && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 backdrop-blur-sm px-6"
          >
            <motion.div 
              initial={{ scale: 0.9, y: 20 }}
              animate={{ scale: 1, y: 0 }}
              className="glass-card max-w-md w-full p-10 bg-[#262421] border-white/10 text-center relative"
            >
              <div className="absolute -top-12 left-1/2 -translate-x-1/2 w-24 h-24 bg-chess-green rounded-full flex items-center justify-center shadow-[0_0_50px_rgba(119,149,86,0.5)] border-4 border-[#262421]">
                <Trophy size={48} className="text-white" fill="currentColor" />
              </div>
              
              <h2 className="text-4xl font-black mt-8 mb-2 tracking-tight">
                {gameOver.result === 'WHITE_WIN' ? (isWhite ? 'YOU WON!' : 'BLACK WON') : 
                 gameOver.result === 'BLACK_WIN' ? (isWhite ? 'BLACK WON' : 'YOU WON!') : 'DRAW'}
              </h2>
              <p className="text-chess-green font-bold tracking-widest uppercase mb-8">by {gameOver.reason}</p>

              <div className="grid grid-cols-2 gap-4 mb-10">
                <div className="bg-[#161512] p-4 rounded-2xl border border-white/5">
                  <div className="text-gray-500 text-xs font-bold uppercase mb-1">New Rating</div>
                  <div className="text-2xl font-black">{isWhite ? gameOver.whiteRating : gameOver.blackRating}</div>
                  <div className={`text-sm font-bold ${(isWhite ? gameOver.whiteRatingChange : gameOver.blackRatingChange) >= 0 ? 'text-chess-green' : 'text-red-500'}`}>
                    {(isWhite ? gameOver.whiteRatingChange : gameOver.blackRatingChange) >= 0 ? '+' : ''}
                    {isWhite ? gameOver.whiteRatingChange : gameOver.blackRatingChange}
                  </div>
                </div>
                <div className="bg-[#161512] p-4 rounded-2xl border border-white/5">
                  <div className="text-gray-500 text-xs font-bold uppercase mb-1">Opponent</div>
                  <div className="text-2xl font-black">{isWhite ? gameOver.blackRating : gameOver.whiteRating}</div>
                  <div className={`text-sm font-bold ${(isWhite ? gameOver.blackRatingChange : gameOver.whiteRatingChange) >= 0 ? 'text-chess-green' : 'text-red-500'}`}>
                    {(isWhite ? gameOver.blackRatingChange : gameOver.whiteRatingChange) >= 0 ? '+' : ''}
                    {isWhite ? gameOver.blackRatingChange : gameOver.whiteRatingChange}
                  </div>
                </div>
              </div>

              <div className="space-y-3">
                <button onClick={() => navigate('/play')} className="w-full btn-primary py-4 text-lg">New Game</button>
                <button onClick={() => setGameOver(null)} className="w-full btn-secondary py-4 text-lg">Close</button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default GamePage;
