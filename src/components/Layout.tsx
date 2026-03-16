import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { LayoutDashboard, StickyNote, Receipt, ScanLine, Bot, Image as ImageIcon, LogOut, Menu, ShoppingCart } from 'lucide-react';
import { useState } from 'react';

export const Layout: React.FC = () => {
  const { user, logout } = useAuth();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const navItems = [
    { to: '/', icon: <LayoutDashboard size={20} />, label: 'Dashboard' },
    { to: '/notes', icon: <StickyNote size={20} />, label: 'Notes' },
    { to: '/market-memo', icon: <ShoppingCart size={20} />, label: 'Market Memo' },
    { to: '/expenses', icon: <Receipt size={20} />, label: 'Expenses' },
    { to: '/scanner', icon: <ScanLine size={20} />, label: 'Voucher Scanner' },
    { to: '/chatbot', icon: <Bot size={20} />, label: 'AI Chatbot' },
    { to: '/image-gen', icon: <ImageIcon size={20} />, label: 'Image Gen' },
  ];

  return (
    <div className="flex h-screen bg-gray-50 text-gray-900 font-sans">
      {/* Sidebar (Desktop) */}
      <aside className="hidden md:flex flex-col w-64 bg-white border-r border-gray-200">
        <div className="h-16 flex items-center px-6 border-b border-gray-200">
          <h1 className="text-xl font-bold text-indigo-600">Life Manager Pro</h1>
        </div>
        <nav className="flex-1 overflow-y-auto py-4">
          <ul className="space-y-1 px-3">
            {navItems.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  className={({ isActive }) =>
                    `flex items-center px-3 py-2.5 rounded-lg transition-colors ${
                      isActive
                        ? 'bg-indigo-50 text-indigo-700 font-medium'
                        : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                    }`
                  }
                >
                  <span className="mr-3">{item.icon}</span>
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <div className="p-4 border-t border-gray-200">
          <div className="flex items-center mb-4">
            <img src={user?.photoURL || 'https://via.placeholder.com/40'} alt="Profile" className="w-10 h-10 rounded-full mr-3" referrerPolicy="no-referrer" />
            <div className="overflow-hidden">
              <p className="text-sm font-medium text-gray-900 truncate">{user?.displayName}</p>
              <p className="text-xs text-gray-500 truncate">{user?.email}</p>
            </div>
          </div>
          <button
            onClick={logout}
            className="flex items-center w-full px-3 py-2 text-sm font-medium text-red-600 rounded-lg hover:bg-red-50 transition-colors"
          >
            <LogOut size={18} className="mr-2" />
            Logout
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Mobile Header */}
        <header className="md:hidden h-16 bg-white border-b border-gray-200 flex items-center justify-between px-4">
          <h1 className="text-lg font-bold text-indigo-600">Life Manager Pro</h1>
          <button onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)} className="p-2 text-gray-600">
            <Menu size={24} />
          </button>
        </header>

        {/* Mobile Menu */}
        {isMobileMenuOpen && (
          <div className="md:hidden absolute top-16 left-0 right-0 bg-white border-b border-gray-200 z-50 shadow-lg">
            <nav className="p-4">
              <ul className="space-y-2">
                {navItems.map((item) => (
                  <li key={item.to}>
                    <NavLink
                      to={item.to}
                      onClick={() => setIsMobileMenuOpen(false)}
                      className={({ isActive }) =>
                        `flex items-center px-4 py-3 rounded-lg ${
                          isActive ? 'bg-indigo-50 text-indigo-700' : 'text-gray-600'
                        }`
                      }
                    >
                      <span className="mr-3">{item.icon}</span>
                      {item.label}
                    </NavLink>
                  </li>
                ))}
                <li>
                  <button onClick={logout} className="flex items-center w-full px-4 py-3 text-red-600 rounded-lg">
                    <LogOut size={20} className="mr-3" />
                    Logout
                  </button>
                </li>
              </ul>
            </nav>
          </div>
        )}

        <main className="flex-1 overflow-y-auto p-4 md:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
