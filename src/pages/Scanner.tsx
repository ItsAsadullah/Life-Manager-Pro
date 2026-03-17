import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useSettings } from '../contexts/SettingsContext';
import { collection, addDoc } from 'firebase/firestore';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { db, storage } from '../lib/firebase';
import { Upload, FileText, CheckCircle, Loader2 } from 'lucide-react';
import Tesseract from 'tesseract.js';
import { SwipeableNumberInput } from '../components/SwipeableNumberInput';

export const Scanner: React.FC = () => {
  const { user } = useAuth();
  const { t, currencySymbol } = useSettings();
  const [image, setImage] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [extractedText, setExtractedText] = useState('');
  const [totalAmount, setTotalAmount] = useState<number | ''>('');
  const [isSaving, setIsSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setImage(file);
      setPreview(URL.createObjectURL(file));
      setExtractedText('');
      setTotalAmount('');
      setSaved(false);
    }
  };

  const scanImage = async () => {
    if (!image) return;
    
    setIsScanning(true);
    try {
      const result = await Tesseract.recognize(image, 'eng', {
        logger: m => console.log(m)
      });
      
      const text = result.data.text;
      setExtractedText(text);

      // Simple regex to find a total amount (looks for 'Total' followed by numbers)
      const totalMatch = text.match(/Total[\s:]*[\$৳]?\s*(\d+(\.\d{1,2})?)/i);
      if (totalMatch && totalMatch[1]) {
        setTotalAmount(parseFloat(totalMatch[1]));
      }

    } catch (error) {
      console.error('OCR Error:', error);
    } finally {
      setIsScanning(false);
    }
  };

  const saveAsExpense = async () => {
    if (!user || !image || !extractedText) return;
    
    setIsSaving(true);
    try {
      // 1. Upload Image to Storage
      const storageRef = ref(storage, `users/${user.uid}/vouchers/${Date.now()}_${image.name}`);
      await uploadBytes(storageRef, image);
      const downloadURL = await getDownloadURL(storageRef);

      // 2. Save Voucher Record
      const voucherRef = await addDoc(collection(db, 'users', user.uid, 'vouchers'), {
        imageURL: downloadURL,
        extractedText,
        totalAmount: Number(totalAmount) || 0,
        createdAt: new Date().toISOString()
      });

      // 3. Save Expense Record
      if (totalAmount) {
        await addDoc(collection(db, 'users', user.uid, 'expenses'), {
          amount: Number(totalAmount),
          category: 'shopping',
          description: t('scannedVoucher'),
          date: new Date().toISOString(),
          paymentMethod: 'cash',
          voucherId: voucherRef.id,
          createdAt: new Date().toISOString()
        });
      }

      setSaved(true);
    } catch (error) {
      console.error('Error saving voucher:', error);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <h2 className="text-2xl font-bold text-gray-900 dark:text-white">{t('scanner')}</h2>
      <p className="text-gray-600 dark:text-gray-400">{t('scannerDescription')}</p>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Upload Section */}
        <div className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700">
          <div className="border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-xl p-8 text-center hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors cursor-pointer relative">
            <input
              type="file"
              accept="image/*"
              onChange={handleImageChange}
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
            />
            {preview ? (
              <img src={preview} alt="Preview" className="max-h-64 mx-auto rounded-lg" />
            ) : (
              <div className="space-y-4">
                <Upload className="mx-auto h-12 w-12 text-gray-400 dark:text-gray-500" />
                <div className="text-sm text-gray-600 dark:text-gray-400">
                  <span className="font-semibold text-indigo-600 dark:text-indigo-400">{t('clickToUpload')}</span> {t('dragAndDrop')}
                </div>
                <p className="text-xs text-gray-500 dark:text-gray-500">{t('fileTypes')}</p>
              </div>
            )}
          </div>

          {image && !extractedText && (
            <button
              onClick={scanImage}
              disabled={isScanning}
              className="mt-6 w-full flex justify-center items-center px-4 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
            >
              {isScanning ? (
                <>
                  <Loader2 className="animate-spin mr-2" size={20} />
                  {t('scanning')}
                </>
              ) : (
                <>
                  <FileText className="mr-2" size={20} />
                  {t('extractText')}
                </>
              )}
            </button>
          )}
        </div>

        {/* Results Section */}
        {extractedText && (
          <div className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 flex flex-col">
            <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4">{t('extractedInfo')}</h3>
            
            <div className="flex-1 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('totalAmount')} ({currencySymbol})</label>
                <SwipeableNumberInput
                  value={String(totalAmount)}
                  onChange={(val) => setTotalAmount(val ? Number(val) : '')}
                  className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  placeholder="0.00"
                  isPrice={true}
                />
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{t('editAmountInfo')}</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{t('rawText')}</label>
                <textarea
                  value={extractedText}
                  onChange={(e) => setExtractedText(e.target.value)}
                  rows={8}
                  className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 font-mono text-sm"
                />
              </div>
            </div>

            <div className="mt-6">
              {saved ? (
                <div className="flex items-center justify-center p-3 bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300 rounded-lg">
                  <CheckCircle className="mr-2" size={20} />
                  {t('savedSuccessfully')}
                </div>
              ) : (
                <button
                  onClick={saveAsExpense}
                  disabled={isSaving}
                  className="w-full flex justify-center items-center px-4 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
                >
                  {isSaving ? (
                    <>
                      <Loader2 className="animate-spin mr-2" size={20} />
                      {t('saving')}
                    </>
                  ) : (
                    t('saveAsExpense')
                  )}
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
