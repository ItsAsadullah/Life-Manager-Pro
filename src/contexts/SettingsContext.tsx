import React, { createContext, useContext, useState, useEffect } from 'react';

type Currency = 'BDT' | 'INR' | 'USD';
type Language = 'bn' | 'en';

interface SettingsContextType {
  currency: Currency;
  currencySymbol: string;
  language: Language;
  setCurrency: (c: Currency) => void;
  setLanguage: (l: Language) => void;
  t: (key: string) => string;
}

const SettingsContext = createContext<SettingsContextType | undefined>(undefined);

const translations: Record<Language, Record<string, string>> = {
  en: {
    dashboard: 'Dashboard',
    notes: 'Notes',
    marketMemo: 'Market Memo',
    expenses: 'Expenses',
    debts: 'Debts',
    scanner: 'Voucher Scanner',
    chatbot: 'AI Chatbot',
    imageGen: 'Image Gen',
    settings: 'Settings',
    logout: 'Logout',
    more: 'More',
    addIncome: 'Add Income',
    addExpense: 'Add Expense',
    addDebt: 'Add Debt',
    addMarket: 'Market List',
    addNote: 'Notes',
    calculator: 'Calculator',
    language: 'Language',
    currency: 'Currency',
    backup: 'Data Backup',
    import: 'Import Data',
    export: 'Export Data',
    save: 'Save',
    cancel: 'Cancel',
    delete: 'Delete',
    edit: 'Edit',
    totalBalance: 'Total Balance',
    monthlyExpense: 'Monthly Expense',
    monthlyIncome: 'Monthly Income',
    recentTransactions: 'Recent Transactions',
    noTransactions: 'No transactions found',
    search: 'Search...',
    all: 'All',
    personal: 'Personal',
    work: 'Work',
    shopping: 'Shopping',
    health: 'Health',
    education: 'Education',
    other: 'Other',
    confirmDelete: 'Are you sure you want to delete this?',
    backupSuccess: 'Backup successful!',
    importSuccess: 'Data imported successfully!',
    importError: 'Error importing data. Please check the file format.',
  },
  bn: {
    dashboard: 'ড্যাশবোর্ড',
    notes: 'নোটস',
    marketMemo: 'বাজার মেমো',
    expenses: 'খরচ',
    debts: 'ধার/লোন',
    scanner: 'ভাউচার স্ক্যানার',
    chatbot: 'এআই চ্যাটবট',
    imageGen: 'ইমেজ জেন',
    settings: 'সেটিংস',
    logout: 'লগআউট',
    more: 'আরও',
    addIncome: 'আয় যুক্ত করুন',
    addExpense: 'খরচ যুক্ত করুন',
    addDebt: 'ধার যুক্ত করুন',
    addMarket: 'বাজার লিস্ট',
    addNote: 'নোটস',
    calculator: 'ক্যালকুলেটর',
    language: 'ভাষা',
    currency: 'কারেন্সি',
    backup: 'ডেটা ব্যাকআপ',
    import: 'ডেটা ইম্পোর্ট',
    export: 'ডেটা এক্সপোর্ট',
    save: 'সংরক্ষণ',
    cancel: 'বাতিল',
    delete: 'মুছে ফেলুন',
    edit: 'সম্পাদনা',
    totalBalance: 'মোট ব্যালেন্স',
    monthlyExpense: 'মাসিক খরচ',
    monthlyIncome: 'মাসিক আয়',
    recentTransactions: 'সাম্প্রতিক লেনদেন',
    noTransactions: 'কোন লেনদেন পাওয়া যায়নি',
    search: 'খুঁজুন...',
    all: 'সব',
    personal: 'ব্যক্তিগত',
    work: 'কাজ',
    shopping: 'কেনাকাটা',
    health: 'স্বাস্থ্য',
    education: 'শিক্ষা',
    other: 'অন্যান্য',
    confirmDelete: 'আপনি কি নিশ্চিত যে এটি মুছে ফেলতে চান?',
    backupSuccess: 'ব্যাকআপ সফল হয়েছে!',
    importSuccess: 'ডেটা সফলভাবে ইম্পোর্ট করা হয়েছে!',
    importError: 'ডেটা ইম্পোর্ট করতে সমস্যা হয়েছে। অনুগ্রহ করে ফাইলের ফরম্যাট চেক করুন।',
  }
};

const currencySymbols: Record<Currency, string> = {
  BDT: '৳',
  INR: '₹',
  USD: '$'
};

export const SettingsProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currency, setCurrencyState] = useState<Currency>(() => (localStorage.getItem('currency') as Currency) || 'BDT');
  const [language, setLanguageState] = useState<Language>(() => (localStorage.getItem('language') as Language) || 'bn');

  useEffect(() => {
    localStorage.setItem('currency', currency);
  }, [currency]);

  useEffect(() => {
    localStorage.setItem('language', language);
  }, [language]);

  const setCurrency = (c: Currency) => setCurrencyState(c);
  const setLanguage = (l: Language) => setLanguageState(l);

  const t = (key: string) => {
    return translations[language][key] || key;
  };

  const currencySymbol = currencySymbols[currency];

  return (
    <SettingsContext.Provider value={{ currency, currencySymbol, language, setCurrency, setLanguage, t }}>
      {children}
    </SettingsContext.Provider>
  );
};

export const useSettings = () => {
  const context = useContext(SettingsContext);
  if (context === undefined) {
    throw new Error('useSettings must be used within a SettingsProvider');
  }
  return context;
};
