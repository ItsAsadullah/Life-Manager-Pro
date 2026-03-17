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
    loginSubtitle: 'Your All-in-one Personal Life Management System',
    aiTools: 'AI Tools',
    mobileReady: 'Mobile Ready',
    continueWithGoogle: 'Continue with Google',
    authenticating: 'Authenticating...',
    secureAuth: 'Secure Authentication via Firebase',
    developedBy: 'Developed by',
    allNotes: 'All Notes',
    archived: 'Archived',
    addMemo: 'New Memo',
    unitPrice: 'Unit Price',
    totalAmount: 'Total Amount',
    askSomething: 'Ask something...',
    borrowed: 'Borrowed',
    lent: 'Lent',
    welcomeBack: 'Welcome back',
    expenseAnalysis: 'Expense Analysis',
    noExpenseData: 'No expense data available yet.',
    latestNotes: 'Latest Notes',
    noNotesFound: 'No notes found.',
    loading: 'Loading...',
    confirmDeleteNote: 'Are you sure you want to delete this note?',
    noteTitle: 'Note Title',
    category: 'Category',
    idea: 'Idea',
    reminder: 'Reminder',
    notePlaceholder: 'Write your note here or use voice typing...',
    stopRecording: 'Stop recording',
    startVoiceTyping: 'Start voice typing',
    listItem: 'List item...',
    addItem: 'Add item',
    updateNote: 'Update Note',
    saveNote: 'Save Note',
    pinned: 'Pinned',
    others: 'Others',
    createFirstNote: 'Create your first note to stay organized!',
    moreItems: 'more items',
    secureFinancialManager: 'Secure Financial Manager',
    standard: "Standard",
    highQuality: "High Quality",
    ultraHD: "Ultra HD",
    generating: "Generating...",
    generateImage: "Generate Image",
    result: "Result",
    download: "Download",
    imageGenFailed: "Failed to generate image. Please try again.",
    paidAmount: "Paid",
    remainingAmount: "Remaining",
    voice: "VOICE",
    textNote: "Text Note",
    changeColor: "Change Color",
    pinNote: "Pin Note",
    kg: 'kg',
    g: 'g',
    liter: 'liter',
    ml: 'ml',
    pcs: 'pcs',
    dozen: 'dozen',
    packet: 'packet',
    food: 'Food',
    transport: 'Transport',
    bills: 'Bills',
    entertainment: 'Entertainment',
    salary: 'Salary',
    freelance: 'Freelance',
    business: 'Business',
    gift: 'Gift',
    investment: 'Investment',
    cash: 'Cash',
    bkash: 'bKash',
    nagad: 'Nagad',
    card: 'Card',
    bankTransfer: 'Bank Transfer',
    manageIncomeExpenses: 'Manage your income and expenses',
    addNew: 'Add New',
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
    loginSubtitle: 'আপনার অল-ইন-ওয়ান পার্সোনাল লাইফ ম্যানেজমেন্ট সিস্টেম',
    aiTools: 'এআই টুলস',
    mobileReady: 'মোবাইল রেডি',
    continueWithGoogle: 'গুগল দিয়ে এগিয়ে যান',
    authenticating: 'যাচাই করা হচ্ছে...',
    secureAuth: 'ফায়ারবেসের মাধ্যমে নিরাপদ প্রমাণীকরণ',
    developedBy: 'ডেভেলপ করেছেন',
    allNotes: 'সব নোটস',
    archived: 'আর্কাইভ করা',
    addMemo: 'নতুন মেমো',
    unitPrice: 'একক মূল্য',
    totalAmount: 'মোট পরিমাণ',
    askSomething: 'কিছু জিজ্ঞাসা করুন...',
    borrowed: 'ধার নেওয়া',
    lent: 'ধার দেওয়া',
    welcomeBack: 'স্বাগতম',
    expenseAnalysis: 'ব্যয় বিশ্লেষণ',
    noExpenseData: 'এখনো কোনো ব্যয়ের তথ্য নেই।',
    latestNotes: 'সাম্প্রতিক নোটস',
    noNotesFound: 'কোনো নোট পাওয়া যায়নি।',
    loading: 'লোড হচ্ছে...',
    confirmDeleteNote: 'আপনি কি নিশ্চিত যে আপনি এই নোটটি মুছে ফেলতে চান?',
    noteTitle: 'নোটের শিরোনাম',
    category: 'বিভাগ',
    idea: 'আইডিয়া',
    reminder: 'অনুস্মারক',
    notePlaceholder: 'আপনার নোট এখানে লিখুন বা ভয়েস টাইপিং ব্যবহার করুন...',
    stopRecording: 'রেকর্ডিং বন্ধ করুন',
    startVoiceTyping: 'ভয়েস টাইপিং শুরু করুন',
    listItem: 'তালিকার আইটেম...',
    addItem: 'আইটেম যোগ করুন',
    updateNote: 'নোট আপডেট করুন',
    saveNote: 'নোট সংরক্ষণ করুন',
    pinned: 'পিন করা',
    others: 'অন্যান্য',
    createFirstNote: 'সংগঠিত থাকতে আপনার প্রথম নোট তৈরি করুন!',
    moreItems: 'আরও আইটেম',
    secureFinancialManager: 'নিরাপদ আর্থিক ব্যবস্থাপক',
    standard: "সাধারণ (Standard)",
    highQuality: "উচ্চ মানের (High Quality)",
    ultraHD: "আল্ট্রা এইচডি (Ultra HD)",
    generating: "তৈরি করা হচ্ছে...",
    generateImage: "ছবি তৈরি করুন",
    result: "ফলাফল",
    download: "ডাউনলোড",
    imageGenFailed: "ছবি তৈরি করতে ব্যর্থ হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।",
    paidAmount: "পরিশোধিত",
    remainingAmount: "বাকি",
    voice: "ভয়েস",
    textNote: "টেক্সট নোট",
    changeColor: "রঙ পরিবর্তন করুন",
    pinNote: "পিন করুন",
    kg: 'কেজি',
    g: 'গ্রাম',
    liter: 'লিটার',
    ml: 'মিলি',
    pcs: 'টি',
    dozen: 'ডজন',
    packet: 'প্যাকেট',
    food: 'খাবার',
    transport: 'পরিবহন',
    bills: 'বিল',
    entertainment: 'বিনোদন',
    salary: 'বেতন',
    freelance: 'ফ্রিল্যান্স',
    business: 'ব্যবসা',
    gift: 'উপহার',
    investment: 'বিনিয়োগ',
    cash: 'নগদ',
    bkash: 'বিকাশ',
    nagad: 'নগদ (Nagad)',
    card: 'কার্ড',
    bankTransfer: 'ব্যাংক ট্রান্সফার',
    manageIncomeExpenses: 'আপনার আয় এবং ব্যয় পরিচালনা করুন',
    addNew: 'নতুন যোগ করুন',
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
