// TODO: typed API client, frontend build step 1.
import { getSession, setSession, type Session } from "../auth/session";

const BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Content-Type", "application/json");

  const session = getSession();
  if (session) {
    headers.set("Authorization", `Bearer ${session.token}`);
  }

  let response: Response;
  try {
    response = await fetch(BASE_URL + path, { ...init, headers });
  } catch {
    throw new ApiError(0, "Could not reach the server.");
  }

  if (response.status === 401) {
    // The server no longer accepts this token, so drop it.
    setSession(null);
  }

  if (!response.ok) {
    throw new ApiError(response.status, `Request failed with status ${response.status}`);
  }

  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}

export function login(email: string, password: string): Promise<Session> {
  return api<Session>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}