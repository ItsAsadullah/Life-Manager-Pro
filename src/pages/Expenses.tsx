import React, { useEffect, useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useAuth } from '../contexts/AuthContext';
import { useSettings } from '../contexts/SettingsContext';
import { useLocation } from 'react-router-dom';
import { collection, query, onSnapshot, orderBy, addDoc, deleteDoc, doc, setDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Plus, Save, X, Receipt, ArrowUpCircle, ArrowDownCircle, DollarSign, Trash2, Search, Calendar, Filter, MoreVertical, TrendingUp, TrendingDown, CheckSquare, LayoutGrid, List, Edit2, ArrowLeft, CalendarDays, ChevronLeft, ChevronRight } from 'lucide-react';
import { format, subMonths, addMonths, startOfMonth, endOfMonth, getDaysInMonth, isFuture, isToday } from 'date-fns';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip } from 'recharts';
import { SwipeableNumberInput } from '../components/SwipeableNumberInput';

export const Expenses: React.FC = () => {
  const { user } = useAuth();
  const { t, currencySymbol } = useSettings();
  const location = useLocation();
  const [transactions, setTransactions] = useState<any[]>([]);
  const [isAdding, setIsAdding] = useState(false);
  const [activeTab, setActiveTab] = useState<'expense' | 'income'>('expense');
  const [transactionToDelete, setTransactionToDelete] = useState<{id: string, type: string} | null>(null);

  // New state variables for filters and UI toggles
  const [viewMode, setViewMode] = useState<'month' | 'total'>('month');
  const [typeFilter, setTypeFilter] = useState<'all' | 'income' | 'expense'>('all');
  const [searchQuery, setSearchQuery] = useState('');
  
  // Toggles
  const [showSearchBox, setShowSearchBox] = useState(true);
  const [showMonthCompare, setShowMonthCompare] = useState(true);
  const [showMoreMenu, setShowMoreMenu] = useState(false);
  const [showFilterMenu, setShowFilterMenu] = useState(false);
  const [showCategoryMenu, setShowCategoryMenu] = useState(false);
  const [sortOrder, setSortOrder] = useState<'newest' | 'oldest' | 'highest' | 'lowest'>('newest');
  const [currentDate, setCurrentDate] = useState(new Date());
  const [categoryFilter, setCategoryFilter] = useState<string>('all');
  const [isManagingCategories, setIsManagingCategories] = useState(false);
  const [expenseCategories, setExpenseCategories] = useState<string[]>(['food', 'transport', 'shopping', 'bills', 'entertainment', 'health', 'other']);
  const [incomeSources, setIncomeSources] = useState<string[]>(['salary', 'freelance', 'business', 'gift', 'investment', 'other']);
  const [manageCategoryTab, setManageCategoryTab] = useState<'expense' | 'income'>('expense');
  const [newCategoryName, setNewCategoryName] = useState('');

  const [editingCategory, setEditingCategory] = useState<string | null>(null);
  const [editCategoryName, setEditCategoryName] = useState('');
  const [showGraphView, setShowGraphView] = useState(false);
  const [graphTab, setGraphTab] = useState<'income' | 'expense'>('expense');

  const handleAddCategory = async () => {
    if (!user || !newCategoryName.trim()) return;
    try {
      const field = manageCategoryTab === 'expense' ? 'expense' : 'income';
      const currentList = manageCategoryTab === 'expense' ? expenseCategories : incomeSources;
      const updatedList = [...currentList, newCategoryName.trim()];
      
      await setDoc(doc(db, 'users', user.uid, 'settings', 'categories'), {
        [field]: updatedList
      }, { merge: true });
      
      setNewCategoryName('');
    } catch (e) {
      console.error('Error adding category:', e);
    }
  };

  const handleEditCategory = async (oldName: string) => {
    if (!user || !editCategoryName.trim() || oldName === editCategoryName.trim()) {
      setEditingCategory(null);
      return;
    }
    try {
      const field = manageCategoryTab === 'expense' ? 'expense' : 'income';
      const currentList = manageCategoryTab === 'expense' ? expenseCategories : incomeSources;
      const updatedList = currentList.map(c => c === oldName ? editCategoryName.trim() : c);
      
      await setDoc(doc(db, 'users', user.uid, 'settings', 'categories'), {
        [field]: updatedList
      }, { merge: true });
      
      setEditingCategory(null);
    } catch (e) {
      console.error('Error editing category:', e);
    }
  };

  const handleDeleteCategory = async (catToDelete: string) => {
    if (!user) return;
    try {
      const field = manageCategoryTab === 'expense' ? 'expense' : 'income';
      const currentList = manageCategoryTab === 'expense' ? expenseCategories : incomeSources;
      const updatedList = currentList.filter(c => c !== catToDelete);
      
      await setDoc(doc(db, 'users', user.uid, 'settings', 'categories'), {
        [field]: updatedList
      }, { merge: true });
    } catch (e) {
      console.error('Error deleting category:', e);
    }
  };

  useEffect(() => {
    if (location.state?.openAddModal) {
      setIsAdding(true);
      if (location.state.type) {
        setActiveTab(location.state.type);
      }
      window.history.replaceState({}, document.title);
    }
  }, [location]);
  
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('food');
  const [source, setSource] = useState('salary');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [paymentMethod, setPaymentMethod] = useState('cash');

  useEffect(() => {
    if (!user) return;
    
    const expensesRef = collection(db, 'users', user.uid, 'expenses');
    const unsubscribeExpenses = onSnapshot(query(expensesRef, orderBy('date', 'desc')), (snapshot) => {
      const exps = snapshot.docs.map(doc => ({ id: doc.id, type: 'expense', ...doc.data() }));
      setAllExps(exps);
    });

    const incomeRef = collection(db, 'users', user.uid, 'income');
    const unsubscribeIncome = onSnapshot(query(incomeRef, orderBy('date', 'desc')), (snapshot) => {
      const incs = snapshot.docs.map(doc => ({ id: doc.id, type: 'income', ...doc.data() }));
      setAllIncs(incs);
    });

    const categoriesRef = doc(db, 'users', user.uid, 'settings', 'categories');
    const unsubscribeCats = onSnapshot(categoriesRef, (doc) => {
      if (doc.exists()) {
        const data = doc.data();
        if (data.expense && Array.isArray(data.expense) && data.expense.length > 0) setExpenseCategories(data.expense);
        if (data.income && Array.isArray(data.income) && data.income.length > 0) setIncomeSources(data.income);
      }
    });

    return () => {
      unsubscribeExpenses();
      unsubscribeIncome();
      unsubscribeCats();
    };
  }, [user]);

  const [allExps, setAllExps] = useState<any[]>([]);
  const [allIncs, setAllIncs] = useState<any[]>([]);

  useEffect(() => {
    const combined = [...allExps, ...allIncs].sort((a, b) => 
      new Date(b.date).getTime() - new Date(a.date).getTime()
    );
    setTransactions(combined);
  }, [allExps, allIncs]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !amount || isNaN(Number(amount))) return;
    
    try {
      if (activeTab === 'expense') {
        await addDoc(collection(db, 'users', user.uid, 'expenses'), {
          amount: Number(amount),
          category,
          description,
          date: new Date(date).toISOString(),
          paymentMethod,
          createdAt: new Date().toISOString()
        });
      } else {
        await addDoc(collection(db, 'users', user.uid, 'income'), {
          amount: Number(amount),
          source,
          description,
          date: new Date(date).toISOString(),
          createdAt: new Date().toISOString()
        });
      }
      setIsAdding(false);
      setAmount('');
      setDescription('');
    } catch (error) {
      console.error('Error adding transaction:', error);
    }
  };

  const confirmDelete = async () => {
    if (!user || !transactionToDelete) return;
    try {
      const collectionName = transactionToDelete.type === 'expense' ? 'expenses' : 'income';
      await deleteDoc(doc(db, 'users', user.uid, collectionName, transactionToDelete.id));
      setTransactionToDelete(null);
    } catch (error) {
      console.error('Error deleting transaction:', error);
    }
  };

  const paymentMethods = ['cash', 'bkash', 'nagad', 'card', 'bankTransfer'];
  
  // Filter transactions based on viewMode (Month or Total)
  const filteredByViewMode = transactions.filter(t => {
    if (viewMode === 'total') return true;
    const tDate = new Date(t.date);
    return tDate.getMonth() === currentDate.getMonth() && tDate.getFullYear() === currentDate.getFullYear();
  });

  // Calculate totals based on viewMode
  const displayTransactions = filteredByViewMode.filter(item => {
    if (typeFilter !== 'all' && item.type !== typeFilter) return false;
    
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      const descMatch = item.description?.toLowerCase().includes(query);
      const catMatch = item.category?.toLowerCase().includes(query);
      const sourceMatch = item.source?.toLowerCase().includes(query);
      const translatedCatMatch = item.category ? t(item.category).toLowerCase().includes(query) : false;
      const translatedSourceMatch = item.source ? t(item.source).toLowerCase().includes(query) : false;
      const amountMatch = item.amount?.toString().includes(query);
      
      if (!descMatch && !catMatch && !sourceMatch && !translatedCatMatch && !translatedSourceMatch && !amountMatch) {
         return false;
      }
    }

    const itemCategory = item.type === 'expense' ? item.category : item.source;
    if (categoryFilter !== 'all' && itemCategory !== categoryFilter) return false;
    return true;
  }).sort((a, b) => {
    if (sortOrder === 'newest') return new Date(b.date).getTime() - new Date(a.date).getTime();
    if (sortOrder === 'oldest') return new Date(a.date).getTime() - new Date(b.date).getTime();
    if (sortOrder === 'highest') return b.amount - a.amount;
    if (sortOrder === 'lowest') return a.amount - b.amount;
    return 0;
  });

  const totalIncome = filteredByViewMode.filter(t => t.type === 'income').reduce((sum, item) => sum + item.amount, 0);
  const totalExpense = filteredByViewMode.filter(t => t.type === 'expense').reduce((sum, item) => sum + item.amount, 0);
  const balance = totalIncome - totalExpense;
  const currentMonthNameBn = currentDate.toLocaleString('bn-BD', { month: 'long' });

  // Calculate previous month data for comparison
  const prevMonthStart = startOfMonth(subMonths(currentDate, 1));
  const prevMonthEnd = endOfMonth(subMonths(currentDate, 1));
  
  const prevMonthIncome = transactions
    .filter(t => t.type === 'income' && new Date(t.date) >= prevMonthStart && new Date(t.date) <= prevMonthEnd)
    .reduce((sum, item) => sum + item.amount, 0);
    
  const prevMonthExpense = transactions
    .filter(t => t.type === 'expense' && new Date(t.date) >= prevMonthStart && new Date(t.date) <= prevMonthEnd)
    .reduce((sum, item) => sum + item.amount, 0);

  const getPercentageChange = (current: number, previous: number) => {
    if (previous === 0) return current > 0 ? 100 : 0;
    return Number((((current - previous) / previous) * 100).toFixed(1));
  };

  const incomeChange = getPercentageChange(totalIncome, prevMonthIncome);
  const expenseChange = getPercentageChange(totalExpense, prevMonthExpense);

  // Group transactions by date
  const groupedTransactions = displayTransactions.reduce((acc, transaction) => {
    const groupKey = transaction.date ? format(new Date(transaction.date), 'dd MMMM, yyyy') : 'Unknown';
    if (!acc[groupKey]) {
      acc[groupKey] = [];
    }
    acc[groupKey].push(transaction);
    return acc;
  }, {});

  // Extract unique categories for the filter menu
  const uniqueCategories = Array.from(new Set(
    filteredByViewMode.map(t => t.type === 'expense' ? t.category : t.source).filter(Boolean)
  ));

  return (
    <div className="space-y-4 pt-4 sm:pt-6 px-4 pb-20 dark:text-gray-100 max-w-4xl lg:max-w-6xl mx-auto relative h-[calc(100vh-4rem)] overflow-hidden flex flex-col">
      {/* Top compare banner */}
      {showMonthCompare && (
        <div className="shrink-0 bg-white dark:bg-gray-800 rounded-2xl p-3 flex justify-between items-center shadow-sm border border-gray-100 dark:border-gray-700 relative">
          <div className="flex-1 flex flex-col items-center border-r border-gray-200 dark:border-gray-700">
             <div className="flex items-center text-xs text-gray-500 mb-1">
               <TrendingUp size={12} className="text-green-500 mr-1" />
               "আয়" গত মাস থেকে
             </div>
             <span className={`text-sm font-bold ${incomeChange >= 0 ? 'text-green-500' : 'text-red-500'}`}>
               {incomeChange > 0 ? '+' : ''}{incomeChange}% {incomeChange >= 0 ? 'বেশি' : 'কম'}
             </span>
          </div>
          <div className="flex-1 flex flex-col items-center">
             <div className="flex items-center text-xs text-gray-500 mb-1">
               <TrendingDown size={12} className="text-red-500 mr-1" />
               "ব্যয়" গত মাস থেকে
             </div>
             <span className={`text-sm font-bold ${expenseChange <= 0 ? 'text-green-500' : 'text-red-500'}`}>
               {expenseChange > 0 ? '+' : ''}{expenseChange}% {expenseChange >= 0 ? 'বেশি' : 'কম'}
             </span>
          </div>
          <button onClick={() => setShowMonthCompare(false)} className="absolute top-2 right-2 text-gray-400 hover:text-gray-600">
            <X size={14} />
          </button>
        </div>
      )}

      {/* Header section - fixed at top */}
      <div className="shrink-0 bg-white dark:bg-gray-800 rounded-3xl shadow-sm pb-4 pt-2 z-10 border border-gray-100 dark:border-gray-700">
        <div className="flex justify-between items-center bg-gray-50 dark:bg-gray-700/50 p-1 rounded-2xl mb-4 mx-4 border border-gray-100 dark:border-gray-700">
           <button 
             onClick={() => setViewMode('month')}
             className={`flex-1 py-2 rounded-xl text-sm font-bold transition-all ${viewMode === 'month' ? 'bg-blue-500 text-white shadow-sm' : 'text-gray-500 dark:text-gray-400'}`}
           >
             {currentMonthNameBn}
           </button>
           <button 
             onClick={() => setViewMode('total')}
             className={`flex-1 py-2 rounded-xl text-sm font-bold transition-all ${viewMode === 'total' ? 'bg-blue-500 text-white shadow-sm' : 'text-gray-500 dark:text-gray-400'}`}
           >
             মোট
           </button>
        </div>

        <div className="flex justify-between items-center px-8 text-center">
           <div>
             <p className="text-xs text-green-500 mb-1 font-medium">আয়</p>
             <p className="text-lg font-bold text-green-500">{totalIncome > 0 ? `${currencySymbol}${totalIncome}` : '0'}</p>
           </div>
           <div>
             <p className="text-xs text-red-500 mb-1 font-medium">ব্যয়</p>
             <p className="text-lg font-bold text-red-500">{totalExpense > 0 ? `${currencySymbol}${totalExpense}` : '0'}</p>
           </div>
           <div>
             <p className="text-xs text-blue-500 mb-1 font-medium">ব্যালেন্স</p>
             <p className="text-lg font-bold text-blue-500">{balance !== 0 ? `${currencySymbol}${balance}` : '0'}</p>
           </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="shrink-0 pt-1 mb-2">
        <div className="flex bg-blue-50/50 dark:bg-gray-800 rounded-2xl p-1 items-center mb-3">
          <div className="flex-1 flex text-sm font-medium">
             <button 
               onClick={() => setTypeFilter('all')}
               className={`flex-1 py-2 rounded-xl transition-all ${typeFilter === 'all' ? 'bg-blue-100 text-blue-600 dark:bg-blue-900/40 dark:text-blue-400' : 'text-gray-400 dark:text-gray-500'}`}
             >
               সব
             </button>
             <button 
               onClick={() => setTypeFilter('income')}
               className={`flex-1 py-2 rounded-xl transition-all ${typeFilter === 'income' ? 'bg-blue-100 text-blue-600 dark:bg-blue-900/40 dark:text-blue-400' : 'text-gray-400 dark:text-gray-500'}`}
             >
               আয়
             </button>
             <button 
               onClick={() => setTypeFilter('expense')}
               className={`flex-1 py-2 rounded-xl transition-all ${typeFilter === 'expense' ? 'bg-blue-100 text-blue-600 dark:bg-blue-900/40 dark:text-blue-400' : 'text-gray-400 dark:text-gray-500'}`}
             >
               ব্যয়
             </button>
          </div>
          <div className="flex space-x-1 px-2 text-gray-400 relative">
            <label className="cursor-pointer p-2 hover:bg-gray-100 rounded-lg dark:hover:bg-gray-700 flex items-center relative">
              <Calendar size={18} />
              <input 
                type="month" 
                value={format(currentDate, 'yyyy-MM')} 
                onChange={(e) => {
                  if (e.target.value) setCurrentDate(new Date(e.target.value));
                }}
                className="opacity-0 absolute inset-0 w-full h-full cursor-pointer" 
              />
            </label>
            <div className="relative">
              <button 
                onClick={() => { setShowFilterMenu(!showFilterMenu); setShowCategoryMenu(false); setShowMoreMenu(false); }} 
                className="p-2 hover:bg-gray-100 rounded-lg dark:hover:bg-gray-700"
              >
                <Filter size={18} className={sortOrder !== 'newest' ? 'text-blue-500' : ''} />
              </button>
              {showFilterMenu && (
                <div className="absolute right-0 top-full mt-2 w-48 bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700 z-50 py-2">
                  <div className="px-4 py-1 text-xs font-semibold text-gray-500">সর্ট করুন</div>
                  <button onClick={() => { setSortOrder('newest'); setShowFilterMenu(false); }} className={`w-full text-left px-4 py-2 text-sm hover:bg-gray-50 dark:hover:bg-gray-700 ${sortOrder === 'newest' ? 'text-blue-500 font-bold' : 'text-gray-700 dark:text-gray-200'}`}>নতুন থেকে পুরনো</button>
                  <button onClick={() => { setSortOrder('oldest'); setShowFilterMenu(false); }} className={`w-full text-left px-4 py-2 text-sm hover:bg-gray-50 dark:hover:bg-gray-700 ${sortOrder === 'oldest' ? 'text-blue-500 font-bold' : 'text-gray-700 dark:text-gray-200'}`}>পুরনো থেকে নতুন</button>
                  <button onClick={() => { setSortOrder('highest'); setShowFilterMenu(false); }} className={`w-full text-left px-4 py-2 text-sm hover:bg-gray-50 dark:hover:bg-gray-700 ${sortOrder === 'highest' ? 'text-blue-500 font-bold' : 'text-gray-700 dark:text-gray-200'}`}>অ্যামাউন্ট (বেশি থেকে কম)</button>
                  <button onClick={() => { setSortOrder('lowest'); setShowFilterMenu(false); }} className={`w-full text-left px-4 py-2 text-sm hover:bg-gray-50 dark:hover:bg-gray-700 ${sortOrder === 'lowest' ? 'text-blue-500 font-bold' : 'text-gray-700 dark:text-gray-200'}`}>অ্যামাউন্ট (কম থেকে বেশি)</button>
                </div>
              )}
            </div>
            <div className="relative">
              <button 
                onClick={() => { setShowCategoryMenu(!showCategoryMenu); setShowFilterMenu(false); setShowMoreMenu(false); }} 
                className={`p-2 hover:bg-gray-100 rounded-lg dark:hover:bg-gray-700 ${categoryFilter !== 'all' ? 'text-blue-500' : ''}`}
              >
                <List size={18} />
              </button>
              {showCategoryMenu && (
                <div className="absolute right-0 top-full mt-2 w-48 bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700 z-50 py-2 max-h-64 overflow-y-auto">
                  <div className="px-4 py-1 text-xs font-semibold text-gray-500">ক্যাটাগরি ফিল্টার</div>
                  <button onClick={() => { setCategoryFilter('all'); setShowCategoryMenu(false); }} className={`w-full text-left px-4 py-2 text-sm hover:bg-gray-50 dark:hover:bg-gray-700 ${categoryFilter === 'all' ? 'text-blue-500 font-bold' : 'text-gray-700 dark:text-gray-200'}`}>সব ক্যাটাগরি</button>
                  {uniqueCategories.map(cat => (
                    <button key={String(cat)} onClick={() => { setCategoryFilter(String(cat)); setShowCategoryMenu(false); }} className={`w-full text-left px-4 py-2 text-sm hover:bg-gray-50 dark:hover:bg-gray-700 ${categoryFilter === cat ? 'text-blue-500 font-bold' : 'text-gray-700 dark:text-gray-200'}`}>
                      {t(String(cat))}
                    </button>
                  ))}
                </div>
              )}
            </div>
            <button onClick={() => { setShowMoreMenu(!showMoreMenu); setShowFilterMenu(false); setShowCategoryMenu(false); }} className="p-2 hover:bg-gray-100 rounded-lg dark:hover:bg-gray-700"><MoreVertical size={18} /></button>
            
            {/* More Menu Dropdown */}
            {showMoreMenu && (
              <div className="absolute right-0 top-full mt-2 w-56 bg-white dark:bg-gray-800 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-700 z-50 py-2">
                <button 
                  onClick={() => { setIsManagingCategories(true); setShowMoreMenu(false); }} 
                  className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  ক্যাটাগরি তৈরি/ডিলিট করুন
                </button>
                <button 
                  onClick={() => { setShowGraphView(true); setShowMoreMenu(false); }} 
                  className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  গ্রাফ দেখুন
                </button>
                <div className="h-px bg-gray-100 dark:bg-gray-700 my-1"></div>
                <button 
                  onClick={() => setShowSearchBox(!showSearchBox)}
                  className="w-full flex items-center px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  <CheckSquare size={16} className={`mr-2 ${showSearchBox ? 'text-blue-500' : 'text-gray-300'}`} />
                  সার্চ বক্স দেখান
                </button>
                <button 
                  onClick={() => setShowMonthCompare(!showMonthCompare)}
                  className="w-full flex items-center px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  <CheckSquare size={16} className={`mr-2 ${showMonthCompare ? 'text-blue-500' : 'text-gray-300'}`} />
                  মাসিক পার্সেন্টেজ দেখান
                </button>
              </div>
            )}
          </div>
        </div>

        {showSearchBox && (
          <div className="relative">
            <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-300 dark:text-gray-500" />
            <input 
              type="text" 
              value={searchQuery}
              onChange={(e) => {
                const convertedValue = e.target.value.replace(/[০-৯]/g, d => '০১২৩৪৫৬৭৮৯'.indexOf(d).toString());
                setSearchQuery(convertedValue);
              }}
              placeholder="সার্চ..." 
              className="w-full bg-gray-50 dark:bg-gray-800 border-none rounded-2xl py-3 pl-10 pr-4 text-sm focus:ring-0 text-gray-600 placeholder-gray-300 dark:text-gray-300 dark:placeholder-gray-600 focus:outline-none"
            />
          </div>
        )}
      </div>

      {/* Scrollable List Section */}
      <div className="flex-1 overflow-y-auto no-scrollbar pb-20 -mx-4 px-4 sm:mx-0 sm:px-0">
        {transactions.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-gray-300 dark:text-gray-600 mt-10">
            <Receipt size={60} strokeWidth={1} className="mb-4" />
            <p className="text-sm">{currentDate.toLocaleString('bn-BD', { month: 'long', year: 'numeric' })}</p>
            <p className="text-sm font-medium">মাসে কোন লেনদেন নেই</p>
          </div>
        ) : (
          <div className="space-y-5 mt-4">
             {Object.entries(groupedTransactions).map(([date, items]: [string, any]) => (
               <div key={date}>
                 <div className="flex justify-between items-center mb-3">
                   <h4 className="text-xs font-medium text-gray-400 dark:text-gray-500">{date}</h4>
                   <div className="text-xs font-medium text-gray-400 flex gap-2">
                      {items.some((i:any) => i.type === 'income') && (
                        <span className="text-green-500/70 dark:text-green-400">
                          {currencySymbol}{items.filter((i:any) => i.type==='income').reduce((sum:number, i:any) => sum + i.amount, 0).toLocaleString()}
                        </span>
                      )}
                      {items.some((i:any) => i.type === 'expense') && (
                        <span className="text-red-500/70 dark:text-red-400">
                          {currencySymbol}{items.filter((i:any) => i.type==='expense').reduce((sum:number, i:any) => sum + i.amount, 0).toLocaleString()}
                        </span>
                      )}
                   </div>
                 </div>
                 
                 <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                   {items.map((transaction: any) => (
                     <div key={transaction.id} className="relative group bg-gray-50/50 dark:bg-gray-800 rounded-2xl p-4 flex items-center justify-between border border-gray-100/50 dark:border-gray-700">
                        <div className="flex items-center">
                          <div className={`w-8 h-8 rounded-full flex items-center justify-center mr-3 ${
                            transaction.type === 'income' ? 'bg-green-100 text-green-600' : 'bg-red-100 text-red-500'
                          }`}>
                             {transaction.type === 'income' ? <Plus size={16} /> : <span className="h-[2px] w-3 bg-current rounded-full"></span>}
                          </div>
                          <div>
                            <p className="text-sm font-bold text-gray-800 dark:text-gray-200">
                               {transaction.type === 'expense' ? t(transaction.category) : t(transaction.source)}
                            </p>
                            <div className="flex items-center text-[10px] text-gray-400 mt-0.5">
                              <span>{format(new Date(transaction.date), 'hh:mm a')}</span>
                              {transaction.description && (
                                <>
                                  <span className="mx-1">•</span>
                                  <span className="truncate max-w-[120px]">{transaction.description}</span>
                                </>
                              )}
                            </div>
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className={`text-lg font-bold ${
                            transaction.type === 'income' ? 'text-green-500' : 'text-red-500'
                          }`}>
                            {transaction.amount}
                          </span>
                          <button 
                            onClick={() => setTransactionToDelete({ id: transaction.id, type: transaction.type })}
                            className="text-gray-300 hover:text-red-500"
                          >
                            <MoreVertical size={16} />
                          </button>
                        </div>
                     </div>
                   ))}
                 </div>
               </div>
             ))}
          </div>
        )}
      </div>

      {/* Floating Action Button for Add Note */}
      <button 
        onClick={() => setIsAdding(true)}
        className="fixed bottom-20 right-6 w-14 h-14 bg-blue-500 hover:bg-blue-600 text-white rounded-full flex items-center justify-center shadow-lg shadow-blue-500/30 z-40 transition-transform active:scale-95"
      >
        <Plus size={28} />
      </button>

      {isAdding && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl w-full max-w-lg overflow-hidden animate-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center p-6 border-b dark:border-gray-700">
              <h3 className="text-lg font-bold text-gray-900 dark:text-white">{t('addTransaction')}</h3>
              <button onClick={() => setIsAdding(false)} className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300">
                <X size={24} />
              </button>
            </div>
            
            <form onSubmit={handleSave} className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4 p-1 bg-gray-100 dark:bg-gray-700 rounded-lg">
                <button
                  type="button"
                  onClick={() => setActiveTab('expense')}
                  className={`py-2 text-sm font-medium rounded-md transition-all ${
                    activeTab === 'expense' ? 'bg-white dark:bg-gray-600 text-red-600 dark:text-red-400 shadow-sm' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                  }`}
                >
                  {t('expense')}
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab('income')}
                  className={`py-2 text-sm font-medium rounded-md transition-all ${
                    activeTab === 'income' ? 'bg-white dark:bg-gray-600 text-emerald-600 dark:text-emerald-400 shadow-sm' : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
                  }`}
                >
                  {t('income')}
                </button>
              </div>

              <div className="grid grid-cols-1 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('amount')} ({currencySymbol})</label>
                  <div className="relative">
                    <DollarSign className="absolute left-3 top-2.5 text-gray-400 dark:text-gray-500" size={18} />
                    <SwipeableNumberInput
                      required
                      value={amount}
                      onChange={setAmount}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      placeholder="0.00"
                      isPrice={true}
                    />
                  </div>
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('date')}</label>
                  <input
                    type="date"
                    required
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  />
                </div>

                {activeTab === 'expense' ? (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('category')}</label>
                      <select
                        value={category}
                        onChange={(e) => setCategory(e.target.value)}
                        className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      >
                        {expenseCategories.map(c => <option key={c} value={c}>{t(c)}</option>)}
                      </select>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('paymentMethod')}</label>
                      <select
                        value={paymentMethod}
                        onChange={(e) => setPaymentMethod(e.target.value)}
                        className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      >
                        {paymentMethods.map(p => <option key={p} value={p}>{t(p)}</option>)}
                      </select>
                    </div>
                  </>
                ) : (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('source')}</label>
                    <select
                      value={source}
                      onChange={(e) => setSource(e.target.value)}
                      className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    >
                      {incomeSources.map(s => <option key={s} value={s}>{t(s)}</option>)}
                    </select>
                  </div>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('description')}</label>
                <input
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder={activeTab === 'expense' ? t('expensePlaceholder') : t('incomePlaceholder')}
                  className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>

              <button
                type="submit"
                disabled={!amount}
                className={`w-full flex items-center justify-center py-3 px-4 border border-transparent rounded-xl shadow-sm text-sm font-bold text-white transition-all ${
                  activeTab === 'expense' ? 'bg-red-600 hover:bg-red-700' : 'bg-emerald-600 hover:bg-emerald-700'
                } disabled:opacity-50`}
              >
                <Save size={20} className="mr-2" />
                {t('save')} {activeTab === 'expense' ? t('expense') : t('income')}
              </button>
            </form>
          </div>
        </div>,
        document.body
      )}



      {/* Delete Confirmation Modal */}
      {transactionToDelete && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
          <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl p-6 max-w-sm w-full relative animate-in zoom-in-95 duration-200">
            <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-2">{t('delete')}</h3>
            <p className="text-gray-600 dark:text-gray-400 mb-6">{t('confirmDelete')}</p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setTransactionToDelete(null)}
                className="px-4 py-2 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors font-medium"
              >
                {t('cancel')}
              </button>
              <button
                onClick={confirmDelete}
                className="px-4 py-2 bg-red-600 text-white hover:bg-red-700 rounded-lg transition-colors font-medium"
              >
                {t('delete')}
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* Category Management Modal */}
      {isManagingCategories && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl w-full max-w-md overflow-hidden animate-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center p-6 border-b dark:border-gray-700">
              <h3 className="text-lg font-bold text-gray-900 dark:text-white">ক্যাটাগরি ম্যানেজ করুন</h3>
              <button onClick={() => setIsManagingCategories(false)} className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300">
                <X size={24} />
              </button>
            </div>
            
            <div className="p-6">
              <div className="flex bg-gray-100 dark:bg-gray-700 p-1 rounded-xl mb-4">
                <button
                  onClick={() => setManageCategoryTab('expense')}
                  className={`flex-1 py-2 text-sm font-bold rounded-lg transition-colors ${
                    manageCategoryTab === 'expense'
                      ? 'bg-white dark:bg-gray-600 text-red-600 dark:text-red-400 shadow-sm'
                      : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
                  }`}
                >
                  ব্যয় (Expense)
                </button>
                <button
                  onClick={() => setManageCategoryTab('income')}
                  className={`flex-1 py-2 text-sm font-bold rounded-lg transition-colors ${
                    manageCategoryTab === 'income'
                      ? 'bg-white dark:bg-gray-600 text-emerald-600 dark:text-emerald-400 shadow-sm'
                      : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
                  }`}
                >
                  আয় (Income)
                </button>
              </div>

              <div className="flex mb-4">
                <input
                  type="text"
                  value={newCategoryName}
                  onChange={(e) => setNewCategoryName(e.target.value)}
                  placeholder="নতুন ক্যাটাগরির নাম..."
                  className="flex-1 bg-gray-50 dark:bg-gray-900 border border-gray-300 dark:border-gray-600 rounded-l-xl py-2 px-4 text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none dark:text-white"
                  onKeyDown={(e) => e.key === 'Enter' && handleAddCategory()}
                />
                <button 
                  onClick={handleAddCategory}
                  disabled={!newCategoryName.trim()}
                  className="bg-blue-500 text-white px-4 rounded-r-xl font-medium hover:bg-blue-600 disabled:opacity-50"
                >
                  যোগ করুন
                </button>
              </div>

              <div className="max-h-60 overflow-y-auto space-y-2 pr-2">
                {(manageCategoryTab === 'expense' ? expenseCategories : incomeSources).map((cat, index) => (
                  <div key={index} className="flex justify-between items-center bg-gray-50 dark:bg-gray-700/50 p-3 rounded-xl border border-gray-100 dark:border-gray-600">
                    {editingCategory === cat ? (
                      <div className="flex-1 flex mr-2">
                        <input
                          type="text"
                          value={editCategoryName}
                          onChange={(e) => setEditCategoryName(e.target.value)}
                          className="flex-1 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-500 rounded-l-lg py-1 px-3 text-sm focus:outline-none dark:text-white"
                          autoFocus
                          onKeyDown={(e) => e.key === 'Enter' && handleEditCategory(cat)}
                        />
                        <button 
                          onClick={() => handleEditCategory(cat)}
                          className="bg-blue-500 text-white px-3 rounded-r-lg text-sm hover:bg-blue-600"
                        >
                          <CheckSquare size={16} />
                        </button>
                      </div>
                    ) : (
                      <span className="text-gray-800 dark:text-gray-200 font-medium text-sm">{t(cat) !== cat ? t(cat) : cat}</span>
                    )}
                    <div className="flex space-x-1">
                      {editingCategory !== cat && (
                        <button 
                          onClick={() => { 
                            setEditingCategory(cat); 
                            setEditCategoryName(t(cat) !== cat ? t(cat) : cat); 
                          }}
                          className="text-gray-400 hover:text-blue-500 transition-colors p-1"
                          title="Edit category"
                        >
                          <Edit2 size={16} />
                        </button>
                      )}
                      <button 
                        onClick={() => handleDeleteCategory(cat)}
                        className="text-gray-400 hover:text-red-500 transition-colors p-1"
                        title="Delete category"
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* Graph & Summary View Portal */}
      {showGraphView && createPortal(
        <div className="fixed inset-0 z-[10000] bg-gray-50 dark:bg-gray-900 overflow-y-auto no-scrollbar w-full h-full animate-in slide-in-from-bottom-4 duration-300 sm:slide-in-from-right-8">
          <div className="max-w-4xl mx-auto md:max-w-6xl min-h-screen bg-gray-50 dark:bg-gray-900 pb-20 relative shadow-2xl">
            {/* Header */}
            <div className="flex items-center justify-between p-4 bg-white dark:bg-gray-800 shadow-sm sticky top-0 z-10 border-b border-gray-100 dark:border-gray-700">
              <button onClick={() => setShowGraphView(false)} className="p-2 -ml-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition-colors">
                <ArrowLeft size={24} className="text-gray-700 dark:text-gray-200" />
              </button>
              
              <div className="flex items-center gap-3">
                <button onClick={() => setCurrentDate(subMonths(currentDate, 1))} className="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full">
                  <ChevronLeft size={20} className="text-gray-600 dark:text-gray-300" />
                </button>
                <h2 className="text-lg font-bold text-gray-800 dark:text-white min-w-[100px] text-center">
                  {currentDate.toLocaleString('bn-BD', { month: 'long', year: 'numeric' })}
                </h2>
                <button onClick={() => setCurrentDate(addMonths(currentDate, 1))} className="p-1 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full">
                  <ChevronRight size={20} className="text-gray-600 dark:text-gray-300" />
                </button>
              </div>

              <div className="relative">
                <button className="p-2 -mr-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition-colors relative z-0">
                  <CalendarDays size={24} className="text-gray-700 dark:text-gray-200" />
                  <input 
                    type="month" 
                    className="absolute inset-0 opacity-0 cursor-pointer w-full h-full"
                    value={format(currentDate, 'yyyy-MM')}
                    onChange={(e) => {
                      if (e.target.value) {
                         setCurrentDate(new Date(e.target.value + '-01'));
                      }
                    }}
                  />
                </button>
              </div>
            </div>

            <div className="p-4 space-y-6">
               {/* Account Calendar */}
               <div className="bg-white dark:bg-gray-800 rounded-2xl p-4 shadow-sm border border-gray-100 dark:border-gray-700">
                 <h3 className="text-base font-semibold text-gray-800 dark:text-white mb-4">হিসাব ক্যালেন্ডার</h3>
                 <div className="grid grid-cols-7 gap-2 text-center text-sm mb-5">
                    {Array.from({ length: getDaysInMonth(currentDate) }, (_, i) => {
                       const day = i + 1;
                       const dayStr = day.toString().padStart(2, '0');
                       const dateStr = `${format(currentDate, 'yyyy-MM')}-${dayStr}`;
                       const todayStr = format(new Date(), 'yyyy-MM-dd');
                       
                       let statusClass = "bg-gray-50 dark:bg-gray-700/50 text-gray-500 dark:text-gray-400"; // Has no account
                       if (dateStr > todayStr) {
                           statusClass = "text-gray-300 dark:text-gray-600/50"; // Future
                       } else {
                           const hasTrx = filteredByViewMode.some(t => t.date === dateStr);
                           if (hasTrx) {
                              statusClass = "bg-blue-100 dark:bg-blue-900/60 text-blue-600 dark:text-blue-400 font-bold shadow-sm"; // Has account
                           }
                       }

                       return (
                          <div key={day} className={`aspect-square flex items-center justify-center rounded-xl text-xs sm:text-sm transition-all ${statusClass}`}>
                            {day.toLocaleString('bn-BD')}
                          </div>
                       );
                    })}
                 </div>
                 {/* Legends */}
                 <div className="flex justify-center gap-5 text-xs">
                    <div className="flex items-center gap-1.5"><div className="w-3 h-3 rounded-md bg-blue-100 dark:bg-blue-900/60 shadow-sm"></div><span className="text-gray-600 dark:text-gray-400 font-medium">হিসাব আছে</span></div>
                    <div className="flex items-center gap-1.5"><div className="w-3 h-3 rounded-md bg-gray-50 dark:bg-gray-700/50"></div><span className="text-gray-600 dark:text-gray-400">হিসাব নেই</span></div>
                    <div className="flex items-center gap-1.5"><div className="w-3 h-3 rounded-md bg-transparent text-gray-300 dark:text-gray-600/50 flex flex-col justify-center">■</div><span className="text-gray-400 dark:text-gray-500">আসেনি</span></div>
                 </div>
               </div>

               {/* Summary Cards */}
               <div className="grid grid-cols-3 gap-3">
                 <div className="bg-green-50/80 dark:bg-green-900/20 p-3 rounded-2xl text-center border border-green-100/50 dark:border-green-800/30">
                   <div className="w-8 h-8 mx-auto bg-green-100 dark:bg-green-900/40 rounded-full flex items-center justify-center mb-2">
                     <ArrowDownCircle size={18} className="text-green-500" />
                   </div>
                   <p className="text-[11px] sm:text-xs text-gray-600 dark:text-gray-400 mb-1">মোট আয়</p>
                   <p className="text-sm font-bold text-green-600 dark:text-green-400">{totalIncome.toLocaleString('bn-BD')}</p>
                 </div>
                 <div className="bg-red-50/80 dark:bg-red-900/20 p-3 rounded-2xl text-center border border-red-100/50 dark:border-red-800/30">
                    <div className="w-8 h-8 mx-auto bg-red-100 dark:bg-red-900/40 rounded-full flex items-center justify-center mb-2">
                      <ArrowUpCircle size={18} className="text-red-500" />
                    </div>
                    <p className="text-[11px] sm:text-xs text-gray-600 dark:text-gray-400 mb-1">মোট ব্যয়</p>
                    <p className="text-sm font-bold text-red-600 dark:text-red-400">{totalExpense.toLocaleString('bn-BD')}</p>
                 </div>
                 <div className="bg-blue-50/80 dark:bg-blue-900/20 p-3 rounded-2xl text-center border border-blue-100/50 dark:border-blue-800/30">
                    <div className="w-8 h-8 mx-auto bg-blue-100 dark:bg-blue-900/40 rounded-full flex items-center justify-center mb-2">
                      <Receipt size={18} className="text-blue-500" />
                    </div>
                    <p className="text-[11px] sm:text-xs text-gray-600 dark:text-gray-400 mb-1">ব্যালেন্স</p>
                    <p className="text-sm font-bold text-blue-600 dark:text-blue-400">{balance.toLocaleString('bn-BD')}</p>
                 </div>
               </div>

               {/* Type Toggle */}
               <div className="flex bg-white dark:bg-gray-800 p-1.5 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700">
                 <button 
                   onClick={() => setGraphTab('income')} 
                   className={`flex-1 py-3 text-sm font-bold rounded-xl transition-all duration-300 ${graphTab === 'income' ? 'bg-green-500 text-white shadow-md' : 'text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700'}`}
                 >
                   আয়
                 </button>
                 <button 
                   onClick={() => setGraphTab('expense')} 
                   className={`flex-1 py-3 text-sm font-bold rounded-xl transition-all duration-300 ${graphTab === 'expense' ? 'bg-red-500 text-white shadow-md' : 'text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700'}`}
                 >
                   ব্যয়
                 </button>
               </div>

               {/* Categories List */}
               <div className="bg-white dark:bg-gray-800 rounded-2xl p-5 shadow-sm border border-gray-100 dark:border-gray-700">
                 <h3 className="text-base font-bold text-gray-800 dark:text-white mb-5">ক্যাটাগরি অনুযায়ী হিসাব</h3>
                 <div className="space-y-4">
                    {(() => {
                       const data = filteredByViewMode.filter(item => item.type === graphTab);
                       const grouped: Record<string, number> = {};
                       let total = 0;
                       data.forEach(item => {
                          const catName = t(graphTab === 'expense' ? item.category : item.source) || 'অন্যান্য';
                          grouped[catName] = (grouped[catName] || 0) + item.amount;
                          total += item.amount;
                       });
                       
                       const graphData = Object.entries(grouped)
                         .sort((a,b) => b[1] - a[1])
                         .map(([name, value], index) => ({
                           name,
                           value,
                           percentage: total > 0 ? ((value / total) * 100).toFixed(1) : '0.0',
                           color: ['#10B981', '#3B82F6', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#14B8A6'][index % 7]
                         }));

                       if (graphData.length === 0) {
                          return <div className="text-center text-gray-400 py-6 text-sm bg-gray-50 dark:bg-gray-900/50 rounded-xl">কোন তথ্য পাওয়া যায়নি</div>;
                       }

                       return graphData.map(item => (
                         <div key={item.name} className="relative bg-gray-50/50 dark:bg-gray-900/20 p-3.5 rounded-xl border border-gray-50 dark:border-gray-800/50">
                           <div className="flex justify-between items-start mb-2.5">
                              <div className="flex items-center gap-3">
                                 <div className="w-1.5 h-full min-h-[22px] rounded-full" style={{ backgroundColor: item.color }}></div>
                                 <span className="font-bold text-gray-800 dark:text-gray-200 text-sm">{item.name}</span>
                              </div>
                              <div className="text-right">
                                 <div className={`font-bold text-base ${graphTab === 'income' ? 'text-green-600 dark:text-green-400' : 'text-red-500 dark:text-red-400'}`}>
                                   {item.value.toLocaleString('bn-BD')}
                                 </div>
                                 <div className="text-[11px] text-gray-400 font-medium">টাকা</div>
                              </div>
                           </div>
                           <div className="flex items-center justify-between gap-2 text-xs text-gray-500 dark:text-gray-400 mb-2">
                              <span className="bg-white dark:bg-gray-800 shadow-sm border border-gray-100 dark:border-gray-700 text-gray-700 dark:text-gray-300 px-2 py-0.5 rounded font-bold">{item.percentage}%</span>
                              <span className="text-[11px]">মোট {graphTab === 'income' ? 'আয়ের' : 'ব্যয়ের'} {item.percentage}%</span>
                           </div>
                           <div className="w-full bg-gray-200 dark:bg-gray-700/50 rounded-full h-2 overflow-hidden shadow-inner">
                              <div className="h-full rounded-full transition-all duration-1000 ease-out relative" style={{ width: `${item.percentage}%`, backgroundColor: item.color }}>
                                 <div className="absolute inset-0 bg-white/20"></div>
                              </div>
                           </div>
                         </div>
                       ));
                    })()}
                 </div>
               </div>

               {/* Donut Chart */}
               <div className="bg-white dark:bg-gray-800 rounded-2xl p-5 shadow-sm border border-gray-100 dark:border-gray-700 mb-8">
                 <h3 className="text-base font-bold text-gray-800 dark:text-white mb-2">ক্যাটাগরি অনুপাত</h3>
                 {(() => {
                       const data = filteredByViewMode.filter(item => item.type === graphTab);
                       if (data.length === 0) {
                          return <div className="text-center text-gray-400 py-10 text-sm">কোন তথ্য পাওয়া যায়নি</div>;
                       }

                       const grouped: Record<string, number> = {};
                       data.forEach(item => {
                          const catName = t(graphTab === 'expense' ? item.category : item.source) || 'অন্যান্য';
                          grouped[catName] = (grouped[catName] || 0) + item.amount;
                       });
                       
                       const graphData = Object.entries(grouped)
                         .sort((a,b) => b[1] - a[1])
                         .map(([name, value], index) => ({
                           name,
                           value,
                           color: ['#10B981', '#3B82F6', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#14B8A6'][index % 7]
                         }));

                       return (
                          <div className="w-full relative min-h-[280px]" style={{ height: '280px' }}>
                             <ResponsiveContainer width="100%" height="100%">
                               <PieChart width={300} height={280}>
                                 <Pie
                                   data={graphData}
                                   cx="50%"
                                   cy="50%"
                                   innerRadius={85}
                                   outerRadius={115}
                                   paddingAngle={4}
                                   dataKey="value"
                                   stroke="none"
                                   cornerRadius={8}
                                 >
                                   {graphData.map((entry, index) => (
                                     <Cell key={`cell-${index}`} fill={entry.color} />
                                   ))}
                                 </Pie>
                                 <RechartsTooltip 
                                   formatter={(value: number) => [`${value.toLocaleString('bn-BD')} টাকা`, 'পরিমাণ']}
                                   contentStyle={{ 
                                      borderRadius: '16px', 
                                      border: 'none', 
                                      boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)',
                                      backgroundColor: 'var(--tw-colors-white, #ffffff)',
                                      padding: '12px'
                                   }}
                                 />
                               </PieChart>
                             </ResponsiveContainer>
                             {/* Central Text for donut */}
                             <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none mt-2">
                               <p className="text-xs text-gray-500 font-medium">মোট {graphTab === 'income' ? 'আয়' : 'ব্যয়'}</p>
                               <p className={`text-xl font-bold mt-1 ${graphTab === 'income' ? 'text-green-500' : 'text-red-500'}`}>
                                  {Object.values(grouped).reduce((a, b) => a + b, 0).toLocaleString('bn-BD')}
                               </p>
                             </div>
                          </div>
                       );
                 })()}
               </div>
            </div>
          </div>
        </div>,
        document.body
      )}
    </div>
  );
};

