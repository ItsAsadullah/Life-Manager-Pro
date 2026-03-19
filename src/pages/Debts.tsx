import React, { useEffect, useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useAuth } from '../contexts/AuthContext';
import { useSettings } from '../contexts/SettingsContext';
import { collection, query, onSnapshot, orderBy, addDoc, doc, updateDoc, deleteDoc, getDocs } from 'firebase/firestore';
import { useLocation } from 'react-router-dom';
import { db } from '../lib/firebase';
import { 
  ArrowDownCircle, ArrowUpCircle, UserRound, Phone, MapPin, 
  Calendar, Wallet, ImagePlus, X, MoreVertical, Search, 
  ArrowLeft, FileText, ArrowDownLeft, ArrowUpRight, Check, Plus, Divide, Download, Loader2
} from 'lucide-react';
import { jsPDF } from 'jspdf';
import html2canvas from 'html2canvas';

interface Repayment {
  id: string;
  amount: number;
  type: 'got' | 'gave';
  date: string;
  note?: string;
  createdAt: string;
}

interface Debt {
  id: string;
  personName: string;
  phoneNumber?: string;
  address?: string;
  imageUrl?: string;
  accountId?: string;
  date?: string;
  amount: number;
  type: 'borrowed' | 'lent'; // borrowed = dibo (I owe), lent = pabo (They owe)
  createdAt: string;
}

export const Debts: React.FC = () => {
  const { user } = useAuth();
  const { t, currencySymbol } = useSettings();
  
  const [debts, setDebts] = useState<Debt[]>([]);
  
  const [activeMenuId, setActiveMenuId] = useState<string | null>(null);
const [searchQuery, setSearchQuery] = useState('');
  
  const location = useLocation();
  
  useEffect(() => {
    const handleClickOutside = () => setActiveMenuId(null);
    window.addEventListener('click', handleClickOutside);
    return () => window.removeEventListener('click', handleClickOutside);
  }, []);
useEffect(() => {
    if (location.state?.openAddModal) {
      setShowAddPerson(true);
      window.history.replaceState({}, document.title);
    }
  }, [location]);
  
  // Navigation State
  const [selectedPerson, setSelectedPerson] = useState<Debt | null>(null);

  // Add Person Modal States
  const [editingPersonId, setEditingPersonId] = useState<string | null>(null);
  const [showAddPerson, setShowAddPerson] = useState(false);
  const [personName, setPersonName] = useState('');
  const [personAddress, setPersonAddress] = useState('');
  const [personImage, setPersonImage] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [personDate, setPersonDate] = useState(new Date().toISOString().split('T')[0]);
  const [personAccount, setPersonAccount] = useState('ক্যাশ');

  // Add Transaction Modal States
  const [showAddTransaction, setShowAddTransaction] = useState(false);
  const [trxType, setTrxType] = useState<'got' | 'gave'>('gave');
  const [trxAmount, setTrxAmount] = useState('');
  const [trxNote, setTrxNote] = useState('');
  const [trxDate, setTrxDate] = useState(new Date().toISOString().split('T')[0]);
  const [trxAccount, setTrxAccount] = useState('ক্যাশ');
  const [editingTrxId, setEditingTrxId] = useState<string | null>(null);

  // Repayments for selected person
  const [repayments, setRepayments] = useState<Repayment[]>([]);

  useEffect(() => {
    if (!user) return;
    const debtsRef = collection(db, 'users', user.uid, 'debts');
    const q = query(debtsRef);
    
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const debtsData: Debt[] = [];
      snapshot.forEach((doc) => {
        debtsData.push({ id: doc.id, ...doc.data() } as Debt);
      });
      debtsData.sort((a,b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime());
        setDebts(debtsData);
      
      // Update selected person reference if it changes
      if (selectedPerson) {
        const updated = debtsData.find(d => d.id === selectedPerson.id);
        if (updated) setSelectedPerson(updated);
      }
    });

    return () => unsubscribe();
  }, [user, selectedPerson?.id]);

  useEffect(() => {
    if (!user || !selectedPerson) return;
    const repaymentsRef = collection(db, 'users', user.uid, 'debts', selectedPerson.id, 'repayments');
    const q = query(repaymentsRef);
    
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const repData: Repayment[] = [];
      snapshot.forEach((doc) => {
        const d = doc.data();
        let derivedType = 'gave';
        let derivedNote = d.note || '';
        if (d.note && (d.note.startsWith('got|') || d.note.startsWith('gave|'))) {
          const parts = d.note.split('|');
          derivedType = parts[0];
          derivedNote = parts.slice(1).join('|');
        } else if (d.type) { 
           derivedType = d.type; 
        }
        repData.push({ id: doc.id, ...d, type: derivedType, note: derivedNote } as Repayment);
      });
      repData.sort((a,b) => new Date(b.createdAt || b.date || 0).getTime() - new Date(a.createdAt || a.date || 0).getTime());
        setRepayments(repData);
    });

    return () => unsubscribe();
    }, [user, selectedPerson?.id]);
  // Aggregate Stats
  const { totalPabo, totalDibo } = useMemo(() => {
    let p = 0;
    let d = 0;
    debts.forEach(debt => {
      if (debt.type === 'lent') p += debt.amount;
      if (debt.type === 'borrowed') d += debt.amount;
    });
    return { totalPabo: p, totalDibo: d };
  }, [debts]);

  const filteredDebts = debts.filter(d => 
    d.personName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    (d.phoneNumber && d.phoneNumber.includes(searchQuery))
  );

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        const img = new Image();
        img.onload = () => {
          const canvas = document.createElement('canvas');
          const maxDim = 300;
          let width = img.width;
          let height = img.height;
          if (width > height && width > maxDim) {
            height *= maxDim / width;
            width = maxDim;
          } else if (height > maxDim) {
            width *= maxDim / height;
            height = maxDim;
          }
          canvas.width = width;
          canvas.height = height;
          const ctx = canvas.getContext('2d');
          ctx?.drawImage(img, 0, 0, width, height);
          setPersonImage(canvas.toDataURL('image/jpeg', 0.8));
        };
        img.src = event.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  };


  const handleSavePerson = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !personName.trim()) return;

    try {
      if (editingPersonId) {
        await updateDoc(doc(db, 'users', user.uid, 'debts', editingPersonId), {
          personName,
          phoneNumber,
          address: personAddress,
          imageUrl: personImage,
          accountId: personAccount,
          date: new Date(personDate).toISOString(),
          updatedAt: new Date().toISOString()
        });

        // Update selectedPerson if it is currently open
        if (selectedPerson && selectedPerson.id === editingPersonId) {
            setSelectedPerson({
                ...selectedPerson,
                personName,
                phoneNumber,
                address: personAddress,
                imageUrl: personImage,
                accountId: personAccount,
                date: new Date(personDate).toISOString(),
            });
        }
      } else {
        await addDoc(collection(db, 'users', user.uid, 'debts'), {
            personName,
            phoneNumber,
            address: personAddress,
            imageUrl: personImage,
            accountId: personAccount,
            date: new Date(personDate).toISOString(),
            amount: 0,
            type: 'lent',
            status: 'pending',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          });
      }
      setShowAddPerson(false);
      setEditingPersonId(null);
      setPersonName('');
      setPhoneNumber('');
      setPersonAddress('');
      setPersonImage('');
    } catch (error) {
      console.error('Error saving person:', error);
    }
  };

  const syncPersonBalance = async (personId: string) => {
    if(!user) return;
    try {
      const snap = await getDocs(collection(db, 'users', user.uid, 'debts', personId, 'repayments'));
      let net = 0;
      snap.docs.forEach(doc => {
        const d = doc.data();
        let t = 'gave';
        if (d.note && (d.note.startsWith('got|') || d.note.startsWith('gave|'))) { t = d.note.split('|')[0]; }
        else if (d.type) { t = d.type; }
        
        let amt = Number(d.amount) || 0;
        if(t === 'gave') net += amt;
        else net -= amt;
      });
      
      const newType = net >= 0 ? 'lent' : 'borrowed';
      await updateDoc(doc(db, 'users', user.uid, 'debts', personId), {
        amount: Math.abs(net),
        type: newType,
        updatedAt: new Date().toISOString()
      });
    } catch(err) { console.error('Sync Error', err); }
  };

  const [isGeneratingPDF, setIsGeneratingPDF] = useState(false);

  const handleDownloadPDF = async () => {
    if (!selectedPerson) return;
    setIsGeneratingPDF(true);
    
    try {
      const container = document.createElement('div');
      container.style.position = 'absolute';
      container.style.left = '-9999px';
      container.style.top = '0';
      container.style.width = '800px';
      container.style.backgroundColor = '#ffffff';
      container.style.padding = '40px';
      container.style.color = '#000000';
      container.style.fontFamily = '"Hind Siliguri", sans-serif'; 
      
      let totalGot = 0;
      let totalGave = 0;
      repayments.forEach(r => {
        if(r.type === 'got') totalGot += r.amount;
        else totalGave += r.amount;
      });

      container.innerHTML = `
        <div style="text-align: center; margin-bottom: 30px; border-bottom: 2px solid #eee; padding-bottom: 20px;">
          <h1 style="font-size: 28px; margin: 0; color: #1e3a8a;">হিসাব নিকাশ</h1>
          <p style="color: #666; margin-top: 5px;">দেনা-পাওনার রিপোর্ট</p>
        </div>
        
        <div style="display: flex; justify-content: space-between; margin-bottom: 30px;">
          <div>
            <h2 style="margin: 0; font-size: 20px; color: #333;">${selectedPerson.personName}</h2>
            ${selectedPerson.phoneNumber ? `<p style="margin: 5px 0 0; color: #666;">মোবাইল: ${selectedPerson.phoneNumber}</p>` : ''}
            ${selectedPerson.address ? `<p style="margin: 5px 0 0; color: #666;">ঠিকানা: ${selectedPerson.address}</p>` : ''}
          </div>
          <div style="text-align: right;">
            <p style="margin: 0; color: #666;">রিপোর্ট তৈরির তারিখ:</p>
            <p style="margin: 5px 0 0; font-weight: bold;">${new Date().toLocaleDateString('bn-BD')}</p>
          </div>
        </div>
        
        <div style="display: flex; gap: 20px; margin-bottom: 30px;">
           <div style="flex: 1; padding: 15px; border-radius: 8px; background: #f8fafc; border: 1px solid #e2e8f0;">
              <p style="margin: 0; color: #64748b; font-size: 14px;">বর্তমান ব্যালেন্স</p>
              <h3 style="margin: 5px 0 0; font-size: 22px; color: ${selectedPerson.amount === 0 ? '#333' : selectedPerson.type === 'lent' ? '#16a34a' : '#dc2626'};">
                 ${selectedPerson.amount === 0 ? '৳ ০ (সমান)' : `৳ ${selectedPerson.amount.toLocaleString('en-US')} (${selectedPerson.type === 'lent' ? 'পাবো' : 'দিবো'})`}
              </h3>
           </div>
           <div style="flex: 1; padding: 15px; border-radius: 8px; background: #f0fdf4; border: 1px solid #bbf7d0;">
              <p style="margin: 0; color: #16a34a; font-size: 14px;">মোট পেয়েছি</p>
              <h3 style="margin: 5px 0 0; font-size: 20px; color: #16a34a;">৳ ${totalGot.toLocaleString('en-US')}</h3>
           </div>
           <div style="flex: 1; padding: 15px; border-radius: 8px; background: #fef2f2; border: 1px solid #fecaca;">
              <p style="margin: 0; color: #dc2626; font-size: 14px;">মোট দিয়েছি</p>
              <h3 style="margin: 5px 0 0; font-size: 20px; color: #dc2626;">৳ ${totalGave.toLocaleString('en-US')}</h3>
           </div>
        </div>

        <h3 style="margin: 0 0 15px; font-size: 18px; color: #333; border-bottom: 1px solid #eee; padding-bottom: 10px;">লেনদেনের বিবরণ</h3>
        
        <table style="width: 100%; border-collapse: collapse; margin-bottom: 30px;">
          <thead>
            <tr style="background-color: #f1f5f9;">
              <th style="padding: 12px; text-align: left; border: 1px solid #cbd5e1; color: #475569;">তারিখ</th>
              <th style="padding: 12px; text-align: left; border: 1px solid #cbd5e1; color: #475569;">বিবরণ</th>
              <th style="padding: 12px; text-align: right; border: 1px solid #cbd5e1; color: #475569;">পরিমাণ (৳)</th>
            </tr>
          </thead>
          <tbody>
            ${repayments.length > 0 ? repayments.map(rep => `
              <tr>
                <td style="padding: 10px; border: 1px solid #e2e8f0; color: #333;">${new Date(rep.createdAt || rep.date).toLocaleString('en-GB', { day:'numeric', month:'short', year:'numeric', hour:'numeric', minute:'2-digit' })}</td>
                <td style="padding: 10px; border: 1px solid #e2e8f0; color: #555;">
                   <span style="font-weight: bold; color: ${rep.type === 'got' ? '#16a34a' : '#dc2626'}">${rep.type === 'got' ? 'পেলাম' : 'দিলাম'}</span>
                   ${rep.note ? `<br/><span style="font-size: 12px;">${rep.note}</span>` : ''}
                </td>
                <td style="padding: 10px; border: 1px solid #e2e8f0; text-align: right; font-weight: bold; color: ${rep.type === 'got' ? '#16a34a' : '#dc2626'}">${rep.amount.toLocaleString('en-US')}</td>
              </tr>
            `).join('') : `
              <tr>
                 <td colspan="3" style="padding: 20px; text-align: center; border: 1px solid #e2e8f0; color: #64748b;">কোনো লেনদেন পাওয়া যায়নি</td>
              </tr>
            `}
          </tbody>
        </table>

        <div style="text-align: center; margin-top: 40px; padding-top: 20px; border-top: 1px dashed #cbd5e1; color: #94a3b8; font-size: 12px;">
          <p>এই রিপোর্টটি <strong>হিসাব নিকাশ</strong> অ্যাপ দ্বারা স্বয়ংক্রিয়ভাবে তৈরি করা হয়েছে।</p>
        </div>
      `;

      document.body.appendChild(container);
      
      const canvas = await html2canvas(container, {
        scale: 2,
        useCORS: true,
        logging: false
      });
      
      document.body.removeChild(container);
      
      const imgData = canvas.toDataURL('image/png');
      const pdf = new jsPDF('p', 'mm', 'a4');
      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pdfHeight = (canvas.height * pdfWidth) / canvas.width;
      
      pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight);
      pdf.save(`Transactions_${selectedPerson.personName.replace(/\s+/g, '_')}.pdf`);
      
    } catch (err) {
      console.error("Error generating PDF:", err);
      alert("PDF তৈরি করতে সমস্যা হয়েছে!");
    } finally {
      setIsGeneratingPDF(false);
    }
  };

  const handleSaveTransaction = async (e: React.FormEvent) => {
      e.preventDefault();
      if (!user || !selectedPerson || !trxAmount) return;

      const amountVal = parseFloat(trxAmount);
      if (isNaN(amountVal) || amountVal <= 0) return;

      try {
          if(editingTrxId) {
              await updateDoc(doc(db, 'users', user.uid, 'debts', selectedPerson.id, 'repayments', editingTrxId), {
                  amount: amountVal,
                  date: new Date(trxDate).toISOString(),
                  note: `${trxType}|${trxNote}`,
                  updatedAt: new Date().toISOString()
              });
          } else {
              await addDoc(collection(db, 'users', user.uid, 'debts', selectedPerson.id, 'repayments'), {
                  amount: amountVal,
                  date: new Date(trxDate).toISOString(),
                  note: `${trxType}|${trxNote}`,
                  createdAt: new Date().toISOString()
              });
          }
          await syncPersonBalance(selectedPerson.id);
          setShowAddTransaction(false);
          setTrxAmount('');
          setTrxNote('');
          setEditingTrxId(null);
      } catch (err) {
          console.error('REPAYMENT ERROR:', err);
      }
    };

  if (selectedPerson) {
    // ==== Person Details View (Screenshot 3 & 4) ====
    return createPortal(
      <div className="fixed inset-0 z-[50] justify-center bg-gray-50 dark:bg-gray-900 animate-in slide-in-from-right duration-200 flex flex-col items-center">
        <div className="w-full max-w-2xl mx-auto h-full flex flex-col bg-gray-50 dark:bg-gray-900 relative">
        
        {/* Fixed Header Region */}
        <div className="flex-shrink-0 w-full z-20 pb-4 shadow-sm border-b border-gray-200 dark:border-gray-800 bg-gray-50 dark:bg-gray-900 pt-safe relative">
          <div className="bg-white dark:bg-gray-800 px-4 py-4 flex items-center justify-between border-b border-gray-100 dark:border-gray-700">
            <button onClick={() => setSelectedPerson(null)} className="p-2 -ml-2 text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition-colors">
              <ArrowLeft size={24} />
            </button>
            <h1 className="text-xl font-bold text-gray-800 dark:text-white">ব্যক্তির বিবরণ</h1>
            <button 
              onClick={handleDownloadPDF} 
              disabled={isGeneratingPDF}
              className={`p-2 rounded-full transition-colors flex items-center justify-center ${isGeneratingPDF ? 'text-blue-400 bg-blue-50 dark:bg-blue-900/30 cursor-not-allowed' : 'text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700'}`}
            >
              {isGeneratingPDF ? <Loader2 size={24} className="animate-spin" /> : <Download size={24} />}
            </button>
          </div>

          <div className="px-4 pt-4 space-y-4 w-full">
            {/* Person Header */}
          <div className="flex items-center gap-4 bg-white dark:bg-gray-800 p-4 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700">
            <div className="w-16 h-16 rounded-full bg-blue-50 dark:bg-blue-900/30 flex items-center justify-center overflow-hidden border border-blue-100 dark:border-blue-800 text-blue-500 shadow-inner">
              {selectedPerson.imageUrl ? (
                <img src={selectedPerson.imageUrl} alt="" className="w-full h-full object-cover" />
              ) : (
                <UserRound size={32} strokeWidth={1.5} />
              )}
            </div>
            <div>
              <h2 className="text-xl font-bold text-gray-900 dark:text-white">{selectedPerson.personName}</h2>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                {selectedPerson.date ? new Date(selectedPerson.date).toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' }) : 'No date'}
              </p>
            </div>
          </div>

          {/* Balance Card */}
          {selectedPerson.amount > 0 && (
            <div className={`p-4 rounded-xl shadow-sm border flex items-center justify-between ${selectedPerson.type === 'lent' ? 'bg-green-50 border-green-100 dark:bg-green-900/20 dark:border-green-800/30 text-green-700 dark:text-green-400' : 'bg-red-50 border-red-100 dark:bg-red-900/20 dark:border-red-800/30 text-red-700 dark:text-red-400'}`}>
              <div className="flex items-center gap-2 font-bold">
                {selectedPerson.type === 'lent' ? <ArrowDownLeft size={20} /> : <ArrowUpRight size={20} />}
                <span>{selectedPerson.type === 'lent' ? 'পাবো' : 'দিবো'}</span>
              </div>
              <div className="text-xl font-bold">
                {currencySymbol} {selectedPerson.amount.toLocaleString()}
              </div>
            </div>
          )}

          {selectedPerson.amount === 0 && (
             <div className="p-4 rounded-xl shadow-sm border bg-gray-50 border-gray-200 dark:bg-gray-800 text-gray-600 dark:border-gray-700 flex items-center justify-between">
                <div className="font-bold flex items-center gap-2"><Check size={20}/> ব্যালেন্স সমান</div>
                <div className="text-xl font-bold">৳ ০</div>
             </div>
          )}
          </div>
          
          <div className="absolute -bottom-4 left-0 right-0 h-4 bg-gradient-to-b from-gray-50/50 dark:from-gray-900/50 to-transparent pointer-events-none"></div>
        </div>

        {/* Scrollable Region */}
        <div className="flex-1 overflow-y-auto px-4 w-full pb-[100px] pt-4">
          {/* Transactions List */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {repayments.map(rep => (
              <div key={rep.id} className={`p-4 rounded-2xl border flex items-center justify-between shadow-sm ${rep.type === 'got' ? 'bg-green-50/50 border-green-100 dark:bg-green-900/10 dark:border-green-800/30' : 'bg-red-50/50 border-red-100 dark:bg-red-900/10 dark:border-red-800/30'}`}>
                <div className="flex items-center gap-3">
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center ${rep.type === 'got' ? 'bg-green-100 text-green-600 dark:bg-green-900/40 dark:text-green-400' : 'bg-red-100 text-red-600 dark:bg-red-900/40 dark:text-red-400'}`}>
                    {rep.type === 'got' ? <ArrowDownLeft size={20} /> : <ArrowUpRight size={20} />}
                  </div>
                  <div>
                    <h3 className="font-bold text-gray-800 dark:text-gray-100">{rep.type === 'got' ? 'পেলাম' : 'দিলাম'}</h3>
                    <p className="text-xs text-gray-500 dark:text-gray-400">{new Date(rep.createdAt).toLocaleString('en-GB', { day:'numeric', month:'short', year:'numeric', hour:'numeric', minute:'2-digit' })}</p>
                    {rep.note && <p className="text-xs text-gray-600 mt-0.5">{rep.note}</p>}
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className={`font-bold text-lg ${rep.type === 'got' ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}`}>
                    {rep.amount.toLocaleString()}
                  </span>
                  <div className="relative" onClick={(e) => { e.preventDefault(); e.stopPropagation(); }}>
                  <button 
                    onClick={(e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        setActiveMenuId(activeMenuId === rep.id ? null : rep.id);
                      }}
                    className="text-gray-400 hover:text-gray-600 p-1 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition"
                  >
                    <MoreVertical size={20} />
                  </button>
                  {activeMenuId === rep.id && (
                    <div className="absolute right-0 mt-2 w-32 bg-white dark:bg-gray-800 rounded-xl shadow-lg border border-gray-100 dark:border-gray-700 py-1 z-[9999] animate-in fade-in zoom-in-95 duration-100">
                      <button 
                        onClick={(e) => {
                            e.stopPropagation();
                            setActiveMenuId(null);
                            setEditingTrxId(rep.id);
                            setTrxAmount(rep.amount.toString());
                            setTrxType(rep.type);
                            setTrxNote(rep.note || '');
                            if(rep.date) setTrxDate(rep.date.split('T')[0]);
                            setShowAddTransaction(true);
                          }}
                          className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700"
                      >
                       এডিট
                      </button>
                      <button 
                         onClick={async (e) => {
                          e.stopPropagation();
                          setActiveMenuId(null);
                          if(window.confirm('আপনি কি এই লেনদেন ডিলিট করতে চান?')) {
                             try {
                               await deleteDoc(doc(db, 'users', user!.uid, 'debts', selectedPerson.id, 'repayments', rep.id));
                                 await syncPersonBalance(selectedPerson.id);
                             } catch(err) { console.error(err) }
                          }
                        }} 
                        className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20"
                      >
                       ডিলেট
                      </button>
                    </div>
                  )}
                </div>
                </div>
              </div>
            ))}
            {repayments.length === 0 && (
              <div className="text-center py-10 text-gray-500">কোনো লেনদেন পাওয়া যায়নি</div>
            )}
          </div>
        </div>

        {/* FAB for Transaction */}
        <button 
          onClick={() => { setEditingTrxId(null); setTrxAmount(''); setTrxNote(''); setTrxDate(new Date().toISOString().split('T')[0]); setShowAddTransaction(true); }}
          className="fixed bottom-6 right-6 w-14 h-14 bg-blue-600 text-white rounded-full flex items-center justify-center shadow-lg hover:bg-blue-700 transition transform hover:scale-105 active:scale-95"
        >
          <Plus size={30} />
        </button>

        {/* Add Transaction Modal */}
        {showAddTransaction && createPortal(
          <div className="fixed inset-0 z-[50] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="bg-white dark:bg-gray-800 rounded-3xl w-full max-w-sm overflow-hidden shadow-2xl animate-in zoom-in-95 duration-200 p-6 relative">
              <button 
                onClick={() => setShowAddTransaction(false)} 
                className="absolute top-4 right-4 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full p-2 transition"
              >
                <X size={20} />
              </button>

              <h2 className="text-xl font-bold text-center text-gray-800 dark:text-white mb-6 pt-2">লেনদেন অ্যাড করুন</h2>

              <form onSubmit={handleSaveTransaction} className="space-y-4">
                
                {/* Got/Gave Segmented Control */}
                <div className="flex p-1 bg-gray-100 dark:bg-gray-700 rounded-xl relative">
                  <button
                    type="button"
                    onClick={() => setTrxType('got')}
                    className={`flex-1 py-3 text-center text-sm font-bold rounded-lg transition-all ${trxType === 'got' ? 'bg-green-500 text-white shadow-md' : 'text-gray-600 dark:text-gray-300'}`}
                  >
                    পেলাম
                  </button>
                  <button
                    type="button"
                    onClick={() => setTrxType('gave')}
                    className={`flex-1 py-3 text-center text-sm font-bold rounded-lg transition-all ${trxType === 'gave' ? 'bg-red-500 text-white shadow-md' : 'text-gray-600 dark:text-gray-300'}`}
                  >
                    দিলাম
                  </button>
                </div>

                {/* Amount */}
                <div className="relative border border-gray-200 dark:border-gray-600 rounded-xl overflow-hidden focus-within:ring-2 focus-within:ring-blue-500 bg-white dark:bg-gray-800">
                  <span className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500 font-bold">@</span>
                  <input required type="number" step="any" value={trxAmount} onChange={e => setTrxAmount(e.target.value)} placeholder="টাকার পরিমাণ" className="w-full bg-transparent border-none py-3.5 pl-12 pr-4 text-sm font-medium dark:text-white focus:ring-0 outline-none placeholder-gray-400" />
                </div>
                
                {/* Details */}
                <div className="relative border border-gray-200 dark:border-gray-600 rounded-xl overflow-hidden focus-within:ring-2 focus-within:ring-blue-500 bg-white dark:bg-gray-800">
                  <FileText size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500" />
                  <input type="text" value={trxNote} onChange={e => setTrxNote(e.target.value)} placeholder="বিবরণ (ঐচ্ছিক)" className="w-full bg-transparent border-none py-3.5 pl-12 pr-4 text-sm font-medium dark:text-white focus:ring-0 outline-none placeholder-gray-400" />
                </div>

                {/* Date */}
                <div className="relative border border-gray-200 dark:border-gray-600 rounded-xl overflow-hidden focus-within:ring-2 focus-within:ring-blue-500 bg-white dark:bg-gray-800">
                    <Calendar size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-blue-500 pointer-events-none" />
                    <input type="date" value={trxDate} onChange={e => setTrxDate(e.target.value)} className="w-full bg-transparent border-none py-3.5 pl-12 pr-4 text-sm font-medium dark:text-white focus:ring-0 outline-none cursor-pointer" />
                  </div>

                {/* Account */}
                <div className="relative border border-blue-200 dark:border-blue-900 rounded-xl overflow-hidden focus-within:ring-2 focus-within:ring-blue-500 bg-white dark:bg-gray-800 flex items-center h-[46px] shadow-sm">
                  <Wallet size={18} className="absolute left-4 text-gray-500" />
                  <select value={trxAccount} onChange={e => setTrxAccount(e.target.value)} className="w-full h-full appearance-none bg-transparent border-none pl-12 pr-10 text-sm font-medium text-gray-800 dark:text-white focus:ring-0 outline-none cursor-pointer">
                     <option value="ক্যাশ">ক্যাশ</option>
                     <option value="বিকাশ">বিকাশ</option>
                     <option value="নগদ">নগদ</option>
                     <option value="ব্যাংক">ব্যাংক একাউন্ট</option>
                  </select>
                  <div className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none text-xs">▼</div>
                </div>

                <div className="pt-2">
                  <button type="submit" className="w-full py-3.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-bold text-base transition-colors shadow-md">
                    সেভ
                  </button>
                </div>
              </form>
            </div>
          </div>, document.body
        )}
        </div>
      </div>, document.body
    );
  }


  // ==== Main List View (Screenshot 2) ====
  return (
    <div className="min-h-screen pb-24 max-w-6xl mx-auto w-full sm:px-4 dark:text-white animate-in fade-in">

      {/* Top Stats Banner */}
      <div className="bg-white dark:bg-gray-800 px-4 py-3 border-b border-gray-100 dark:border-gray-700 mb-4 sticky top-0 z-10 pt-safe transition-colors">
        <div className="flex items-center justify-between border border-gray-100 dark:border-gray-700 rounded-xl p-3 shadow-sm">
          <div className="flex-1 text-center border-r border-gray-100 dark:border-gray-700">
            <div className="flex items-center justify-center gap-1 text-xs sm:text-xs text-green-600 font-bold mb-1">
              <ArrowDownCircle size={14}/> পাবো
            </div>
            <div className="text-sm font-bold text-green-700 dark:text-green-500">{totalPabo}</div>
          </div>
          <div className="flex-1 text-center border-r border-gray-100 dark:border-gray-700">
            <div className="flex items-center justify-center gap-1 text-xs sm:text-xs text-red-600 font-bold mb-1">
              <ArrowUpCircle size={14}/> দিবো
            </div>
            <div className="text-sm font-bold text-red-700 dark:text-red-500">{totalDibo}</div>
          </div>
          <div className="flex-1 text-center">
            <div className="flex items-center justify-center gap-1 text-xs sm:text-xs text-blue-600 font-bold mb-1">
              <UserRound size={14}/> মোট
            </div>
            <div className="text-sm font-bold text-blue-700 dark:text-blue-500">{debts.length} জন</div>
          </div>
        </div>

        {/* Search */}
        <div className="mt-4 relative">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input 
            type="text" 
            placeholder="নাম বা ফোন নম্বর দিয়ে খুঁজুন..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-full py-3.5 pl-11 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-shadow text-gray-700 dark:text-gray-200 shadow-inner"
          />
        </div>
      </div>

      {/* Persons List */}
      <div className="px-4 pb-8 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredDebts.map((debt) => (
          <div
            key={debt.id}
            onClick={() => setSelectedPerson(debt)}
            className="flex items-center p-3 sm:p-4 bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-700 cursor-pointer hover:shadow-md transition-shadow active:scale-[0.99]"
          >
            <div className="w-12 h-12 rounded-full flex-shrink-0 bg-blue-50 dark:bg-blue-900/30 flex items-center justify-center text-blue-500 border border-blue-100 dark:border-blue-800/50 overflow-hidden shadow-inner">
               {debt.imageUrl ? (
                 <img src={debt.imageUrl} alt="" className="w-full h-full object-cover" />
               ) : (
                 <UserRound strokeWidth={1} size={24} />
               )}
            </div>
            
            <div className="ml-4 flex-1">
              <h3 className="font-semibold text-gray-800 dark:text-white text-base">{debt.personName}</h3>
              {debt.phoneNumber && <p className="text-xs text-gray-500">{debt.phoneNumber}</p>}
            </div>

            <div className="flex items-center gap-3">
               {debt.amount > 0 ? (
                 <div className={`text-sm font-bold ${debt.type === 'lent' ? 'text-green-600' : 'text-red-500'}`}>
                    {debt.amount}
                 </div>
               ) : (
                 <div className="text-sm font-bold text-gray-400">০</div>
               )}
               {/* Dot Indicator */}
               {debt.amount > 0 ? (
                 <div className={`w-2 h-2 rounded-full shadow-sm ${debt.type === 'lent' ? 'bg-green-500' : 'bg-red-500'}`}></div>
               ) : (
                 <div className="w-2 h-2 rounded-full shadow-sm bg-gray-300 dark:bg-gray-600"></div>
               )}
               <div className="relative" onClick={(e) => e.stopPropagation()}>
                  <button 
                    onClick={(e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        setActiveMenuId(activeMenuId === debt.id ? null : debt.id);
                      }}
                    className="p-1 -mr-2 text-gray-400 hover:text-gray-600 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition"
                  >
                    <MoreVertical size={20} />
                  </button>
                  
                  {activeMenuId === debt.id && (
                    <div className="absolute right-0 mt-2 w-32 bg-white dark:bg-gray-800 rounded-xl shadow-lg border border-gray-100 dark:border-gray-700 py-1 z-[9999] animate-in fade-in zoom-in-95 duration-100">
                      <button 
                        onClick={(e) => {
                          e.stopPropagation();
                          setActiveMenuId(null);
                          setEditingPersonId(debt.id);
                          setPersonName(debt.personName);
                          setPhoneNumber(debt.phoneNumber || '');
                          setPersonAddress(debt.address || '');
                          setPersonImage(debt.imageUrl || '');
                          if(debt.date) setPersonDate(debt.date.split('T')[0]);
                          if(debt.accountId) setPersonAccount(debt.accountId);
                          setShowAddPerson(true);
                        }} 
                        className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700 flex items-center gap-2"
                      >
                       এডিট
                      </button>
                      <button 
                         onClick={async (e) => {
                          e.stopPropagation();
                          setActiveMenuId(null);
                          if(window.confirm('আপনি কি নিশ্চিত যে এটি ডিলিট করতে চান?')) {
                             try {
                               await deleteDoc(doc(db, 'users', user!.uid, 'debts', debt.id));
                             } catch(err) { console.error(err) }
                          }
                        }} 
                        className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 flex items-center gap-2"
                      >
                       ডিলেট
                      </button>
                    </div>
                  )}
               </div>
            </div>
          </div>
        ))}
        {filteredDebts.length === 0 && (
          <div className="text-center text-gray-500 py-10">
             কোনো ব্যক্তি পাওয়া যায়নি
          </div>
        )}
      </div>

      {/* Main View FAB */}
      <button 
        onClick={() => { setEditingPersonId(null); setPersonName(''); setPhoneNumber(''); setPersonAddress(''); setPersonImage(''); setShowAddPerson(true); }}
        className="fixed bottom-[80px] right-6 w-14 h-14 bg-teal-600 hover:bg-teal-700 text-white rounded-full flex items-center justify-center shadow-lg transition-transform transform active:scale-95 z-20"
      >
        <Plus size={28} />
      </button>

      {/* Add Person Modal */}
      {showAddPerson && createPortal(
        <div className="fixed inset-0 z-[50] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white dark:bg-gray-800 rounded-3xl w-full max-w-sm overflow-hidden shadow-2xl animate-in zoom-in-95 duration-200 p-6 relative">
            <button onClick={() => { setShowAddPerson(false); setEditingPersonId(null); }} className="absolute top-4 right-4 text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full p-2 transition">
              <X size={20} />
            </button>

            <form onSubmit={handleSavePerson} className="space-y-4 mt-2">
              {/* Image Upload UI */}
              <div className="flex justify-center mb-6 relative">
                <label className="relative cursor-pointer w-24 h-24 rounded-full border border-blue-200 dark:border-blue-500/30 bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center overflow-hidden group shadow-sm">
                  {personImage ? (
                    <img src={personImage} alt="Profile" className="w-full h-full object-cover" />
                  ) : (
                    <div className="text-blue-500 text-center flex flex-col items-center">
                      <ImagePlus size={32} strokeWidth={1.5} />
                    </div>
                  )}
                  <input type="file" accept="image/*" onChange={handleImageUpload} className="hidden" />
                </label>
              </div>

              {/* Name */}
              <div className="relative border border-gray-100 dark:border-gray-700 rounded-xl overflow-hidden focus-within:ring-2 focus-within:ring-blue-500 bg-white dark:bg-gray-800">
                <UserRound size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500" />
                <input required type="text" value={personName} onChange={e => setPersonName(e.target.value)} placeholder="নাম" className="w-full bg-transparent border-none py-3.5 pl-12 pr-4 text-sm font-medium dark:text-white focus:ring-0 outline-none placeholder-gray-400" />
              </div>
              
              {/* Mobile */}
              <div className="relative border border-gray-100 dark:border-gray-700 rounded-xl overflow-hidden focus-within:ring-2 focus-within:ring-blue-500 bg-white dark:bg-gray-800">
                <Phone size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500" />
                <input type="tel" value={phoneNumber} onChange={e => setPhoneNumber(e.target.value)} placeholder="ফোন নম্বর (ঐচ্ছিক)" className="w-full bg-transparent border-none py-3.5 pl-12 pr-4 text-sm font-medium dark:text-white focus:ring-0 outline-none placeholder-gray-400" />
              </div>

              {/* Address */}
              <div className="relative border border-gray-100 dark:border-gray-700 rounded-xl overflow-hidden focus-within:ring-2 focus-within:ring-blue-500 bg-white dark:bg-gray-800">
                <MapPin size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-500" />
                <input type="text" value={personAddress} onChange={e => setPersonAddress(e.target.value)} placeholder="ঠিকানা (ঐচ্ছিক)" className="w-full bg-transparent border-none py-3.5 pl-12 pr-4 text-sm font-medium dark:text-white focus:ring-0 outline-none placeholder-gray-400" />
              </div>

              {/* Date */}
              <div className="relative border border-gray-100 dark:border-gray-700 rounded-xl overflow-hidden focus-within:ring-2 focus-within:ring-blue-500 bg-white dark:bg-gray-800">
                  <Calendar size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-blue-500 pointer-events-none" />
                  <input type="date" value={personDate} onChange={e => setPersonDate(e.target.value)} className="w-full bg-transparent border-none py-3.5 pl-12 pr-4 text-sm font-medium dark:text-white focus:ring-0 outline-none cursor-pointer" />
                </div>

              {/* Account Dropdown */}
              <div className="relative border border-gray-100 dark:border-gray-700 rounded-xl overflow-hidden focus-within:ring-2 focus-within:ring-blue-500 bg-white dark:bg-gray-800 flex items-center h-[46px]">
                <Wallet size={18} className="absolute left-4 text-gray-500" />
                <select value={personAccount} onChange={e => setPersonAccount(e.target.value)} className="w-full h-full appearance-none bg-transparent border-none pl-12 pr-10 text-sm font-medium text-gray-800 dark:text-white focus:ring-0 outline-none cursor-pointer">
                   <option value="ক্যাশ">ক্যাশ</option>
                   <option value="বিকাশ">বিকাশ</option>
                   <option value="নগদ">নগদ</option>
                   <option value="ব্যাংক">ব্যাংক একাউন্ট</option>
                </select>
                <div className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none text-xs">▼</div>
              </div>

              <button type="submit" className="w-full py-3.5 mt-4 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-bold text-base transition-colors shadow-sm">
                সেভ করুন
              </button>
            </form>
          </div>
        </div>, document.body
      )}

    </div>
  );
};
export default Debts;
