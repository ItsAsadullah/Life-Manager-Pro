import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useSettings } from '../contexts/SettingsContext';
import { Navigate } from 'react-router-dom';
import { AlertCircle, Loader2, ShieldCheck, Layout, Sparkles, Smartphone } from 'lucide-react';
import { motion } from 'motion/react';

export const Login: React.FC = () => {
  const { user, signInWithGoogle } = useAuth();
  const { t } = useSettings();
  const [error, setError] = useState<string | null>(null);
  const [isLoggingIn, setIsLoggingIn] = useState(false);

  if (user) {
    return <Navigate to="/" replace />;
  }

  const handleLogin = async () => {
    setError(null);
    setIsLoggingIn(true);
    try {
      await signInWithGoogle();
    } catch (err: any) {
      console.error(err);
      if (err.code === 'auth/unauthorized-domain') {
        setError('This domain is not authorized in Firebase. In Firebase Console → Authentication → Settings → Authorized domains, add localhost and your web domain.');
      } else if ((err.message || '').toLowerCase().includes('no credentials available')) {
        setError('No Google credential found on this device. Please choose a Google account from the account picker and try again.');
      } else if (err.code === 'auth/invalid-credential' || err.code === 'auth/credential-already-in-use') {
        setError('Google credential validation failed. In Firebase Console enable Google sign-in and ensure this app package is registered correctly.');
      } else if (err.code === 'auth/operation-not-supported-in-this-environment') {
        setError('Google popup is not supported in this environment. Redirect sign-in is enabled, please try again.');
      } else if (err.code === 'auth/popup-blocked') {
        setError('Login popup was blocked by your browser. Please allow popups for this site.');
      } else {
        setError(err.message || 'Failed to sign in. Please try again.');
      }
    } finally {
      setIsLoggingIn(false);
    }
  };

  return (
    <div className="min-h-dvh bg-[#0f172a] flex items-center justify-center p-4 overflow-hidden relative">
      {/* Decorative Background Elements */}
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-indigo-600/20 rounded-full blur-[120px]" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-purple-600/20 rounded-full blur-[120px]" />
      
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="max-w-md w-full relative z-10"
      >
        <div className="bg-white/10 backdrop-blur-xl border border-white/20 rounded-3xl shadow-2xl overflow-hidden">
          <div className="p-8 pt-12 text-center">
            <motion.div 
              initial={{ scale: 0.5, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ delay: 0.2, type: 'spring', stiffness: 200 }}
                className="w-24 h-24 flex items-center justify-center mx-auto mb-6"
              >
                <img src="/logo.svg" alt="Hisab Nikash Logo" className="w-full h-full object-contain" />
              </motion.div>

              <h1 className="text-4xl font-black text-white mb-2 tracking-tight">
                Hisab <span className="text-indigo-400">Nikash</span>
              </h1>

            <div className="grid grid-cols-3 gap-4 mb-10">
              <div className="flex flex-col items-center gap-2">
                <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center text-indigo-300">
                  <Layout size={20} />
                </div>
                <span className="text-[10px] text-indigo-200/50 uppercase font-bold tracking-widest">{t('dashboard')}</span>
              </div>
              <div className="flex flex-col items-center gap-2">
                <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center text-indigo-300">
                  <Sparkles size={20} />
                </div>
                <span className="text-[10px] text-indigo-200/50 uppercase font-bold tracking-widest">{t('aiTools')}</span>
              </div>
              <div className="flex flex-col items-center gap-2">
                <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center text-indigo-300">
                  <Smartphone size={20} />
                </div>
                <span className="text-[10px] text-indigo-200/50 uppercase font-bold tracking-widest">{t('mobileReady')}</span>
              </div>
            </div>

            {error && (
              <motion.div 
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                className="mb-6 p-4 bg-red-500/10 border border-red-500/20 rounded-2xl flex items-start text-left"
              >
                <AlertCircle className="text-red-400 mr-3 flex-shrink-0" size={20} />
                <p className="text-xs text-red-200 font-medium leading-relaxed">{error}</p>
              </motion.div>
            )}

            <button
              onClick={handleLogin}
              disabled={isLoggingIn}
              className="group relative w-full flex justify-center items-center py-4 px-6 border border-transparent rounded-2xl shadow-xl text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-all duration-200 disabled:opacity-50 overflow-hidden"
            >
              <div className="absolute inset-0 bg-gradient-to-r from-indigo-600 to-purple-600 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
              <span className="relative flex items-center">
                {isLoggingIn ? (
                  <>
                    <Loader2 className="animate-spin mr-2" size={20} />
                    {t('authenticating')}
                  </>
                ) : (
                  <>
                    <svg className="w-5 h-5 mr-3" viewBox="0 0 24 24">
                      <path
                        fill="currentColor"
                        d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                      />
                      <path
                        fill="currentColor"
                        d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                      />
                      <path
                        fill="currentColor"
                        d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"
                      />
                      <path
                        fill="currentColor"
                        d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                      />
                    </svg>
                    {t('continueWithGoogle')}
                  </>
                )}
              </span>
            </button>
            
            <p className="mt-8 text-[10px] text-indigo-200/30 uppercase font-bold tracking-[0.2em]">
              {t('secureAuth')}
            </p>
          </div>
        </div>
        
        <div className="mt-8 text-center">
          <p className="text-indigo-200/40 text-xs">
            {t('developedBy')} <span className="text-indigo-200/60 font-bold">Asadullah Al Galib</span>
          </p>
        </div>
      </motion.div>
    </div>
  );
};
