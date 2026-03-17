import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { Layout } from './components/Layout';
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { Notes } from './pages/Notes';
import { MarketMemo } from './pages/MarketMemo';
import { SharedMemoView } from './pages/SharedMemoView';
import { Expenses } from './pages/Expenses';
import { Debts } from './pages/Debts';
import { Scanner } from './pages/Scanner';
import { Chatbot } from './pages/Chatbot';
import { ImageGen } from './pages/ImageGen';
import { Preloader } from './components/Preloader';
import React, { useState, useEffect } from 'react';

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { user, loading } = useAuth();
  if (loading) return <Preloader />;
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
};

const AppContent = () => {
  const { loading } = useAuth();
  const [showPreloader, setShowPreloader] = useState(true);

  useEffect(() => {
    if (!loading) {
      const timer = setTimeout(() => setShowPreloader(false), 1500);
      return () => clearTimeout(timer);
    }
  }, [loading]);

  if (showPreloader) return <Preloader />;

  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/shared-memo" element={<SharedMemoView />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Dashboard />} />
          <Route path="notes" element={<Notes />} />
          <Route path="market-memo" element={<MarketMemo />} />
          <Route path="expenses" element={<Expenses />} />
          <Route path="debts" element={<Debts />} />
          <Route path="scanner" element={<Scanner />} />
          <Route path="chatbot" element={<Chatbot />} />
          <Route path="image-gen" element={<ImageGen />} />
        </Route>
      </Routes>
    </Router>
  );
};

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
