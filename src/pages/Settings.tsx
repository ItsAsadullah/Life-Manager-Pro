import React, { useState } from 'react';
import { useSettings } from '../contexts/SettingsContext';
import { useAuth } from '../contexts/AuthContext';
import { db } from '../lib/firebase';
import { collection, getDocs, writeBatch, doc } from 'firebase/firestore';
import { Globe, DollarSign, Download, Upload, Check, AlertCircle, ShieldCheck, Sun, Moon, Bell, Plus, Trash2 } from 'lucide-react';
import { motion } from 'motion/react';
import { ThemeToggle } from '../components/ThemeToggle';

export const Settings: React.FC = () => {
  const {
    currency,
    setCurrency,
    language,
    setLanguage,
    theme,
    t,
    notificationsEnabled,
    notificationPermission,
    notificationTimes,
    setNotificationsEnabled,
    setNotificationTimes,
    requestNotificationPermission,
    sendTestNotification,
  } = useSettings();
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
      setMessage({ type: 'error', text: t('exportFailed') });
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

  const handleNotificationsToggle = async (enabled: boolean) => {
    if (!enabled) {
      setNotificationsEnabled(false);
      return;
    }

    const permission = await requestNotificationPermission();
    if (permission === 'granted') {
      setNotificationsEnabled(true);
      setMessage({ type: 'success', text: t('permissionGranted') });
    } else if (permission === 'denied') {
      setMessage({ type: 'error', text: t('permissionDenied') });
    } else if (permission === 'unsupported') {
      setMessage({ type: 'error', text: t('notificationsUnsupported') });
    }
  };

  const updateNotificationTime = (index: number, value: string) => {
    const next = [...notificationTimes];
    next[index] = value;
    setNotificationTimes(next);
  };

  const addNotificationTime = () => {
    setNotificationTimes([...notificationTimes, '21:00']);
  };

  const removeNotificationTime = (index: number) => {
    setNotificationTimes(notificationTimes.filter((_, itemIndex) => itemIndex !== index));
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <header className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">{t('settings')}</h1>
        <p className="text-gray-500">{t('managePreferences')}</p>
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

      {/* Theme Section */}
      <section className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 dark:bg-gray-800 dark:border-gray-700">
        <div className="flex items-center space-x-3 mb-6">
          <div className="p-2 bg-purple-50 text-purple-600 rounded-lg dark:bg-purple-900 dark:text-purple-300">
            {theme === 'light' ? <Sun size={20} /> : <Moon size={20} />}
          </div>
          <h2 className="text-lg font-bold text-gray-900 dark:text-white">{t('theme')}</h2>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-gray-600 dark:text-gray-300">
            {theme === 'light' ? t('lightMode') : t('darkMode')}
          </span>
          <ThemeToggle />
        </div>
      </section>

      {/* Language Section */}
      <section className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 dark:bg-gray-800 dark:border-gray-700">
        <div className="flex items-center space-x-3 mb-6">
          <div className="p-2 bg-indigo-50 text-indigo-600 rounded-lg dark:bg-indigo-900 dark:text-indigo-300">
            <Globe size={20} />
          </div>
          <h2 className="text-lg font-bold text-gray-900 dark:text-white">{t('language')}</h2>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <button
            onClick={() => setLanguage('bn')}
            className={`p-4 rounded-xl border-2 transition-all text-center ${
              language === 'bn' ? 'border-indigo-600 bg-indigo-50 text-indigo-700 dark:bg-indigo-900 dark:text-indigo-200' : 'border-gray-100 hover:border-gray-200 text-gray-600 dark:border-gray-700 dark:text-gray-400'
            }`}
          >
            <span className="block text-lg font-bold mb-1">বাংলা</span>
            <span className="text-xs opacity-70">Bengali</span>
          </button>
          <button
            onClick={() => setLanguage('en')}
            className={`p-4 rounded-xl border-2 transition-all text-center ${
              language === 'en' ? 'border-indigo-600 bg-indigo-50 text-indigo-700 dark:bg-indigo-900 dark:text-indigo-200' : 'border-gray-100 hover:border-gray-200 text-gray-600 dark:border-gray-700 dark:text-gray-400'
            }`}
          >
            <span className="block text-lg font-bold mb-1">English</span>
            <span className="text-xs opacity-70">ইংরেজি</span>
          </button>
        </div>
      </section>

      {/* Currency Section */}
      <section className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 dark:bg-gray-800 dark:border-gray-700">
        <div className="flex items-center space-x-3 mb-6">
          <div className="p-2 bg-emerald-50 text-emerald-600 rounded-lg dark:bg-emerald-900/30 dark:text-emerald-400">
            <DollarSign size={20} />
          </div>
          <h2 className="text-lg font-bold text-gray-900 dark:text-white">{t('currency')}</h2>
        </div>
        <div className="grid grid-cols-3 gap-4">
          {[
            { id: 'BDT', label: t('taka'), symbol: '৳' },
            { id: 'INR', label: t('rupee'), symbol: '₹' },
            { id: 'USD', label: t('dollar'), symbol: '$' },
          ].map((c) => (
            <button
              key={c.id}
              onClick={() => setCurrency(c.id as any)}
              className={`p-4 rounded-xl border-2 transition-all text-center ${
                currency === c.id ? 'border-emerald-600 bg-emerald-50 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300' : 'border-gray-100 hover:border-gray-200 text-gray-600 dark:border-gray-700 dark:hover:border-gray-600 dark:text-gray-400'
              }`}
            >
              <span className="block text-xl font-bold mb-1">{c.symbol}</span>
              <span className="text-xs font-medium">{c.label} ({c.id})</span>
            </button>
          ))}
        </div>
      </section>

      {/* Notification Section */}
      <section className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 dark:bg-gray-800 dark:border-gray-700">
        <div className="flex items-center space-x-3 mb-6">
          <div className="p-2 bg-blue-50 text-blue-600 rounded-lg dark:bg-blue-900/30 dark:text-blue-400">
            <Bell size={20} />
          </div>
          <div>
            <h2 className="text-lg font-bold text-gray-900 dark:text-white">{t('notifications')}</h2>
            <p className="text-xs text-gray-500 dark:text-gray-400">{t('notificationSchedule')}</p>
          </div>
        </div>

        <div className="space-y-4">
          <div className="flex items-center justify-between p-4 rounded-xl bg-gray-50 dark:bg-gray-900/50">
            <span className="font-medium text-gray-700 dark:text-gray-300">{t('enableNotifications')}</span>
            <button
              type="button"
              onClick={() => handleNotificationsToggle(!notificationsEnabled)}
              className={`relative inline-flex h-7 w-12 items-center rounded-full transition-colors ${notificationsEnabled ? 'bg-indigo-600' : 'bg-gray-300 dark:bg-gray-600'}`}
            >
              <span
                className={`inline-block h-5 w-5 transform rounded-full bg-white transition-transform ${notificationsEnabled ? 'translate-x-6' : 'translate-x-1'}`}
              />
            </button>
          </div>

          <div className="text-xs font-medium px-1">
            {notificationPermission === 'granted' && <span className="text-emerald-600 dark:text-emerald-400">{t('permissionGranted')}</span>}
            {notificationPermission === 'denied' && <span className="text-rose-600 dark:text-rose-400">{t('permissionDenied')}</span>}
            {notificationPermission === 'default' && <span className="text-amber-600 dark:text-amber-400">{t('permissionDefault')}</span>}
            {notificationPermission === 'unsupported' && <span className="text-rose-600 dark:text-rose-400">{t('notificationsUnsupported')}</span>}
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold text-gray-800 dark:text-gray-200">{t('notificationTimes')}</span>
              <button
                type="button"
                onClick={addNotificationTime}
                className="inline-flex items-center text-sm font-semibold text-indigo-600 dark:text-indigo-400"
              >
                <Plus size={16} className="mr-1" />
                {t('addTime')}
              </button>
            </div>

            {notificationTimes.map((time, index) => (
              <div key={`${time}-${index}`} className="flex items-center gap-3">
                <input
                  type="time"
                  value={time}
                  onChange={(event) => updateNotificationTime(index, event.target.value)}
                  className="flex-1 px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-900 dark:bg-gray-900 dark:text-gray-100 dark:border-gray-700"
                />
                <button
                  type="button"
                  onClick={() => removeNotificationTime(index)}
                  disabled={notificationTimes.length === 1}
                  className="p-2 rounded-lg text-rose-600 hover:bg-rose-50 disabled:opacity-40 disabled:cursor-not-allowed dark:text-rose-400 dark:hover:bg-rose-900/20"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
          </div>

          <button
            type="button"
            onClick={sendTestNotification}
            disabled={!notificationsEnabled || notificationPermission !== 'granted'}
            className="w-full py-3 rounded-xl bg-indigo-600 text-white font-semibold hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {t('testNotification')}
          </button>
        </div>
      </section>

      {/* Data Management */}
      <section className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 dark:bg-gray-800 dark:border-gray-700">
        <div className="flex items-center space-x-3 mb-6">
          <div className="p-2 bg-amber-50 text-amber-600 rounded-lg dark:bg-amber-900/30 dark:text-amber-400">
            <ShieldCheck size={20} />
          </div>
          <h2 className="text-lg font-bold text-gray-900 dark:text-white">{t('backup')}</h2>
        </div>
        <div className="space-y-4">
          <button
            onClick={handleExport}
            disabled={isExporting}
            className="w-full flex items-center justify-between p-4 bg-gray-50 hover:bg-gray-100 rounded-xl transition-colors group dark:bg-gray-900/50 dark:hover:bg-gray-700"
          >
            <div className="flex items-center space-x-3">
              <Download size={20} className="text-gray-500 group-hover:text-indigo-600 dark:text-gray-400 dark:group-hover:text-indigo-400" />
              <div className="text-left">
                <span className="block font-bold text-gray-900 dark:text-white">{t('export')}</span>
                <span className="text-xs text-gray-500 dark:text-gray-400">{t('exportDescription')}</span>
              </div>
            </div>
            {isExporting && <div className="animate-spin rounded-full h-5 w-5 border-2 border-indigo-600 border-t-transparent dark:border-indigo-400" />}
          </button>

          <label className="w-full flex items-center justify-between p-4 bg-gray-50 hover:bg-gray-100 rounded-xl transition-colors group cursor-pointer dark:bg-gray-900/50 dark:hover:bg-gray-700">
            <div className="flex items-center space-x-3">
              <Upload size={20} className="text-gray-500 group-hover:text-amber-600 dark:text-gray-400 dark:group-hover:text-amber-400" />
              <div className="text-left">
                <span className="block font-bold text-gray-900 dark:text-white">{t('import')}</span>
                <span className="text-xs text-gray-500 dark:text-gray-400">{t('importDescription')}</span>
              </div>
            </div>
            <input type="file" accept=".json" onChange={handleImport} className="hidden" disabled={isImporting} />
            {isImporting && <div className="animate-spin rounded-full h-5 w-5 border-2 border-amber-600 border-t-transparent dark:border-amber-400" />}
          </label>
        </div>
      </section>
    </div>
  );
};
