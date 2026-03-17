import React, { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { collection, query, onSnapshot, orderBy, addDoc, where } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Plus, Save, X, Receipt, ArrowUpCircle, ArrowDownCircle, DollarSign } from 'lucide-react';
import { format } from 'date-fns';

export const Expenses: React.FC = () => {
  const { user } = useAuth();
  const [transactions, setTransactions] = useState<any[]>([]);
  const [isAdding, setIsAdding] = useState(false);
  const [activeTab, setActiveTab] = useState<'expense' | 'income'>('expense');
  
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('Food');
  const [source, setSource] = useState('Salary');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [paymentMethod, setPaymentMethod] = useState('Cash');

  useEffect(() => {
    if (!user) return;
    
    // Listen to expenses
    const expensesRef = collection(db, 'users', user.uid, 'expenses');
    const unsubscribeExpenses = onSnapshot(query(expensesRef, orderBy('date', 'desc')), (snapshot) => {
      const exps = snapshot.docs.map(doc => ({ id: doc.id, type: 'expense', ...doc.data() }));
      updateTransactions(exps, 'expense');
    });

    // Listen to income
    const incomeRef = collection(db, 'users', user.uid, 'income');
    const unsubscribeIncome = onSnapshot(query(incomeRef, orderBy('date', 'desc')), (snapshot) => {
      const incs = snapshot.docs.map(doc => ({ id: doc.id, type: 'income', ...doc.data() }));
      updateTransactions(incs, 'income');
    });

    return () => {
      unsubscribeExpenses();
      unsubscribeIncome();
    };
  }, [user]);

  const [allExps, setAllExps] = useState<any[]>([]);
  const [allIncs, setAllIncs] = useState<any[]>([]);

  const updateTransactions = (data: any[], type: 'expense' | 'income') => {
    if (type === 'expense') setAllExps(data);
    else setAllIncs(data);
  };

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

  const categories = ['Food', 'Transport', 'Shopping', 'Bills', 'Entertainment', 'Health', 'Other'];
  const incomeSources = ['Salary', 'Freelance', 'Business', 'Gift', 'Investment', 'Other'];
  const paymentMethods = ['Cash', 'bKash', 'Nagad', 'Card', 'Bank Transfer'];

  const totalIncome = allIncs.reduce((sum, t) => sum + t.amount, 0);
  const totalExpense = allExps.reduce((sum, t) => sum + t.amount, 0);

  return (
    <div className="space-y-6 pb-10">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Transactions</h2>
          <p className="text-gray-500 text-sm">Manage your income and expenses</p>
        </div>
        <button
          onClick={() => setIsAdding(true)}
          className="flex items-center px-4 py-2 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 shadow-lg shadow-indigo-100"
        >
          <Plus size={20} className="mr-2" />
          Add New
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-emerald-100 flex items-center">
          <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center mr-4">
            <ArrowUpCircle className="text-emerald-600" size={24} />
          </div>
          <div>
            <h3 className="text-xs font-medium text-gray-500 uppercase tracking-wider">Total Income</h3>
            <p className="text-2xl font-bold text-emerald-600">৳{totalIncome.toLocaleString()}</p>
          </div>
        </div>
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-red-100 flex items-center">
          <div className="w-12 h-12 rounded-full bg-red-50 flex items-center justify-center mr-4">
            <ArrowDownCircle className="text-red-600" size={24} />
          </div>
          <div>
            <h3 className="text-xs font-medium text-gray-500 uppercase tracking-wider">Total Expenses</h3>
            <p className="text-2xl font-bold text-red-600">৳{totalExpense.toLocaleString()}</p>
          </div>
        </div>
      </div>

      {isAdding && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden animate-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center p-6 border-b">
              <h3 className="text-lg font-bold">Add Transaction</h3>
              <button onClick={() => setIsAdding(false)} className="text-gray-400 hover:text-gray-600">
                <X size={24} />
              </button>
            </div>
            
            <form onSubmit={handleSave} className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4 p-1 bg-gray-100 rounded-lg">
                <button
                  type="button"
                  onClick={() => setActiveTab('expense')}
                  className={`py-2 text-sm font-medium rounded-md transition-all ${
                    activeTab === 'expense' ? 'bg-white text-red-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'
                  }`}
                >
                  Expense
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab('income')}
                  className={`py-2 text-sm font-medium rounded-md transition-all ${
                    activeTab === 'income' ? 'bg-white text-emerald-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'
                  }`}
                >
                  Income
                </button>
              </div>

              <div className="grid grid-cols-1 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Amount (৳)</label>
                  <div className="relative">
                    <DollarSign className="absolute left-3 top-2.5 text-gray-400" size={18} />
                    <input
                      type="number"
                      required
                      min="0"
                      step="0.01"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      placeholder="0.00"
                    />
                  </div>
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Date</label>
                  <input
                    type="date"
                    required
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  />
                </div>

                {activeTab === 'expense' ? (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
                      <select
                        value={category}
                        onChange={(e) => setCategory(e.target.value)}
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      >
                        {categories.map(c => <option key={c} value={c}>{c}</option>)}
                      </select>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">Payment Method</label>
                      <select
                        value={paymentMethod}
                        onChange={(e) => setPaymentMethod(e.target.value)}
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      >
                        {paymentMethods.map(p => <option key={p} value={p}>{p}</option>)}
                      </select>
                    </div>
                  </>
                ) : (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Source</label>
                    <select
                      value={source}
                      onChange={(e) => setSource(e.target.value)}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    >
                      {incomeSources.map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </div>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <input
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder={activeTab === 'expense' ? "What did you spend on?" : "Where did this come from?"}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
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
                Save {activeTab === 'expense' ? 'Expense' : 'Income'}
              </button>
            </form>
          </div>
        </div>
      )}

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Category/Source</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Description</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Amount</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {transactions.map((t) => (
                <tr key={t.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {t.date ? format(new Date(t.date), 'MMM d, yyyy') : ''}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 inline-flex text-[10px] leading-5 font-bold uppercase rounded-full ${
                      t.type === 'expense' ? 'bg-red-100 text-red-800' : 'bg-emerald-100 text-emerald-800'
                    }`}>
                      {t.type}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {t.type === 'expense' ? t.category : t.source}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {t.description || '-'}
                  </td>
                  <td className={`px-6 py-4 whitespace-nowrap text-sm font-bold text-right ${
                    t.type === 'expense' ? 'text-red-600' : 'text-emerald-600'
                  }`}>
                    {t.type === 'expense' ? '-' : '+'}৳{t.amount.toLocaleString()}
                  </td>
                </tr>
              ))}
              {transactions.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-gray-500">
                    <Receipt className="mx-auto h-12 w-12 text-gray-300 mb-3" />
                    <p>No transactions found. Add your first entry!</p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

