const LOCAL_API_BASE_URL = 'http://localhost:1111';
const LOCAL_WS_BASE_URL = 'http://localhost:1111/ws';
const PROD_API_BASE_URL = 'https://chessforge-production.up.railway.app';
const PROD_WS_BASE_URL = 'https://chessforge-production.up.railway.app/ws';

const isLocalNetworkHost = (hostname: string) =>
  hostname === 'localhost' ||
  hostname === '127.0.0.1' ||
  hostname === '0.0.0.0' ||
  hostname.endsWith('.local') ||
  hostname.startsWith('192.168.') ||
  hostname.startsWith('10.') ||
  /^172\.(1[6-9]|2\d|3[0-1])\./.test(hostname);

const isLocalhost = typeof window !== 'undefined' && isLocalNetworkHost(window.location.hostname);

const API_BASE_URL = import.meta.env.VITE_API_URL ||
  (isLocalhost ? LOCAL_API_BASE_URL : PROD_API_BASE_URL);
const WS_BASE_URL = import.meta.env.VITE_WS_URL ||
  (isLocalhost ? LOCAL_WS_BASE_URL : PROD_WS_BASE_URL);

export { API_BASE_URL, WS_BASE_URL };
