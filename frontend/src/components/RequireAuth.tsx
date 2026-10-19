import { useSyncExternalStore, type ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { getSession, subscribe } from "../auth/session";

export default function RequireAuth({ children }: { children: ReactNode }) {
  const session = useSyncExternalStore(subscribe, getSession);

  if (!session) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}