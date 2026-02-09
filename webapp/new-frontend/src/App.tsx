import './app.css';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { BrowserRouter, Navigate, NavLink, Outlet, Route, Routes } from 'react-router-dom';

import { RequireUser } from './components/RequireUser';
import { UserMenu } from './components/UserMenu';
import { AiTranslatePage } from './page/ai-translate/AiTranslatePage';
import { RepositoriesPage } from './page/repositories/RepositoriesPage';
import { ReviewProjectPage } from './page/review-project/ReviewProjectPage';
import { ReviewProjectCreatePage } from './page/review-projects/ReviewProjectCreatePage';
import { ReviewProjectsPage } from './page/review-projects/ReviewProjectsPage';
import { ScreenshotsDropzonePage } from './page/screenshots/ScreenshotsDropzonePage';
import { AdminSettingsPage } from './page/settings/AdminSettingsPage';
import { AdminUserBatchPage } from './page/settings/AdminUserBatchPage';
import { AdminUserDetailPage } from './page/settings/AdminUserDetailPage';
import { AdminUserSettingsPage } from './page/settings/AdminUserSettingsPage';
import { TextUnitDetailPage } from './page/text-unit-detail/TextUnitDetailPage';
import { CharCodeHelperPage } from './page/tools/CharCodeHelperPage';
import { IcuMessagePreviewPage } from './page/tools/IcuMessagePreviewPage';
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
  { to: '/screenshots', label: 'Screenshots', element: <ScreenshotsDropzonePage /> },
];

const queryClient = new QueryClient();

function AppLayout({ showHeader }: { showHeader: boolean }) {
  return (
    <div className={`app-shell${showHeader ? '' : ' app-shell--bare'}`}>
      {showHeader ? (
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
      ) : null}
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
                <AppLayout showHeader />
              </RequireUser>
            }
          >
            <Route path="/" element={<Navigate to="/repositories" replace />} />
            {navItems.map(({ to, element }) => (
              <Route key={to} path={to} element={element} />
            ))}
            <Route path="/ai-translate" element={<AiTranslatePage />} />
            <Route path="/review-projects/new" element={<ReviewProjectCreatePage />} />
            <Route path="/screenshots" element={<ScreenshotsDropzonePage />} />
            <Route path="/settings/admin" element={<AdminSettingsPage />} />
            <Route path="/settings/admin/users" element={<AdminUserSettingsPage />} />
            <Route path="/tools/char-code" element={<CharCodeHelperPage />} />
            <Route path="/tools/icu-preview" element={<IcuMessagePreviewPage />} />
            <Route path="*" element={<Navigate to="/repositories" replace />} />
          </Route>
          <Route
            element={
              <RequireUser>
                <AppLayout showHeader={false} />
              </RequireUser>
            }
          >
            <Route path="/review-projects/:projectId" element={<ReviewProjectPage />} />
            <Route path="/text-units/:tmTextUnitId" element={<TextUnitDetailPage />} />
            <Route path="/settings/admin/users/:userId" element={<AdminUserDetailPage />} />
            <Route path="/settings/admin/users/batch" element={<AdminUserBatchPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
