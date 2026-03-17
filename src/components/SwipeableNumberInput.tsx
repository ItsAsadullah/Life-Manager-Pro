import React, { useState } from 'react';
import { playTick } from '../utils/audio';

interface SwipeableNumberInputProps {
  value: string | number;
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
  required?: boolean;
  isPrice?: boolean;
}

const PRICE_SNAPS = [
  0, 5, 10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 90, 100, 
  120, 150, 200, 250, 300, 350, 400, 450, 500, 600, 700, 800, 900, 1000, 
  1200, 1500, 2000, 2500, 3000, 4000, 5000, 10000
];

export const SwipeableNumberInput: React.FC<SwipeableNumberInputProps> = ({ value, onChange, placeholder, className, required, isPrice }) => {
  const [startY, setStartY] = useState<number | null>(null);
  const [startValue, setStartValue] = useState<number>(0);
  const [startIndex, setStartIndex] = useState<number>(0);
  const [lastTickValue, setLastTickValue] = useState<number>(0);

  const handleTouchStart = (e: React.TouchEvent) => {
    setStartY(e.touches[0].clientY);
    const initialVal = Number(value) || 0;
    setStartValue(initialVal);
    setLastTickValue(initialVal);

    if (isPrice) {
      let closestIdx = 0;
      let minDiff = Infinity;
      PRICE_SNAPS.forEach((snap, idx) => {
        if (Math.abs(snap - initialVal) < minDiff) {
          minDiff = Math.abs(snap - initialVal);
          closestIdx = idx;
        }
      });
      setStartIndex(closestIdx);
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (startY === null) return;
    const currentY = e.touches[0].clientY;
    const diff = startY - currentY; // positive if swiping up

    let newValue = 0;

    if (isPrice) {
      const step = Math.floor(diff / 8); // High sensitivity
      const newIdx = Math.max(0, Math.min(PRICE_SNAPS.length - 1, startIndex + step));
      newValue = PRICE_SNAPS[newIdx];
    } else {
      const step = Math.floor(diff / 15);
      newValue = Math.max(0, startValue + step);
    }

    if (newValue !== lastTickValue) {
       onChange(String(newValue));
       setLastTickValue(newValue);
       
       // Haptic feedback
       if (navigator.vibrate) {
         navigator.vibrate(10);
       }
       // Sound
       playTick();
    }
  };

  const handleTouchEnd = () => {
    setStartY(null);
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let val = e.target.value;
    const b2e: Record<string, string> = {'০': '0', '১': '1', '২': '2', '৩': '3', '৪': '4', '৫': '5', '৬': '6', '৭': '7', '৮': '8', '৯': '9'};
    val = val.replace(/[০-৯]/g, m => b2e[m]);
    val = val.replace(/[^0-9.]/g, '');
    const parts = val.split('.');
    if (parts.length > 2) val = parts[0] + '.' + parts.slice(1).join('');
    onChange(val);
  };

  return (
    <input
      type="text"
      inputMode="decimal"
      value={value}
      onChange={handleChange}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      placeholder={placeholder}
      className={className}
      required={required}
      style={{ touchAction: 'none' }}
    />
  );
};
