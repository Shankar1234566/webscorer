/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Dark navy base with electric blue / violet / cyan accents.
        navy: {
          950: '#050914',
          900: '#070d1f',
          850: '#0a1226',
          800: '#0d1730',
          700: '#132144',
          600: '#1b2d5c',
        },
        electric: {
          400: '#5b8cff',
          500: '#3b6dfb',
          600: '#2a55e0',
        },
        violetish: {
          400: '#a78bfa',
          500: '#8b5cf6',
          600: '#7c3aed',
        },
        cyanx: {
          300: '#67e8f9',
          400: '#22d3ee',
          500: '#06b6d4',
        },
      },
      fontFamily: {
        sans: [
          'Inter',
          'ui-sans-serif',
          'system-ui',
          '-apple-system',
          'Segoe UI',
          'Roboto',
          'Helvetica Neue',
          'Arial',
          'sans-serif',
        ],
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'Consolas', 'monospace'],
      },
      boxShadow: {
        card: '0 10px 40px -18px rgba(2, 6, 23, 0.9)',
        glow: '0 0 0 1px rgba(34, 211, 238, 0.18), 0 12px 40px -20px rgba(59, 109, 251, 0.55)',
      },
      backgroundImage: {
        'grid-fade':
          'radial-gradient(circle at 50% 0%, rgba(59,109,251,0.16), transparent 55%), radial-gradient(circle at 85% 20%, rgba(139,92,246,0.12), transparent 45%)',
      },
      keyframes: {
        shimmer: {
          '0%': { backgroundPosition: '-400px 0' },
          '100%': { backgroundPosition: '400px 0' },
        },
      },
      animation: {
        shimmer: 'shimmer 1.4s linear infinite',
      },
    },
  },
  plugins: [],
};
