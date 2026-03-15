import React from 'react';
import { Link } from 'react-router-dom';
import { Play, Shield, Zap, Trophy, Github, Star } from 'lucide-react';
import { motion } from 'framer-motion';

const LandingPage: React.FC = () => {
  return (
    <div className="relative overflow-hidden">
      {/* Hero Section */}
      <section className="relative h-[90vh] flex items-center justify-center px-6 overflow-hidden bg-gradient-to-br from-[#161512] via-[#262421] to-[#161512]">
        <div className="container mx-auto grid lg:grid-cols-2 gap-12 items-center">
          <motion.div 
            initial={{ opacity: 0, x: -50 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.8 }}
            className="text-left"
          >
            <div className="inline-flex items-center space-x-2 px-3 py-1 bg-chess-green/20 text-chess-green rounded-full mb-6 border border-chess-green/30">
              <Star size={16} fill="currentColor" />
              <span className="text-sm font-bold tracking-wide">ELITE CHESS BACKEND</span>
            </div>
            <h1 className="text-6xl lg:text-8xl font-black mb-6 leading-tight tracking-tight">
              Master the <br />
              <span className="text-chess-green">Sixty-Four</span> <br />
              Squares.
            </h1>
            <p className="text-xl text-gray-400 mb-10 max-w-xl leading-relaxed">
              Experience the world's most responsive chess platform. Play against advanced AI, challenge players globally, and climb the leaderboard.
            </p>
            <div className="flex items-center space-x-6">
              <Link to="/play" className="btn-primary py-4 px-10 text-xl flex items-center space-x-3">
                <Play size={24} fill="currentColor" />
                <span>Play Now</span>
              </Link>
              <a href="https://github.com/AyushRaj1311/ChessForge" className="btn-secondary py-4 px-10 text-xl flex items-center space-x-3 bg-white/5 hover:bg-white/10">
                <Github size={24} />
                <span>GitHub</span>
              </a>
            </div>
          </motion.div>

          <motion.div 
            initial={{ opacity: 0, scale: 0.8, rotate: -5 }}
            animate={{ opacity: 1, scale: 1, rotate: 0 }}
            transition={{ duration: 1, delay: 0.2 }}
            className="relative"
          >
            <div className="absolute -top-20 -left-20 w-80 h-80 bg-chess-green/10 rounded-full blur-[100px] animate-pulse"></div>
            <div className="absolute -bottom-20 -right-20 w-80 h-80 bg-blue-500/10 rounded-full blur-[100px] animate-pulse"></div>
            
            <div className="relative glass-card p-2 border-white/20 shadow-[0_0_100px_rgba(0,0,0,0.5)] transform hover:scale-[1.02] transition-transform duration-500">
               <div className="aspect-square bg-chess-dark rounded-lg overflow-hidden grid grid-cols-8 grid-rows-8">
                  {Array.from({ length: 64 }).map((_, i) => {
                    const r = Math.floor(i / 8);
                    const c = i % 8;
                    const isDark = (r + c) % 2 === 1;
                    return (
                      <div 
                        key={i} 
                        className={`w-full h-full ${isDark ? 'bg-[#779556]' : 'bg-[#ebecd0]'} relative transition-colors duration-500 hover:brightness-110`}
                      >
                        {i === 4 && <span className="absolute inset-0 flex items-center justify-center text-5xl drop-shadow-lg">♔</span>}
                        {i === 60 && <span className="absolute inset-0 flex items-center justify-center text-5xl drop-shadow-lg text-[#262421]">♚</span>}
                      </div>
                    );
                  })}
               </div>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Features */}
      <section className="py-32 bg-[#161512]">
        <div className="container mx-auto px-6">
          <div className="text-center mb-20">
            <h2 className="text-4xl font-bold mb-4">Why ChessForge?</h2>
            <p className="text-gray-400 max-w-2xl mx-auto">Built with performance and security at its core, ChessForge provides the ultimate environment for competitive play.</p>
          </div>
          <div className="grid md:grid-cols-3 gap-8">
            <FeatureCard 
              icon={<Zap className="text-yellow-400" />} 
              title="Ultra-Low Latency" 
              description="Powered by STOMP WebSockets, every move is synchronized in real-time with sub-50ms latency."
            />
            <FeatureCard 
              icon={<Shield className="text-blue-400" />} 
              title="Secure Play" 
              description="Enterprise-grade JWT authentication and secure backend validation ensure fair play and account security."
            />
            <FeatureCard 
              icon={<Trophy className="text-chess-green" />} 
              title="Competitive Ranking" 
              description="A robust ELO rating system tracks your progress and matches you with players of similar skill."
            />
          </div>
        </div>
      </section>
    </div>
  );
};

const FeatureCard = ({ icon, title, description }: { icon: React.ReactNode, title: string, description: string }) => (
  <motion.div 
    whileHover={{ y: -10 }}
    className="glass-card flex flex-col items-center text-center p-10 bg-white/5 border-white/5 hover:border-chess-green/30 transition-all duration-300"
  >
    <div className="w-16 h-16 bg-white/5 rounded-2xl flex items-center justify-center mb-6 shadow-xl border border-white/5">
      {icon}
    </div>
    <h3 className="text-2xl font-bold mb-4">{title}</h3>
    <p className="text-gray-400 leading-relaxed">{description}</p>
  </motion.div>
);

export default LandingPage;
