import {StrictMode} from 'react';
import {createRoot} from 'react-dom/client';
import App from './App.tsx';
import './index.css';
import { ErrorBoundary } from './components/ErrorBoundary';
import { onForegroundPushMessage } from './lib/firebase';
import { registerPushServiceWorker } from './lib/pushNotifications';

if ('serviceWorker' in navigator) {
  registerPushServiceWorker().catch((error) => {
    console.error('Push service worker registration failed:', error);
  });
}

onForegroundPushMessage((payload) => {
  if (Notification.permission !== 'granted') return;
  const title = payload.notification?.title || payload.data?.title || 'Hisab Nikash Reminder';
  const body = payload.notification?.body || payload.data?.body || 'You have a new reminder';
  new Notification(title, {
    body,
    icon: '/favicon.svg',
    badge: '/favicon.svg',
  });
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </StrictMode>,
);
