import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, login, signup } from "../api/client";
import { setSession } from "../auth/session";

export default function SignUp() {
  const navigate = useNavigate();
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await signup(email.trim(), password, displayName.trim());
      // Sign in straight away, so the new owner lands on their documents.
      const session = await login(email.trim(), password);
      setSession(session);
      navigate("/", { replace: true });
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError("An account with this email already exists. Sign in instead.");
      } else if (err instanceof ApiError && err.status === 400) {
        setError(err.message);
      } else if (err instanceof ApiError && err.status === 0) {
        setError("The server cannot be reached. Check that the backend is running.");
      } else {
        setError("Account could not be created. Try again.");
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="shell">
      <aside className="brand-panel">
        <p className="brand-name">TrustVault</p>
        <p className="brand-statement">Create an account to share documents securely.</p>
        <ul className="rules">
          <li>New accounts can upload, share and receive documents.</li>
          <li>Passwords are at least 12 characters.</li>
          <li>Account creation is recorded in the audit log.</li>
        </ul>
      </aside>

      <main className="form-panel">
        <div className="form-inner">
          <h1 className="form-title">Create account</h1>
          <p className="form-note">You will sign in as an owner. Your details can be changed later.</p>

          <form onSubmit={onSubmit}>
            <div className="field">
              <label htmlFor="name">Full name</label>
              <input id="name" autoComplete="name" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
            </div>
            <div className="field">
              <label htmlFor="email">Email address</label>
              <input id="email" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div className="field">
              <label htmlFor="password">Password (at least 12 characters)</label>
              <input id="password" type="password" autoComplete="new-password" required minLength={12} value={password} onChange={(e) => setPassword(e.target.value)} />
            </div>

            {error && <p className="error" role="alert">{error}</p>}

            <button type="submit" className="submit" disabled={busy}>
              {busy ? "Creating account" : "Create account"}
            </button>
          </form>

          <div className="form-footer">
            <p>Already have an account? <Link to="/login" className="link">Sign in</Link></p>
          </div>
        </div>
      </main>
    </div>
  );
}