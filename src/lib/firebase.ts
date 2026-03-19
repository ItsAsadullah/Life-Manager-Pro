import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';
import { getMessaging, getToken, isSupported, onMessage, type MessagePayload } from 'firebase/messaging';
import aiStudioConfig from '../../firebase-applet-config.json';

// Safely merge environment variables with AI Studio config
// This prevents crashes if only some environment variables are set in Vercel
const firebaseConfig = {
  apiKey: (import.meta as any).env.VITE_FIREBASE_API_KEY || aiStudioConfig.apiKey,
  authDomain: (import.meta as any).env.VITE_FIREBASE_AUTH_DOMAIN || aiStudioConfig.authDomain,
  projectId: (import.meta as any).env.VITE_FIREBASE_PROJECT_ID || aiStudioConfig.projectId,
  storageBucket: (import.meta as any).env.VITE_FIREBASE_STORAGE_BUCKET || aiStudioConfig.storageBucket,
  messagingSenderId: (import.meta as any).env.VITE_FIREBASE_MESSAGING_SENDER_ID || aiStudioConfig.messagingSenderId,
  appId: (import.meta as any).env.VITE_FIREBASE_APP_ID || aiStudioConfig.appId,
  firestoreDatabaseId: (import.meta as any).env.VITE_FIRESTORE_DATABASE_ID || aiStudioConfig.firestoreDatabaseId || '(default)'
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app, firebaseConfig.firestoreDatabaseId || '(default)');
export const storage = getStorage(app);

const vapidKey = (import.meta as any).env.VITE_FIREBASE_VAPID_KEY as string | undefined || (aiStudioConfig as any).vapidKey;

export const getFirebaseMessagingConfig = () => ({
  apiKey: firebaseConfig.apiKey,
  authDomain: firebaseConfig.authDomain,
  projectId: firebaseConfig.projectId,
  storageBucket: firebaseConfig.storageBucket,
  messagingSenderId: firebaseConfig.messagingSenderId,
  appId: firebaseConfig.appId,
});

export const isPushNotificationsSupported = async () => {
  if (typeof window === 'undefined') return false;
  if (!('serviceWorker' in navigator) || !('Notification' in window)) return false;
  return isSupported();
};

export const getPushNotificationToken = async (registration: ServiceWorkerRegistration) => {
  if (!vapidKey) return null;
  const supported = await isPushNotificationsSupported();
  if (!supported) return null;
  
  if (!registration || !('pushManager' in registration)) {
    console.warn('Push manager unavailable. Browser might not support push notifications.');
    return null;
  }

  const messaging = getMessaging(app);
  try {
    const token = await getToken(messaging, {
      vapidKey,
      serviceWorkerRegistration: registration,
    });
    return token || null;
  } catch (error) {
    console.error('Firebase GetToken Error:', error);
    return null;
  }
};

export const onForegroundPushMessage = (callback: (payload: MessagePayload) => void) => {
  if (typeof window === 'undefined') return () => {};
  if (!('Notification' in window) || !('serviceWorker' in navigator)) return () => {};
  const messaging = getMessaging(app);
  return onMessage(messaging, callback);
};
