import { Capacitor } from '@capacitor/core';
import { FirebaseMessaging } from '@capacitor-firebase/messaging';
import { doc, setDoc } from 'firebase/firestore';
import { db } from './firebase';

let listenersInitialized = false;
let activeUid: string | null = null;

const toNotificationPermission = (permission: 'granted' | 'denied' | 'prompt' | 'prompt-with-rationale'): NotificationPermission => {
  if (permission === 'granted') return 'granted';
  if (permission === 'denied') return 'denied';
  return 'default';
};

export const isNativePushRuntime = () => Capacitor.isNativePlatform();

export const checkNativeNotificationPermission = async (): Promise<NotificationPermission | 'unsupported'> => {
  if (!isNativePushRuntime()) return 'unsupported';

  try {
    const result = await FirebaseMessaging.checkPermissions();
    return toNotificationPermission(result.receive);
  } catch (error) {
    console.warn('Native notification permission check failed:', error);
    return 'unsupported';
  }
};

export const requestNativeNotificationPermission = async (): Promise<NotificationPermission | 'unsupported'> => {
  if (!isNativePushRuntime()) return 'unsupported';

  try {
    const result = await FirebaseMessaging.requestPermissions();
    return toNotificationPermission(result.receive);
  } catch (error) {
    console.warn('Native notification permission request failed:', error);
    return 'unsupported';
  }
};

const saveNativeToken = async (uid: string, token: string) => {
  await setDoc(
    doc(db, 'users', uid),
    {
      push: {
        token,
        enabled: true,
        source: 'native-fcm',
        updatedAt: new Date().toISOString(),
        userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : 'native-webview',
      },
    },
    { merge: true }
  );
};

const initializeNativeListeners = async () => {
  if (listenersInitialized) return;

  await FirebaseMessaging.addListener('tokenReceived', async ({ token }) => {
    if (!activeUid || !token) return;
    try {
      await saveNativeToken(activeUid, token);
    } catch (error) {
      console.error('Failed to persist native token from listener:', error);
    }
  });

  await FirebaseMessaging.addListener('notificationReceived', ({ notification }) => {
    console.log('Native notification received:', notification);
  });

  await FirebaseMessaging.addListener('notificationActionPerformed', ({ notification }) => {
    console.log('Native notification action:', notification);
  });

  listenersInitialized = true;
};

export const registerNativePushToken = async (uid: string) => {
  if (!isNativePushRuntime()) {
    return { ok: false as const, reason: 'unsupported' as const };
  }

  activeUid = uid;

  const permission = await checkNativeNotificationPermission();
  if (permission !== 'granted') {
    return { ok: false as const, reason: 'permission-not-granted' as const };
  }

  try {
    await initializeNativeListeners();

    const result = await FirebaseMessaging.getToken();
    if (!result.token) {
      return { ok: false as const, reason: 'token-missing' as const };
    }

    await saveNativeToken(uid, result.token);
    return { ok: true as const, token: result.token };
  } catch (error) {
    console.error('Native token registration failed:', error);
    return { ok: false as const, reason: 'service-worker-failed' as const };
  }
};
