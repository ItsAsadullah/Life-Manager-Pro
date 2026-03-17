import React, { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useAuth } from '../contexts/AuthContext';
import { useSettings } from '../contexts/SettingsContext';
import { useLocation } from 'react-router-dom';
import { collection, query, onSnapshot, orderBy, addDoc, deleteDoc, doc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Plus, Save, X, Receipt, ArrowUpCircle, ArrowDownCircle, DollarSign, Trash2 } from 'lucide-react';
import { format } from 'date-fns';
import { SwipeableNumberInput } from '../components/SwipeableNumberInput';

export const Expenses: React.FC = () => {
  const { user } = useAuth();
  const { t, currencySymbol } = useSettings();
  const location = useLocation();
  const [transactions, setTransactions] = useState<any[]>([]);
  const [isAdding, setIsAdding] = useState(false);
  const [activeTab, setActiveTab] = useState<'expense' | 'income'>('expense');
  const [transactionToDelete, setTransactionToDelete] = useState<{id: string, type: string} | null>(null);

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

    return () => {
      unsubscribeExpenses();
      unsubscribeIncome();
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

  const categories = ['food', 'transport', 'shopping', 'bills', 'entertainment', 'health', 'other'];
  const incomeSources = ['salary', 'freelance', 'business', 'gift', 'investment', 'other'];
  const paymentMethods = ['cash', 'bkash', 'nagad', 'card', 'bankTransfer'];

  const totalIncome = allIncs.reduce((sum, item) => sum + item.amount, 0);
  const totalExpense = allExps.reduce((sum, item) => sum + item.amount, 0);

  return (
    <div className="space-y-6 pb-10">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">{t('expenses')}</h2>
          <p className="text-gray-500 text-sm">{t('manageIncomeExpenses')}</p>
        </div>
        <button
          onClick={() => setIsAdding(true)}
          className="flex items-center px-4 py-2 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 shadow-lg shadow-indigo-100"
        >
          <Plus size={20} className="mr-2" />
          {t('addNew')}
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-emerald-100 flex items-center">
          <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center mr-4">
            <ArrowUpCircle className="text-emerald-600" size={24} />
          </div>
          <div>
            <h3 className="text-xs font-medium text-gray-500 uppercase tracking-wider">{t('monthlyIncome')}</h3>
            <p className="text-2xl font-bold text-emerald-600">{currencySymbol}{totalIncome.toLocaleString()}</p>
          </div>
        </div>
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-red-100 flex items-center">
          <div className="w-12 h-12 rounded-full bg-red-50 flex items-center justify-center mr-4">
            <ArrowDownCircle className="text-red-600" size={24} />
          </div>
          <div>
            <h3 className="text-xs font-medium text-gray-500 uppercase tracking-wider">{t('monthlyExpense')}</h3>
            <p className="text-2xl font-bold text-red-600">{currencySymbol}{totalExpense.toLocaleString()}</p>
          </div>
        </div>
      </div>

      {isAdding && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden animate-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center p-6 border-b">
              <h3 className="text-lg font-bold">{t('addTransaction')}</h3>
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
                  {t('expense')}
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab('income')}
                  className={`py-2 text-sm font-medium rounded-md transition-all ${
                    activeTab === 'income' ? 'bg-white text-emerald-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'
                  }`}
                >
                  {t('income')}
                </button>
              </div>

              <div className="grid grid-cols-1 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('amount')} ({currencySymbol})</label>
                  <div className="relative">
                    <DollarSign className="absolute left-3 top-2.5 text-gray-400" size={18} />
                    <SwipeableNumberInput
                      required
                      value={amount}
                      onChange={setAmount}
                      className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      placeholder="0.00"
                      isPrice={true}
                    />
                  </div>
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{t('date')}</label>
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
                      <label className="block text-sm font-medium text-gray-700 mb-1">{t('category')}</label>
                      <select
                        value={category}
                        onChange={(e) => setCategory(e.target.value)}
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      >
                        {categories.map(c => <option key={c} value={c}>{t(c)}</option>)}
                      </select>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">{t('paymentMethod')}</label>
                      <select
                        value={paymentMethod}
                        onChange={(e) => setPaymentMethod(e.target.value)}
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      >
                        {paymentMethods.map(p => <option key={p} value={p}>{t(p)}</option>)}
                      </select>
                    </div>
                  </>
                ) : (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">{t('source')}</label>
                    <select
                      value={source}
                      onChange={(e) => setSource(e.target.value)}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    >
                      {incomeSources.map(s => <option key={s} value={s}>{t(s)}</option>)}
                    </select>
                  </div>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t('description')}</label>
                <input
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder={activeTab === 'expense' ? t('expensePlaceholder') : t('incomePlaceholder')}
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
                {t('save')} {activeTab === 'expense' ? t('expense') : t('income')}
              </button>
            </form>
          </div>
        </div>,
        document.body
      )}

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('date')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('type')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('categorySource')}</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">{t('description')}</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">{t('amount')}</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">{t('actions')}</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {transactions.map((transaction) => (
                <tr key={transaction.id} className="hover:bg-gray-50 transition-colors group">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {transaction.date ? format(new Date(transaction.date), 'MMM d, yyyy') : ''}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 inline-flex text-[10px] leading-5 font-bold uppercase rounded-full ${
                      transaction.type === 'expense' ? 'bg-red-100 text-red-800' : 'bg-emerald-100 text-emerald-800'
                    }`}>
                      {transaction.type === 'expense' ? t('expense') : t('income')}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {transaction.type === 'expense' ? t(transaction.category) : t(transaction.source)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {transaction.description || '-'}
                  </td>
                  <td className={`px-6 py-4 whitespace-nowrap text-sm font-bold text-right ${
                    transaction.type === 'expense' ? 'text-red-600' : 'text-emerald-600'
                  }`}>
                    {transaction.type === 'expense' ? '-' : '+'}{currencySymbol}{transaction.amount.toLocaleString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button
                      onClick={() => setTransactionToDelete({ id: transaction.id, type: transaction.type })}
                      className="text-red-400 hover:text-red-600 p-1 rounded-lg hover:bg-red-50 transition-colors opacity-0 group-hover:opacity-100"
                    >
                      <Trash2 size={18} />
                    </button>
                  </td>
                </tr>
              ))}
              {transactions.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                    <Receipt className="mx-auto h-12 w-12 text-gray-300 mb-3" />
                    <p>{t('noTransactions')}</p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {transactionToDelete && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl p-6 max-w-sm w-full relative animate-in zoom-in-95 duration-200">
            <h3 className="text-xl font-bold text-gray-900 mb-2">{t('delete')}</h3>
            <p className="text-gray-600 mb-6">{t('confirmDelete')}</p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setTransactionToDelete(null)}
                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors font-medium"
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
    </div>
  );
};

