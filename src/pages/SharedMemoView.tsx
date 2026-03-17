import React, { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { useSettings } from '../contexts/SettingsContext';
import { format } from 'date-fns';
import { ShoppingCart, ArrowLeft, Download, FileText, Image as ImageIcon } from 'lucide-react';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import { doc, getDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';

export const SharedMemoView: React.FC = () => {
  const [searchParams] = useSearchParams();
  const { t, currencySymbol } = useSettings();
  const [memo, setMemo] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchMemo = async () => {
      const idParam = searchParams.get('id');
      const dataParam = searchParams.get('data');

      if (idParam) {
        try {
          const docRef = doc(db, 'sharedMemos', idParam);
          const docSnap = await getDoc(docRef);
          if (docSnap.exists()) {
            setMemo(docSnap.data());
          } else {
            setError('Memo not found or has been deleted.');
          }
        } catch (err) {
          console.error('Error fetching memo:', err);
          setError('Failed to load memo.');
        }
      } else if (dataParam) {
        try {
          // Fix for + characters being replaced by spaces in URL search params
          const fixedDataParam = dataParam.replace(/ /g, '+');
          
          // Try the new encoding first
          try {
            const binString = atob(fixedDataParam);
            const utf8Bytes = new Uint8Array(Array.from(binString, (char) => char.charCodeAt(0)));
            const decodedStr = new TextDecoder().decode(utf8Bytes);
            const decoded = JSON.parse(decodedStr);
            setMemo({
              title: decoded.t,
              items: decoded.i.map((item: any) => ({
                name: item.n,
                quantity: item.q,
                unit: item.u,
                unitPrice: item.p,
                total: item.t
              })),
              totalAmount: decoded.ta,
              createdAt: decoded.d
            });
          } catch (newEncodingErr) {
            // Fallback to old encoding if new encoding fails
            const decoded = JSON.parse(decodeURIComponent(escape(atob(fixedDataParam))));
            setMemo({
              title: decoded.t,
              items: decoded.i.map((item: any) => ({
                name: item.n,
                quantity: item.q,
                unit: item.u,
                unitPrice: item.p,
                total: item.t
              })),
              totalAmount: decoded.ta,
              createdAt: decoded.d
            });
          }
        } catch (err) {
          console.error('Failed to parse memo data', err);
          setError('Invalid or corrupted memo link.');
        }
      } else {
        setError('No memo data found in the link.');
      }
    };

    fetchMemo();
  }, [searchParams]);

  const handleDownloadImage = async () => {
    const element = document.getElementById('shared-memo-content');
    if (!element) return;
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
    } catch (err) {
      console.error('Failed to generate image', err);
      alert('Failed to generate image. Please try again.');
    }
  };

  const handleDownloadPDF = async () => {
    const element = document.getElementById('shared-memo-content');
    if (!element) return;
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
    } catch (err) {
      console.error('Failed to generate PDF', err);
      alert('Failed to generate PDF. Please try again.');
    }
  };

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-4">
        <div className="bg-white p-8 rounded-2xl shadow-sm text-center max-w-md w-full">
          <div className="w-16 h-16 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto mb-4">
            <ShoppingCart size={32} />
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">Oops!</h2>
          <p className="text-gray-600 mb-6">{error}</p>
          <Link to="/" className="inline-flex items-center justify-center px-6 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 font-medium transition-colors">
            Go to Home
          </Link>
        </div>
      </div>
    );
  }

  if (!memo) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-3xl mx-auto">
        <div className="mb-6 flex items-center justify-between">
          <Link to="/" className="inline-flex items-center text-indigo-600 hover:text-indigo-800 font-medium">
            <ArrowLeft size={20} className="mr-2" />
            {t('backToApp')}
          </Link>
          <div className="flex gap-2">
            <button onClick={handleDownloadImage} className="flex items-center px-4 py-2 bg-white text-gray-700 rounded-lg border border-gray-200 hover:bg-gray-50 shadow-sm text-sm font-medium transition-colors">
              <ImageIcon size={16} className="mr-2" />
              {t('image')}
            </button>
            <button onClick={handleDownloadPDF} className="flex items-center px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 shadow-sm text-sm font-medium transition-colors">
              <FileText size={16} className="mr-2" />
              {t('pdf')}
            </button>
          </div>
        </div>

        <div id="shared-memo-content" className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="bg-indigo-600 p-8 text-white">
            <div className="flex items-center gap-3 mb-2">
              <ShoppingCart size={32} className="text-indigo-200" />
              <h1 className="text-3xl font-bold">{t('marketMemoTitle')}</h1>
            </div>
            <h2 className="text-xl text-indigo-100">{memo.title}</h2>
            {memo.createdAt && (
              <p className="text-indigo-200 mt-2 text-sm">
                {format(new Date(memo.createdAt), 'MMMM d, yyyy')}
              </p>
            )}
          </div>

          <div className="p-8">
            <div className="overflow-x-auto">
              <table className="w-full mb-8">
                <thead>
                  <tr className="border-b-2 border-gray-200">
                    <th className="py-3 text-left text-sm font-semibold text-gray-600 uppercase tracking-wider">{t('item')}</th>
                    <th className="py-3 text-right text-sm font-semibold text-gray-600 uppercase tracking-wider">{t('qty')}</th>
                    <th className="py-3 text-right text-sm font-semibold text-gray-600 uppercase tracking-wider">{t('price')}</th>
                    <th className="py-3 text-right text-sm font-semibold text-gray-600 uppercase tracking-wider">{t('total')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {memo.items.map((item: any, idx: number) => (
                    <tr key={idx}>
                      <td className="py-4 text-gray-900 font-medium">{item.name}</td>
                      <td className="py-4 text-right text-gray-600">{item.quantity} {t(item.unit)}</td>
                      <td className="py-4 text-right text-gray-600">{currencySymbol}{item.unitPrice}</td>
                      <td className="py-4 text-right text-gray-900 font-bold">{currencySymbol}{item.total.toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex justify-between items-center bg-gray-50 p-6 rounded-xl border border-gray-100">
              <span className="text-lg font-bold text-gray-600 uppercase tracking-wider">{t('grandTotal')}</span>
              <span className="text-3xl font-bold text-indigo-600">{currencySymbol}{memo.totalAmount.toLocaleString()}</span>
            </div>
            
            <div className="mt-8 pt-8 border-t border-gray-100 text-center text-sm text-gray-400">
              <p className="font-bold text-gray-500 mb-1">{t('developedBy')}: Asadullah Al Galib</p>
              <p className="mb-2">B.Sc in CSE, 01911777694</p>
              <p>{t('createdWith')}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
