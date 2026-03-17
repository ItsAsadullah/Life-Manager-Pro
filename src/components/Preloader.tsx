import React from 'react';
import { motion } from 'motion/react';
import { HandCoins } from 'lucide-react';
import { useSettings } from '../contexts/SettingsContext';

export const Preloader: React.FC = () => {
  const { t } = useSettings();
  return (
    <div className="fixed inset-0 z-[100] flex flex-col items-center justify-center bg-white">
      <motion.div
        initial={{ scale: 0.8, opacity: 0 }}
        animate={{ 
          scale: [0.8, 1.1, 1],
          opacity: 1,
          rotate: [0, 10, -10, 0]
        }}
        transition={{ 
          duration: 1.5,
          repeat: Infinity,
          ease: "easeInOut"
        }}
        className="mb-6"
      >
        <div className="w-20 h-20 bg-indigo-600 rounded-3xl flex items-center justify-center shadow-2xl shadow-indigo-200">
          <HandCoins className="text-white w-10 h-10" />
        </div>
      </motion.div>
      
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5 }}
        className="text-center"
      >
        <h1 className="text-2xl font-black text-gray-900 tracking-tighter mb-1">HISAB NIKASH</h1>
        <div className="flex items-center justify-center space-x-1">
          {[0, 1, 2].map((i) => (
            <motion.div
              key={i}
              animate={{ 
                scale: [1, 1.5, 1],
                opacity: [0.3, 1, 0.3]
              }}
              transition={{ 
                duration: 1,
                repeat: Infinity,
                delay: i * 0.2
              }}
              className="w-1.5 h-1.5 bg-indigo-600 rounded-full"
            />
          ))}
        </div>
      </motion.div>
      
      <div className="absolute bottom-10 text-gray-400 text-[10px] font-bold tracking-widest uppercase">
        {t('secureFinancialManager')}
      </div>
    </div>
  );
};
