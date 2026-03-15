# ChessForge Frontend

A modern, high-performance chess frontend built with React, Vite, and Tailwind CSS.

## Features
- **Real-time Play**: Integrated with STOMP WebSockets for instant move synchronization.
- **Advanced AI**: Challenge a Stockfish-inspired engine with 10 difficulty levels.
- **Beautiful UI**: Framer Motion animations, Lucide icons, and a chess.com-inspired aesthetic.
- **Responsive Design**: Play on any device, from desktop to mobile.
- **Dynamic Board**: Powered by `react-chessboard` with custom styling and smooth piece movements.

## Getting Started

### Prerequisites
- Node.js (v18+)
- npm

### Installation
1. `cd frontend`
2. `npm install`
3. `npm run dev`

### Environment Variables
The application uses the following defaults:
- Backend URL: `http://localhost:1111`
- WebSocket: `/ws`

## Tech Stack
- **Framework**: React 18
- **Build Tool**: Vite
- **Styling**: Tailwind CSS
- **Animations**: Framer Motion
- **Icons**: Lucide React
- **Networking**: Axios, StompJS, SockJS
- **Chess Logic**: chess.js, react-chessboard
