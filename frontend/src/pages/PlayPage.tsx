import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { motion } from 'framer-motion';
import { Play, User, Cpu, Timer, Shield, Info } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const PlayPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [aiLevel, setAiLevel] = useState(1);
  const [gameMode, setGameMode] = useState('BLITZ');
  const { user } = useAuth();
  const navigate = useNavigate();

  const handleStartAiGame = async () => {
    setLoading(true);
    try {
      console.log('Starting AI game...', { gameMode, aiLevel });
      const response = await axios.post('/api/games', {
        gameMode,
        vsAi: true,
        aiLevel
      });
      console.log('Game created:', response.data);
      if (response.data?.data?.gameId) {
        navigate(`/game/${response.data.data.gameId}`);
      } else {
        alert('Game created but no ID returned');
      }
    } catch (err: any) {
      console.error('Failed to start AI game', err);
      alert(`Error starting game: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleFindMatch = async () => {
    setLoading(true);
    try {
      console.log('Joining matchmaking...', { gameMode });
      await axios.post('/api/matchmaking/join', { gameMode });
      alert("Finding match... We will redirect you once a player is found.");
    } catch (err: any) {
      console.error('Failed to join matchmaking', err);
      alert(`Error joining matchmaking: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-6 py-12">
      <div className="grid lg:grid-cols-2 gap-12">
        {/* Play AI Card */}
        <motion.div 
          whileHover={{ y: -5 }}
          className="glass-card bg-white/5 border-white/10 p-10 flex flex-col h-full"
        >
          <div className="flex items-center space-x-4 mb-8">
            <div className="w-14 h-14 bg-blue-500/20 rounded-2xl flex items-center justify-center text-blue-400">
              <Cpu size={32} />
            </div>
            <div>
              <h2 className="text-3xl font-black tracking-tight">Practice vs AI</h2>
              <p className="text-gray-400">Sharpen your skills against the engine.</p>
            </div>
          </div>

          <div className="space-y-8 flex-grow">
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <label className="text-sm font-bold text-gray-400 tracking-wider uppercase">AI Level</label>
                <span className="text-blue-400 font-bold px-3 py-1 bg-blue-500/10 rounded-full">{aiLevel} / 10</span>
              </div>
              <input 
                type="range" 
                min="1" 
                max="10" 
                value={aiLevel}
                onChange={(e) => setAiLevel(parseInt(e.target.value))}
                className="w-full h-2 bg-[#161512] rounded-lg appearance-none cursor-pointer accent-blue-500"
              />
              <div className="flex justify-between text-xs text-gray-500 font-bold px-1">
                <span>NOVICE</span>
                <span>GRANDMASTER</span>
              </div>
            </div>

            <div className="space-y-4">
              <label className="text-sm font-bold text-gray-400 tracking-wider uppercase">Time Control</label>
              <div className="grid grid-cols-3 gap-3">
                {['BULLET', 'BLITZ', 'RAPID'].map((mode) => (
                  <button
                    key={mode}
                    onClick={() => setGameMode(mode)}
                    className={`py-3 rounded-xl border font-bold transition-all ${
                      gameMode === mode 
                      ? 'bg-blue-500 border-blue-500 text-white shadow-[0_0_20px_rgba(59,130,246,0.3)]' 
                      : 'bg-[#161512] border-white/10 text-gray-400 hover:border-white/20'
                    }`}
                  >
                    {mode}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <button 
            onClick={handleStartAiGame}
            disabled={loading}
            className="w-full bg-blue-500 hover:bg-blue-600 text-white font-black py-5 rounded-2xl text-xl flex items-center justify-center space-x-3 mt-10 shadow-lg transform transition-all active:scale-95"
          >
            <Play size={24} fill="currentColor" />
            <span>START GAME</span>
          </button>
        </motion.div>

        {/* Play Online Card */}
        <motion.div 
          whileHover={{ y: -5 }}
          className="glass-card bg-white/5 border-white/10 p-10 flex flex-col h-full border-chess-green/20"
        >
          <div className="flex items-center space-x-4 mb-8">
            <div className="w-14 h-14 bg-chess-green/20 rounded-2xl flex items-center justify-center text-chess-green">
              <User size={32} />
            </div>
            <div>
              <h2 className="text-3xl font-black tracking-tight text-white">Play Online</h2>
              <p className="text-gray-400">Challenge players from around the world.</p>
            </div>
          </div>

          <div className="space-y-8 flex-grow">
            <div className="bg-[#161512] rounded-2xl p-6 border border-white/5">
              <div className="flex items-center space-x-3 mb-4 text-chess-green">
                <Shield size={20} />
                <span className="font-bold tracking-tight">RATED MATCHMAKING</span>
              </div>
              <p className="text-sm text-gray-400 leading-relaxed">
                Win games to increase your ELO. You will be matched with players of similar rating ({user?.rating || 1200} ± 200).
              </p>
            </div>

            <div className="space-y-4">
              <label className="text-sm font-bold text-gray-400 tracking-wider uppercase">Select Format</label>
              <div className="grid grid-cols-1 gap-3">
                <button className="flex items-center justify-between p-4 rounded-xl border border-chess-green/30 bg-chess-green/5 text-white hover:bg-chess-green/10 transition-all">
                  <div className="flex items-center space-x-3">
                    <Timer size={24} className="text-chess-green" />
                    <div className="text-left">
                      <div className="font-bold">Blitz (3+2)</div>
                      <div className="text-xs text-gray-500">Fast & Competitive</div>
                    </div>
                  </div>
                  <div className="text-chess-green text-sm font-black">POPULAR</div>
                </button>
              </div>
            </div>
          </div>

          <button 
            onClick={handleFindMatch}
            disabled={loading}
            className="w-full btn-primary py-5 rounded-2xl text-xl flex items-center justify-center space-x-3 mt-10 font-black shadow-[0_0_30px_rgba(119,149,86,0.3)]"
          >
            <Play size={24} fill="currentColor" />
            <span>FIND MATCH</span>
          </button>
        </motion.div>
      </div>

      {/* Info Section */}
      <div className="mt-12 flex items-center justify-center space-x-2 text-gray-500 text-sm italic">
        <Info size={16} />
        <p>All games follow official FIDE chess rules with 50-move and insufficient material draw detection.</p>
      </div>
    </div>
  );
};

export default PlayPage;
