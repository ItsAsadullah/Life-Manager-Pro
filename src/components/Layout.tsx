import React, { useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useSettings } from '../contexts/SettingsContext';
import { LayoutDashboard, StickyNote, Receipt, ScanLine, Bot, Image as ImageIcon, LogOut, Menu, ShoppingCart, HandCoins, Settings as SettingsIcon, X, Bell } from 'lucide-react';
import { QuickActionFAB } from './QuickActionFAB';
import { motion, AnimatePresence } from 'motion/react';
import { ThemeToggle } from './ThemeToggle';
import hisabNikashLogo from './images/Hisab Nikash.png';

export const Layout: React.FC = () => {
  const { user, logout } = useAuth();
  const { t, theme } = useSettings();
  const location = useLocation();
  const navigate = useNavigate();
  const [isMoreMenuOpen, setIsMoreMenuOpen] = useState(false);

  const navItems = [
    { to: '/', icon: <LayoutDashboard size={20} />, label: t('dashboard'), key: 'dashboard' },
    { to: '/expenses', icon: <Receipt size={20} />, label: t('expenses'), key: 'expenses' },
    { to: '/debts', icon: <HandCoins size={20} />, label: t('debts'), key: 'debts' },
    { to: '/notes', icon: <StickyNote size={20} />, label: t('notes'), key: 'notes' },
    { to: '/market-memo', icon: <ShoppingCart size={20} />, label: t('marketMemo'), key: 'marketMemo' },
    { to: '/scanner', icon: <ScanLine size={20} />, label: t('scanner'), key: 'scanner' },
    { to: '/chatbot', icon: <Bot size={20} />, label: t('chatbot'), key: 'chatbot' },
    { to: '/image-gen', icon: <ImageIcon size={20} />, label: t('imageGen'), key: 'imageGen' },
    { to: '/settings', icon: <SettingsIcon size={20} />, label: t('settings'), key: 'settings' },
  ];

  const isDashboard = location.pathname === '/';

  return (
    <div className="flex h-screen bg-gray-50 text-gray-900 font-sans dark:bg-gray-900 dark:text-gray-100">
      {/* Sidebar (Desktop) */}
      <aside className="hidden md:flex flex-col w-72 bg-white border-r border-gray-200 shadow-sm z-10 dark:bg-gray-800 dark:border-gray-700">
        <div className="h-20 flex items-center px-8 border-b border-gray-100 dark:border-gray-700">
            <div className="w-8 h-8 flex items-center justify-center mr-3">
              <img src={hisabNikashLogo} alt="Logo" className="w-full h-full object-contain" />
            </div>
            <h1 className="text-xl font-bold text-gray-900 tracking-tight dark:text-white">Hisab <span className="text-indigo-600">Nikash</span></h1>
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
                        ? 'bg-indigo-50 text-indigo-700 font-semibold shadow-sm border border-indigo-100/50 dark:bg-indigo-900 dark:text-indigo-200 dark:border-indigo-800'
                        : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-gray-100'
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
        <div className="p-6 border-t border-gray-100 bg-gray-50/30 dark:border-gray-700 dark:bg-gray-800/50">
          <div className="flex items-center mb-5 bg-white p-3 rounded-xl border border-gray-100 shadow-sm dark:bg-gray-700 dark:border-gray-600">
            <img src={user?.photoURL || 'https://via.placeholder.com/40'} alt="Profile" className="w-10 h-10 rounded-full mr-3 border border-gray-200 dark:border-gray-600" referrerPolicy="no-referrer" />
            <div className="overflow-hidden">
              <p className="text-sm font-bold text-gray-900 truncate dark:text-white">{user?.displayName}</p>
              <p className="text-xs text-gray-500 truncate dark:text-gray-400">{user?.email}</p>
            </div>
          </div>
          <button
            onClick={logout}
            className="flex items-center justify-center w-full px-4 py-2.5 text-sm font-semibold text-red-600 bg-red-50 rounded-xl hover:bg-red-100 transition-colors border border-red-100 dark:bg-red-900/20 dark:text-red-400 dark:border-red-900/50 dark:hover:bg-red-900/30"
          >
            <LogOut size={18} className="mr-2" />
            {t('logout')}
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Mobile Header (No Hamburger) */}
        <header className="md:hidden h-16 bg-[#f7f9fc] flex items-center justify-between px-4 dark:bg-gray-800 shrink-0">
          <div className="flex items-center space-x-2">
            <img src={hisabNikashLogo} alt="Logo" className="w-8 h-8 object-contain" />
            <h1 className="text-lg font-bold text-blue-600 dark:text-blue-400">হিসাব নিকাশ</h1>
          </div>
          
          <div className="flex items-center space-x-4">
             <div className="flex items-center bg-blue-50 text-blue-600 px-3 py-1 rounded-full text-xs font-bold dark:bg-blue-900/30 dark:text-blue-400">
               <span className="mr-1">★</span> 10
             </div>
             <button className="text-gray-700 dark:text-gray-300">
               <Bell size={20} className="fill-current" />
             </button>
             <ThemeToggle />
             <button onClick={() => navigate('/settings')} className="text-gray-700 dark:text-gray-300">
                <SettingsIcon size={20} className="fill-current" />
             </button>
          </div>
        </header>

        {/* Mobile Top Navigation */}
        <nav className="md:hidden bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 px-1 py-1 flex justify-between items-center z-40 shadow-sm shrink-0 overflow-x-auto hide-scrollbar scroll-smooth">
          {navItems.slice(0, 5).map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className="flex flex-col items-center justify-center p-1 min-w-[50px] transition-colors"
            >
              <div className={`mb-1 p-1.5 rounded-lg ${location.pathname === item.to ? 'bg-blue-600 text-white dark:bg-blue-500' : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'}`}>
                 {React.cloneElement(item.icon, { size: 18 })}
              </div>
              <span className={`text-[10px] font-medium whitespace-nowrap ${location.pathname === item.to ? 'text-blue-600 dark:text-blue-400' : 'text-gray-500 dark:text-gray-400'}`}>{item.label}</span>
            </NavLink>
          ))}
          <button
            onClick={() => setIsMoreMenuOpen(true)}
            className="flex flex-col items-center justify-center p-1 min-w-[50px] transition-colors"
          >
            <div className={`mb-1 p-1.5 rounded-lg ${isMoreMenuOpen ? 'bg-blue-600 text-white dark:bg-blue-500' : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'}`}>
              <Menu size={18} />
            </div>
            <span className={`text-[10px] font-medium whitespace-nowrap ${isMoreMenuOpen ? 'text-blue-600 dark:text-blue-400' : 'text-gray-500 dark:text-gray-400'}`}>{t('more')}</span>
          </button>
        </nav>

        <main className="flex-1 overflow-y-auto p-4 md:p-8 bg-gray-50/50 dark:bg-gray-900">
          <div className="max-w-7xl mx-auto w-full">
            <Outlet />
          </div>
        </main>

        {isDashboard && <QuickActionFAB />}

        {/* More Menu Overlay */}
        <AnimatePresence>
          {isMoreMenuOpen && (
            <>
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setIsMoreMenuOpen(false)}
                className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 md:hidden"
              />
              <motion.div
                initial={{ y: '100%' }}
                animate={{ y: 0 }}
                exit={{ y: '100%' }}
                transition={{ type: 'spring', damping: 25, stiffness: 200 }}
                className="fixed bottom-0 left-0 right-0 bg-white rounded-t-[32px] p-6 z-[60] md:hidden shadow-2xl border-t border-gray-100"
              >
                <div className="flex justify-between items-center mb-6">
                  <h2 className="text-xl font-bold text-gray-900 dark:text-white">{t('more')}</h2>
                  <button onClick={() => setIsMoreMenuOpen(false)} className="p-2 bg-gray-100 dark:bg-gray-700 rounded-full text-gray-500 dark:text-gray-300">
                    <X size={20} />
                  </button>
                </div>
                <div className="grid grid-cols-3 gap-4 mb-8">
                  {navItems.slice(5).map((item) => (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      onClick={() => setIsMoreMenuOpen(false)}
                      className={({ isActive }) =>
                        `flex flex-col items-center p-4 rounded-2xl transition-all ${
                          isActive ? 'bg-indigo-50 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-300' : 'bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-300'
                        }`
                      }
                    >
                      <div className="mb-2">{item.icon}</div>
                      <span className="text-[10px] font-bold text-center leading-tight">{item.label}</span>
                    </NavLink>
                  ))}
                  <button
                    onClick={() => {
                      logout();
                      setIsMoreMenuOpen(false);
                    }}
                    className="flex flex-col items-center p-4 rounded-2xl bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400"
                  >
                    <div className="mb-2"><LogOut size={20} /></div>
                    <span className="text-[10px] font-bold">{t('logout')}</span>
                  </button>
                </div>
              </motion.div>
            </>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
};

