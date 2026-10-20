import { useSyncExternalStore, type ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { getSession, subscribe, type Role } from "../auth/session";

// UX only. The server enforces every permission; this just avoids showing pages a role cannot use.
export default function RequireAuth({ children, role }: { children: ReactNode; role?: Role }) {
  const session = useSyncExternalStore(subscribe, getSession);

  if (!session) {
    return <Navigate to="/login" replace />;
  }
  if (role && session.role !== role) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}