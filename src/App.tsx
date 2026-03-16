import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { Layout } from './components/Layout';
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { Notes } from './pages/Notes';
import { MarketMemo } from './pages/MarketMemo';
import { Expenses } from './pages/Expenses';
import { Scanner } from './pages/Scanner';
import { Chatbot } from './pages/Chatbot';
import { ImageGen } from './pages/ImageGen';
import { useAuth } from './contexts/AuthContext';
import { Navigate } from 'react-router-dom';

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
};

export default function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
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
            <Route path="scanner" element={<Scanner />} />
            <Route path="chatbot" element={<Chatbot />} />
            <Route path="image-gen" element={<ImageGen />} />
          </Route>
        </Routes>
      </Router>
    </AuthProvider>
  );
}
