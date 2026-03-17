import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useLocation } from 'react-router-dom';
import { collection, query, onSnapshot, orderBy, addDoc, deleteDoc, doc, updateDoc, setDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Plus, Save, X, ShoppingCart, Trash2, Receipt, Edit2, CheckCircle, Check, Share2, Copy, Image as ImageIcon, FileText, Link as LinkIcon } from 'lucide-react';
import { format } from 'date-fns';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import { SwipeableNumberInput } from '../components/SwipeableNumberInput';
import { playTick } from '../utils/audio';

interface MemoItem {
  id: string;
  name: string;
  quantity: number;
  unit: string;
  unitPrice: number;
  total: number;
  checked?: boolean;
}

export const MarketMemo: React.FC = () => {
  const { user } = useAuth();
  const location = useLocation();
  const [memos, setMemos] = useState<any[]>([]);
  const [isAdding, setIsAdding] = useState(false);
  const [isAddItemVisible, setIsAddItemVisible] = useState(false);

  useEffect(() => {
    if (location.state?.openAddModal) {
      setIsAdding(true);
      window.history.replaceState({}, document.title);
    }
  }, [location]);
  
  const [title, setTitle] = useState('');
  const [items, setItems] = useState<MemoItem[]>([]);
  
  // New item form state
  const [itemName, setItemName] = useState('');
  const [itemQuantity, setItemQuantity] = useState<string>('');
  const [itemUnit, setItemUnit] = useState('kg');
  const [itemUnitPrice, setItemUnitPrice] = useState<string>('');
  const [inlineEditingId, setInlineEditingId] = useState<string | null>(null);
  const [inlineName, setInlineName] = useState('');
  const [inlineQty, setInlineQty] = useState('');
  const [inlineUnit, setInlineUnit] = useState('kg');
  const [inlinePrice, setInlinePrice] = useState('');
  const [itemError, setItemError] = useState('');
  const [editingMemoId, setEditingMemoId] = useState<string | null>(null);
  const [editingMemoExpenseId, setEditingMemoExpenseId] = useState<string | null>(null);
  const [highlightedItemId, setHighlightedItemId] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [sharingMemo, setSharingMemo] = useState<any>(null);

  const nameInputRef = useRef<HTMLInputElement>(null);
  const itemRefs = useRef<{ [key: string]: HTMLElement | null }>({});

  const handleShareText = (memo: any) => {
    let text = `🛒 Market Memo: ${memo.title}\n`;
    if (memo.createdAt) {
      text += `📅 Date: ${format(new Date(memo.createdAt), 'MMM d, yyyy')}\n`;
    }
    text += `------------------------\n`;
    memo.items.forEach((item: any, index: number) => {
      text += `${index + 1}. ${item.name} - ${item.quantity}${item.unit} @ ৳${item.unitPrice} = ৳${item.total}\n`;
    });
    text += `------------------------\n`;
    text += `💰 Total: ৳${memo.totalAmount.toLocaleString()}\n\n`;
    text += `Developed by: Asadullah Al Galib\n`;
    text += `B.Sc in CSE, 01911777694\n`;
    text += `Created with Hisab Nikash App\n`;

    if (navigator.share) {
      navigator.share({
        title: memo.title,
        text: text,
      }).catch(console.error);
    } else {
      navigator.clipboard.writeText(text);
      setSuccessMessage('Copied to clipboard!');
    }
    setSharingMemo(null);
  };

  const handleShareImage = async (memo: any) => {
    const element = document.getElementById(`memo-capture-${memo.id}`);
    if (!element) return;
    
    // Ensure element is visible for capture
    const originalStyle = element.getAttribute('style') || '';
    element.style.display = 'block';
    element.style.position = 'fixed';
    element.style.left = '-9999px';
    element.style.top = '0';
    element.style.visibility = 'visible';
    
    try {
      const canvas = await html2canvas(element, { 
        scale: 2, 
        useCORS: true,
        backgroundColor: '#ffffff',
        logging: false,
        allowTaint: true
      });
      const dataUrl = canvas.toDataURL('image/png');
      
      const link = document.createElement('a');
      link.download = `Market_Memo_${memo.title.replace(/\s+/g, '_')}.png`;
      link.href = dataUrl;
      link.click();
      setSuccessMessage('Image downloaded!');
    } catch (err) {
      console.error('Failed to generate image', err);
      alert('Failed to generate image. Please try again.');
    } finally {
      element.setAttribute('style', originalStyle);
      setSharingMemo(null);
    }
  };

  const handleSharePDF = async (memo: any) => {
    const element = document.getElementById(`memo-capture-${memo.id}`);
    if (!element) return;
    
    // Ensure element is visible for capture
    const originalStyle = element.getAttribute('style') || '';
    element.style.display = 'block';
    element.style.position = 'fixed';
    element.style.left = '-9999px';
    element.style.top = '0';
    element.style.visibility = 'visible';
    
    try {
      const canvas = await html2canvas(element, { 
        scale: 2, 
        useCORS: true,
        backgroundColor: '#ffffff',
        logging: false,
        allowTaint: true
      });
      const imgData = canvas.toDataURL('image/png');
      const pdf = new jsPDF({
        orientation: 'portrait',
        unit: 'px',
        format: [canvas.width, canvas.height]
      });
      pdf.addImage(imgData, 'PNG', 0, 0, canvas.width, canvas.height);
      pdf.save(`Market_Memo_${memo.title.replace(/\s+/g, '_')}.pdf`);
      setSuccessMessage('PDF downloaded!');
    } catch (err) {
      console.error('Failed to generate PDF', err);
      alert('Failed to generate PDF. Please try again.');
    } finally {
      element.setAttribute('style', originalStyle);
      setSharingMemo(null);
    }
  };

  const handleShareLink = async (memo: any) => {
    if (!user) return;
    
    let url = '';
    try {
      // Generate a shorter ID for the shared memo (8 characters)
      const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0120093908';
      let shortId = '';
      for (let i = 0; i < 8; i++) {
        shortId += chars.charAt(Math.floor(Math.random() * chars.length));
      }

      const sharedMemoRef = doc(db, 'sharedMemos', shortId);
      await setDoc(sharedMemoRef, {
        title: memo.title,
        items: memo.items,
        totalAmount: memo.totalAmount,
        createdAt: memo.createdAt || new Date().toISOString(),
        sharedBy: user.uid,
        sharedAt: new Date().toISOString()
      });

      url = `${window.location.origin}/shared-memo?id=${shortId}`;
    } catch (error) {
      console.error("Error sharing memo to Firestore, falling back to base64:", error);
      // Fallback to base64 encoding if Firestore fails
      const data = {
        t: memo.title,
        i: memo.items.map((item: any) => ({
          n: item.name,
          q: item.quantity,
          u: item.unit,
          p: item.unitPrice,
          t: item.total
        })),
        ta: memo.totalAmount,
        d: memo.createdAt
      };
      
      const utf8Bytes = new TextEncoder().encode(JSON.stringify(data));
      const binString = Array.from(utf8Bytes, (byte) => String.fromCharCode(byte)).join('');
      const base64 = btoa(binString);
      
      url = `${window.location.origin}/shared-memo?data=${base64}`;
    }

    try {
      if (navigator.share) {
        navigator.share({
          title: memo.title,
          url: url
        }).catch((e) => {
          navigator.clipboard.writeText(url);
          setSuccessMessage('Link copied to clipboard!');
        });
      } else {
        navigator.clipboard.writeText(url);
        setSuccessMessage('Link copied to clipboard!');
      }
    } catch (err) {
      console.error("Clipboard error:", err);
      alert("Failed to copy link. Please try again.");
    } finally {
      setSharingMemo(null);
    }
  };

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
    
    const newItem: MemoItem = {
      id: Date.now().toString(),
      name: itemName,
      quantity,
      unit: itemUnit,
      unitPrice,
      total: quantity * unitPrice,
      checked: false
    };
    const newItems = [...items, newItem];
    newItems.sort((a, b) => (a.checked === b.checked ? 0 : a.checked ? 1 : -1));
    setItems(newItems);
    
    // Reset item form
    setItemName('');
    setItemQuantity('');
    setItemUnitPrice('');
    setItemError('');
  };

  const startInlineEdit = (item: MemoItem, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setInlineEditingId(item.id);
    setInlineName(item.name);
    setInlineQty(String(item.quantity));
    setInlineUnit(item.unit);
    setInlinePrice(String(item.unitPrice));
  };

  const saveInlineEdit = (id: string, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    const q = Number(inlineQty);
    const p = Number(inlinePrice);
    if (!inlineName.trim()) {
      alert("Name cannot be empty");
      return;
    }
    if (isNaN(q) || isNaN(p) || q <= 0 || p < 0) {
      alert("Please enter valid positive numbers for quantity and price");
      return;
    }
    const newItems = items.map(item => 
      item.id === id 
        ? { ...item, name: inlineName, quantity: q, unit: inlineUnit, unitPrice: p, total: q * p }
        : item
    );
    newItems.sort((a, b) => (a.checked === b.checked ? 0 : a.checked ? 1 : -1));
    setItems(newItems);
    setInlineEditingId(null);
  };

  const cancelInlineEdit = (e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setInlineEditingId(null);
  };

  const handleRemoveItem = (id: string) => {
    setItems(items.filter(item => item.id !== id));
  };

  const toggleItemCheck = (id: string) => {
    const newItems = items.map(item => 
      item.id === id ? { ...item, checked: !item.checked } : item
    );
    newItems.sort((a, b) => (a.checked === b.checked ? 0 : a.checked ? 1 : -1));
    setItems(newItems);
    playTick();
  };

  const totalAmount = items.reduce((sum, item) => sum + item.total, 0);
  const purchasedAmount = items.filter(item => item.checked).reduce((sum, item) => sum + item.total, 0);
  const remainingAmount = totalAmount - purchasedAmount;

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
    setInlineEditingId(null);
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
    } catch (error) {
      console.error('Error saving memo:', error);
      alert('Failed to save memo.');
    }
  };

  const handleEditMemo = (memo: any) => {
    setEditingMemoId(memo.id);
    setEditingMemoExpenseId(memo.expenseId || null);
    setTitle(memo.title);
    const sortedItems = [...(memo.items || [])].sort((a, b) => (a.checked === b.checked ? 0 : a.checked ? 1 : -1));
    setItems(sortedItems);
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
        <div className="fixed inset-0 z-[100] flex flex-col bg-gray-50 animate-in slide-in-from-bottom-4">
          <div className="flex justify-between items-center p-4 bg-white shadow-sm sticky top-0 z-20">
            <h3 className="text-lg font-bold text-gray-800">{editingMemoId ? 'Edit Market Memo' : 'Create Market Memo'}</h3>
            <div className="flex items-center gap-2">
              <button
                onClick={handleSaveMemo}
                disabled={items.length === 0 || !title.trim()}
                className="flex items-center justify-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 font-medium transition-colors disabled:opacity-50 text-sm"
              >
                <Save className="w-4 h-4" />
                {editingMemoId ? 'Update' : 'Save'}
              </button>
              <button onClick={closeModal} className="p-2 text-gray-500 hover:bg-gray-100 rounded-full transition-colors">
                <X size={24} />
              </button>
            </div>
          </div>
          
          <div className="flex-1 overflow-y-auto p-4 md:p-6">
            <div className="max-w-4xl mx-auto space-y-6">
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

              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-bold text-gray-800">Items</h3>
                <button
                  onClick={() => setIsAddItemVisible(!isAddItemVisible)}
                  className="flex items-center px-3 py-1.5 bg-indigo-100 text-indigo-700 rounded-lg hover:bg-indigo-200 text-sm font-medium transition-colors"
                >
                  {isAddItemVisible ? <X size={16} className="mr-1" /> : <Plus size={16} className="mr-1" />}
                  {isAddItemVisible ? 'Cancel' : 'Add new item'}
                </button>
              </div>

              {isAddItemVisible && (
                <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 mb-6">
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
                      <SwipeableNumberInput
                        placeholder="Qty"
                        required
                        value={itemQuantity}
                        onChange={setItemQuantity}
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
                      <SwipeableNumberInput
                        placeholder="Unit Price (৳)"
                        required
                        value={itemUnitPrice}
                        onChange={setItemUnitPrice}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                        isPrice={true}
                      />
                    </div>
                    <div className="sm:col-span-2 md:col-span-5 flex justify-end gap-2 mt-2">
                      <button
                        type="submit"
                        className="px-4 py-2 bg-indigo-600 text-white rounded-lg text-sm hover:bg-indigo-700 flex items-center gap-2"
                      >
                        <Plus size={16} />
                        যুক্ত করুন
                      </button>
                    </div>
                  </form>
                </div>
              )}

              {items.length > 0 && (
                <>
                  <div className="mb-4 bg-white border border-indigo-100 rounded-lg p-3 shadow-sm">
                    <div className="flex justify-between text-sm mb-2">
                      <div>
                        <span className="text-gray-500">কেনা হয়েছে:</span>{' '}
                        <span className="font-bold text-green-600">৳{purchasedAmount.toLocaleString()}</span>
                      </div>
                      <div>
                        <span className="text-gray-500">বাকি আছে:</span>{' '}
                        <span className="font-bold text-red-600">৳{remainingAmount.toLocaleString()}</span>
                      </div>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div 
                        className="bg-green-500 h-2 rounded-full transition-all duration-500" 
                        style={{ width: `${totalAmount > 0 ? (purchasedAmount / totalAmount) * 100 : 0}%` }}
                      ></div>
                    </div>
                  </div>

                  <div className="border border-indigo-200 rounded-lg overflow-hidden shadow-sm">
                    <table className="w-full text-sm text-left">
                      <thead className="bg-[#9b87f5] text-white">
                        <tr>
                          <th className="px-3 py-3 font-medium">বিবরণ</th>
                          <th className="px-3 py-3 font-medium text-center">পরিমাণ</th>
                          <th className="px-3 py-3 font-medium text-right">দর</th>
                          <th className="px-3 py-3 font-medium text-right">টাকার পরিমাণ</th>
                          <th className="px-3 py-3 w-10 text-center"></th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-indigo-100 bg-white">
                        {[...items].sort((a, b) => (a.checked === b.checked ? 0 : a.checked ? 1 : -1)).map((item, index) => (
                          inlineEditingId === item.id ? (
                            <tr key={item.id} className="bg-indigo-50">
                              <td className="px-2 py-2">
                                <input type="text" value={inlineName} onChange={e => setInlineName(e.target.value)} className="w-full px-1 py-1 text-sm border border-indigo-300 rounded" />
                              </td>
                              <td className="px-2 py-2">
                                <div className="flex gap-1 justify-center">
                                  <SwipeableNumberInput value={inlineQty} onChange={setInlineQty} className="w-12 px-1 py-1 text-sm border border-indigo-300 rounded text-center" />
                                  <select value={inlineUnit} onChange={e => setInlineUnit(e.target.value)} className="w-14 px-1 py-1 text-sm border border-indigo-300 rounded p-0 bg-white">
                                    {units.map(u => <option key={u} value={u}>{u}</option>)}
                                  </select>
                                </div>
                              </td>
                              <td className="px-2 py-2">
                                <SwipeableNumberInput value={inlinePrice} onChange={setInlinePrice} className="w-16 px-1 py-1 text-sm border border-indigo-300 rounded text-right ml-auto block" isPrice={true} />
                              </td>
                              <td className="px-2 py-2 text-right text-sm font-medium text-gray-900">
                                {(Number(inlineQty) * Number(inlinePrice)).toLocaleString()}
                              </td>
                              <td className="px-2 py-2 text-center">
                                <div className="flex flex-col gap-1 items-center">
                                  <button onClick={(e) => saveInlineEdit(item.id, e)} className="text-green-600 bg-green-100 hover:bg-green-200 p-1 rounded transition-colors"><Check size={14}/></button>
                                  <button onClick={cancelInlineEdit} className="text-red-600 bg-red-100 hover:bg-red-200 p-1 rounded transition-colors"><X size={14}/></button>
                                </div>
                              </td>
                            </tr>
                          ) : (
                            <tr 
                              key={item.id} 
                              ref={(el) => { itemRefs.current[item.id] = el; }}
                              onClick={() => toggleItemCheck(item.id)}
                              className={`cursor-pointer transition-colors ${index % 2 === 0 ? 'bg-indigo-50/30' : 'bg-white'} ${item.checked ? 'opacity-50' : ''} ${highlightedItemId === item.id ? 'bg-green-100' : ''}`}
                            >
                              <td className="px-3 py-3">
                                <span className={`${item.checked ? 'line-through text-gray-500' : 'text-gray-900'}`}>{item.name}</span>
                              </td>
                              <td className="px-3 py-3 text-center" onClick={(e) => { e.stopPropagation(); startInlineEdit(item, e); }}>
                                <span className={`${item.checked ? 'line-through text-gray-500' : 'text-gray-700'}`}>{item.quantity} {item.unit}</span>
                              </td>
                              <td className="px-3 py-3 text-right" onClick={(e) => { e.stopPropagation(); startInlineEdit(item, e); }}>
                                <span className={`${item.checked ? 'line-through text-gray-500' : 'text-gray-900'}`}>{item.unitPrice}</span>
                              </td>
                              <td className="px-3 py-3 text-right" onClick={(e) => { e.stopPropagation(); startInlineEdit(item, e); }}>
                                <span className={`${item.checked ? 'line-through text-gray-500' : 'text-gray-900 font-medium'}`}>{item.total}</span>
                              </td>
                              <td className="px-3 py-3 text-center">
                                <button 
                                  onClick={(e) => { e.stopPropagation(); handleRemoveItem(item.id); }} 
                                  className="text-indigo-500 hover:text-red-500 bg-indigo-100 hover:bg-red-100 p-1.5 rounded transition-colors"
                                >
                                  <Trash2 size={14} />
                                </button>
                              </td>
                            </tr>
                          )
                        ))}
                      </tbody>
                    </table>
                    
                    {/* Footer Summary */}
                    <div className="bg-[#9b87f5] text-white px-4 py-3 flex justify-between items-center text-sm font-medium">
                      <span>মোট পণ্য: {items.length} টি</span>
                      <span>মোট মূল্য: {totalAmount.toLocaleString()} ৳</span>
                    </div>
                  </div>
                </>
              )}
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
                  onClick={() => setSharingMemo(memo)} 
                  className="p-2 text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
                  title="Share Memo"
                >
                  <Share2 size={18} />
                </button>
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
              <div className="space-y-2 mb-4">
                <div className="flex justify-between items-center text-xs">
                  <span className="text-gray-500">Purchased</span>
                  <span className="text-green-600 font-bold">৳{(memo.items?.filter((i: any) => i.checked).reduce((s: number, i: any) => s + i.total, 0) || 0).toLocaleString()}</span>
                </div>
                <div className="flex justify-between items-center text-xs">
                  <span className="text-gray-500">Remaining</span>
                  <span className="text-red-600 font-bold">৳{(memo.totalAmount - (memo.items?.filter((i: any) => i.checked).reduce((s: number, i: any) => s + i.total, 0) || 0)).toLocaleString()}</span>
                </div>
                <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden">
                  <div 
                    className="bg-indigo-600 h-full transition-all duration-500" 
                    style={{ width: `${(memo.items?.filter((i: any) => i.checked).reduce((s: number, i: any) => s + i.total, 0) || 0) / memo.totalAmount * 100}%` }}
                  ></div>
                </div>
              </div>
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

            {/* Hidden Capture Div for Image/PDF */}
            <div id={`memo-capture-${memo.id}`} className="absolute left-[-9999px] top-[-9999px] bg-white p-8 w-[600px] font-sans">
              <div className="border-b-2 border-indigo-600 pb-4 mb-6">
                <h2 className="text-3xl font-bold text-gray-900 mb-2">🛒 Market Memo</h2>
                <h3 className="text-xl text-gray-700">{memo.title}</h3>
                {memo.createdAt && <p className="text-gray-500 mt-2">Date: {format(new Date(memo.createdAt), 'MMMM d, yyyy')}</p>}
              </div>
              
              <table className="w-full mb-6">
                <thead>
                  <tr className="border-b border-gray-300 text-left">
                    <th className="py-2 text-gray-600">Item</th>
                    <th className="py-2 text-right text-gray-600">Qty</th>
                    <th className="py-2 text-right text-gray-600">Price</th>
                    <th className="py-2 text-right text-gray-600">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {memo.items.map((item: any, idx: number) => (
                    <tr key={idx} className="border-b border-gray-100">
                      <td className="py-3 text-gray-900 font-medium">{item.name}</td>
                      <td className="py-3 text-right text-gray-600">{item.quantity} {item.unit}</td>
                      <td className="py-3 text-right text-gray-600">৳{item.unitPrice}</td>
                      <td className="py-3 text-right text-gray-900 font-bold">৳{item.total}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              
              <div className="flex justify-between items-center bg-indigo-50 p-4 rounded-lg">
                <span className="text-xl font-bold text-indigo-900">Grand Total</span>
                <span className="text-2xl font-bold text-indigo-700">৳{memo.totalAmount.toLocaleString()}</span>
              </div>

              <div className="mt-8 pt-6 border-t border-gray-100 text-center text-xs text-gray-400">
                <p className="font-bold text-gray-500 mb-1">Developed by: Asadullah Al Galib</p>
                <p className="mb-2">B.Sc in CSE, 01911777694</p>
                <p>Created with Hisab Nikash App</p>
              </div>
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

      {/* Share Modal */}
      {sharingMemo && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-sm relative">
            <button 
              onClick={() => setSharingMemo(null)}
              className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 hover:bg-gray-100 p-1 rounded-full transition-colors"
            >
              <X size={20} />
            </button>
            <h3 className="text-xl font-bold text-gray-900 mb-6">Share Memo</h3>
            
            <div className="grid grid-cols-2 gap-4">
              <button onClick={() => handleShareText(sharingMemo)} className="flex flex-col items-center justify-center p-4 bg-gray-50 hover:bg-indigo-50 hover:text-indigo-600 rounded-xl transition-colors border border-gray-100">
                <FileText size={24} className="mb-2" />
                <span className="text-sm font-medium">Text</span>
              </button>
              <button onClick={() => handleShareImage(sharingMemo)} className="flex flex-col items-center justify-center p-4 bg-gray-50 hover:bg-indigo-50 hover:text-indigo-600 rounded-xl transition-colors border border-gray-100">
                <ImageIcon size={24} className="mb-2" />
                <span className="text-sm font-medium">Image</span>
              </button>
              <button onClick={() => handleSharePDF(sharingMemo)} className="flex flex-col items-center justify-center p-4 bg-gray-50 hover:bg-indigo-50 hover:text-indigo-600 rounded-xl transition-colors border border-gray-100">
                <FileText size={24} className="mb-2" />
                <span className="text-sm font-medium">PDF</span>
              </button>
              <button onClick={() => handleShareLink(sharingMemo)} className="flex flex-col items-center justify-center p-4 bg-gray-50 hover:bg-indigo-50 hover:text-indigo-600 rounded-xl transition-colors border border-gray-100">
                <LinkIcon size={24} className="mb-2" />
                <span className="text-sm font-medium">Link</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Success Modal */}
      {successMessage && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl p-8 flex flex-col items-center max-w-sm w-full relative">
            <button 
              onClick={() => setSuccessMessage(null)}
              className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 hover:bg-gray-100 p-1 rounded-full transition-colors"
            >
              <X size={20} />
            </button>
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

export default MarketMemo;