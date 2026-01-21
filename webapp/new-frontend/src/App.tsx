import './app.css';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { BrowserRouter, Navigate, NavLink, Outlet, Route, Routes } from 'react-router-dom';

import { RequireUser } from './components/RequireUser';
import { UserMenu } from './components/UserMenu';
import { RepositoriesPage } from './page/repositories/RepositoriesPage';
import { ReviewProjectPage } from './page/review-project/ReviewProjectPage';
import { ReviewProjectPageV2Route } from './page/review-project/ReviewProjectPageV2Route';
import { ReviewProjectCreatePage } from './page/review-projects/ReviewProjectCreatePage';
import { ReviewProjectsPage } from './page/review-projects/ReviewProjectsPage';
import { AdminSettingsPage } from './page/settings/AdminSettingsPage';
import { CharCodeHelperPage } from './page/tools/CharCodeHelperPage';
import { WorkbenchPage } from './page/workbench/WorkbenchPage';

type NavItem = {
  to: string;
  label: string;
  element: ReactNode;
};

const navItems: NavItem[] = [
  { to: '/repositories', label: 'Repositories', element: <RepositoriesPage /> },
  { to: '/workbench', label: 'Workbench', element: <WorkbenchPage /> },
  { to: '/review-projects', label: 'Review Projects', element: <ReviewProjectsPage /> },
  { to: '/review-projects-v2', label: 'Review Projects V2', element: <ReviewProjectsPage /> },
];

const queryClient = new QueryClient();

function AppLayout() {
  return (
    <div className="app-shell">
      <header className="app-shell__header">
        <div className="app-shell__header-content">
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
          <UserMenu />
        </div>
      </header>
      <main className="app-shell__main">
        <Outlet />
      </main>
    </div>
  );
}

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter basename="/n">
        <Routes>
          <Route
            element={
              <RequireUser>
                <AppLayout />
              </RequireUser>
            }
          >
            <Route path="/" element={<Navigate to="/repositories" replace />} />
            {navItems.map(({ to, element }) => (
              <Route key={to} path={to} element={element} />
            ))}
            <Route path="/review-projects/new" element={<ReviewProjectCreatePage />} />
            <Route path="/review-projects/:projectId" element={<ReviewProjectPage />} />
            <Route path="/review-projects-v2/:projectId" element={<ReviewProjectPageV2Route />} />
            <Route path="/settings/admin" element={<AdminSettingsPage />} />
            <Route path="/tools/char-code" element={<CharCodeHelperPage />} />
            <Route path="*" element={<Navigate to="/repositories" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
