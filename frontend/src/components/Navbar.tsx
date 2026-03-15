import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Trophy, LogOut, User, Play, LogIn, UserPlus } from 'lucide-react';

const Navbar: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="bg-[#262421] border-b border-white/5 px-4 sm:px-8 py-4 flex justify-between items-center sticky top-0 z-50 backdrop-blur-md bg-opacity-90">
      <Link to="/" className="flex items-center space-x-2 group">
        <div className="w-10 h-10 bg-chess-green rounded flex items-center justify-center transform transition-transform group-hover:rotate-12">
          <span className="text-white text-2xl font-bold">C</span>
        </div>
        <span className="text-white text-xl font-bold tracking-tight">ChessForge</span>
      </Link>

      <div className="flex items-center space-x-6">
        <Link to="/leaderboard" className="text-gray-300 hover:text-white flex items-center space-x-1 transition-colors">
          <Trophy size={18} />
          <span>Leaderboard</span>
        </Link>

        {user ? (
          <>
            <Link to="/play" className="text-chess-green hover:text-[#85a462] flex items-center space-x-1 font-semibold transition-colors">
              <Play size={18} fill="currentColor" />
              <span>Play</span>
            </Link>
            <div className="h-6 w-px bg-white/10 mx-2"></div>
            <div className="flex items-center space-x-4">
              <div className="flex flex-col items-end">
                <span className="text-white font-medium text-sm">{user.username}</span>
                <span className="text-chess-green text-xs font-bold">{user.rating} ELO</span>
              </div>
              <button onClick={handleLogout} className="text-gray-400 hover:text-red-400 transition-colors p-2 hover:bg-red-400/10 rounded-full">
                <LogOut size={20} />
              </button>
            </div>
          </>
        ) : (
          <div className="flex items-center space-x-4">
            <Link to="/login" className="text-gray-300 hover:text-white flex items-center space-x-1 transition-colors px-4 py-2">
              <LogIn size={18} />
              <span>Login</span>
            </Link>
            <Link to="/register" className="btn-primary flex items-center space-x-1">
              <UserPlus size={18} />
              <span>Sign Up</span>
            </Link>
          </div>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
