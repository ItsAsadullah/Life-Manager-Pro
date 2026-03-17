import React, { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { collection, query, onSnapshot, orderBy, addDoc, deleteDoc, doc, updateDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Plus, Save, X, Trash2, Edit2, CheckCircle, Clock, User, Phone, DollarSign, Calendar } from 'lucide-react';
import { format } from 'date-fns';

interface Debt {
  id: string;
  personName: string;
  phoneNumber?: string;
  amount: number;
  type: 'borrowed' | 'lent';
  dueDate?: string;
  status: 'pending' | 'paid';
  createdAt: string;
}

export const Debts: React.FC = () => {
  const { user } = useAuth();
  const [debts, setDebts] = useState<Debt[]>([]);
  const [isAdding, setIsAdding] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // Form state
  const [personName, setPersonName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [amount, setAmount] = useState('');
  const [type, setType] = useState<'borrowed' | 'lent'>('borrowed');
  const [dueDate, setDueDate] = useState('');
  const [status, setStatus] = useState<'pending' | 'paid'>('pending');

  useEffect(() => {
    if (!user) return;
    const debtsRef = collection(db, 'users', user.uid, 'debts');
    const q = query(debtsRef, orderBy('createdAt', 'desc'));
    
    const unsubscribe = onSnapshot(q, (snapshot) => {
      setDebts(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Debt)));
      setLoading(false);
    });

    return () => unsubscribe();
  }, [user]);

  const resetForm = () => {
    setPersonName('');
    setPhoneNumber('');
    setAmount('');
    setType('borrowed');
    setDueDate('');
    setStatus('pending');
    setEditingId(null);
    setIsAdding(false);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !personName.trim() || !amount) return;

    const debtData = {
      personName,
      phoneNumber,
      amount: Number(amount),
      type,
      dueDate: dueDate || null,
      status,
      updatedAt: new Date().toISOString()
    };

    try {
      if (editingId) {
        await updateDoc(doc(db, 'users', user.uid, 'debts', editingId), debtData);
      } else {
        await addDoc(collection(db, 'users', user.uid, 'debts'), {
          ...debtData,
          createdAt: new Date().toISOString()
        });
      }
      resetForm();
    } catch (error) {
      console.error('Error saving debt:', error);
      alert('Failed to save debt record.');
    }
  };

  const handleEdit = (debt: Debt) => {
    setEditingId(debt.id);
    setPersonName(debt.personName);
    setPhoneNumber(debt.phoneNumber || '');
    setAmount(String(debt.amount));
    setType(debt.type);
    setDueDate(debt.dueDate || '');
    setStatus(debt.status);
    setIsAdding(true);
  };

  const handleDelete = async (id: string) => {
    if (!user || !window.confirm('Are you sure you want to delete this record?')) return;
    try {
      await deleteDoc(doc(db, 'users', user.uid, 'debts', id));
    } catch (error) {
      console.error('Error deleting debt:', error);
      alert('Failed to delete record.');
    }
  };

  const toggleStatus = async (debt: Debt) => {
    if (!user) return;
    const newStatus = debt.status === 'pending' ? 'paid' : 'pending';
    try {
      await updateDoc(doc(db, 'users', user.uid, 'debts', debt.id), {
        status: newStatus,
        updatedAt: new Date().toISOString()
      });
    } catch (error) {
      console.error('Error updating status:', error);
    }
  };

  const totalBorrowed = debts
    .filter(d => d.type === 'borrowed' && d.status === 'pending')
    .reduce((sum, d) => sum + d.amount, 0);
    
  const totalLent = debts
    .filter(d => d.type === 'lent' && d.status === 'pending')
    .reduce((sum, d) => sum + d.amount, 0);

  if (loading) return <div className="flex justify-center items-center h-64">Loading...</div>;

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Debt Management</h2>
          <p className="text-gray-600">Track money you owe or are owed</p>
        </div>
        <button
          onClick={() => setIsAdding(true)}
          className="flex items-center px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
        >
          <Plus size={20} className="mr-2" />
          Add Record
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-red-50 p-6 rounded-xl border border-red-100">
          <h3 className="text-sm font-medium text-red-800 mb-1">Total Borrowed (Pending)</h3>
          <p className="text-3xl font-bold text-red-600">৳{totalBorrowed.toLocaleString()}</p>
        </div>
        <div className="bg-green-50 p-6 rounded-xl border border-green-100">
          <h3 className="text-sm font-medium text-green-800 mb-1">Total Lent (Pending)</h3>
          <p className="text-3xl font-bold text-green-600">৳{totalLent.toLocaleString()}</p>
        </div>
      </div>

      {isAdding && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden animate-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center p-6 border-b">
              <h3 className="text-lg font-bold text-gray-900">{editingId ? 'Edit Record' : 'Add New Record'}</h3>
              <button onClick={resetForm} className="text-gray-400 hover:text-gray-600">
                <X size={24} />
              </button>
            </div>
            
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4 p-1 bg-gray-100 rounded-lg">
                <button
                  type="button"
                  onClick={() => setType('borrowed')}
                  className={`py-2 text-sm font-medium rounded-md transition-all ${
                    type === 'borrowed' ? 'bg-white text-red-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'
                  }`}
                >
                  Borrowed
                </button>
                <button
                  type="button"
                  onClick={() => setType('lent')}
                  className={`py-2 text-sm font-medium rounded-md transition-all ${
                    type === 'lent' ? 'bg-white text-green-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'
                  }`}
                >
                  Lent
                </button>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Person Name</label>
                <div className="relative">
                  <User className="absolute left-3 top-2.5 text-gray-400" size={18} />
                  <input
                    type="text"
                    required
                    value={personName}
                    onChange={(e) => setPersonName(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    placeholder="Enter name"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Phone Number (Optional)</label>
                <div className="relative">
                  <Phone className="absolute left-3 top-2.5 text-gray-400" size={18} />
                  <input
                    type="tel"
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    placeholder="Enter phone number"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Amount (৳)</label>
                <div className="relative">
                  <DollarSign className="absolute left-3 top-2.5 text-gray-400" size={18} />
                  <input
                    type="number"
                    required
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                    placeholder="0.00"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Due Date (Optional)</label>
                <div className="relative">
                  <Calendar className="absolute left-3 top-2.5 text-gray-400" size={18} />
                  <input
                    type="date"
                    value={dueDate}
                    onChange={(e) => setDueDate(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as 'pending' | 'paid')}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                >
                  <option value="pending">Pending</option>
                  <option value="paid">Paid</option>
                </select>
              </div>

              <button
                type="submit"
                className="w-full flex items-center justify-center py-3 px-4 border border-transparent rounded-xl shadow-sm text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-all"
              >
                <Save size={20} className="mr-2" />
                {editingId ? 'Update Record' : 'Save Record'}
              </button>
            </form>
          </div>
        </div>
      )}

      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Person</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Amount</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Due Date</th>
                <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {debts.map((debt) => (
                <tr key={debt.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className={`w-8 h-8 rounded-full flex items-center justify-center mr-3 ${
                        debt.type === 'borrowed' ? 'bg-red-100 text-red-600' : 'bg-green-100 text-green-600'
                      }`}>
                        <User size={16} />
                      </div>
                      <div>
                        <div className="text-sm font-medium text-gray-900">{debt.personName}</div>
                        {debt.phoneNumber && <div className="text-xs text-gray-500">{debt.phoneNumber}</div>}
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-1 text-xs font-medium rounded-full ${
                      debt.type === 'borrowed' ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-700'
                    }`}>
                      {debt.type === 'borrowed' ? 'Borrowed' : 'Lent'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right">
                    <div className={`text-sm font-bold ${
                      debt.type === 'borrowed' ? 'text-red-600' : 'text-green-600'
                    }`}>
                      ৳{debt.amount.toLocaleString()}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center text-sm text-gray-500">
                      <Clock size={14} className="mr-1" />
                      {debt.dueDate ? format(new Date(debt.dueDate), 'MMM d, yyyy') : 'No date'}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-center">
                    <button
                      onClick={() => toggleStatus(debt)}
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium transition-colors ${
                        debt.status === 'paid' 
                          ? 'bg-indigo-100 text-indigo-800' 
                          : 'bg-yellow-100 text-yellow-800'
                      }`}
                    >
                      {debt.status === 'paid' ? (
                        <><CheckCircle size={12} className="mr-1" /> Paid</>
                      ) : (
                        <><Clock size={12} className="mr-1" /> Pending</>
                      )}
                    </button>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div className="flex justify-end space-x-3">
                      <button onClick={() => handleEdit(debt)} className="text-blue-600 hover:text-blue-900">
                        <Edit2 size={18} />
                      </button>
                      <button onClick={() => handleDelete(debt.id)} className="text-red-600 hover:text-red-900">
                        <Trash2 size={18} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {debts.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                    No debt records found.
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
