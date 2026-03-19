import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { useAuth } from '../contexts/AuthContext';
import { useSettings } from '../contexts/SettingsContext';
import { useLocation } from 'react-router-dom';
import { collection, query, onSnapshot, orderBy, addDoc, deleteDoc, doc, updateDoc, setDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Plus, Save, X, ShoppingCart, Trash2, Receipt, Edit2, CheckCircle, Check, Share2, Copy, Image as ImageIcon, FileText, Link as LinkIcon } from 'lucide-react';
import { format } from 'date-fns';
import { toPng } from 'html-to-image';
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
  const { t, currencySymbol } = useSettings();
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
  const [history, setHistory] = useState<MemoItem[][]>([]);
  const [historyIndex, setHistoryIndex] = useState(-1);
  
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
  const [itemToDelete, setItemToDelete] = useState<string | null>(null);
  const [memoToDelete, setMemoToDelete] = useState<string | null>(null);

  const nameInputRef = useRef<HTMLInputElement>(null);
  const itemRefs = useRef<{ [key: string]: HTMLElement | null }>({});

const copyToClipboard = async (text: string, successMsg: string) => {
      try {
        if (navigator.clipboard && window.isSecureContext) {
          await navigator.clipboard.writeText(text);
          setSuccessMessage(successMsg);
        } else {
          const textArea = document.createElement("textarea");
          textArea.value = text;
          textArea.style.position = "fixed";
          textArea.style.left = "-999999px";
          textArea.style.top = "-999999px";
          document.body.appendChild(textArea);
          textArea.focus();
          textArea.select();
          try {
            document.execCommand('copy');
            setSuccessMessage(successMsg);
          } catch (err) {
            console.error('Fallback copy failed', err);
          }
          textArea.remove();
        }
      } catch (err) {
        console.error('Clipboard copy failed:', err);
      }
    };

    const handleShareText = (memo: any) => {
      let text = `🛒 ${t('marketMemoTitle')}: ${memo.title}\n`;
      if (memo.createdAt) {
        text += `📅 ${t('date')}: ${format(new Date(memo.createdAt), 'MMM d, yyyy')}\n`;
      }
      text += `------------------------\n`;
      memo.items.forEach((item: any, index: number) => {
        text += `${index + 1}. ${item.name} - ${item.quantity}${t(item.unit)} @ ${currencySymbol}${item.unitPrice} = ${currencySymbol}${item.total}\n`;
      });
      text += `------------------------\n`;
      text += `💰 ${t('total')}: ${currencySymbol}${memo.totalAmount.toLocaleString()}\n\n`;
      text += `${t('developedBy')}: Asadullah Al Galib\n`;
      text += `B.Sc in CSE, 01911777694\n`;
      text += `${t('createdWith')}\n`;

      if (navigator.share) {
        navigator.share({
          title: memo.title,
          text: text,
        }).catch((e) => {
          copyToClipboard(text, t('copiedToClipboard') || 'Copied!');
        });
      } else {
        copyToClipboard(text, t('copiedToClipboard') || 'Copied!');
      }
      setSharingMemo(null);
    };

    const handleShareImage = async (memo: any) => {
      const element = document.getElementById(`memo-capture-${memo.id}`);
      if (!element) return;

      // Ensure element is visible for capture, avoiding negative coordinates that break html-to-image
      const originalStyle = element.getAttribute('style') || '';
      element.style.display = 'block';
      element.style.position = 'absolute';
      element.style.left = '0';
      element.style.top = '0';
      element.style.zIndex = '-9999';
      element.style.visibility = 'visible';

      // Give a tiny delay for DOM to reflect positioning before capturing
      setTimeout(async () => {
        try {
          // Explicitly grab dimensions so we prevent cropping on large memos
          const width = element.scrollWidth;
          const height = element.scrollHeight;

          const dataUrl = await toPng(element, { 
            backgroundColor: '#ffffff', 
            pixelRatio: 2,
            width: width,
            height: height,
            style: {
              transform: 'scale(1)',
              transformOrigin: 'top left'
            }
          });
          const link = document.createElement('a');
          link.download = `Market_Memo_${memo.title.replace(/\s+/g, '_')}.png`;
          link.href = dataUrl;
          link.click();
          setSuccessMessage(t('imageDownloaded'));
        } catch (err) {
          console.error('Failed to generate image', err);
        } finally {
          element.setAttribute('style', originalStyle);
          setSharingMemo(null);
        }
      }, 50);
    };

    const handleSharePDF = async (memo: any) => {
      // Create a hidden print iframe for real, editable, and uncropped PDF generation
      const iframe = document.createElement('iframe');
      iframe.style.position = 'fixed';
      iframe.style.right = '0';
      iframe.style.bottom = '0';
      iframe.style.width = '0px';
      iframe.style.height = '0px';
      iframe.style.border = 'none';
      document.body.appendChild(iframe);

      const iframeDoc = iframe.contentWindow?.document;
      if (!iframeDoc) return;

      const formattedDate = memo.createdAt ? format(new Date(memo.createdAt), 'MMMM d, yyyy') : '';
      const totalItems = memo.items.length;
      
      const rowsHTML = memo.items.map((item: any, idx: number) => `
        <tr>
          <td style="text-align: center; border-bottom: 1px solid #e5e7eb; padding: 12px 16px; color: #4b5563;">${idx + 1}</td>
          <td style="border-bottom: 1px solid #e5e7eb; padding: 12px 16px; color: #111827; font-weight: 600;">${item.name}</td>
          <td style="text-align: center; border-bottom: 1px solid #e5e7eb; padding: 12px 16px; color: #4b5563;">${item.quantity} ${t(item.unit)}</td>
          <td style="text-align: right; border-bottom: 1px solid #e5e7eb; padding: 12px 16px; color: #4b5563;">${currencySymbol}${item.unitPrice}</td>
          <td style="text-align: right; border-bottom: 1px solid #e5e7eb; padding: 12px 16px; color: #111827; font-weight: 700;">${currencySymbol}${item.total}</td>
        </tr>
      `).join('');

      iframeDoc.write(`
        <!DOCTYPE html>
        <html>
          <head>
            <title>${memo.title || 'Market_Memo'}</title>
            <style>
              @import url('https://fonts.googleapis.com/css2?family=Hind+Siliguri:wght@400;500;600;700&display=swap');
              
              body { 
                font-family: 'Hind Siliguri', sans-serif; 
                background-color: white; 
                margin: 0;
                padding: 0;
                color: #111827;
                -webkit-print-color-adjust: exact !important; 
                print-color-adjust: exact !important; 
              }
              @page { margin: 15mm; size: auto; }
              
              .container {
                max-width: 800px;
                margin: 0 auto;
                padding: 20px 0;
              }
              
              .header {
                display: flex;
                justify-content: space-between;
                align-items: flex-start;
                border-bottom: 3px solid #4f46e5;
                padding-bottom: 24px;
                margin-bottom: 32px;
              }
              
              .brand h1 {
                margin: 0;
                font-size: 32px;
                color: #4f46e5;
                font-weight: 700;
              }
              .brand p {
                margin: 4px 0 0;
                font-size: 14px;
                color: #6b7280;
                text-transform: uppercase;
                letter-spacing: 0.05em;
                font-weight: 600;
              }
              
              .memo-info {
                text-align: right;
              }
              .memo-info h2 {
                margin: 0 0 8px;
                font-size: 24px;
                color: #111827;
                font-weight: 700;
              }
              .memo-info p {
                margin: 4px 0 0;
                color: #4b5563;
                font-size: 15px;
              }
              
              table {
                width: 100%;
                border-collapse: collapse;
                margin-bottom: 32px;
              }
              th {
                background-color: #f3f4f6;
                color: #374151;
                font-weight: 700;
                text-transform: uppercase;
                font-size: 13px;
                padding: 14px 16px;
                border-bottom: 2px solid #d1d5db;
              }
              
              .summary {
                display: flex;
                justify-content: flex-end;
              }
              .summary-box {
                width: 320px;
                background-color: #f9fafb;
                border: 1px solid #e5e7eb;
                border-radius: 12px;
                padding: 24px;
                box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
                page-break-inside: avoid;
              }
              .summary-row {
                display: flex;
                justify-content: space-between;
                margin-bottom: 12px;
                font-size: 15px;
                color: #4b5563;
              }
              .summary-row.total {
                margin-bottom: 0;
                padding-top: 16px;
                margin-top: 12px;
                border-top: 2px dashed #d1d5db;
                font-size: 20px;
                font-weight: 800;
                color: #4f46e5;
              }
              
              .footer {
                margin-top: 60px;
                text-align: center;
                color: #9ca3af;
                font-size: 13px;
                padding-top: 24px;
                border-top: 1px solid #f3f4f6;
                page-break-inside: avoid;
              }
              .footer div { width: 40px; height: 4px; background: #e5e7eb; margin: 0 auto 16px; border-radius: 2px; }
            </style>
          </head>
          <body>
            <div class="container">
              <div class="header">
                <div class="brand">
                  <h1>📋 ${t('marketMemoTitle')}</h1>
                  <p>Life Manager Pro Receipts</p>
                </div>
                <div class="memo-info">
                  <h2>${memo.title}</h2>
                  <p><strong>${t('date')}:</strong> ${formattedDate}</p>
                  <p><strong>Total Items:</strong> ${totalItems}</p>
                </div>
              </div>
              
              <table>
                <thead>
                  <tr>
                    <th style="text-align: center; width: 8%;">#</th>
                    <th style="text-align: left; width: 42%;">${t('item')}</th>
                    <th style="text-align: center; width: 15%;">${t('qty')}</th>
                    <th style="text-align: right; width: 15%;">${t('price')}</th>
                    <th style="text-align: right; width: 20%;">${t('total')}</th>
                  </tr>
                </thead>
                <tbody>
                  ${rowsHTML}
                </tbody>
              </table>
              
              <div class="summary">
                <div class="summary-box">
                  <div class="summary-row">
                    <span>Subtotal</span>
                    <span>${currencySymbol}${memo.totalAmount}</span>
                  </div>
                  <div class="summary-row total">
                    <span>${t('total')}</span>
                    <span>${currencySymbol}${memo.totalAmount}</span>
                  </div>
                </div>
              </div>
              
              <div class="footer">
                <div></div>
                <p><strong>Thank you for using Life Manager Pro!</strong></p>
                <p>Designed  & Developed by Asadullah Al Galib</p>
              </div>
            </div>
          </body>
        </html>
      `);
      iframeDoc.close();

      setSuccessMessage(t('pdf') + ' Loading...'); // Temporary indicator

      // Wait for fonts and styles to render before triggering the print dialog
      setTimeout(() => {
        if (iframe.contentWindow) {
           iframe.contentWindow.focus();
           iframe.contentWindow.print();
        }
        setTimeout(() => {
          document.body.removeChild(iframe);
          setSharingMemo(null);
          setSuccessMessage(null);
        }, 1000);
      }, 500);
    };

    const handleShareLink = async (memo: any) => {
    if (!user) return;
    setSharingMemo(memo.id);
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
          copyToClipboard(url, t('linkCopied') || 'Link copied!');
        });
      } else {
        copyToClipboard(url, t('linkCopied') || 'Link copied!');
      }
    } catch (err) {
      console.error("Clipboard error:", err);
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

  const updateItemsWithHistory = (newItems: MemoItem[]) => {
    const newHistory = history.slice(0, historyIndex + 1);
    newHistory.push(newItems);
    setHistory(newHistory);
    setHistoryIndex(newHistory.length - 1);
    setItems(newItems);
  };

  const handleUndo = () => {
    if (historyIndex > 0) {
      setHistoryIndex(historyIndex - 1);
      setItems(history[historyIndex - 1]);
    }
  };

  const handleRedo = () => {
    if (historyIndex < history.length - 1) {
      setHistoryIndex(historyIndex + 1);
      setItems(history[historyIndex + 1]);
    }
  };

  const handleAddItem = (e: React.FormEvent) => {
    e.preventDefault();
    setItemError('');

    if (!itemName.trim()) {
      setItemError(t('itemNameEmpty'));
      return;
    }
    
    const quantity = Number(itemQuantity);
    const unitPrice = Number(itemUnitPrice);

    if (!itemQuantity || isNaN(quantity) || quantity <= 0) {
      setItemError(t('qtyPositive'));
      return;
    }

    if (itemUnitPrice === '' || isNaN(unitPrice) || unitPrice < 0) {
      setItemError(t('unitPricePositive'));
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
    updateItemsWithHistory(newItems);
    
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
      return;
    }
    if (isNaN(q) || isNaN(p) || q <= 0 || p < 0) {
      return;
    }
    const newItems = items.map(item => 
      item.id === id 
        ? { ...item, name: inlineName, quantity: q, unit: inlineUnit, unitPrice: p, total: q * p }
        : item
    );
    newItems.sort((a, b) => (a.checked === b.checked ? 0 : a.checked ? 1 : -1));
    updateItemsWithHistory(newItems);
    setInlineEditingId(null);
  };

  const cancelInlineEdit = (e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    setInlineEditingId(null);
  };

  const handleRemoveItem = (id: string) => {
    setItemToDelete(id);
  };

  const confirmRemoveItem = () => {
    if (itemToDelete) {
      updateItemsWithHistory(items.filter(item => item.id !== itemToDelete));
      setItemToDelete(null);
    }
  };

  const toggleItemCheck = (id: string) => {
    const newItems = items.map(item => 
      item.id === id ? { ...item, checked: !item.checked } : item
    );
    newItems.sort((a, b) => (a.checked === b.checked ? 0 : a.checked ? 1 : -1));
    updateItemsWithHistory(newItems);
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
    setHistory([]);
    setHistoryIndex(-1);
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
              description: `${t('marketMemo')}: ${title}`,
            });
          } catch (expenseError) {
            console.warn('Failed to update linked expense. It may have been deleted.', expenseError);
          }
        }
        setSuccessMessage(t('memoUpdated'));
      } else {
        await addDoc(collection(db, 'users', user.uid, 'marketMemos'), {
          title,
          items,
          totalAmount,
          createdAt: new Date().toISOString()
        });
        setSuccessMessage(t('memoSaved'));
      }
      closeModal();
    } catch (error) {
      console.error('Error saving memo:', error);
    }
  };

  const handleNewMemo = () => {
    setEditingMemoId(null);
    setEditingMemoExpenseId(null);
    setTitle('');
    setItems([]);
    setHistory([[]]);
    setHistoryIndex(0);
    setIsAdding(true);
  };

  const handleEditMemo = (memo: any) => {
    setEditingMemoId(memo.id);
    setEditingMemoExpenseId(memo.expenseId || null);
    setTitle(memo.title);
    const sortedItems = [...(memo.items || [])].sort((a, b) => (a.checked === b.checked ? 0 : a.checked ? 1 : -1));
    setItems(sortedItems);
    setHistory([sortedItems]);
    setHistoryIndex(0);
    setIsAdding(true);
  };

  const handleDeleteMemo = async (id: string) => {
    setMemoToDelete(id);
  };

  const confirmDeleteMemo = async () => {
    if (!memoToDelete || !user) return;
    try {
      await deleteDoc(doc(db, 'users', user.uid, 'marketMemos', memoToDelete));
      setMemoToDelete(null);
    } catch (error) {
      console.error('Error deleting memo:', error);
    }
  };

  const handleConvertToExpense = async (memo: any) => {
    if (!user) return;
    
    try {
      const expenseRef = await addDoc(collection(db, 'users', user.uid, 'expenses'), {
        amount: memo.totalAmount,
        category: 'Shopping',
        description: `${t('marketMemo')}: ${memo.title}`,
        date: new Date().toISOString(),
        paymentMethod: 'Cash',
        createdAt: new Date().toISOString()
      });
      
      await updateDoc(doc(db, 'users', user.uid, 'marketMemos', memo.id), {
        expenseId: expenseRef.id
      });
      
      setSuccessMessage(t('convertedToExpense'));
    } catch (error) {
      console.error('Error converting to expense:', error);
    }
  };

  const units = ['kg', 'g', 'liter', 'ml', 'pcs', 'dozen', 'packet'];

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white">{t('marketMemo')}</h2>
          <p className="text-gray-600 dark:text-gray-400">{t('createBazaarList')}</p>
        </div>
        <button
          onClick={handleNewMemo}
          className="flex items-center px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
        >
          <Plus size={20} className="mr-2" />
          {t('addMemo')}
        </button>
      </div>

      {isAdding && createPortal(
        <div className="fixed inset-0 z-[9999] flex flex-col bg-gray-50 dark:bg-gray-900 animate-in slide-in-from-bottom-4">
          <div className="flex justify-between items-center p-4 bg-white dark:bg-gray-800 shadow-sm sticky top-0 z-20">
            <h3 className="text-lg font-bold text-gray-800 dark:text-white">{editingMemoId ? t('editMarketMemo') : t('createMarketMemo')}</h3>
            <div className="flex items-center gap-2">
              <button
                onClick={handleUndo}
                disabled={historyIndex <= 0}
                className="p-2 text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors disabled:opacity-30"
                title={t('undo')}
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 7v6h6"/><path d="M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6 2.3L3 13"/></svg>
              </button>
              <button
                onClick={handleRedo}
                disabled={historyIndex >= history.length - 1}
                className="p-2 text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors disabled:opacity-30"
                title={t('redo')}
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 7v6h-6"/><path d="M3 17a9 9 0 0 1 9-9 9 9 0 0 1 6 2.3l3 2.7"/></svg>
              </button>
              <button
                onClick={handleSaveMemo}
                disabled={items.length === 0 || !title.trim()}
                className="flex items-center justify-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 font-medium transition-colors disabled:opacity-50 text-sm ml-2"
              >
                <Save className="w-4 h-4" />
                {editingMemoId ? t('update') : t('save')}
              </button>
              <button onClick={closeModal} className="p-2 text-gray-500 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition-colors">
                <X size={24} />
              </button>
            </div>
          </div>
          
          <div className="flex-1 overflow-y-auto p-4 md:p-6">
            <div className="max-w-4xl mx-auto space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('memoTitle')}</label>
                <input
                  type="text"
                  placeholder={t('memoPlaceholder')}
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>

              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-bold text-gray-800 dark:text-white">{t('items')}</h3>
                <button
                  onClick={() => setIsAddItemVisible(!isAddItemVisible)}
                  className="flex items-center px-3 py-1.5 bg-indigo-100 dark:bg-indigo-900 text-indigo-700 dark:text-indigo-300 rounded-lg hover:bg-indigo-200 dark:hover:bg-indigo-800 text-sm font-medium transition-colors"
                >
                  {isAddItemVisible ? <X size={16} className="mr-1" /> : <Plus size={16} className="mr-1" />}
                  {isAddItemVisible ? t('cancel') : t('addNewItem')}
                </button>
              </div>

              {isAddItemVisible && (
                <div className="bg-white dark:bg-gray-800 p-4 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 mb-6">
                  <div className="flex justify-between items-center mb-3">
                    <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-300">{t('addItem')}</h4>
                    {itemError && <span className="text-xs text-red-600 font-medium bg-red-50 dark:bg-red-900/30 px-2 py-1 rounded">{itemError}</span>}
                  </div>
                  <form onSubmit={handleAddItem} className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-3">
                    <div className="sm:col-span-2 md:col-span-2">
                      <input
                        ref={nameInputRef}
                        type="text"
                        placeholder={t('itemName')}
                        required
                        value={itemName}
                        onChange={(e) => setItemName(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-lg text-sm"
                      />
                    </div>
                    <div>
                      <SwipeableNumberInput
                        placeholder={t('qty')}
                        required
                        value={itemQuantity}
                        onChange={setItemQuantity}
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-lg text-sm"
                      />
                    </div>
                    <div>
                      <select
                        value={itemUnit}
                        onChange={(e) => setItemUnit(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-lg text-sm"
                      >
                        {units.map(u => <option key={u} value={u}>{t(u)}</option>)}
                      </select>
                    </div>
                    <div>
                      <SwipeableNumberInput
                        placeholder={`${t('unitPrice')} (${currencySymbol})`}
                        required
                        value={itemUnitPrice}
                        onChange={setItemUnitPrice}
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-lg text-sm"
                        isPrice={true}
                      />
                    </div>
                    <div className="sm:col-span-2 md:col-span-5 flex justify-end gap-2 mt-2">
                      <button
                        type="submit"
                        className="px-4 py-2 bg-indigo-600 text-white rounded-lg text-sm hover:bg-indigo-700 flex items-center gap-2"
                      >
                        <Plus size={16} />
                        {t('add')}
                      </button>
                    </div>
                  </form>
                </div>
              )}

              {items.length > 0 && (
                <>
                  <div className="mb-4 bg-white dark:bg-gray-800 border border-indigo-100 dark:border-gray-700 rounded-lg p-3 shadow-sm">
                    <div className="flex justify-between text-sm mb-2">
                      <div>
                        <span className="text-gray-500 dark:text-gray-400">{t('purchased')}:</span>{' '}
                        <span className="font-bold text-green-600 dark:text-green-400">{currencySymbol}{purchasedAmount.toLocaleString()}</span>
                      </div>
                      <div>
                        <span className="text-gray-500 dark:text-gray-400">{t('remaining')}:</span>{' '}
                        <span className="font-bold text-red-600 dark:text-red-400">{currencySymbol}{remainingAmount.toLocaleString()}</span>
                      </div>
                    </div>
                    <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                      <div 
                        className="bg-green-500 h-2 rounded-full transition-all duration-500" 
                        style={{ width: `${totalAmount > 0 ? (purchasedAmount / totalAmount) * 100 : 0}%` }}
                      ></div>
                    </div>
                  </div>

                  <div className="border border-indigo-200 dark:border-gray-700 rounded-lg overflow-hidden shadow-sm">
                    <table className="w-full text-sm text-left">
                      <thead className="bg-[#9b87f5] text-white">
                        <tr>
                          <th className="px-3 py-3 font-medium">{t('description')}</th>
                          <th className="px-3 py-3 font-medium text-center">{t('quantity')}</th>
                          <th className="px-3 py-3 font-medium text-right">{t('rate')}</th>
                          <th className="px-3 py-3 font-medium text-right">{t('totalAmount')}</th>
                          <th className="px-3 py-3 w-10 text-center"></th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-indigo-100 dark:divide-gray-700 bg-white dark:bg-gray-800">
                        {[...items].sort((a, b) => (a.checked === b.checked ? 0 : a.checked ? 1 : -1)).map((item, index) => (
                          inlineEditingId === item.id ? (
                            <tr key={item.id} className="bg-indigo-50 dark:bg-indigo-900/30">
                              <td className="px-2 py-2">
                                <input type="text" value={inlineName} onChange={e => setInlineName(e.target.value)} className="w-full px-1 py-1 text-sm border border-indigo-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded" />
                              </td>
                              <td className="px-2 py-2">
                                <div className="flex gap-1 justify-center">
                                  <SwipeableNumberInput value={inlineQty} onChange={setInlineQty} className="w-12 px-1 py-1 text-sm border border-indigo-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded text-center" />
                                  <select value={inlineUnit} onChange={e => setInlineUnit(e.target.value)} className="w-14 px-1 py-1 text-sm border border-indigo-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded p-0">
                                    {units.map(u => <option key={u} value={u}>{t(u)}</option>)}
                                  </select>
                                </div>
                              </td>
                              <td className="px-2 py-2">
                                <SwipeableNumberInput value={inlinePrice} onChange={setInlinePrice} className="w-16 px-1 py-1 text-sm border border-indigo-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded text-right ml-auto block" isPrice={true} />
                              </td>
                              <td className="px-2 py-2 text-right text-sm font-medium text-gray-900 dark:text-white">
                                {(Number(inlineQty) * Number(inlinePrice)).toLocaleString()}
                              </td>
                              <td className="px-2 py-2 text-center">
                                <div className="flex flex-col gap-1 items-center">
                                  <button onClick={(e) => saveInlineEdit(item.id, e)} className="text-green-600 bg-green-100 dark:bg-green-900/30 hover:bg-green-200 dark:hover:bg-green-900/50 p-1 rounded transition-colors"><Check size={14}/></button>
                                  <button onClick={cancelInlineEdit} className="text-red-600 bg-red-100 dark:bg-red-900/30 hover:bg-red-200 dark:hover:bg-red-900/50 p-1 rounded transition-colors"><X size={14}/></button>
                                </div>
                              </td>
                            </tr>
                          ) : (
                            <tr 
                              key={item.id} 
                              ref={(el) => { itemRefs.current[item.id] = el; }}
                              onClick={() => toggleItemCheck(item.id)}
                              className={`cursor-pointer transition-colors ${index % 2 === 0 ? 'bg-indigo-50/30 dark:bg-gray-700/30' : 'bg-white dark:bg-gray-800'} ${item.checked ? 'opacity-50' : ''} ${highlightedItemId === item.id ? 'bg-green-100 dark:bg-green-900/30' : ''}`}
                            >
                              <td className="px-3 py-3">
                                <span className={`${item.checked ? 'line-through text-gray-500 dark:text-gray-500' : 'text-gray-900 dark:text-gray-100'}`}>{item.name}</span>
                              </td>
                              <td className="px-3 py-3 text-center" onClick={(e) => { e.stopPropagation(); startInlineEdit(item, e); }}>
                                <span className={`${item.checked ? 'line-through text-gray-500 dark:text-gray-500' : 'text-gray-700 dark:text-gray-300'}`}>{item.quantity} {item.unit}</span>
                              </td>
                              <td className="px-3 py-3 text-right" onClick={(e) => { e.stopPropagation(); startInlineEdit(item, e); }}>
                                <span className={`${item.checked ? 'line-through text-gray-500 dark:text-gray-500' : 'text-gray-900 dark:text-gray-100'}`}>{item.unitPrice}</span>
                              </td>
                              <td className="px-3 py-3 text-right" onClick={(e) => { e.stopPropagation(); startInlineEdit(item, e); }}>
                                <span className={`${item.checked ? 'line-through text-gray-500 dark:text-gray-500' : 'text-gray-900 dark:text-gray-100 font-medium'}`}>{item.total}</span>
                              </td>
                              <td className="px-3 py-3 text-center">
                                <button 
                                  onClick={(e) => { e.stopPropagation(); handleRemoveItem(item.id); }} 
                                  className="text-indigo-500 dark:text-indigo-400 hover:text-red-500 dark:hover:text-red-400 bg-indigo-100 dark:bg-indigo-900/30 hover:bg-red-100 dark:hover:bg-red-900/30 p-1.5 rounded transition-colors"
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
                      <span>{t('totalItems')}: {items.length}</span>
                      <span>{t('totalAmount')}: {currencySymbol}{totalAmount.toLocaleString()}</span>
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>,
        document.body
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {memos.map(memo => (
          <div key={memo.id} className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 flex flex-col">
            <div className="flex justify-between items-start mb-4">
              <div 
                onClick={() => handleEditMemo(memo)}
                className="cursor-pointer hover:opacity-80 transition-opacity"
              >
                <h3 className="font-bold text-lg text-gray-900 dark:text-white">{memo.title}</h3>
                <p className="text-xs text-gray-400 dark:text-gray-500">
                  {memo.createdAt ? format(new Date(memo.createdAt), 'MMM d, yyyy h:mm a') : ''}
                </p>
              </div>
              <div className="flex gap-2">
                <button 
                  onClick={() => setSharingMemo(memo)} 
                  className="p-2 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-900/30 rounded-lg transition-colors"
                  title={t('shareMemo')}
                >
                  <Share2 size={18} />
                </button>
                <button 
                  onClick={() => handleEditMemo(memo)} 
                  className="p-2 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/30 rounded-lg transition-colors"
                  title="কেনাকাটা / এডিট"
                >
                  <ShoppingCart size={18} />
                </button>
                <button 
                  onClick={() => handleDeleteMemo(memo.id)} 
                  className="p-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-lg transition-colors"
                  title={t('deleteMemo')}
                >
                  <Trash2 size={18} />
                </button>
              </div>
            </div>
            
            <div className="flex-1">
              <ul className="space-y-2 mb-4">
                {memo.items.slice(0, 3).map((item: any, idx: number) => (
                  <li key={idx} className="flex justify-between text-sm">
                    <span className="text-gray-600 dark:text-gray-400">{item.name} <span className="text-xs text-gray-400 dark:text-gray-500">({item.quantity}{item.unit})</span></span>
                    <span className="font-medium text-gray-900 dark:text-gray-100">{currencySymbol}{item.total}</span>
                  </li>
                ))}
                {memo.items.length > 3 && (
                  <li className="text-xs text-gray-400 dark:text-gray-500 italic">+{memo.items.length - 3} {t('moreItems')}</li>
                )}
              </ul>
            </div>

            <div className="pt-4 border-t border-gray-100 dark:border-gray-700">
              <div className="space-y-2 mb-4">
                <div className="flex justify-between items-center text-xs">
                  <span className="text-gray-500 dark:text-gray-400">{t('purchased')}</span>
                  <span className="text-green-600 dark:text-green-400 font-bold">{currencySymbol}{(memo.items?.filter((i: any) => i.checked).reduce((s: number, i: any) => s + i.total, 0) || 0).toLocaleString()}</span>
                </div>
                <div className="flex justify-between items-center text-xs">
                  <span className="text-gray-500 dark:text-gray-400">{t('remaining')}</span>
                  <span className="text-red-600 dark:text-red-400 font-bold">{currencySymbol}{(memo.totalAmount - (memo.items?.filter((i: any) => i.checked).reduce((s: number, i: any) => s + i.total, 0) || 0)).toLocaleString()}</span>
                </div>
                <div className="w-full bg-gray-100 dark:bg-gray-700 h-1.5 rounded-full overflow-hidden">
                  <div 
                    className="bg-indigo-600 dark:bg-indigo-500 h-full transition-all duration-500" 
                    style={{ width: `${(memo.items?.filter((i: any) => i.checked).reduce((s: number, i: any) => s + i.total, 0) || 0) / memo.totalAmount * 100}%` }}
                  ></div>
                </div>
              </div>
              <div className="flex justify-between items-center mb-4">
                <span className="text-sm font-medium text-gray-500 dark:text-gray-400">{t('totalAmount')}</span>
                <span className="text-lg font-bold text-indigo-600 dark:text-indigo-400">{currencySymbol}{memo.totalAmount.toLocaleString()}</span>
              </div>
              
              {memo.expenseId ? (
                <div className="w-full flex justify-center items-center px-4 py-2 bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300 rounded-lg border border-green-200 dark:border-green-800">
                  <CheckCircle size={16} className="mr-2" />
                  <span className="text-sm font-medium">{t('addedToExpenses')}</span>
                </div>
              ) : (
                <button
                  onClick={() => handleConvertToExpense(memo)}
                  className="w-full flex justify-center items-center px-4 py-2 bg-gray-50 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg border border-gray-200 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors"
                >
                  <Receipt size={16} className="mr-2" />
                  {t('convertToExpense')}
                </button>
              )}
            </div>

            {/* Hidden Capture Div for Image/PDF */}
            <div id={`memo-capture-${memo.id}`} className="absolute left-[-9999px] top-[-9999px] bg-white p-8 w-[600px] font-sans">
              <div className="border-b-2 border-indigo-600 pb-4 mb-6">
                <h2 className="text-3xl font-bold text-gray-900 mb-2">🛒 {t('marketMemoTitle')}</h2>
                <h3 className="text-xl text-gray-700">{memo.title}</h3>
                {memo.createdAt && <p className="text-gray-500 mt-2">{t('date')}: {format(new Date(memo.createdAt), 'MMMM d, yyyy')}</p>}
              </div>
              
              <table className="w-full mb-6">
                <thead>
                  <tr className="border-b border-gray-300 text-left">
                    <th className="py-2 text-gray-600">{t('item')}</th>
                    <th className="py-2 text-right text-gray-600">{t('qty')}</th>
                    <th className="py-2 text-right text-gray-600">{t('price')}</th>
                    <th className="py-2 text-right text-gray-600">{t('total')}</th>
                  </tr>
                </thead>
                <tbody>
                  {memo.items.map((item: any, idx: number) => (
                    <tr key={idx} className="border-b border-gray-100">
                      <td className="py-3 text-gray-900 font-medium">{item.name}</td>
                      <td className="py-3 text-right text-gray-600">{item.quantity} {t(item.unit)}</td>
                      <td className="py-3 text-right text-gray-600">{currencySymbol}{item.unitPrice}</td>
                      <td className="py-3 text-right text-gray-900 font-bold">{currencySymbol}{item.total}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              
              <div className="flex justify-between items-center bg-indigo-50 p-4 rounded-lg">
                <span className="text-xl font-bold text-indigo-900">{t('grandTotal')}</span>
                <span className="text-2xl font-bold text-indigo-700">{currencySymbol}{memo.totalAmount.toLocaleString()}</span>
              </div>

              <div className="mt-8 pt-6 border-t border-gray-100 text-center text-xs text-gray-400">
                <p className="font-bold text-gray-500 mb-1">{t('developedBy')}: Asadullah Al Galib</p>
                <p className="mb-2">B.Sc in CSE, 01911777694</p>
                <p>{t('createdWith')}</p>
              </div>
            </div>
          </div>
        ))}
        {memos.length === 0 && !isAdding && (
          <div className="col-span-full text-center py-12 text-gray-500">
            <ShoppingCart className="mx-auto h-12 w-12 text-gray-300 mb-3" />
            <p>{t('noMemosFound')}</p>
          </div>
        )}
      </div>

      {/* Share Modal */}
      {sharingMemo && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-sm relative">
            <button 
              onClick={() => setSharingMemo(null)}
              className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 hover:bg-gray-100 p-1 rounded-full transition-colors"
            >
              <X size={20} />
            </button>
            <h3 className="text-xl font-bold text-gray-900 mb-6">{t('shareMemo')}</h3>
            
            <div className="grid grid-cols-2 gap-4">
              <button onClick={() => handleShareText(sharingMemo)} className="flex flex-col items-center justify-center p-4 bg-gray-50 hover:bg-indigo-50 hover:text-indigo-600 rounded-xl transition-colors border border-gray-100">
                <FileText size={24} className="mb-2" />
                <span className="text-sm font-medium">{t('text')}</span>
              </button>
              <button onClick={() => handleShareImage(sharingMemo)} className="flex flex-col items-center justify-center p-4 bg-gray-50 hover:bg-indigo-50 hover:text-indigo-600 rounded-xl transition-colors border border-gray-100">
                <ImageIcon size={24} className="mb-2" />
                <span className="text-sm font-medium">{t('image')}</span>
              </button>
              <button onClick={() => handleSharePDF(sharingMemo)} className="flex flex-col items-center justify-center p-4 bg-gray-50 hover:bg-indigo-50 hover:text-indigo-600 rounded-xl transition-colors border border-gray-100">
                <FileText size={24} className="mb-2" />
                <span className="text-sm font-medium">{t('pdf')}</span>
              </button>
              <button onClick={() => handleShareLink(sharingMemo)} className="flex flex-col items-center justify-center p-4 bg-gray-50 hover:bg-indigo-50 hover:text-indigo-600 rounded-xl transition-colors border border-gray-100">
                <LinkIcon size={24} className="mb-2" />
                <span className="text-sm font-medium">{t('link')}</span>
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* Success Modal */}
      {successMessage && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
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
            <h3 className="text-xl font-bold text-gray-900 mb-2">{t('success')}</h3>
            <p className="text-gray-600 text-center">{successMessage}</p>
          </div>
        </div>,
        document.body
      )}

      {/* Delete Confirmation Modal */}
      {itemToDelete && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl p-6 max-w-sm w-full relative animate-in zoom-in-95 duration-200">
            <h3 className="text-xl font-bold text-gray-900 mb-2">{t('deleteItem')}</h3>
            <p className="text-gray-600 mb-6">{t('deleteItemConfirm')}</p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setItemToDelete(null)}
                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors font-medium"
              >
                {t('cancel')}
              </button>
              <button
                onClick={confirmRemoveItem}
                className="px-4 py-2 bg-red-600 text-white hover:bg-red-700 rounded-lg transition-colors font-medium"
              >
                {t('delete')}
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* Delete Memo Confirmation Modal */}
      {memoToDelete && createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl p-6 max-w-sm w-full relative animate-in zoom-in-95 duration-200">
            <h3 className="text-xl font-bold text-gray-900 mb-2">{t('deleteMemo')}</h3>
            <p className="text-gray-600 mb-6">{t('deleteMemoConfirm')}</p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setMemoToDelete(null)}
                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors font-medium"
              >
                {t('cancel')}
              </button>
              <button
                onClick={confirmDeleteMemo}
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

export default MarketMemo;