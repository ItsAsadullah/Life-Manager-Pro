import React, { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useSettings } from '../contexts/SettingsContext';
import { collection, query, onSnapshot, orderBy, limit, getDocs } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip } from 'recharts';
import { format } from 'date-fns';
import { ArrowUpRight, ArrowDownRight, Wallet, FileText, CalendarDays } from 'lucide-react';

export const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const { t, currencySymbol } = useSettings();
  const [expenses, setExpenses] = useState<any[]>([]);
  const [notes, setNotes] = useState<any[]>([]);
  const [debts, setDebts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;

    const expensesRef = collection(db, 'users', user.uid, 'expenses');
    const qExpenses = query(expensesRef, orderBy('createdAt', 'desc'), limit(50));
    
    const unsubscribeExpenses = onSnapshot(qExpenses, (snapshot) => {
      const exps = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      setExpenses(exps);
    });

    const notesRef = collection(db, 'users', user.uid, 'notes');
    const qNotes = query(notesRef, orderBy('createdAt', 'desc'), limit(3));
    
    const unsubscribeNotes = onSnapshot(qNotes, (snapshot) => {
      const nts = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      setNotes(nts);
    });

    const debtsRef = collection(db, 'users', user.uid, 'debts');
    const qDebts = query(debtsRef, orderBy('createdAt', 'desc'));
    const unsubscribeDebts = onSnapshot(qDebts, async (snapshot) => {
      const dbts = await Promise.all(snapshot.docs.map(async (debtDoc) => {
        const data = debtDoc.data();
        let totalPaid = data.totalPaid;
        
        if (totalPaid === undefined) {
          const repaymentsRef = collection(db, 'users', user.uid!, 'debts', debtDoc.id, 'repayments');
          const repaymentsSnap = await getDocs(repaymentsRef);
          totalPaid = repaymentsSnap.docs.reduce((sum, d) => sum + (d.data().amount || 0), 0);
        }
        
        return { id: debtDoc.id, ...data, totalPaid };
      }));
      setDebts(dbts);
      setLoading(false);
    });

    return () => {
      unsubscribeExpenses();
      unsubscribeNotes();
      unsubscribeDebts();
    };
  }, [user]);

  if (loading) return <div className="flex justify-center items-center h-64">{t('loading')}</div>;

  const totalExpense = expenses.reduce((sum, exp) => sum + (exp.amount || 0), 0);
  const totalIncome = expenses.reduce((sum, exp) => sum + (exp.type === 'income' ? (exp.amount || 0) : 0), 0);
  const currentMonthName = format(new Date(), 'MMMM');
  const currentMonthNameBn = new Date().toLocaleString('bn-BD', { month: 'long' });
  const totalBorrowed = debts.filter(d => d.type === 'borrowed' && d.status === 'pending').reduce((sum, d) => sum + (d.amount - (d.totalPaid || 0)), 0);
  const totalLent = debts.filter(d => d.type === 'lent' && d.status === 'pending').reduce((sum, d) => sum + (d.amount - (d.totalPaid || 0)), 0);
  
  const categoryData = expenses.reduce((acc: any, exp) => {
    if(exp.type === 'income') return acc;
    const cat = exp.category || 'Other';
    acc[cat] = (acc[cat] || 0) + exp.amount;
    return acc;
  }, {});

  const chartData = Object.keys(categoryData).map(key => ({
    name: key,
    value: categoryData[key]
  }));

  const COLORS = ['#818cf8', '#34d399', '#fbbf24', '#f87171', '#a78bfa', '#fb923c', '#60a5fa'];

  return (
    <div className="space-y-4 pb-20 dark:text-gray-100">
      
      {/* Top Banner / Summary Card */}
      <div className="bg-gradient-to-br from-blue-600 to-indigo-700 rounded-3xl p-6 text-white shadow-lg relative overflow-hidden">
        <div className="absolute top-0 right-0 -mt-4 -mr-4 w-32 h-32 bg-white opacity-10 rounded-full blur-2xl"></div>
        <div className="absolute bottom-0 left-0 -mb-4 -ml-4 w-24 h-24 bg-white opacity-10 rounded-full blur-xl"></div>
        
        <div className="relative z-10 flex flex-col items-center text-center">
           <span className="text-blue-100 text-sm font-medium mb-1 drop-shadow-sm">{currentMonthNameBn} মাসের হিসাব</span>
           <div className="flex items-center space-x-6 mb-4 mt-2">
             <div className="flex flex-col items-center">
               <span className="text-xs text-blue-100 mb-1">আয়</span>
               <h2 className="text-2xl font-bold tracking-tight drop-shadow-md text-emerald-300">
                 {currencySymbol}{totalIncome.toLocaleString()}
               </h2>
             </div>
             <div className="w-px h-8 bg-blue-400/50"></div>
             <div className="flex flex-col items-center">
               <span className="text-xs text-blue-100 mb-1">ব্যয়</span>
               <h2 className="text-2xl font-bold tracking-tight drop-shadow-md text-rose-300">
                 {currencySymbol}{totalExpense.toLocaleString()}
               </h2>
             </div>
           </div>
           
           <div className="w-full bg-white/20 backdrop-blur-md rounded-2xl p-4 flex justify-between items-center shadow-inner border border-white/10">
             <div className="flex flex-col items-center flex-1 border-r border-white/20">
                <div className="flex items-center text-red-200 mb-1">
                  <ArrowDownRight size={14} className="mr-1" />
                  <span className="text-xs font-semibold">আমি দেবো</span>
                </div>
                <span className="text-lg font-bold">{currencySymbol}{totalBorrowed.toLocaleString()}</span>
             </div>
             
             <div className="flex flex-col items-center flex-1">
                <div className="flex items-center text-green-200 mb-1">
                  <ArrowUpRight size={14} className="mr-1" />
                  <span className="text-xs font-semibold">আমি পাবো</span>
                </div>
                <span className="text-lg font-bold">{currencySymbol}{totalLent.toLocaleString()}</span>
             </div>
           </div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="bg-white p-4 rounded-3xl shadow-sm border border-gray-100 flex items-center space-x-3 dark:bg-gray-800 dark:border-gray-700">
          <div className="p-3 bg-purple-50 text-purple-600 rounded-2xl dark:bg-purple-900/30 dark:text-purple-400">
            <FileText size={22} className="fill-current opacity-20" />
          </div>
          <div>
            <h3 className="text-[11px] font-bold text-gray-400 uppercase tracking-widest leading-none dark:text-gray-500">{t('notes')}</h3>
            <p className="text-xl font-bold text-gray-900 dark:text-white mt-1 leading-none">{notes.length}</p>
          </div>
        </div>
        
        <div className="bg-white p-4 rounded-3xl shadow-sm border border-gray-100 flex items-center space-x-3 dark:bg-gray-800 dark:border-gray-700">
          <div className="p-3 bg-blue-50 text-blue-600 rounded-2xl dark:bg-blue-900/30 dark:text-blue-400">
            <CalendarDays size={22} className="fill-current opacity-20" />
          </div>
          <div>
            <h3 className="text-[11px] font-bold text-gray-400 uppercase tracking-widest leading-none dark:text-gray-500">লেনদেন</h3>
            <p className="text-xl font-bold text-gray-900 dark:text-white mt-1 leading-none">{expenses.length}</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2 bg-white p-5 rounded-3xl shadow-sm border border-gray-100 dark:bg-gray-800 dark:border-gray-700">
          <div className="flex justify-between items-center mb-0">
            <h3 className="text-sm font-bold text-gray-900 dark:text-white flex items-center">
              <Wallet size={16} className="mr-2 text-indigo-500" />
              {t('expenseAnalysis')}
            </h3>
          </div>
          {chartData.length > 0 ? (
            <div className="h-64 mt-2 min-h-[256px]">
              <ResponsiveContainer width="100%" height={200}>
                <PieChart width={300} height={200}>
                  <Pie
                    data={chartData}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={80}
                    paddingAngle={4}
                    dataKey="value"
                    stroke="none"
                    cornerRadius={4}
                  >
                    {chartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} opacity={0.85} />
                    ))}
                  </Pie>
                  <RechartsTooltip 
                    contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.05)', backgroundColor: 'rgba(255,255,255,0.9)', color: '#374151', fontSize: '12px', padding: '4px 8px' }}
                    itemStyle={{ color: '#111827', fontWeight: 'bold' }}
                    formatter={(value) => `${currencySymbol}${value}`} 
                  />
                </PieChart>
              </ResponsiveContainer>
              <div className="flex flex-wrap justify-center gap-x-3 gap-y-1.5 mt-1 overflow-y-auto max-h-12">
                {chartData.map((entry, index) => (
                  <div key={entry.name} className="flex items-center text-[10px] font-medium text-gray-600 dark:text-gray-300 bg-gray-50 dark:bg-gray-700/50 px-2 py-0.5 rounded-full">
                    <div className="w-2 h-2 rounded-full mr-1.5" style={{ backgroundColor: COLORS[index % COLORS.length] }}></div>
                    {entry.name}
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-64 text-gray-400 dark:text-gray-500">
              <p>{t('noExpenseData')}</p>
            </div>
          )}
        </div>

        <div className="bg-white p-5 rounded-3xl shadow-sm border border-gray-100 dark:bg-gray-800 dark:border-gray-700">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-bold text-gray-900 dark:text-white flex items-center">
              <FileText size={18} className="mr-2 text-purple-500" />
              {t('latestNotes')}
            </h3>
          </div>
          {notes.length > 0 ? (
            <div className="space-y-3">
              {notes.map(note => (
                <div key={note.id} className="p-4 bg-gray-50/80 rounded-2xl hover:bg-gray-100 transition-colors cursor-pointer dark:bg-gray-900/50 dark:hover:bg-gray-700 border border-transparent hover:border-gray-200 dark:hover:border-gray-600">
                  <h4 className="font-bold text-gray-900 text-sm dark:text-white line-clamp-1">{note.title}</h4>
                  <p className="text-xs text-gray-500 line-clamp-2 mt-1.5 dark:text-gray-400 leading-relaxed">{note.content}</p>
                  <div className="flex items-center justify-between mt-3 pt-3 border-t border-gray-100 dark:border-gray-700/50">
                    <span className="text-[10px] uppercase font-bold text-gray-400 dark:text-gray-500">
                      {note.createdAt ? format(new Date(note.createdAt), 'MMM d, yyyy') : ''}
                    </span>
                    {note.isVoiceNote && (
                      <span className="text-[9px] font-bold tracking-wider text-purple-600 bg-purple-50 px-2 py-1 rounded-lg dark:bg-purple-900/30 dark:text-purple-300">{t('voice')}</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-48 text-gray-400 dark:text-gray-500 bg-gray-50/50 rounded-2xl border border-dashed border-gray-200 dark:bg-gray-900/30 dark:border-gray-700">
              <FileText size={32} className="mb-2 opacity-20" />
              <p className="text-sm font-medium">{t('noNotesFound')}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
