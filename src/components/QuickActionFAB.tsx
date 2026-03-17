import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import { Plus, X, StickyNote, ShoppingCart, HandCoins, Receipt, Calculator as CalcIcon, ArrowUpCircle, ArrowDownCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'motion/react';
import { useSettings } from '../contexts/SettingsContext';

export const QuickActionFAB: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [showCalculator, setShowCalculator] = useState(false);
  const navigate = useNavigate();
  const { t } = useSettings();

  const actions = [
    { icon: <ArrowDownCircle size={20} />, label: t('addIncome'), color: 'bg-emerald-500', path: '/expenses', state: { openAddModal: true, type: 'income' } },
    { icon: <ArrowUpCircle size={20} />, label: t('addExpense'), color: 'bg-rose-500', path: '/expenses', state: { openAddModal: true, type: 'expense' } },
    { icon: <HandCoins size={20} />, label: t('addDebt'), color: 'bg-amber-500', path: '/debts', state: { openAddModal: true } },
    { icon: <ShoppingCart size={20} />, label: t('addMarket'), color: 'bg-blue-500', path: '/market-memo', state: { openAddModal: true } },
    { icon: <StickyNote size={20} />, label: t('addNote'), color: 'bg-purple-500', path: '/notes', state: { openAddModal: true } },
    { icon: <CalcIcon size={20} />, label: t('calculator'), color: 'bg-gray-600', action: () => setShowCalculator(true) },
  ];

  const handleAction = (action: any) => {
    setIsOpen(false);
    if (action.path) {
      navigate(action.path, { state: action.state });
    } else if (action.action) {
      action.action();
    }
  };

  return (
    <>
      {/* Overlay when open */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setIsOpen(false)}
            className="fixed inset-0 bg-black/20 backdrop-blur-[2px] z-40"
          />
        )}
      </AnimatePresence>

      <div className="fixed bottom-24 md:bottom-8 right-6 z-50 flex flex-col items-end">
        <AnimatePresence>
          {isOpen && (
            <div className="flex flex-col-reverse items-end mb-4 space-y-reverse space-y-4">
              {actions.map((action, index) => (
                <motion.button
                  key={action.label}
                  initial={{ opacity: 0, scale: 0.5, y: 20 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.5, y: 20 }}
                  transition={{ delay: index * 0.05 }}
                  onClick={() => handleAction(action)}
                  className="flex items-center group"
                >
                  <span className="mr-3 px-3 py-1.5 bg-white text-gray-800 text-[10px] font-black rounded-xl shadow-xl border border-gray-100 whitespace-nowrap uppercase tracking-wider">
                    {action.label}
                  </span>
                  <div className={`w-12 h-12 ${action.color} text-white rounded-2xl flex items-center justify-center shadow-2xl hover:scale-110 transition-transform active:scale-95`}>
                    {action.icon}
                  </div>
                </motion.button>
              ))}
            </div>
          )}
        </AnimatePresence>

        <button
          onClick={() => setIsOpen(!isOpen)}
          className={`w-14 h-14 rounded-2xl flex items-center justify-center shadow-2xl transition-all duration-300 ${
            isOpen ? 'bg-gray-900' : 'bg-indigo-600 hover:bg-indigo-700'
          } text-white active:scale-90`}
        >
          <motion.div
            animate={{ rotate: isOpen ? 45 : 0 }}
            transition={{ type: "spring", stiffness: 260, damping: 20 }}
          >
            <Plus size={32} />
          </motion.div>
        </button>
      </div>

      {/* Calculator Modal */}
      <AnimatePresence>
        {showCalculator && createPortal(
          <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9 }}
              className="bg-white rounded-2xl shadow-2xl w-full max-w-xs overflow-hidden"
            >
              <Calculator onClose={() => setShowCalculator(false)} />
            </motion.div>
          </div>,
          document.body
        )}
      </AnimatePresence>
    </>
  );
};

const Calculator: React.FC<{ onClose: () => void }> = ({ onClose }) => {
  const [display, setDisplay] = useState('0');
  const [equation, setEquation] = useState('');
  const [prevValue, setPrevValue] = useState<number | null>(null);
  const [operator, setOperator] = useState<string | null>(null);
  const [waitingForOperand, setWaitingForOperand] = useState(false);

  const handleNumber = (num: string) => {
    if (waitingForOperand) {
      setDisplay(num);
      setWaitingForOperand(false);
    } else {
      setDisplay(display === '0' ? num : display + num);
    }
  };

  const handleOperator = (nextOperator: string) => {
    const inputValue = parseFloat(display);

    if (prevValue === null) {
      setPrevValue(inputValue);
    } else if (operator) {
      const currentValue = prevValue || 0;
      const newValue = performCalculation[operator](currentValue, inputValue);
      setPrevValue(newValue);
      setDisplay(String(newValue));
    }

    setWaitingForOperand(true);
    setOperator(nextOperator);
    setEquation(`${prevValue === null ? inputValue : prevValue} ${nextOperator}`);
  };

  const performCalculation: Record<string, (a: number, b: number) => number> = {
    '/': (a, b) => a / b,
    '*': (a, b) => a * b,
    '+': (a, b) => a + b,
    '-': (a, b) => a - b,
  };

  const calculate = () => {
    const inputValue = parseFloat(display);

    if (operator && prevValue !== null) {
      const result = performCalculation[operator](prevValue, inputValue);
      setDisplay(String(result));
      setPrevValue(null);
      setOperator(null);
      setEquation('');
      setWaitingForOperand(true);
    }
  };

  const clear = () => {
    setDisplay('0');
    setEquation('');
    setPrevValue(null);
    setOperator(null);
    setWaitingForOperand(false);
  };

  const buttons = [
    { label: 'C', action: clear, color: 'text-red-500' },
    { label: '÷', action: () => handleOperator('/'), color: 'text-indigo-600' },
    { label: '×', action: () => handleOperator('*'), color: 'text-indigo-600' },
    { label: '⌫', action: () => setDisplay(display.length > 1 ? display.slice(0, -1) : '0'), color: 'text-gray-500' },
    { label: '7', action: () => handleNumber('7') },
    { label: '8', action: () => handleNumber('8') },
    { label: '9', action: () => handleNumber('9') },
    { label: '-', action: () => handleOperator('-'), color: 'text-indigo-600' },
    { label: '4', action: () => handleNumber('4') },
    { label: '5', action: () => handleNumber('5') },
    { label: '6', action: () => handleNumber('6') },
    { label: '+', action: () => handleOperator('+'), color: 'text-indigo-600' },
    { label: '1', action: () => handleNumber('1') },
    { label: '2', action: () => handleNumber('2') },
    { label: '3', action: () => handleNumber('3') },
    { label: '=', action: calculate, color: 'bg-indigo-600 text-white row-span-2' },
    { label: '0', action: () => handleNumber('0'), className: 'col-span-2' },
    { label: '.', action: () => handleNumber('.') },
  ];

  return (
    <div className="p-4 bg-gray-50">
      <div className="flex justify-between items-center mb-4">
        <h3 className="font-bold text-gray-700">Calculator</h3>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
          <X size={20} />
        </button>
      </div>
      
      <div className="bg-white p-4 rounded-xl mb-4 text-right shadow-inner border border-gray-100">
        <div className="text-xs text-gray-400 h-4 mb-1">{equation}</div>
        <div className="text-2xl font-bold text-gray-900 truncate">{display}</div>
      </div>

      <div className="grid grid-cols-4 gap-2">
        {buttons.map((btn, i) => (
          <button
            key={i}
            onClick={btn.action}
            className={`h-12 rounded-lg text-sm font-bold transition-all active:scale-95 ${
              btn.color || 'bg-white text-gray-700 hover:bg-gray-100 border border-gray-100'
            } ${btn.className || ''}`}
          >
            {btn.label}
          </button>
        ))}
      </div>
    </div>
  );
};
