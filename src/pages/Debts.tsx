import React, { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useLocation } from 'react-router-dom';
import { collection, query, onSnapshot, orderBy, addDoc, deleteDoc, doc, updateDoc, getDocs } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Plus, Save, X, Trash2, Edit2, CheckCircle, Clock, User, Phone, DollarSign, Calendar, History, ArrowDownCircle, ArrowUpCircle, ChevronDown, ChevronUp } from 'lucide-react';
import { format } from 'date-fns';

interface Repayment {
  id: string;
  amount: number;
  date: string;
  note?: string;
  createdAt: string;
}

interface Debt {
  id: string;
  personName: string;
  phoneNumber?: string;
  amount: number;
  type: 'borrowed' | 'lent';
  dueDate?: string;
  status: 'pending' | 'paid';
  createdAt: string;
  updatedAt?: string;
  totalPaid?: number;
  repayments?: Repayment[];
}

export const Debts: React.FC = () => {
  const { user } = useAuth();
  const location = useLocation();
  const [debts, setDebts] = useState<Debt[]>([]);
  const [isAdding, setIsAdding] = useState(false);

  useEffect(() => {
    if (location.state?.openAddModal) {
      setIsAdding(true);
      window.history.replaceState({}, document.title);
    }
  }, [location]);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // Repayment state
  const [isAddingRepayment, setIsAddingRepayment] = useState<string | null>(null);
  const [repaymentAmount, setRepaymentAmount] = useState('');
  const [repaymentDate, setRepaymentDate] = useState(new Date().toISOString().split('T')[0]);
  const [repaymentNote, setRepaymentNote] = useState('');

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
    
    const unsubscribe = onSnapshot(q, async (snapshot) => {
      const debtsData = await Promise.all(snapshot.docs.map(async (debtDoc) => {
        const repaymentsRef = collection(db, 'users', user.uid!, 'debts', debtDoc.id, 'repayments');
        const repaymentsSnap = await getDocs(query(repaymentsRef, orderBy('date', 'desc')));
        const repayments = repaymentsSnap.docs.map(d => ({ id: d.id, ...d.data() } as Repayment));
        
        return { 
          id: debtDoc.id, 
          ...debtDoc.data(),
          repayments 
        } as Debt;
      }));
      
      setDebts(debtsData);
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
      dueDate: dueDate ? new Date(dueDate).toISOString() : null,
      status,
      totalPaid: 0,
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

  const handleAddRepayment = async (debtId: string) => {
    if (!user) return;
    if (!repaymentAmount) {
      alert('Please enter repayment amount');
      return;
    }
    
    try {
      const repaymentData = {
        amount: Number(repaymentAmount),
        date: new Date(repaymentDate).toISOString(),
        note: repaymentNote,
        createdAt: new Date().toISOString()
      };
      
      await addDoc(collection(db, 'users', user.uid, 'debts', debtId, 'repayments'), repaymentData);
      
      // Update parent debt to trigger onSnapshot listener and update totalPaid
      const debt = debts.find(d => d.id === debtId);
      if (debt) {
        const currentTotalPaid = debt.totalPaid !== undefined ? debt.totalPaid : (debt.repayments?.reduce((sum, r) => sum + r.amount, 0) || 0);
        const newTotalPaid = currentTotalPaid + Number(repaymentAmount);
        const newStatus = newTotalPaid >= debt.amount ? 'paid' : 'pending';
        await updateDoc(doc(db, 'users', user.uid, 'debts', debtId), { 
          status: newStatus,
          totalPaid: newTotalPaid,
          updatedAt: new Date().toISOString()
        });
      }
      
      setIsAddingRepayment(null);
      setRepaymentAmount('');
      setRepaymentNote('');
      alert('Payment added successfully!');
    } catch (error) {
      console.error('Error adding repayment:', error);
      alert('Failed to add payment. Please try again.');
    }
  };

  const handleDeleteRepayment = async (debtId: string, repaymentId: string) => {
    if (!user || !window.confirm('Delete this payment record?')) return;
    try {
      const debt = debts.find(d => d.id === debtId);
      const repaymentToDelete = debt?.repayments?.find(r => r.id === repaymentId);
      
      await deleteDoc(doc(db, 'users', user.uid, 'debts', debtId, 'repayments', repaymentId));
      
      // Update parent debt to trigger onSnapshot listener and update totalPaid
      if (debt && repaymentToDelete) {
        const currentTotalPaid = debt.totalPaid !== undefined ? debt.totalPaid : (debt.repayments?.reduce((sum, r) => sum + r.amount, 0) || 0);
        const newTotalPaid = Math.max(0, currentTotalPaid - repaymentToDelete.amount);
        await updateDoc(doc(db, 'users', user.uid, 'debts', debtId), { 
          totalPaid: newTotalPaid,
          status: newTotalPaid >= debt.amount ? 'paid' : 'pending',
          updatedAt: new Date().toISOString()
        });
      }
    } catch (error) {
      console.error('Error deleting repayment:', error);
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
    .reduce((sum, d) => {
      const paid = d.totalPaid !== undefined ? d.totalPaid : (d.repayments?.reduce((s, r) => s + r.amount, 0) || 0);
      return sum + (d.amount - paid);
    }, 0);
    
  const totalLent = debts
    .filter(d => d.type === 'lent' && d.status === 'pending')
    .reduce((sum, d) => {
      const paid = d.totalPaid !== undefined ? d.totalPaid : (d.repayments?.reduce((s, r) => s + r.amount, 0) || 0);
      return sum + (d.amount - paid);
    }, 0);

  if (loading) return <div className="flex justify-center items-center h-64">Loading...</div>;

  return (
    <div className="space-y-6 pb-10">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Debt Management</h2>
          <p className="text-gray-600">Track money you owe or are owed</p>
        </div>
        <button
          onClick={() => setIsAdding(true)}
          className="w-full md:w-auto flex items-center justify-center px-4 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition-colors shadow-lg shadow-indigo-200"
        >
          <Plus size={20} className="mr-2" />
          Add Record
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-red-100 flex items-center">
          <div className="w-12 h-12 rounded-full bg-red-50 flex items-center justify-center mr-4">
            <ArrowDownCircle className="text-red-600" size={24} />
          </div>
          <div>
            <h3 className="text-xs font-medium text-gray-500 uppercase tracking-wider">Total Borrowed</h3>
            <p className="text-2xl font-bold text-red-600">৳{totalBorrowed.toLocaleString()}</p>
          </div>
        </div>
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-green-100 flex items-center">
          <div className="w-12 h-12 rounded-full bg-green-50 flex items-center justify-center mr-4">
            <ArrowUpCircle className="text-green-600" size={24} />
          </div>
          <div>
            <h3 className="text-xs font-medium text-gray-500 uppercase tracking-wider">Total Lent</h3>
            <p className="text-2xl font-bold text-green-600">৳{totalLent.toLocaleString()}</p>
          </div>
        </div>
      </div>

      {isAdding && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg overflow-hidden animate-in zoom-in-95 duration-200">
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

      {/* Desktop Table View */}
      <div className="hidden md:block bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
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
            {debts.map((debt) => {
              const totalPaid = debt.totalPaid !== undefined ? debt.totalPaid : (debt.repayments?.reduce((sum, r) => sum + r.amount, 0) || 0);
              const remaining = debt.amount - totalPaid;
              
              return (
                <React.Fragment key={debt.id}>
                  <tr className="hover:bg-gray-50 transition-colors">
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
                      {totalPaid > 0 && (
                        <div className="text-[10px] text-gray-400">
                          Paid: ৳{totalPaid.toLocaleString()} | Rem: ৳{remaining.toLocaleString()}
                        </div>
                      )}
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
                        <button 
                          onClick={() => setExpandedId(expandedId === debt.id ? null : debt.id)}
                          className="text-gray-400 hover:text-indigo-600"
                        >
                          <History size={18} />
                        </button>
                        <button onClick={() => handleEdit(debt)} className="text-blue-600 hover:text-blue-900">
                          <Edit2 size={18} />
                        </button>
                        <button onClick={() => handleDelete(debt.id)} className="text-red-600 hover:text-red-900">
                          <Trash2 size={18} />
                        </button>
                      </div>
                    </td>
                  </tr>
                  {expandedId === debt.id && (
                    <tr className="bg-gray-50">
                      <td colSpan={6} className="px-6 py-4">
                        <div className="space-y-4">
                          <div className="flex justify-between items-center">
                            <h4 className="text-sm font-bold text-gray-700">Repayment History</h4>
                            <button 
                              onClick={() => setIsAddingRepayment(debt.id)}
                              className="text-xs font-medium text-indigo-600 hover:text-indigo-800 flex items-center"
                            >
                              <Plus size={14} className="mr-1" /> Add Payment
                            </button>
                          </div>
                          
                          {isAddingRepayment === debt.id && (
                            <div className="bg-white p-4 rounded-lg border border-gray-200 flex flex-wrap gap-3 items-end">
                              <div className="flex-1 min-w-[120px]">
                                <label className="block text-[10px] uppercase text-gray-400 mb-1">Amount</label>
                                <input 
                                  type="number" 
                                  value={repaymentAmount}
                                  onChange={(e) => setRepaymentAmount(e.target.value)}
                                  className="w-full px-3 py-1.5 border rounded text-sm"
                                  placeholder="Amount"
                                />
                              </div>
                              <div className="flex-1 min-w-[120px]">
                                <label className="block text-[10px] uppercase text-gray-400 mb-1">Date</label>
                                <input 
                                  type="date" 
                                  value={repaymentDate}
                                  onChange={(e) => setRepaymentDate(e.target.value)}
                                  className="w-full px-3 py-1.5 border rounded text-sm"
                                />
                              </div>
                              <div className="flex-[2] min-w-[200px]">
                                <label className="block text-[10px] uppercase text-gray-400 mb-1">Note</label>
                                <input 
                                  type="text" 
                                  value={repaymentNote}
                                  onChange={(e) => setRepaymentNote(e.target.value)}
                                  className="w-full px-3 py-1.5 border rounded text-sm"
                                  placeholder="Optional note"
                                />
                              </div>
                              <div className="flex gap-2">
                                <button 
                                  onClick={() => handleAddRepayment(debt.id)}
                                  className="bg-indigo-600 text-white px-4 py-1.5 rounded text-sm font-medium"
                                >
                                  Add
                                </button>
                                <button 
                                  onClick={() => setIsAddingRepayment(null)}
                                  className="bg-gray-100 text-gray-600 px-4 py-1.5 rounded text-sm font-medium"
                                >
                                  Cancel
                                </button>
                              </div>
                            </div>
                          )}

                          <div className="space-y-2">
                            {debt.repayments?.map(repayment => (
                              <div key={repayment.id} className="flex justify-between items-center bg-white p-3 rounded-lg border border-gray-100">
                                <div className="flex items-center">
                                  <div className="w-8 h-8 rounded-full bg-indigo-50 flex items-center justify-center mr-3">
                                    <DollarSign size={14} className="text-indigo-600" />
                                  </div>
                                  <div>
                                    <div className="text-sm font-medium text-gray-900">৳{repayment.amount.toLocaleString()}</div>
                                    <div className="text-xs text-gray-400">{format(new Date(repayment.date), 'MMM d, yyyy')} {repayment.note && `• ${repayment.note}`}</div>
                                  </div>
                                </div>
                                <button 
                                  onClick={() => handleDeleteRepayment(debt.id, repayment.id)}
                                  className="text-gray-300 hover:text-red-600"
                                >
                                  <Trash2 size={14} />
                                </button>
                              </div>
                            ))}
                            {(!debt.repayments || debt.repayments.length === 0) && (
                              <p className="text-xs text-gray-400 text-center py-2">No payment history yet.</p>
                            )}
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Mobile Card View */}
      <div className="md:hidden space-y-4">
        {debts.map((debt) => {
          const totalPaid = debt.totalPaid !== undefined ? debt.totalPaid : (debt.repayments?.reduce((sum, r) => sum + r.amount, 0) || 0);
          const remaining = debt.amount - totalPaid;
          const isExpanded = expandedId === debt.id;

          return (
            <div key={debt.id} className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
              <div className="p-4">
                <div className="flex justify-between items-start mb-3">
                  <div className="flex items-center">
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center mr-3 ${
                      debt.type === 'borrowed' ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-600'
                    }`}>
                      <User size={20} />
                    </div>
                    <div>
                      <h4 className="font-bold text-gray-900">{debt.personName}</h4>
                      <p className="text-xs text-gray-500">{debt.phoneNumber || 'No phone'}</p>
                    </div>
                  </div>
                  <span className={`px-2 py-1 text-[10px] font-bold uppercase rounded-md ${
                    debt.type === 'borrowed' ? 'bg-red-50 text-red-700' : 'bg-green-50 text-green-700'
                  }`}>
                    {debt.type === 'borrowed' ? 'Borrowed' : 'Lent'}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-4 mb-4">
                  <div>
                    <p className="text-[10px] text-gray-400 uppercase font-medium">Total Amount</p>
                    <p className={`text-lg font-bold ${debt.type === 'borrowed' ? 'text-red-600' : 'text-green-600'}`}>
                      ৳{debt.amount.toLocaleString()}
                    </p>
                  </div>
                  <div>
                    <p className="text-[10px] text-gray-400 uppercase font-medium">Remaining</p>
                    <p className="text-lg font-bold text-gray-900">৳{remaining.toLocaleString()}</p>
                  </div>
                </div>

                <div className="flex items-center justify-between pt-3 border-t border-gray-50">
                  <div className="flex items-center text-xs text-gray-500">
                    <Calendar size={14} className="mr-1" />
                    {debt.dueDate ? format(new Date(debt.dueDate), 'MMM d, yyyy') : 'No due date'}
                  </div>
                  <button
                    onClick={() => toggleStatus(debt)}
                    className={`px-3 py-1 rounded-full text-[10px] font-bold uppercase ${
                      debt.status === 'paid' ? 'bg-indigo-50 text-indigo-600' : 'bg-yellow-50 text-yellow-600'
                    }`}
                  >
                    {debt.status}
                  </button>
                </div>

                <div className="flex items-center justify-between mt-4 gap-2">
                  <button 
                    onClick={() => {
                      setExpandedId(isExpanded ? null : debt.id);
                      if (!isExpanded) setIsAddingRepayment(null);
                    }}
                    className="flex-1 flex items-center justify-center py-2 bg-indigo-50 text-indigo-700 rounded-lg text-xs font-bold"
                  >
                    {debt.type === 'borrowed' ? 'Repay' : 'Receive'} & History {isExpanded ? <ChevronUp size={14} className="ml-1" /> : <ChevronDown size={14} className="ml-1" />}
                  </button>
                  <button 
                    onClick={() => handleEdit(debt)}
                    className="p-2 bg-blue-50 text-blue-600 rounded-lg"
                  >
                    <Edit2 size={16} />
                  </button>
                  <button 
                    onClick={() => handleDelete(debt.id)}
                    className="p-2 bg-red-50 text-red-600 rounded-lg"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>

              {isExpanded && (
                <div className="bg-gray-50 p-4 border-t border-gray-100 space-y-4">
                  <div className="flex justify-between items-center">
                    <h5 className="text-xs font-bold text-gray-700 uppercase">Repayment History</h5>
                    <button 
                      onClick={() => setIsAddingRepayment(debt.id)}
                      className="text-[10px] font-bold text-indigo-600 uppercase"
                    >
                      + Add Payment
                    </button>
                  </div>

                  {isAddingRepayment === debt.id && (
                    <div className="bg-white p-4 rounded-xl shadow-sm space-y-3">
                      <input 
                        type="number" 
                        value={repaymentAmount}
                        onChange={(e) => setRepaymentAmount(e.target.value)}
                        className="w-full px-3 py-2 border rounded-lg text-sm"
                        placeholder="Amount"
                      />
                      <input 
                        type="date" 
                        value={repaymentDate}
                        onChange={(e) => setRepaymentDate(e.target.value)}
                        className="w-full px-3 py-2 border rounded-lg text-sm"
                      />
                      <input 
                        type="text" 
                        value={repaymentNote}
                        onChange={(e) => setRepaymentNote(e.target.value)}
                        className="w-full px-3 py-2 border rounded-lg text-sm"
                        placeholder="Note (Optional)"
                      />
                      <div className="flex gap-2">
                        <button 
                          onClick={() => handleAddRepayment(debt.id)}
                          className="flex-1 bg-indigo-600 text-white py-2 rounded-lg text-xs font-bold"
                        >
                          Save
                        </button>
                        <button 
                          onClick={() => setIsAddingRepayment(null)}
                          className="flex-1 bg-gray-100 text-gray-600 py-2 rounded-lg text-xs font-bold"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  )}

                  <div className="space-y-2">
                    {debt.repayments?.map(repayment => (
                      <div key={repayment.id} className="flex justify-between items-center bg-white p-3 rounded-xl border border-gray-100">
                        <div>
                          <p className="text-sm font-bold text-gray-900">৳{repayment.amount.toLocaleString()}</p>
                          <p className="text-[10px] text-gray-400">{format(new Date(repayment.date), 'MMM d, yyyy')} {repayment.note && `• ${repayment.note}`}</p>
                        </div>
                        <button 
                          onClick={() => handleDeleteRepayment(debt.id, repayment.id)}
                          className="p-1.5 text-gray-300"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    ))}
                    {(!debt.repayments || debt.repayments.length === 0) && (
                      <p className="text-[10px] text-gray-400 text-center py-2 italic">No payment history found.</p>
                    )}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {debts.length === 0 && (
        <div className="bg-white rounded-2xl p-12 text-center border border-dashed border-gray-200">
          <div className="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mx-auto mb-4">
            <HandCoins className="text-gray-300" size={32} />
          </div>
          <h3 className="text-lg font-medium text-gray-900">No debt records</h3>
          <p className="text-gray-500 mt-1">Start by adding your first borrowed or lent record.</p>
        </div>
      )}
    </div>
  );
};

// Add missing icon import
import { HandCoins } from 'lucide-react';
