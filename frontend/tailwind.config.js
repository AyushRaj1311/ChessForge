/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'chess-green': '#779556',
        'chess-light': '#ebecd0',
        'chess-dark': '#262421',
        'chess-accent': '#f6f6f6',
      },
    },
  },
  plugins: [],
}
