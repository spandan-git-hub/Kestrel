/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        kestrel: {
          dark: '#0b0f19',
          card: '#131b2e',
          border: '#1e293b',
          accent: '#3b82f6',
        },
      },
    },
  },
  plugins: [],
}
