import { useState, useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from '../context/AuthContext';
import { WS_BASE_URL } from '../utils/constants';

interface MoveResponse {
  gameId: string;
  from: string;
  to: string;
  san: string;
  fen: string;
  isCapture: boolean;
  isCheck: boolean;
  isCheckmate: boolean;
  isStalemate: boolean;
  gameStatus: string;
  gameResult: string;
  resultReason: string;
  whiteTimeRemaining: number;
  blackTimeRemaining: number;
  aiMove?: MoveResponse;
  whiteRating?: number;
  blackRating?: number;
  whiteRatingChange?: number;
  blackRatingChange?: number;
}

export const useChessSocket = (gameId: string | undefined, onMoveReceived: (move: MoveResponse) => void) => {
  const { user } = useAuth();
  const [connected, setConnected] = useState(false);
  const stompClient = useRef<Client | null>(null);

  useEffect(() => {
    if (!gameId || !user) return;

    const socket = new SockJS(WS_BASE_URL);
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${user.accessToken}`,
      },
      onConnect: () => {
        setConnected(true);
        console.log('Connected to WebSocket');
        
        // Subscribe to game moves
        client.subscribe(`/topic/game/${gameId}`, (message) => {
          const move = JSON.parse(message.body);
          onMoveReceived(move);
        });

        // Subscribe to game end
        client.subscribe(`/topic/game/${gameId}/end`, (message) => {
          const result = JSON.parse(message.body);
          onMoveReceived(result);
        });
      },
      onDisconnect: () => {
        setConnected(false);
        console.log('Disconnected from WebSocket');
      },
      onStompError: (frame) => {
        console.error('STOMP Error:', frame);
      },
    });

    client.activate();
    stompClient.current = client;

    return () => {
      client.deactivate();
    };
  }, [gameId, user]);

  const sendMove = useCallback((from: string, to: string, promotion?: string) => {
    if (stompClient.current && connected) {
      stompClient.current.publish({
        destination: `/app/game/${gameId}/move`,
        body: JSON.stringify({ from, to, promotion }),
      });
    }
  }, [gameId, connected]);

  return { connected, sendMove };
};
