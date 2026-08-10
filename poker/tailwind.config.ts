import type { Config } from 'tailwindcss'

export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{vue,ts}'],
  theme: {
    extend: {
      colors: {
        canvas: 'rgb(var(--canvas) / <alpha-value>)',
        felt: 'rgb(var(--felt) / <alpha-value>)',
        ink: 'rgb(var(--ink) / <alpha-value>)',
        muted: 'rgb(var(--muted) / <alpha-value>)',
        line: 'rgb(var(--line) / <alpha-value>)',
        gold: {
          400: '#f5c15c',
          500: '#e8a93e',
          600: '#d4932c'
        }
      },
      boxShadow: {
        card: '0 4px 14px rgb(0 0 0 / 0.28)',
        glow: '0 0 0 3px rgb(245 193 92 / 0.55)'
      },
      fontFamily: {
        sans: [
          '"Plus Jakarta Sans"',
          '"Noto Sans SC"',
          'PingFang SC',
          'Hiragino Sans GB',
          'Microsoft YaHei',
          'ui-sans-serif',
          'system-ui',
          'sans-serif'
        ]
      },
      keyframes: {
        'fade-up': {
          '0%': { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' }
        },
        'deal-card': {
          '0%': { opacity: '0', transform: 'translateY(-24px) scale(0.85)' },
          '100%': { opacity: '1', transform: 'translateY(0) scale(1)' }
        }
      },
      animation: {
        'fade-up': 'fade-up 0.45s ease-out both',
        'deal-card': 'deal-card 0.4s ease-out both'
      }
    }
  },
  plugins: []
} satisfies Config
