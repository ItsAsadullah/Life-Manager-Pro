import React from 'react';
import { Moon, Sun } from 'lucide-react';
import { useSettings } from '../contexts/SettingsContext';

export const ThemeToggle: React.FC = () => {
  const { theme, toggleTheme } = useSettings();
  console.log('ThemeToggle rendered, theme:', theme);

  return (
    <button
      onClick={toggleTheme}
      className="p-2 rounded-full bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300 transition-colors"
      aria-label="Toggle theme"
    >
      {theme === 'light' ? <Moon size={20} /> : <Sun size={20} />}
    </button>
  );
};
