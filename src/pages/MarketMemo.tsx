import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { collection, query, onSnapshot, orderBy, addDoc, deleteDoc, doc, updateDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Plus, Save, X, ShoppingCart, Trash2, Receipt, Edit2, CheckCircle } from 'lucide-react';
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
  const [editingItemId, setEditingItemId] = useState<string | null>(null);
  const [itemError, setItemError] = useState('');
  const [editingMemoId, setEditingMemoId] = useState<string | null>(null);
  const [editingMemoExpenseId, setEditingMemoExpenseId] = useState<string | null>(null);
  const [highlightedItemId, setHighlightedItemId] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const nameInputRef = useRef<HTMLInputElement>(null);
  const itemRefs = useRef<{ [key: string]: HTMLElement | null }>({});

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
    setItemError('');

    if (!itemName.trim()) {
      setItemError('Item name cannot be empty.');
      return;
    }
    
    const quantity = Number(itemQuantity);
    const unitPrice = Number(itemUnitPrice);

    if (!itemQuantity || isNaN(quantity) || quantity <= 0) {
      setItemError('Quantity must be a positive number.');
      return;
    }

    if (itemUnitPrice === '' || isNaN(unitPrice) || unitPrice < 0) {
      setItemError('Unit price must be a valid positive number.');
      return;
    }
    
    if (editingItemId) {
      const updatedId = editingItemId;
      setItems(items.map(item => 
        item.id === updatedId 
          ? { ...item, name: itemName, quantity, unit: itemUnit, unitPrice, total: quantity * unitPrice }
          : item
      ));
      setEditingItemId(null);
      
      // Highlight and scroll to updated item
      setHighlightedItemId(updatedId);
      setTimeout(() => {
        itemRefs.current[updatedId]?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 100);
      
      setTimeout(() => {
        setHighlightedItemId(null);
      }, 3000); // Highlight for 3 seconds
    } else {
      const newItem: MemoItem = {
        id: Date.now().toString(),
        name: itemName,
        quantity,
        unit: itemUnit,
        unitPrice,
        total: quantity * unitPrice
      };
      setItems([...items, newItem]);
    }
    
    // Reset item form
    setItemName('');
    setItemQuantity('');
    setItemUnitPrice('');
    setItemError('');
  };

  const handleEditItem = (item: MemoItem) => {
    setEditingItemId(item.id);
    setItemName(item.name);
    setItemQuantity(item.quantity);
    setItemUnit(item.unit);
    setItemUnitPrice(item.unitPrice);
    setItemError('');

    // Focus and scroll to input
    setTimeout(() => {
      nameInputRef.current?.focus();
      nameInputRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 100);
  };

  const handleRemoveItem = (id: string) => {
    setItems(items.filter(item => item.id !== id));
  };

  const totalAmount = items.reduce((sum, item) => sum + item.total, 0);

  const closeModal = () => {
    setIsAdding(false);
    setEditingMemoId(null);
    setEditingMemoExpenseId(null);
    setTitle('');
    setItems([]);
    setItemName('');
    setItemQuantity('');
    setItemUnitPrice('');
    setItemError('');
    setEditingItemId(null);
  };

  const handleSaveMemo = async () => {
    if (!user || items.length === 0 || !title.trim()) return;
    
    try {
      if (editingMemoId) {
        await updateDoc(doc(db, 'users', user.uid, 'marketMemos', editingMemoId), {
          title,
          items,
          totalAmount,
          updatedAt: new Date().toISOString()
        });

        if (editingMemoExpenseId) {
          try {
            await updateDoc(doc(db, 'users', user.uid, 'expenses', editingMemoExpenseId), {
              amount: totalAmount,
              description: `Market Memo: ${title}`,
            });
          } catch (expenseError) {
            console.warn('Failed to update linked expense. It may have been deleted.', expenseError);
          }
        }
        setSuccessMessage('Memo updated successfully!');
      } else {
        await addDoc(collection(db, 'users', user.uid, 'marketMemos'), {
          title,
          items,
          totalAmount,
          createdAt: new Date().toISOString()
        });
        setSuccessMessage('Memo saved successfully!');
      }
      closeModal();
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (error) {
      console.error('Error saving memo:', error);
      alert('Failed to save memo.');
    }
  };

  const handleEditMemo = (memo: any) => {
    setEditingMemoId(memo.id);
    setEditingMemoExpenseId(memo.expenseId || null);
    setTitle(memo.title);
    setItems(memo.items || []);
    setIsAdding(true);
  };

  const handleDeleteMemo = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this memo?')) return;
    try {
      await deleteDoc(doc(db, 'users', user!.uid, 'marketMemos', id));
    } catch (error) {
      console.error('Error deleting memo:', error);
      alert('Failed to delete memo.');
    }
  };

  const handleConvertToExpense = async (memo: any) => {
    if (!user) return;
    
    try {
      const expenseRef = await addDoc(collection(db, 'users', user.uid, 'expenses'), {
        amount: memo.totalAmount,
        category: 'Shopping',
        description: `Market Memo: ${memo.title}`,
        date: new Date().toISOString(),
        paymentMethod: 'Cash',
        createdAt: new Date().toISOString()
      });
      
      await updateDoc(doc(db, 'users', user.uid, 'marketMemos', memo.id), {
        expenseId: expenseRef.id
      });
      
      setSuccessMessage('Successfully converted to Expense!');
      setTimeout(() => setSuccessMessage(null), 3000);
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
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[90vh] flex flex-col">
            <div className="flex justify-between items-center p-6 border-b border-gray-200">
              <h3 className="text-lg font-bold">{editingMemoId ? 'Edit Market Memo' : 'Create Market Memo'}</h3>
              <button onClick={closeModal} className="text-gray-400 hover:text-gray-600">
                <X size={20} />
              </button>
            </div>
            
            <div className="p-6 overflow-y-auto flex-1 space-y-6">
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
                <div className="flex justify-between items-center mb-3">
                  <h4 className="text-sm font-semibold text-gray-700">Add Item</h4>
                  {itemError && <span className="text-xs text-red-600 font-medium bg-red-50 px-2 py-1 rounded">{itemError}</span>}
                </div>
                <form onSubmit={handleAddItem} className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-3">
                  <div className="sm:col-span-2 md:col-span-2">
                    <input
                      ref={nameInputRef}
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
                  <div className="sm:col-span-2 md:col-span-5 flex justify-end gap-2 mt-2">
                    {editingItemId && (
                      <button
                        type="button"
                        onClick={() => {
                          setEditingItemId(null);
                          setItemName('');
                          setItemQuantity('');
                          setItemUnitPrice('');
                        }}
                        className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg text-sm hover:bg-gray-300"
                      >
                        Cancel
                      </button>
                    )}
                    <button
                      type="submit"
                      className="px-4 py-2 bg-gray-900 text-white rounded-lg text-sm hover:bg-gray-800"
                    >
                      {editingItemId ? 'Update Item' : 'Add to List'}
                    </button>
                  </div>
                </form>
              </div>

              {items.length > 0 && (
                <>
                  {/* Desktop Table View */}
                  <div className="hidden md:block border border-gray-200 rounded-lg overflow-hidden">
                    <table className="min-w-full divide-y divide-gray-200">
                      <thead className="bg-gray-50">
                        <tr>
                          <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Item Name</th>
                          <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Quantity</th>
                          <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Unit Price</th>
                          <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Total</th>
                          <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">Action</th>
                        </tr>
                      </thead>
                      <tbody className="bg-white divide-y divide-gray-200">
                        {items.map((item) => (
                          <tr 
                            key={item.id} 
                            ref={(el) => (itemRefs.current[item.id] = el)}
                            className={`transition-colors duration-700 ${highlightedItemId === item.id ? 'bg-green-100' : 'hover:bg-gray-50'}`}
                          >
                            <td className="px-4 py-3 text-sm font-medium text-gray-900">{item.name}</td>
                            <td className="px-4 py-3 text-sm text-gray-500 text-right">{item.quantity} {item.unit}</td>
                            <td className="px-4 py-3 text-sm text-gray-500 text-right">৳{item.unitPrice}</td>
                            <td className="px-4 py-3 text-sm font-bold text-gray-900 text-right">৳{item.total.toLocaleString()}</td>
                            <td className="px-4 py-3 text-center">
                              <div className="flex justify-center gap-3">
                                <button onClick={() => handleEditItem(item)} className="text-blue-600 hover:text-blue-800 transition-colors" title="Edit">
                                  <Edit2 size={18} />
                                </button>
                                <button onClick={() => handleRemoveItem(item.id)} className="text-red-600 hover:text-red-800 transition-colors flex items-center gap-1" title="Remove">
                                  <Trash2 size={18} />
                                  <span className="hidden lg:inline text-xs font-medium">Remove</span>
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                      <tfoot className="bg-gray-100">
                        <tr>
                          <td colSpan={3} className="px-4 py-4 text-right text-sm font-bold text-gray-900 uppercase tracking-wider">Grand Total:</td>
                          <td className="px-4 py-4 text-right text-base font-bold text-indigo-700">৳{totalAmount.toLocaleString()}</td>
                          <td></td>
                        </tr>
                      </tfoot>
                    </table>
                  </div>

                  {/* Mobile Card View */}
                  <div className="md:hidden space-y-3">
                    {items.map((item) => (
                      <div 
                        key={item.id} 
                        ref={(el) => (itemRefs.current[item.id] = el)}
                        className={`border rounded-lg p-4 shadow-sm transition-colors duration-700 ${highlightedItemId === item.id ? 'bg-green-100 border-green-300' : 'bg-white border-gray-200'}`}
                      >
                        <div className="flex justify-between items-start mb-2">
                          <span className="font-bold text-gray-900 text-base">{item.name}</span>
                          <span className="font-bold text-indigo-600 text-base">৳{item.total.toLocaleString()}</span>
                        </div>
                        <div className="flex justify-between text-sm text-gray-500 mb-4">
                          <span>{item.quantity} {item.unit}</span>
                          <span>@ ৳{item.unitPrice}/{item.unit}</span>
                        </div>
                        <div className="flex justify-end gap-4 pt-3 border-t border-gray-100">
                          <button onClick={() => handleEditItem(item)} className="text-blue-600 hover:text-blue-800 flex items-center gap-1.5 text-sm font-medium transition-colors">
                            <Edit2 size={16} /> Edit
                          </button>
                          <button onClick={() => handleRemoveItem(item.id)} className="text-red-600 hover:text-red-800 flex items-center gap-1.5 text-sm font-medium transition-colors">
                            <Trash2 size={16} /> Remove
                          </button>
                        </div>
                      </div>
                    ))}
                    <div className="bg-indigo-50 border border-indigo-100 rounded-lg p-4 flex justify-between items-center shadow-sm mt-4">
                      <span className="font-bold text-indigo-900 uppercase tracking-wider text-sm">Grand Total</span>
                      <span className="font-bold text-indigo-700 text-xl">৳{totalAmount.toLocaleString()}</span>
                    </div>
                  </div>
                </>
              )}
            </div>
            
            <div className="p-6 border-t border-gray-200 flex justify-end">
              <button
                onClick={handleSaveMemo}
                disabled={items.length === 0 || !title.trim()}
                className="flex items-center px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
              >
                <Save size={20} className="mr-2" />
                {editingMemoId ? 'Update Memo' : 'Save Memo'}
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
              <div className="flex gap-2">
                <button 
                  onClick={() => handleEditMemo(memo)} 
                  className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                  title="Edit Memo"
                >
                  <Edit2 size={18} />
                </button>
                <button 
                  onClick={() => handleDeleteMemo(memo.id)} 
                  className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                  title="Delete Memo"
                >
                  <Trash2 size={18} />
                </button>
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
              
              {memo.expenseId ? (
                <div className="w-full flex justify-center items-center px-4 py-2 bg-green-50 text-green-700 rounded-lg border border-green-200">
                  <CheckCircle size={16} className="mr-2" />
                  <span className="text-sm font-medium">Added to Expenses</span>
                </div>
              ) : (
                <button
                  onClick={() => handleConvertToExpense(memo)}
                  className="w-full flex justify-center items-center px-4 py-2 bg-gray-50 text-gray-700 rounded-lg border border-gray-200 hover:bg-gray-100 transition-colors"
                >
                  <Receipt size={16} className="mr-2" />
                  Convert to Expense
                </button>
              )}
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

      {/* Success Modal */}
      {successMessage && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl p-8 flex flex-col items-center max-w-sm w-full">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mb-4">
              <CheckCircle className="w-8 h-8 text-green-600" />
            </div>
            <h3 className="text-xl font-bold text-gray-900 mb-2">Success!</h3>
            <p className="text-gray-600 text-center">{successMessage}</p>
          </div>
        </div>
      )}
    </div>
  );
};
