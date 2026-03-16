import React, { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { collection, query, onSnapshot, orderBy, limit } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip } from 'recharts';
import { format } from 'date-fns';

export const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const [expenses, setExpenses] = useState<any[]>([]);
  const [notes, setNotes] = useState<any[]>([]);
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
      setLoading(false);
    });

    return () => {
      unsubscribeExpenses();
      unsubscribeNotes();
    };
  }, [user]);

  if (loading) return <div>Loading dashboard...</div>;

  const totalExpense = expenses.reduce((sum, exp) => sum + (exp.amount || 0), 0);
  
  // Group expenses by category for chart
  const categoryData = expenses.reduce((acc: any, exp) => {
    const cat = exp.category || 'Other';
    acc[cat] = (acc[cat] || 0) + exp.amount;
    return acc;
  }, {});

  const chartData = Object.keys(categoryData).map(key => ({
    name: key,
    value: categoryData[key]
  }));

  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8'];

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-900">Dashboard</h2>
      
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h3 className="text-sm font-medium text-gray-500 mb-1">Total Expenses</h3>
          <p className="text-3xl font-bold text-gray-900">৳{totalExpense.toLocaleString()}</p>
        </div>
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h3 className="text-sm font-medium text-gray-500 mb-1">Recent Notes</h3>
          <p className="text-3xl font-bold text-gray-900">{notes.length}</p>
        </div>
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h3 className="text-sm font-medium text-gray-500 mb-1">Total Vouchers</h3>
          <p className="text-3xl font-bold text-gray-900">0</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h3 className="text-lg font-bold text-gray-900 mb-4">Expense by Category</h3>
          {chartData.length > 0 ? (
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={chartData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {chartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <RechartsTooltip formatter={(value) => `৳${value}`} />
                </PieChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <p className="text-gray-500 text-center py-10">No expenses yet.</p>
          )}
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h3 className="text-lg font-bold text-gray-900 mb-4">Recent Notes</h3>
          {notes.length > 0 ? (
            <div className="space-y-4">
              {notes.map(note => (
                <div key={note.id} className="p-4 border border-gray-100 rounded-lg hover:bg-gray-50">
                  <h4 className="font-medium text-gray-900">{note.title}</h4>
                  <p className="text-sm text-gray-500 line-clamp-2 mt-1">{note.content}</p>
                  <p className="text-xs text-gray-400 mt-2">
                    {note.createdAt ? format(new Date(note.createdAt), 'MMM d, yyyy') : ''}
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-gray-500 text-center py-10">No notes yet.</p>
          )}
        </div>
      </div>
    </div>
  );
};
