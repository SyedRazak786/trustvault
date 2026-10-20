import type { ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getSession, setSession } from "../auth/session";

export default function Layout({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const session = getSession();
  const isAdmin = session?.role === "ADMIN";

  // The background follows the signed-in role: theme-owner, theme-viewer or theme-admin.
  const themeClass = session ? `theme-${session.role.toLowerCase()}` : "";

  function signOut() {
    setSession(null);
    navigate("/login", { replace: true });
  }

  return (
    <div className={`app ${themeClass}`}>
      <header className="topbar">
        <Link to="/" className="brand">TrustVault</Link>
        <nav>
          <Link to={isAdmin ? "/admin/documents" : "/"}>Documents</Link>
          {isAdmin && <Link to="/admin">Activity</Link>}
        </nav>
        <div className="who">
          <span>{session?.email}</span>
          <span className="role">{session?.role}</span>
          <button type="button" className="link-button" onClick={signOut}>Sign out</button>
        </div>
      </header>
      <main className="content">{children}</main>
    </div>
  );
}