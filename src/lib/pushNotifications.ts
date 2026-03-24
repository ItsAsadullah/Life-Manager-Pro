import { doc, setDoc } from 'firebase/firestore';
import { db, getFirebaseMessagingConfig, getPushNotificationToken, isPushNotificationsSupported } from './firebase';
import { isNativePushRuntime, registerNativePushToken } from './nativePush';

export const registerPushServiceWorker = async () => {
  if (isNativePushRuntime()) return null;
  if (typeof window === 'undefined' || !('serviceWorker' in navigator)) return null;

  const config = getFirebaseMessagingConfig();
  const query = new URLSearchParams({
    apiKey: config.apiKey || '',
    authDomain: config.authDomain || '',
    projectId: config.projectId || '',
    storageBucket: config.storageBucket || '',
    messagingSenderId: config.messagingSenderId || '',
    appId: config.appId || '',
  });

  await navigator.serviceWorker.register(`/firebase-messaging-sw.js?${query.toString()}`);
  return await navigator.serviceWorker.ready;
};

export const registerUserPushToken = async (uid: string) => {
  if (isNativePushRuntime()) {
    return registerNativePushToken(uid);
  }

  const supported = await isPushNotificationsSupported();
  if (!supported) {
    return { ok: false as const, reason: 'unsupported' as const };
  }

  if (Notification.permission !== 'granted') {
    return { ok: false as const, reason: 'permission-not-granted' as const };
  }

  const registration = await registerPushServiceWorker();
  if (!registration) {
    return { ok: false as const, reason: 'service-worker-failed' as const };
  }

  const token = await getPushNotificationToken(registration);
  if (!token) {
    return { ok: false as const, reason: 'token-missing' as const };
  }

  await setDoc(
    doc(db, 'users', uid),
    {
      push: {
        token,
        enabled: true,
        source: 'web-fcm',
        updatedAt: new Date().toISOString(),
        userAgent: navigator.userAgent,
      },
    },
    { merge: true }
  );

  return { ok: true as const, token };
};
