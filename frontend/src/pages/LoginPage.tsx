import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import axios from 'axios';
import { motion } from 'framer-motion';
import { Mail, Lock, LogIn, AlertCircle, User } from 'lucide-react';

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const response = await axios.post('/api/auth/login', { username, password });
      login(response.data.data.accessToken, response.data.data);
      navigate('/play');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid username or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-6">
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="glass-card max-w-md w-full p-10 bg-white/5 border-white/10"
      >
        <div className="text-center mb-10">
          <div className="w-16 h-16 bg-chess-green rounded-2xl mx-auto flex items-center justify-center mb-6 shadow-2xl transform transition-transform hover:rotate-12">
            <span className="text-white text-3xl font-bold italic">C</span>
          </div>
          <h2 className="text-3xl font-black mb-2 tracking-tight">Welcome Back</h2>
          <p className="text-gray-400">Master your next move. Log in to your account.</p>
        </div>

        {error && (
          <motion.div 
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            className="mb-6 p-4 bg-red-500/10 border border-red-500/20 text-red-400 rounded-lg flex items-center space-x-3 text-sm"
          >
            <AlertCircle size={18} />
            <span>{error}</span>
          </motion.div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-2">
            <label className="text-sm font-bold text-gray-400 tracking-wider uppercase ml-1">Username</label>
            <div className="relative group">
              <User className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-chess-green transition-colors" size={20} />
              <input 
                type="text" 
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full bg-[#161512] border border-white/10 rounded-xl py-4 pl-12 pr-4 focus:outline-none focus:border-chess-green focus:ring-1 focus:ring-chess-green transition-all"
                placeholder="Enter your username"
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-bold text-gray-400 tracking-wider uppercase ml-1">Password</label>
            <div className="relative group">
              <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-chess-green transition-colors" size={20} />
              <input 
                type="password" 
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-[#161512] border border-white/10 rounded-xl py-4 pl-12 pr-4 focus:outline-none focus:border-chess-green focus:ring-1 focus:ring-chess-green transition-all"
                placeholder="••••••••"
              />
            </div>
          </div>

          <button 
            type="submit" 
            disabled={loading}
            className="w-full btn-primary py-4 text-lg flex items-center justify-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed mt-8"
          >
            {loading ? (
              <div className="w-6 h-6 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
            ) : (
              <>
                <LogIn size={20} />
                <span>Sign In</span>
              </>
            )}
          </button>
        </form>

        <p className="mt-8 text-center text-gray-400">
          Don't have an account? <Link to="/register" className="text-chess-green font-bold hover:underline">Sign up now</Link>
        </p>
      </motion.div>
    </div>
  );
};

export default LoginPage;
