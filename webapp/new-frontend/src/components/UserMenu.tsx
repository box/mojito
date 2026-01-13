import './user-menu.css';

import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import type { ApiUserProfile } from '../api/users';
import { useUser } from './RequireUser';

function formatRole(role: ApiUserProfile['role']) {
  switch (role) {
    case 'ROLE_ADMIN':
      return 'Admin';
    case 'ROLE_PM':
      return 'Project manager';
    case 'ROLE_TRANSLATOR':
      return 'Translator';
    default:
      return 'User';
  }
}

export function UserMenu() {
  const user = useUser();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!open) {
      return;
    }

    const handlePointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setOpen(false);
      }
    };

    const handleFocusIn = (event: FocusEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    window.addEventListener('pointerdown', handlePointerDown);
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('focusin', handleFocusIn);

    return () => {
      window.removeEventListener('pointerdown', handlePointerDown);
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('focusin', handleFocusIn);
    };
  }, [open]);

  const displayName = user.username || 'Account';
  const roleLabel = formatRole(user.role);
  const isAdmin = user.role === 'ROLE_ADMIN';

  const handleNavigate = (path: string) => {
    setOpen(false);
    void navigate(path);
  };

  return (
    <div className="user-menu" ref={containerRef}>
      <button
        type="button"
        className={`user-menu__button${open ? ' is-open' : ''}`}
        onClick={() => setOpen((prev) => !prev)}
        aria-haspopup="menu"
        aria-expanded={open}
        title={displayName}
        aria-label={`Account menu for ${displayName}`}
      >
        <span className="user-menu__name">{displayName}</span>
        <span className={`user-menu__chevron${open ? ' is-open' : ''}`} aria-hidden="true" />
      </button>
      {open ? (
        <div className="user-menu__panel" role="menu">
          <div className="user-menu__line">
            <div className="user-menu__line-name">{displayName}</div>
            <div className="user-menu__line-role">{roleLabel}</div>
          </div>
          {isAdmin ? (
            <div className="user-menu__actions" role="none">
              <button
                type="button"
                className="user-menu__action"
                role="menuitem"
                onClick={() => handleNavigate('/settings/admin')}
              >
                Admin settings
              </button>
            </div>
          ) : (
            <div className="user-menu__hint">More account actions will land here soon.</div>
          )}
        </div>
      ) : null}
    </div>
  );
}
