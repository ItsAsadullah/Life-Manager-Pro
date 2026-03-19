import React, { createContext, useContext, useState, useEffect } from 'react';
import { doc, updateDoc } from 'firebase/firestore';
import { auth, db } from '../lib/firebase';
import { registerUserPushToken } from '../lib/pushNotifications';

type Currency = 'BDT' | 'INR' | 'USD';
type Language = 'bn' | 'en';
type Theme = 'light' | 'dark';

type NotificationReminder = {
  id: string;
  time: string;
  message: string;
  enabled: boolean;
};

interface SettingsContextType {
  currency: Currency;
  currencySymbol: string;
  language: Language;
  theme: Theme;
  notificationsEnabled: boolean;
  notificationPermission: NotificationPermission | 'unsupported';
  notificationTimes: string[];
  notificationReminders: NotificationReminder[];
  setCurrency: (c: Currency) => void;
  setLanguage: (l: Language) => void;
  toggleTheme: () => void;
  setNotificationsEnabled: (enabled: boolean) => void;
  setNotificationTimes: (times: string[]) => void;
  addNotificationReminder: (time: string, message: string) => void;
  updateNotificationReminder: (id: string, data: Partial<Omit<NotificationReminder, 'id'>>) => void;
  removeNotificationReminder: (id: string) => void;
  requestNotificationPermission: () => Promise<NotificationPermission | 'unsupported'>;
  sendTestNotification: () => void;
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
    theme: 'Theme',
    lightMode: 'Light Mode',
    darkMode: 'Dark Mode',
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
    expensePlaceholder: 'e.g. Grocery shopping',
    incomePlaceholder: 'e.g. Monthly salary',
    trackMoneyOwed: 'Track money you owe or are owed',
    addRecord: 'Add Record',
    editRecord: 'Edit Record',
    addNewRecord: 'Add New Record',
    personName: 'Person Name',
    enterName: 'Enter name',
    phoneNumberOptional: 'Phone Number (Optional)',
    enterPhoneNumber: 'Enter phone number',
    dueDateOptional: 'Due Date (Optional)',
    updateRecord: 'Update Record',
    saveRecord: 'Save Record',
    person: 'Person',
    type: 'Type',
    dueDate: 'Due Date',
    status: 'Status',
    actions: 'Actions',
    noDate: 'No Date',
    paid: 'Paid',
    pending: 'Pending',
    repaymentHistory: 'Repayment History',
    addPayment: 'Add Payment',
    optionalNote: 'Optional note',
    add: 'Add',
    noPaymentHistory: 'No payment history yet',
    noPhone: 'No Phone',
    remaining: 'Remaining',
    noDueDate: 'No Due Date',
    repay: 'Repay',
    receive: 'Receive',
    history: 'History',
    noteOptional: 'Note (Optional)',
    noDebtRecords: 'No debt records found',
    startAddingDebt: 'Start by adding your first debt record!',
    categorySource: 'Category/Source',
    marketMemoTitle: 'Market Memo',
    createdWith: 'Created with Hisab Nikash App',
    imageDownloaded: 'Image downloaded successfully',
    pdfDownloaded: 'PDF downloaded successfully',
    itemNameEmpty: 'Item name cannot be empty',
    qtyPositive: 'Quantity must be a positive number',
    unitPricePositive: 'Unit price must be a non-negative number',
    memoUpdated: 'Memo updated successfully',
    memoSaved: 'Memo saved successfully',
    convertedToExpense: 'Converted to expense successfully',
    createBazaarList: 'Create and manage your bazaar list',
    editMarketMemo: 'Edit Market Memo',
    createMarketMemo: 'Create Market Memo',
    memoTitle: 'Memo Title',
    memoPlaceholder: 'e.g. Weekly Grocery',
    addNewItem: 'Add New Item',
    itemName: 'Item Name',
    qty: 'Qty',
    purchased: 'Purchased',
    rate: 'Rate',
    totalItems: 'Total Items',
    shareMemo: 'Share Memo',
    editMemo: 'Edit Memo',
    deleteMemo: 'Delete Memo',
    addedToExpenses: 'Added to Expenses',
    linkCopied: 'Link copied to clipboard',
    addTransaction: 'Add Transaction',
    backToApp: 'Back to App',
    image: 'Image',
    pdf: 'PDF',
    item: 'Item',
    price: 'Price',
    grandTotal: 'Grand Total',
    total: 'Total',
    copiedToClipboard: 'Copied to clipboard',
    note: 'Note',
    convertToExpense: 'Convert to Expense',
    text: 'Text',
    link: 'Link',
    success: 'Success!',
    deleteItem: 'Delete Item',
    deleteItemConfirm: 'Are you sure you want to delete this item? This action cannot be undone.',
    deleteMemoConfirm: 'Are you sure you want to delete this entire memo? This action cannot be undone.',
    noMemosFound: 'No market memos found. Create one for your next shopping trip!',
    managePreferences: 'Manage your app preferences',
    exportDescription: 'Download all your app data as a backup file',
    importDescription: 'Restore data from a previously exported backup',
    exportFailed: 'Export failed. Please try again.',
    notifications: 'Notifications',
    notificationSchedule: 'Set when reminders should arrive',
    enableNotifications: 'Enable notifications',
    notificationTimes: 'Notification times',
    addTime: 'Add time',
    testNotification: 'Send test notification',
    permissionGranted: 'Notification permission granted',
    permissionDenied: 'Notification permission denied in browser settings',
    permissionDefault: 'Allow notification permission to receive reminders',
    notificationsUnsupported: 'This browser does not support notifications',
    notificationTitle: 'Hisab Nikash Reminder',
    notificationBody: 'Open the app and update your daily notes and expenses.',
    customReminder: 'Custom reminder',
    reminderMessage: 'Reminder message',
    reminderMessagePlaceholder: 'Write reminder message',
    saveReminder: 'Save reminder',
    remindersSaved: 'Reminder saved',
    noRemindersYet: 'No reminders yet',
    reminderDeleted: 'Reminder deleted',
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
    theme: 'থিম',
    lightMode: 'লাইট মোড',
    darkMode: 'ডার্ক মোড',
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
    expensePlaceholder: 'যেমন: বাজার খরচ',
    incomePlaceholder: 'যেমন: মাসিক বেতন',
    trackMoneyOwed: 'আপনার দেনা-পাওনার হিসাব রাখুন',
    addRecord: 'রেকর্ড যুক্ত করুন',
    editRecord: 'রেকর্ড সম্পাদনা',
    addNewRecord: 'নতুন রেকর্ড যুক্ত করুন',
    personName: 'ব্যক্তির নাম',
    enterName: 'নাম লিখুন',
    phoneNumberOptional: 'ফোন নম্বর (ঐচ্ছিক)',
    enterPhoneNumber: 'ফোন নম্বর লিখুন',
    dueDateOptional: 'পরিশোধের তারিখ (ঐচ্ছিক)',
    updateRecord: 'রেকর্ড আপডেট করুন',
    saveRecord: 'রেকর্ড সংরক্ষণ করুন',
    person: 'ব্যক্তি',
    type: 'ধরণ',
    dueDate: 'তারিখ',
    status: 'অবস্থা',
    actions: 'অ্যাকশন',
    noDate: 'তারিখ নেই',
    paid: 'পরিশোধিত',
    pending: 'বাকি',
    repaymentHistory: 'পরিশোধের ইতিহাস',
    addPayment: 'পেমেন্ট যুক্ত করুন',
    optionalNote: 'ঐচ্ছিক নোট',
    add: 'যোগ করুন',
    noPaymentHistory: 'এখনো কোনো পরিশোধের ইতিহাস নেই',
    noPhone: 'ফোন নম্বর নেই',
    remaining: 'অবশিষ্ট',
    noDueDate: 'তারিখ নেই',
    repay: 'পরিশোধ',
    receive: 'গ্রহণ',
    history: 'ইতিহাস',
    noteOptional: 'নোট (ঐচ্ছিক)',
    noDebtRecords: 'কোনো দেনা-পাওনার রেকর্ড পাওয়া যায়নি',
    startAddingDebt: 'আপনার প্রথম দেনা-পাওনার রেকর্ড যুক্ত করে শুরু করুন!',
    categorySource: 'বিভাগ/উৎস',
    marketMemoTitle: 'বাজার মেমো',
    createdWith: 'হিসাব নিকাশ অ্যাপ দিয়ে তৈরি',
    imageDownloaded: 'ছবি সফলভাবে ডাউনলোড হয়েছে',
    pdfDownloaded: 'পিডিএফ সফলভাবে ডাউনলোড হয়েছে',
    itemNameEmpty: 'আইটেমের নাম খালি হতে পারবে না',
    qtyPositive: 'পরিমাণ অবশ্যই ধনাত্মক সংখ্যা হতে হবে',
    unitPricePositive: 'একক মূল্য অবশ্যই অ-ঋণাত্মক সংখ্যা হতে হবে',
    memoUpdated: 'মেমো সফলভাবে আপডেট হয়েছে',
    memoSaved: 'মেমো সফলভাবে সংরক্ষিত হয়েছে',
    convertedToExpense: 'সফলভাবে খরচে রূপান্তরিত হয়েছে',
    createBazaarList: 'আপনার বাজারের তালিকা তৈরি এবং পরিচালনা করুন',
    editMarketMemo: 'বাজার মেমো সম্পাদনা',
    createMarketMemo: 'বাজার মেমো তৈরি করুন',
    memoTitle: 'মেমোর শিরোনাম',
    memoPlaceholder: 'যেমন: সাপ্তাহিক বাজার',
    addNewItem: 'নতুন আইটেম যোগ করুন',
    itemName: 'আইটেমের নাম',
    qty: 'পরিমাণ',
    purchased: 'কেনা হয়েছে',
    rate: 'দর',
    totalItems: 'মোট আইটেম',
    shareMemo: 'মেমো শেয়ার করুন',
    editMemo: 'মেমো সম্পাদনা',
    deleteMemo: 'মেমো মুছে ফেলুন',
    addedToExpenses: 'খরচে যোগ করা হয়েছে',
    linkCopied: 'লিঙ্ক ক্লিপবোর্ডে কপি করা হয়েছে',
    addTransaction: 'লেনদেন যুক্ত করুন',
    backToApp: 'অ্যাপে ফিরে যান',
    image: 'ছবি',
    pdf: 'পিডিএফ',
    item: 'আইটেম',
    price: 'মূল্য',
    grandTotal: 'সর্বমোট',
    total: 'মোট',
    copiedToClipboard: 'ক্লিপবোর্ডে কপি করা হয়েছে',
    note: 'নোট',
    convertToExpense: 'খরচে রূপান্তর করুন',
    text: 'টেক্সট',
    link: 'লিঙ্ক',
    success: 'সফল!',
    deleteItem: 'আইটেম মুছুন',
    deleteItemConfirm: 'আপনি কি নিশ্চিত যে আপনি এই আইটেমটি মুছতে চান? এই কাজটি আর ফিরে পাওয়া যাবে না।',
    deleteMemoConfirm: 'আপনি কি নিশ্চিত যে আপনি এই পুরো মেমোটি মুছতে চান? এই কাজটি আর ফিরে পাওয়া যাবে না।',
    noMemosFound: 'কোন মার্কেট মেমো পাওয়া যায়নি। আপনার পরবর্তী কেনাকাটার জন্য একটি তৈরি করুন!',
    managePreferences: 'আপনার অ্যাপ সেটিংস পরিচালনা করুন',
    exportDescription: 'ব্যাকআপ হিসেবে সব ডেটা ডাউনলোড করুন',
    importDescription: 'আগের ব্যাকআপ ফাইল থেকে ডেটা পুনরুদ্ধার করুন',
    exportFailed: 'এক্সপোর্ট ব্যর্থ হয়েছে। আবার চেষ্টা করুন।',
    notifications: 'নোটিফিকেশন',
    notificationSchedule: 'কখন নোটিফিকেশন আসবে সেট করুন',
    enableNotifications: 'নোটিফিকেশন চালু করুন',
    notificationTimes: 'নোটিফিকেশনের সময়',
    addTime: 'সময় যোগ করুন',
    testNotification: 'টেস্ট নোটিফিকেশন পাঠান',
    permissionGranted: 'নোটিফিকেশনের অনুমতি দেওয়া হয়েছে',
    permissionDenied: 'ব্রাউজার সেটিংসে নোটিফিকেশন অনুমতি বন্ধ আছে',
    permissionDefault: 'রিমাইন্ডার পেতে নোটিফিকেশন অনুমতি দিন',
    notificationsUnsupported: 'এই ব্রাউজারে নোটিফিকেশন সাপোর্ট নেই',
    notificationTitle: 'হিসাব নিকাশ রিমাইন্ডার',
    notificationBody: 'অ্যাপ খুলে আজকের নোটস ও খরচ আপডেট করুন।',
    customReminder: 'কাস্টম রিমাইন্ডার',
    reminderMessage: 'রিমাইন্ডার বার্তা',
    reminderMessagePlaceholder: 'রিমাইন্ডার বার্তা লিখুন',
    saveReminder: 'রিমাইন্ডার সংরক্ষণ করুন',
    remindersSaved: 'রিমাইন্ডার সংরক্ষণ হয়েছে',
    noRemindersYet: 'এখনো কোনো রিমাইন্ডার নেই',
    reminderDeleted: 'রিমাইন্ডার মুছে ফেলা হয়েছে',
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
  const [notificationsEnabled, setNotificationsEnabledState] = useState<boolean>(() => localStorage.getItem('notificationsEnabled') === 'true');
  const [notificationReminders, setNotificationRemindersState] = useState<NotificationReminder[]>(() => {
    const savedReminders = localStorage.getItem('notificationReminders');
    if (savedReminders) {
      try {
        const parsed = JSON.parse(savedReminders);
        if (Array.isArray(parsed)) {
          const valid = parsed.filter((item: any) => (
            item &&
            typeof item.id === 'string' &&
            typeof item.time === 'string' &&
            typeof item.message === 'string' &&
            typeof item.enabled === 'boolean'
          ));
          if (valid.length > 0) return valid;
        }
      } catch {
        // ignore parse errors and fallback
      }
    }

    const savedTimes = localStorage.getItem('notificationTimes');
    if (savedTimes) {
      try {
        const parsedTimes = JSON.parse(savedTimes);
        if (Array.isArray(parsedTimes)) {
          const validTimes = parsedTimes.filter((item: unknown): item is string => typeof item === 'string');
          if (validTimes.length > 0) {
            return validTimes.map((time, index) => ({
              id: `legacy-${index}-${time}`,
              time,
              message: 'Open the app and update your daily notes and expenses.',
              enabled: true,
            }));
          }
        }
      } catch {
        // ignore parse errors and fallback
      }
    }

    return [{
      id: 'default-09-00',
      time: '09:00',
      message: 'Open the app and update your daily notes and expenses.',
      enabled: true,
    }];
  });

  const notificationTimes = notificationReminders.filter((item) => item.enabled).map((item) => item.time);
  const [notificationPermission, setNotificationPermission] = useState<NotificationPermission | 'unsupported'>(() => {
    if (typeof window === 'undefined' || !('Notification' in window)) return 'unsupported';
    return Notification.permission;
  });
  const [theme, setThemeState] = useState<Theme>(() => {
    document.documentElement.classList.remove('dark');
    const savedTheme = (localStorage.getItem('theme') as Theme) || 'light';
    console.log('Initializing theme:', savedTheme);
    if (savedTheme === 'dark') {
      document.documentElement.classList.add('dark');
    }
    return savedTheme;
  });

  useEffect(() => {
    localStorage.setItem('currency', currency);
  }, [currency]);

  useEffect(() => {
    localStorage.setItem('language', language);
  }, [language]);

  useEffect(() => {
    localStorage.setItem('notificationsEnabled', String(notificationsEnabled));
  }, [notificationsEnabled]);

  useEffect(() => {
    localStorage.setItem('notificationReminders', JSON.stringify(notificationReminders));
    localStorage.setItem('notificationTimes', JSON.stringify(notificationTimes));
    
    // Sync reminders to Firestore to allow backend scheduler to read them
    if (auth.currentUser) {
      updateDoc(doc(db, 'users', auth.currentUser.uid), {
        reminders: notificationReminders
      }).catch(err => console.error('Failed to sync reminders:', err));
    }
  }, [notificationReminders, notificationTimes]);

  useEffect(() => {
    // Ensure light mode on mount if theme is light
    if (theme === 'light') {
      document.documentElement.classList.remove('dark');
    }
  }, []);

  const setCurrency = (c: Currency) => setCurrencyState(c);
  const setLanguage = (l: Language) => setLanguageState(l);
  const setNotificationsEnabled = (enabled: boolean) => setNotificationsEnabledState(enabled);
  const setNotificationTimes = (times: string[]) => {
    const uniqueSorted = Array.from(new Set(times.filter(Boolean))).sort();
    setNotificationRemindersState((prev) => {
      const defaultMessage = translations[language].notificationBody;
      const nextReminders = uniqueSorted.length ? uniqueSorted : ['09:00'];
      return nextReminders.map((time, index) => ({
        id: prev[index]?.id || `time-${Date.now()}-${index}`,
        time,
        message: prev[index]?.message || defaultMessage,
        enabled: prev[index]?.enabled ?? true,
      }));
    });
  };

  const addNotificationReminder = (time: string, message: string) => {
    const defaultMessage = translations[language].notificationBody;
    setNotificationRemindersState((prev) => ([
      ...prev,
      {
        id: `reminder-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        time,
        message: message.trim() || defaultMessage,
        enabled: true,
      },
    ]));
  };

  const updateNotificationReminder = (id: string, data: Partial<Omit<NotificationReminder, 'id'>>) => {
    setNotificationRemindersState((prev) => prev.map((item) => (
      item.id === id
        ? {
            ...item,
            ...data,
            message: typeof data.message === 'string' && data.message.trim().length === 0
              ? item.message
              : (data.message ?? item.message),
          }
        : item
    )));
  };

  const removeNotificationReminder = (id: string) => {
    setNotificationRemindersState((prev) => {
      const next = prev.filter((item) => item.id !== id);
      if (next.length > 0) return next;
      return [{
        id: `fallback-${Date.now()}`,
        time: '09:00',
        message: translations[language].notificationBody,
        enabled: true,
      }];
    });
  };

  const requestNotificationPermission = async (): Promise<NotificationPermission | 'unsupported'> => {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      setNotificationPermission('unsupported');
      return 'unsupported';
    }
    const permission = await Notification.requestPermission();
    setNotificationPermission(permission);
    if (permission === 'granted' && auth.currentUser) {
      registerUserPushToken(auth.currentUser.uid).catch((error) => {
        console.error('Push token registration failed:', error);
      });
    }
    if (permission !== 'granted') {
      setNotificationsEnabledState(false);
    }
    return permission;
  };

  const sendTestNotification = () => {
    if (typeof window === 'undefined' || !('Notification' in window)) return;
    if (notificationPermission !== 'granted') return;
    new Notification(translations[language].notificationTitle, {
      body: translations[language].notificationBody,
      icon: '/favicon.svg',
      badge: '/favicon.svg',
      tag: 'hisab-nikash-reminder-test',
    });
  };

  const toggleTheme = () => {
    console.log('toggleTheme called');
    setThemeState(prev => {
      const next = prev === 'light' ? 'dark' : 'light';
      if (next === 'dark') {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
      localStorage.setItem('theme', next);
      return next;
    });
  };

  const t = (key: string) => {
    return translations[language][key] || key;
  };

  useEffect(() => {
    if (typeof window === 'undefined' || !('Notification' in window)) return;

    const timers: number[] = [];
    const activeReminders = notificationReminders.filter((item) => item.enabled);

    if (!notificationsEnabled || notificationPermission !== 'granted' || activeReminders.length === 0) {
      return () => {
        timers.forEach((timer) => window.clearTimeout(timer));
      };
    }

    const showReminder = (message: string) => {
      new Notification(translations[language].notificationTitle, {
        body: message,
        icon: '/favicon.svg',
        badge: '/favicon.svg',
        tag: 'hisab-nikash-reminder',
      });
    };

    const scheduleDaily = (time: string, message: string) => {
      const [hourText, minuteText] = time.split(':');
      const hour = Number(hourText);
      const minute = Number(minuteText);
      if (Number.isNaN(hour) || Number.isNaN(minute)) return;

      const now = new Date();
      const next = new Date();
      next.setHours(hour, minute, 0, 0);
      if (next.getTime() <= now.getTime()) {
        next.setDate(next.getDate() + 1);
      }

      const firstDelay = next.getTime() - now.getTime();
      const timeoutId = window.setTimeout(() => {
        showReminder(message);
        const intervalId = window.setInterval(() => showReminder(message), 24 * 60 * 60 * 1000);
        timers.push(intervalId);
      }, firstDelay);
      timers.push(timeoutId);
    };

    activeReminders.forEach((item) => scheduleDaily(item.time, item.message));

    return () => {
      timers.forEach((timer) => {
        window.clearTimeout(timer);
        window.clearInterval(timer);
      });
    };
  }, [notificationsEnabled, notificationPermission, notificationReminders, language]);

  const currencySymbol = currencySymbols[currency];

  return (
    <SettingsContext.Provider value={{
      currency,
      currencySymbol,
      language,
      theme,
      notificationsEnabled,
      notificationPermission,
      notificationTimes,
      notificationReminders,
      setCurrency,
      setLanguage,
      toggleTheme,
      setNotificationsEnabled,
      setNotificationTimes,
      addNotificationReminder,
      updateNotificationReminder,
      removeNotificationReminder,
      requestNotificationPermission,
      sendTestNotification,
      t,
    }}>
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
