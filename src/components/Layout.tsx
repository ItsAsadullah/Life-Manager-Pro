import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { LayoutDashboard, StickyNote, Receipt, ScanLine, Bot, Image as ImageIcon, LogOut, Menu, ShoppingCart, HandCoins } from 'lucide-react';
import { useState } from 'react';
import { QuickActionFAB } from './QuickActionFAB';

export const Layout: React.FC = () => {
  const { user, logout } = useAuth();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const navItems = [
    { to: '/', icon: <LayoutDashboard size={20} />, label: 'Dashboard' },
    { to: '/notes', icon: <StickyNote size={20} />, label: 'Notes' },
    { to: '/market-memo', icon: <ShoppingCart size={20} />, label: 'Market Memo' },
    { to: '/expenses', icon: <Receipt size={20} />, label: 'Expenses' },
    { to: '/debts', icon: <HandCoins size={20} />, label: 'Debts' },
    { to: '/scanner', icon: <ScanLine size={20} />, label: 'Voucher Scanner' },
    { to: '/chatbot', icon: <Bot size={20} />, label: 'AI Chatbot' },
    { to: '/image-gen', icon: <ImageIcon size={20} />, label: 'Image Gen' },
  ];

  return (
    <div className="flex h-screen bg-gray-50 text-gray-900 font-sans">
      {/* Sidebar (Desktop) */}
      <aside className="hidden md:flex flex-col w-72 bg-white border-r border-gray-200 shadow-sm z-10">
        <div className="h-20 flex items-center px-8 border-b border-gray-100">
          <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center mr-3 shadow-md shadow-indigo-200">
            <LayoutDashboard size={18} className="text-white" />
          </div>
          <h1 className="text-xl font-bold text-gray-900 tracking-tight">Life Manager <span className="text-indigo-600">Pro</span></h1>
        </div>
        <nav className="flex-1 overflow-y-auto py-6">
          <ul className="space-y-1.5 px-4">
            {navItems.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  className={({ isActive }) =>
                    `flex items-center px-4 py-3 rounded-xl transition-all duration-200 ${
                      isActive
                        ? 'bg-indigo-50 text-indigo-700 font-semibold shadow-sm border border-indigo-100/50'
                        : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                    }`
                  }
                >
                  <span className={`mr-3 transition-colors ${
                    // We can't easily check isActive here without repeating the function, so we rely on parent text color
                    ''
                  }`}>{item.icon}</span>
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <div className="p-6 border-t border-gray-100 bg-gray-50/30">
          <div className="flex items-center mb-5 bg-white p-3 rounded-xl border border-gray-100 shadow-sm">
            <img src={user?.photoURL || 'https://via.placeholder.com/40'} alt="Profile" className="w-10 h-10 rounded-full mr-3 border border-gray-200" referrerPolicy="no-referrer" />
            <div className="overflow-hidden">
              <p className="text-sm font-bold text-gray-900 truncate">{user?.displayName}</p>
              <p className="text-xs text-gray-500 truncate">{user?.email}</p>
            </div>
          </div>
          <button
            onClick={logout}
            className="flex items-center justify-center w-full px-4 py-2.5 text-sm font-semibold text-red-600 bg-red-50 rounded-xl hover:bg-red-100 transition-colors border border-red-100"
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

        <main className="flex-1 overflow-y-auto p-4 md:p-8 pb-24 md:pb-8 bg-gray-50/50">
          <div className="max-w-7xl mx-auto w-full">
            <Outlet />
          </div>
        </main>

        <QuickActionFAB />

        {/* Mobile Bottom Navigation */}
        <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-2 py-2 flex justify-around items-center z-40 shadow-[0_-2px_10px_rgba(0,0,0,0.05)]">
          {navItems.slice(0, 5).map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex flex-col items-center p-2 rounded-lg transition-colors ${
                  isActive ? 'text-indigo-600' : 'text-gray-500'
                }`
              }
            >
              <span className="mb-1">{item.icon}</span>
              <span className="text-[10px] font-medium">{item.label}</span>
            </NavLink>
          ))}
          <button
            onClick={() => setIsMobileMenuOpen(true)}
            className="flex flex-col items-center p-2 text-gray-500"
          >
            <Menu size={20} className="mb-1" />
            <span className="text-[10px] font-medium">More</span>
          </button>
        </nav>
      </div>
    </div>
  );
};
