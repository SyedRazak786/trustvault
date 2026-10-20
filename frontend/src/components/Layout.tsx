import { useEffect, useRef, useState, type ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getSession, setSession } from "../auth/session";

export default function Layout({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const session = getSession();
  const isAdmin = session?.role === "ADMIN";
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  const themeClass = session ? `theme-${session.role.toLowerCase()}` : "";
  const initial = (session?.email ?? "?").charAt(0).toUpperCase();

  // Close the menu when clicking outside it, or pressing Escape.
  useEffect(() => {
    function onDown(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false);
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setMenuOpen(false);
    }
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, []);

  function signOut() {
    setSession(null);
    setMenuOpen(false);
    navigate("/login", { replace: true });
  }

  return (
    <div className={`app ${themeClass}`}>
      <header className="topbar">
        <Link to="/" className="brand">TrustVault</Link>
        <nav>
          <Link to={isAdmin ? "/admin/documents" : "/"}>Documents</Link>
          {!isAdmin && <Link to="/shared">Shared with me</Link>}
          {isAdmin && <Link to="/admin">Activity</Link>}
          <Link to="/about">About</Link>
          <Link to="/contact">Contact</Link>
        </nav>

        <div className="account" ref={menuRef}>
          <button
            type="button"
            className="avatar"
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            aria-label="Account menu"
            onClick={() => setMenuOpen((open) => !open)}
          >
            {initial}
          </button>

          {menuOpen && (
            <div className="menu" role="menu">
              <div className="menu-head">
                <span className="menu-email">{session?.email}</span>
                <span className="role">{session?.role}</span>
              </div>
              <Link role="menuitem" to="/profile" onClick={() => setMenuOpen(false)}>Profile</Link>
              {!isAdmin && (
                <Link role="menuitem" to="/shared" onClick={() => setMenuOpen(false)}>Shared with me</Link>
              )}
              <Link role="menuitem" to="/help" onClick={() => setMenuOpen(false)}>Help and support</Link>
              <button role="menuitem" type="button" className="menu-signout" onClick={signOut}>
                Sign out
              </button>
            </div>
          )}
        </div>
      </header>
      <main className="content">{children}</main>
    </div>
  );
}