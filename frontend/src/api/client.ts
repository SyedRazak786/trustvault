import { getSession, setSession, type Session } from "../auth/session";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await send(path, init);
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function login(email: string, password: string): Promise<Session> {
  return api<Session>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function uploadDocument(file: File, category: string): Promise<{ id: number; name: string }> {
  const form = new FormData();
  form.append("file", file);
  if (category) {
    form.append("category", category);
  }
  return api("/api/documents", { method: "POST", body: form });
}

// Fetches the file with the token and saves it, because a plain link cannot send the Authorization header.
export async function downloadDocument(id: number, name: string): Promise<void> {
  const response = await send(`/api/documents/${id}/download`);
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export function errorMessage(err: unknown): string {
  return err instanceof ApiError ? err.message : "Something went wrong. Try again.";
}

async function send(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  if (typeof init.body === "string" && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const session = getSession();
  if (session) {
    headers.set("Authorization", `Bearer ${session.token}`);
  }

  let response: Response;
  try {
    response = await fetch(path, { ...init, headers });
  } catch {
    throw new ApiError(0, "The server cannot be reached. Check that the backend is running.");
  }

  if (response.status === 401 && session) {
    // The token has expired or was rejected, so the user must sign in again.
    setSession(null);
  }
  if (!response.ok) {
    throw new ApiError(response.status, await readError(response));
  }
  return response;
}

async function readError(response: Response): Promise<string> {
  try {
    const body = await response.json();
    if (body && typeof body.error === "string") {
      return body.error;
    }
  } catch {
    // The body was not JSON, so fall back to a message for the status.
  }
  return statusMessage(response.status);
}

function statusMessage(status: number): string {
  switch (status) {
    case 401:
      return "Your session has ended. Sign in again.";
    case 403:
      return "You do not have permission to do that.";
    case 404:
      return "This document is not available to you.";
    case 413:
      return "The file is larger than 10 MB.";
    case 415:
      return "This file type is not accepted.";
    default:
      return "Something went wrong. Try again.";
  }
}