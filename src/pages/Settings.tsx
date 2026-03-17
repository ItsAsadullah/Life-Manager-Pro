import React, { useState } from 'react';
import { useSettings } from '../contexts/SettingsContext';
import { useAuth } from '../contexts/AuthContext';
import { db } from '../lib/firebase';
import { collection, getDocs, writeBatch, doc } from 'firebase/firestore';
import { Globe, DollarSign, Download, Upload, Check, AlertCircle, ShieldCheck } from 'lucide-react';
import { motion } from 'motion/react';

export const Settings: React.FC = () => {
  const { currency, setCurrency, language, setLanguage, t } = useSettings();
  const { user } = useAuth();
  const [isExporting, setIsExporting] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  const handleExport = async () => {
    if (!user) return;
    setIsExporting(true);
    try {
      const collections = ['notes', 'expenses', 'income', 'debts', 'savings', 'marketMemos'];
      const allData: any = {};

      for (const colName of collections) {
        const querySnapshot = await getDocs(collection(db, 'users', user.uid, colName));
        allData[colName] = querySnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
      }

      const blob = new Blob([JSON.stringify(allData, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `life-manager-backup-${new Date().toISOString().split('T')[0]}.json`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      
      setMessage({ type: 'success', text: t('backupSuccess') });
    } catch (error) {
      console.error('Export error:', error);
      setMessage({ type: 'error', text: 'Export failed' });
    } finally {
      setIsExporting(false);
    }
  };

  const handleImport = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file || !user) return;

    setIsImporting(true);
    const reader = new FileReader();
    reader.onload = async (e) => {
      try {
        const data = JSON.parse(e.target?.result as string);
        const batch = writeBatch(db);

        for (const [colName, docs] of Object.entries(data)) {
          if (Array.isArray(docs)) {
            docs.forEach((docData: any) => {
              const { id, ...rest } = docData;
              const docRef = doc(collection(db, 'users', user.uid, colName));
              batch.set(docRef, { ...rest, updatedAt: new Date().toISOString() });
            });
          }
        }

        await batch.commit();
        setMessage({ type: 'success', text: t('importSuccess') });
      } catch (error) {
        console.error('Import error:', error);
        setMessage({ type: 'error', text: t('importError') });
      } finally {
        setIsImporting(false);
        event.target.value = '';
      }
    };
    reader.readAsText(file);
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <header className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">{t('settings')}</h1>
        <p className="text-gray-500">Manage your application preferences and data</p>
      </header>

      {message && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className={`p-4 rounded-xl flex items-center space-x-3 ${
            message.type === 'success' ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : 'bg-rose-50 text-rose-700 border border-rose-100'
          }`}
        >
          {message.type === 'success' ? <Check size={20} /> : <AlertCircle size={20} />}
          <span className="font-medium">{message.text}</span>
        </motion.div>
      )}

      {/* Language Section */}
      <section className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
        <div className="flex items-center space-x-3 mb-6">
          <div className="p-2 bg-indigo-50 text-indigo-600 rounded-lg">
            <Globe size={20} />
          </div>
          <h2 className="text-lg font-bold text-gray-900">{t('language')}</h2>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <button
            onClick={() => setLanguage('bn')}
            className={`p-4 rounded-xl border-2 transition-all text-center ${
              language === 'bn' ? 'border-indigo-600 bg-indigo-50 text-indigo-700' : 'border-gray-100 hover:border-gray-200 text-gray-600'
            }`}
          >
            <span className="block text-lg font-bold mb-1">বাংলা</span>
            <span className="text-xs opacity-70">Bengali</span>
          </button>
          <button
            onClick={() => setLanguage('en')}
            className={`p-4 rounded-xl border-2 transition-all text-center ${
              language === 'en' ? 'border-indigo-600 bg-indigo-50 text-indigo-700' : 'border-gray-100 hover:border-gray-200 text-gray-600'
            }`}
          >
            <span className="block text-lg font-bold mb-1">English</span>
            <span className="text-xs opacity-70">ইংরেজি</span>
          </button>
        </div>
      </section>

      {/* Currency Section */}
      <section className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
        <div className="flex items-center space-x-3 mb-6">
          <div className="p-2 bg-emerald-50 text-emerald-600 rounded-lg">
            <DollarSign size={20} />
          </div>
          <h2 className="text-lg font-bold text-gray-900">{t('currency')}</h2>
        </div>
        <div className="grid grid-cols-3 gap-4">
          {[
            { id: 'BDT', label: 'Taka', symbol: '৳' },
            { id: 'INR', label: 'Rupee', symbol: '₹' },
            { id: 'USD', label: 'Dollar', symbol: '$' },
          ].map((c) => (
            <button
              key={c.id}
              onClick={() => setCurrency(c.id as any)}
              className={`p-4 rounded-xl border-2 transition-all text-center ${
                currency === c.id ? 'border-emerald-600 bg-emerald-50 text-emerald-700' : 'border-gray-100 hover:border-gray-200 text-gray-600'
              }`}
            >
              <span className="block text-xl font-bold mb-1">{c.symbol}</span>
              <span className="text-xs font-medium">{c.id}</span>
            </button>
          ))}
        </div>
      </section>

      {/* Data Management */}
      <section className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
        <div className="flex items-center space-x-3 mb-6">
          <div className="p-2 bg-amber-50 text-amber-600 rounded-lg">
            <ShieldCheck size={20} />
          </div>
          <h2 className="text-lg font-bold text-gray-900">{t('backup')}</h2>
        </div>
        <div className="space-y-4">
          <button
            onClick={handleExport}
            disabled={isExporting}
            className="w-full flex items-center justify-between p-4 bg-gray-50 hover:bg-gray-100 rounded-xl transition-colors group"
          >
            <div className="flex items-center space-x-3">
              <Download size={20} className="text-gray-500 group-hover:text-indigo-600" />
              <div className="text-left">
                <span className="block font-bold text-gray-900">{t('export')}</span>
                <span className="text-xs text-gray-500">Download all your data as a JSON file</span>
              </div>
            </div>
            {isExporting && <div className="animate-spin rounded-full h-5 w-5 border-2 border-indigo-600 border-t-transparent" />}
          </button>

          <label className="w-full flex items-center justify-between p-4 bg-gray-50 hover:bg-gray-100 rounded-xl transition-colors group cursor-pointer">
            <div className="flex items-center space-x-3">
              <Upload size={20} className="text-gray-500 group-hover:text-amber-600" />
              <div className="text-left">
                <span className="block font-bold text-gray-900">{t('import')}</span>
                <span className="text-xs text-gray-500">Restore data from a backup file</span>
              </div>
            </div>
            <input type="file" accept=".json" onChange={handleImport} className="hidden" disabled={isImporting} />
            {isImporting && <div className="animate-spin rounded-full h-5 w-5 border-2 border-amber-600 border-t-transparent" />}
          </label>
        </div>
      </section>
    </div>
  );
};
