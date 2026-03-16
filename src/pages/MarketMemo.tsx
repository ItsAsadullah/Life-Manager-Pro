import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { collection, query, onSnapshot, orderBy, addDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Plus, Save, X, ShoppingCart, Trash2, Receipt } from 'lucide-react';
import { format } from 'date-fns';

interface MemoItem {
  id: string;
  name: string;
  quantity: number;
  unit: string;
  unitPrice: number;
  total: number;
}

export const MarketMemo: React.FC = () => {
  const { user } = useAuth();
  const [memos, setMemos] = useState<any[]>([]);
  const [isAdding, setIsAdding] = useState(false);
  
  const [title, setTitle] = useState('');
  const [items, setItems] = useState<MemoItem[]>([]);
  
  // New item form state
  const [itemName, setItemName] = useState('');
  const [itemQuantity, setItemQuantity] = useState<number | ''>('');
  const [itemUnit, setItemUnit] = useState('kg');
  const [itemUnitPrice, setItemUnitPrice] = useState<number | ''>('');

  useEffect(() => {
    if (!user) return;
    const memosRef = collection(db, 'users', user.uid, 'marketMemos');
    const q = query(memosRef, orderBy('createdAt', 'desc'));
    
    const unsubscribe = onSnapshot(q, (snapshot) => {
      setMemos(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });

    return () => unsubscribe();
  }, [user]);

  const handleAddItem = (e: React.FormEvent) => {
    e.preventDefault();
    if (!itemName || !itemQuantity || !itemUnitPrice) return;
    
    const quantity = Number(itemQuantity);
    const unitPrice = Number(itemUnitPrice);
    
    const newItem: MemoItem = {
      id: Date.now().toString(),
      name: itemName,
      quantity,
      unit: itemUnit,
      unitPrice,
      total: quantity * unitPrice
    };
    
    setItems([...items, newItem]);
    
    // Reset item form
    setItemName('');
    setItemQuantity('');
    setItemUnitPrice('');
  };

  const handleRemoveItem = (id: string) => {
    setItems(items.filter(item => item.id !== id));
  };

  const totalAmount = items.reduce((sum, item) => sum + item.total, 0);

  const handleSaveMemo = async () => {
    if (!user || items.length === 0 || !title.trim()) return;
    
    try {
      await addDoc(collection(db, 'users', user.uid, 'marketMemos'), {
        title,
        items,
        totalAmount,
        createdAt: new Date().toISOString()
      });
      setIsAdding(false);
      setTitle('');
      setItems([]);
    } catch (error) {
      console.error('Error saving memo:', error);
      alert('Failed to save memo.');
    }
  };

  const handleConvertToExpense = async (memo: any) => {
    if (!user) return;
    
    try {
      await addDoc(collection(db, 'users', user.uid, 'expenses'), {
        amount: memo.totalAmount,
        category: 'Shopping',
        description: `Market Memo: ${memo.title}`,
        date: new Date().toISOString(),
        paymentMethod: 'Cash',
        createdAt: new Date().toISOString()
      });
      alert('Successfully converted to Expense!');
    } catch (error) {
      console.error('Error converting to expense:', error);
      alert('Failed to convert to expense.');
    }
  };

  const units = ['kg', 'g', 'liter', 'ml', 'pcs', 'dozen', 'packet'];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Market Memo</h2>
          <p className="text-gray-600">Create digital bazaar lists and track costs</p>
        </div>
        <button
          onClick={() => setIsAdding(true)}
          className="flex items-center px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
        >
          <Plus size={20} className="mr-2" />
          New Memo
        </button>
      </div>

      {isAdding && (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 mb-6">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-bold">Create Market Memo</h3>
            <button onClick={() => setIsAdding(false)} className="text-gray-400 hover:text-gray-600">
              <X size={20} />
            </button>
          </div>
          
          <div className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Memo Title</label>
              <input
                type="text"
                placeholder="e.g., Weekly Grocery, Friday Market"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
              />
            </div>

            <div className="bg-gray-50 p-4 rounded-lg border border-gray-200">
              <h4 className="text-sm font-semibold text-gray-700 mb-3">Add Item</h4>
              <form onSubmit={handleAddItem} className="grid grid-cols-1 md:grid-cols-5 gap-3">
                <div className="md:col-span-2">
                  <input
                    type="text"
                    placeholder="Item Name"
                    required
                    value={itemName}
                    onChange={(e) => setItemName(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                  />
                </div>
                <div>
                  <input
                    type="number"
                    placeholder="Qty"
                    required
                    min="0.01"
                    step="0.01"
                    value={itemQuantity}
                    onChange={(e) => setItemQuantity(e.target.value ? Number(e.target.value) : '')}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                  />
                </div>
                <div>
                  <select
                    value={itemUnit}
                    onChange={(e) => setItemUnit(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                  >
                    {units.map(u => <option key={u} value={u}>{u}</option>)}
                  </select>
                </div>
                <div>
                  <input
                    type="number"
                    placeholder="Unit Price (৳)"
                    required
                    min="0"
                    step="0.01"
                    value={itemUnitPrice}
                    onChange={(e) => setItemUnitPrice(e.target.value ? Number(e.target.value) : '')}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                  />
                </div>
                <div className="md:col-span-5 flex justify-end">
                  <button
                    type="submit"
                    className="px-4 py-2 bg-gray-900 text-white rounded-lg text-sm hover:bg-gray-800"
                  >
                    Add to List
                  </button>
                </div>
              </form>
            </div>

            {items.length > 0 && (
              <div className="border border-gray-200 rounded-lg overflow-hidden">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Item</th>
                      <th className="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">Qty</th>
                      <th className="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">Price</th>
                      <th className="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">Total</th>
                      <th className="px-4 py-2 text-center text-xs font-medium text-gray-500 uppercase">Action</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {items.map((item) => (
                      <tr key={item.id}>
                        <td className="px-4 py-2 text-sm text-gray-900">{item.name}</td>
                        <td className="px-4 py-2 text-sm text-gray-500 text-right">{item.quantity} {item.unit}</td>
                        <td className="px-4 py-2 text-sm text-gray-500 text-right">৳{item.unitPrice}</td>
                        <td className="px-4 py-2 text-sm font-medium text-gray-900 text-right">৳{item.total.toLocaleString()}</td>
                        <td className="px-4 py-2 text-center">
                          <button onClick={() => handleRemoveItem(item.id)} className="text-red-500 hover:text-red-700">
                            <Trash2 size={16} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot className="bg-gray-50">
                    <tr>
                      <td colSpan={3} className="px-4 py-3 text-right text-sm font-bold text-gray-900">Grand Total:</td>
                      <td className="px-4 py-3 text-right text-sm font-bold text-indigo-600">৳{totalAmount.toLocaleString()}</td>
                      <td></td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            )}

            <div className="flex justify-end">
              <button
                onClick={handleSaveMemo}
                disabled={items.length === 0 || !title.trim()}
                className="flex items-center px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
              >
                <Save size={20} className="mr-2" />
                Save Memo
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {memos.map(memo => (
          <div key={memo.id} className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex flex-col">
            <div className="flex justify-between items-start mb-4">
              <div>
                <h3 className="font-bold text-lg text-gray-900">{memo.title}</h3>
                <p className="text-xs text-gray-400">
                  {memo.createdAt ? format(new Date(memo.createdAt), 'MMM d, yyyy h:mm a') : ''}
                </p>
              </div>
              <div className="p-2 bg-indigo-50 text-indigo-600 rounded-lg">
                <ShoppingCart size={20} />
              </div>
            </div>
            
            <div className="flex-1">
              <ul className="space-y-2 mb-4">
                {memo.items.slice(0, 3).map((item: any, idx: number) => (
                  <li key={idx} className="flex justify-between text-sm">
                    <span className="text-gray-600">{item.name} <span className="text-xs text-gray-400">({item.quantity}{item.unit})</span></span>
                    <span className="font-medium text-gray-900">৳{item.total}</span>
                  </li>
                ))}
                {memo.items.length > 3 && (
                  <li className="text-xs text-gray-400 italic">+{memo.items.length - 3} more items...</li>
                )}
              </ul>
            </div>

            <div className="pt-4 border-t border-gray-100">
              <div className="flex justify-between items-center mb-4">
                <span className="text-sm font-medium text-gray-500">Total Amount</span>
                <span className="text-lg font-bold text-indigo-600">৳{memo.totalAmount.toLocaleString()}</span>
              </div>
              
              <button
                onClick={() => handleConvertToExpense(memo)}
                className="w-full flex justify-center items-center px-4 py-2 bg-gray-50 text-gray-700 rounded-lg border border-gray-200 hover:bg-gray-100 transition-colors"
              >
                <Receipt size={16} className="mr-2" />
                Convert to Expense
              </button>
            </div>
          </div>
        ))}
        {memos.length === 0 && !isAdding && (
          <div className="col-span-full text-center py-12 text-gray-500">
            <ShoppingCart className="mx-auto h-12 w-12 text-gray-300 mb-3" />
            <p>No market memos found. Create one for your next shopping trip!</p>
          </div>
        )}
      </div>
    </div>
  );
};
