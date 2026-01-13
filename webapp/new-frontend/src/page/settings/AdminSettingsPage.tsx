import './settings-page.css';

import { Navigate } from 'react-router-dom';

import { useUser } from '../../components/RequireUser';

export function AdminSettingsPage() {
  const user = useUser();
  const isAdmin = user.role === 'ROLE_ADMIN';

  if (!isAdmin) {
    return <Navigate to="/repositories" replace />;
  }

  return (
    <div className="settings-page">
      <div className="settings-page__header">
        <h1>Admin settings</h1>
      </div>

      <section className="settings-card" aria-labelledby="settings-admin">
        <div className="settings-card__header">
          <h2 id="settings-admin">Admin-only controls</h2>
        </div>
        <p className="settings-page__hint">More admin controls will appear here.</p>
      </section>
    </div>
  );
}
