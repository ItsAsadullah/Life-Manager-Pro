import React, { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useSettings } from '../contexts/SettingsContext';
import { collection, query, onSnapshot, orderBy, limit, getDocs } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip } from 'recharts';
import { format } from 'date-fns';

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
  const totalBorrowed = debts.filter(d => d.type === 'borrowed' && d.status === 'pending').reduce((sum, d) => sum + (d.amount - (d.totalPaid || 0)), 0);
  const totalLent = debts.filter(d => d.type === 'lent' && d.status === 'pending').reduce((sum, d) => sum + (d.amount - (d.totalPaid || 0)), 0);
  
  const categoryData = expenses.reduce((acc: any, exp) => {
    const cat = exp.category || 'Other';
    acc[cat] = (acc[cat] || 0) + exp.amount;
    return acc;
  }, {});

  const chartData = Object.keys(categoryData).map(key => ({
    name: key,
    value: categoryData[key]
  }));

  const COLORS = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6'];

  return (
    <div className="space-y-6 pb-10">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold text-gray-900">{t('dashboard')}</h2>
        <div className="hidden md:block text-sm text-gray-500">
          {t('welcomeBack')}, <span className="font-semibold text-indigo-600">{user?.displayName}</span>
        </div>
      </div>
      
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100">
          <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">{t('monthlyExpense')}</h3>
          <p className="text-2xl font-bold text-gray-900">{currencySymbol}{totalExpense.toLocaleString()}</p>
        </div>
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100">
          <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">{t('borrowed')}</h3>
          <p className="text-2xl font-bold text-red-600">{currencySymbol}{totalBorrowed.toLocaleString()}</p>
        </div>
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100">
          <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">{t('lent')}</h3>
          <p className="text-2xl font-bold text-green-600">{currencySymbol}{totalLent.toLocaleString()}</p>
        </div>
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100">
          <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">{t('notes')}</h3>
          <p className="text-2xl font-bold text-indigo-600">{notes.length}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
          <h3 className="text-lg font-bold text-gray-900 mb-4">{t('expenseAnalysis')}</h3>
          {chartData.length > 0 ? (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={chartData}
                    cx="50%"
                    cy="50%"
                    innerRadius={70}
                    outerRadius={90}
                    paddingAngle={8}
                    dataKey="value"
                    stroke="none"
                  >
                    {chartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <RechartsTooltip 
                    contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
                    formatter={(value) => `${currencySymbol}${value}`} 
                  />
                </PieChart>
              </ResponsiveContainer>
              <div className="flex flex-wrap justify-center gap-4 mt-4">
                {chartData.map((entry, index) => (
                  <div key={entry.name} className="flex items-center text-xs text-gray-500">
                    <div className="w-3 h-3 rounded-full mr-2" style={{ backgroundColor: COLORS[index % COLORS.length] }}></div>
                    {entry.name}
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-64 text-gray-400">
              <p>{t('noExpenseData')}</p>
            </div>
          )}
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
          <h3 className="text-lg font-bold text-gray-900 mb-4">{t('latestNotes')}</h3>
          {notes.length > 0 ? (
            <div className="space-y-4">
              {notes.map(note => (
                <div key={note.id} className="p-4 bg-gray-50 rounded-xl hover:bg-gray-100 transition-colors cursor-pointer">
                  <h4 className="font-bold text-gray-900 text-sm">{note.title}</h4>
                  <p className="text-xs text-gray-500 line-clamp-2 mt-1">{note.content}</p>
                  <div className="flex items-center justify-between mt-3">
                    <span className="text-[10px] text-gray-400">
                      {note.createdAt ? format(new Date(note.createdAt), 'MMM d, yyyy') : ''}
                    </span>
                    {note.isVoiceNote && (
                      <span className="text-[10px] font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">{t('voice')}</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-64 text-gray-400">
              <p>{t('noNotesFound')}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
