import './app.css';

import type { ReactNode } from 'react';

import { BrowserRouter, Navigate, NavLink, Outlet, Route, Routes } from 'react-router-dom';

type NavItem = {
  to: string;
  label: string;
  element: ReactNode;
};

const navItems: NavItem[] = [
  { to: '/repositories', label: 'Repositories', element: <>Repositories</> },
  { to: '/workbench', label: 'Workbench', element: <>Workbench</> },
  { to: '/projects', label: 'Projects', element: <>Projects</> },
];

function AppLayout() {
  return (
    <div className="app-shell">
      <header className="app-shell__header">
        <nav className="app-shell__nav">
          {navItems.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) => `app-shell__nav-link${isActive ? ' is-active' : ''}`}
            >
              {label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="app-shell__main">
        <Outlet />
      </main>
    </div>
  );
}

export function App() {
  return (
    <BrowserRouter basename="/n">
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/repositories" replace />} />
          {navItems.map(({ to, element }) => (
            <Route key={to} path={to} element={element} />
          ))}
          <Route path="*" element={<Navigate to="/repositories" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
