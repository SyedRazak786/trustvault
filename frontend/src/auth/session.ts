export type Role = "ADMIN" | "OWNER" | "VIEWER";

export type Session = {
  token: string;
  email: string;
  role: Role;
};

// Kept in memory, not localStorage, so a script on the page cannot read it later.
// The token is lost on refresh, so the user signs in again. That is acceptable for this project.
let current: Session | null = null;
const listeners = new Set<() => void>();

export function getSession(): Session | null {
  return current;
}

export function setSession(session: Session | null): void {
  current = session;
  listeners.forEach((listener) => listener());
}

export function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}