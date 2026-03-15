import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { motion } from 'framer-motion';
import { Trophy, Medal, Star, TrendingUp, Search } from 'lucide-react';

interface LeaderboardEntry {
  username: string;
  rating: number;
  gamesWon: number;
  gamesPlayed: number;
  winRate: number;
}

const LeaderboardPage: React.FC = () => {
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    const fetchLeaderboard = async () => {
      try {
        const response = await axios.get('/api/users/leaderboard');
        setLeaderboard(response.data.data);
      } catch (err) {
        console.error('Failed to fetch leaderboard', err);
      } finally {
        setLoading(false);
      }
    };
    fetchLeaderboard();
  }, []);

  const filteredLeaderboard = leaderboard.filter(entry => 
    entry.username.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="max-w-6xl mx-auto px-6 py-12">
      <div className="text-center mb-16">
        <motion.div 
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          className="w-20 h-20 bg-chess-green/20 rounded-full flex items-center justify-center mx-auto mb-6 text-chess-green shadow-[0_0_50px_rgba(119,149,86,0.3)]"
        >
          <Trophy size={40} fill="currentColor" />
        </motion.div>
        <h1 className="text-5xl font-black mb-4 tracking-tight">World Rankings</h1>
        <p className="text-gray-400 text-lg">Be the best. Master the sixty-four squares.</p>
      </div>

      <div className="relative max-w-2xl mx-auto mb-12">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500" size={20} />
        <input 
          type="text" 
          placeholder="Search for players..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full bg-[#262421] border border-white/10 rounded-2xl py-5 pl-12 pr-6 focus:outline-none focus:border-chess-green focus:ring-1 focus:ring-chess-green transition-all shadow-xl"
        />
      </div>

      <div className="glass-card bg-[#262421] border-white/5 overflow-hidden shadow-[0_0_100px_rgba(0,0,0,0.3)]">
        <div className="grid grid-cols-[80px_1fr_120px_120px_120px] gap-4 p-6 bg-[#161512] border-b border-white/5 text-xs font-black tracking-widest text-gray-500 uppercase">
          <span>Rank</span>
          <span>Player</span>
          <span className="text-center">Rating</span>
          <span className="text-center">Games</span>
          <span className="text-center">Win Rate</span>
        </div>

        {loading ? (
          <div className="p-20 flex flex-col items-center justify-center">
             <div className="w-10 h-10 border-4 border-chess-green/30 border-t-chess-green rounded-full animate-spin mb-4"></div>
             <p className="text-gray-500 animate-pulse">Calculating rankings...</p>
          </div>
        ) : (
          <div className="divide-y divide-white/5">
            {filteredLeaderboard.map((entry, i) => (
              <motion.div 
                key={entry.username}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.05 }}
                className="grid grid-cols-[80px_1fr_120px_120px_120px] gap-4 p-6 hover:bg-white/5 transition-colors items-center"
              >
                <div className="flex justify-center">
                  {i === 0 ? <Medal className="text-yellow-400" size={24} /> : 
                   i === 1 ? <Medal className="text-gray-300" size={24} /> : 
                   i === 2 ? <Medal className="text-amber-600" size={24} /> : 
                   <span className="text-gray-600 font-black text-lg">#{i + 1}</span>}
                </div>
                <div className="flex items-center space-x-4">
                  <div className={`w-10 h-10 rounded-lg flex items-center justify-center text-sm font-bold ${
                    i === 0 ? 'bg-yellow-400/10 text-yellow-400' : 'bg-white/5 text-gray-400'
                  }`}>
                    {entry.username[0].toUpperCase()}
                  </div>
                  <div className="font-bold text-lg text-gray-200">{entry.username}</div>
                </div>
                <div className="text-center">
                  <span className="text-chess-green font-black text-xl">{entry.rating}</span>
                </div>
                <div className="text-center text-gray-400 font-bold">{entry.gamesPlayed}</div>
                <div className="text-center flex flex-col items-center">
                  <span className="text-white font-bold">{entry.winRate}%</span>
                  <div className="w-16 h-1 bg-white/5 rounded-full mt-2 overflow-hidden">
                     <div className="h-full bg-chess-green" style={{ width: `${entry.winRate}%` }}></div>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default LeaderboardPage;
