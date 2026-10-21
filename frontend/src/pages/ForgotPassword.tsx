import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, confirmPasswordReset, requestPasswordReset } from "../api/client";
import Layout from "../components/Layout";

export default function ForgotPassword() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  // The support link opens this page with the token in the address, so the user only types a new password.
  const tokenFromLink = params.get("token") ?? "";

  const [email, setEmail] = useState("");
  const [sent, setSent] = useState<string | null>(null);
  const [token, setToken] = useState(tokenFromLink);
  const [password, setPassword] = useState("");
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const r = await requestPasswordReset(email.trim());
      setSent(r.message);
    } catch {
      setError("The request could not be sent. Try again.");
    } finally {
      setBusy(false);
    }
  }

  async function onConfirm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await confirmPasswordReset(token.trim(), password);
      setDone(true);
    } catch (err) {
      if (err instanceof ApiError && err.status === 400) {
        setError("This reset link is invalid or has expired. Request a new one.");
      } else {
        setError("The password could not be changed. Try again.");
      }
    } finally {
      setBusy(false);
    }
  }

  if (done) {
    return (
      <Layout>
        <section className="panel">
          <h1>Password changed</h1>
          <p className="muted">You can now sign in with your new password.</p>
          <button type="button" className="btn" onClick={() => navigate("/login")}>Sign in</button>
        </section>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="prose">
        <h1>Forgot your password?</h1>
        <p>
          Enter your account email. If an account exists, support will send a one-time reset link. The link expires in
          30 minutes and works once. We never show whether an email is registered.
        </p>
      </div>

      <section className="panel">
        <h2>1. Request a reset link</h2>
        <form className="row" onSubmit={onRequest}>
          <div>
            <label htmlFor="reset-email">Email address</label>
            <input
              id="reset-email"
              className="input"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <button type="submit" className="btn" disabled={busy}>Request link</button>
        </form>
        {sent && <p className="notice" role="status">{sent}</p>}
      </section>

      <section className="panel">
        <h2>2. Set a new password</h2>
        <p className="muted">Paste the one-time reset token from the link support sent you, then choose a new password.</p>
        <form className="row" onSubmit={onConfirm}>
          <div>
            <label htmlFor="reset-token">Reset token</label>
            <input
              id="reset-token"
              className="input"
              required
              value={token}
              onChange={(e) => setToken(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="new-password">New password (at least 12 characters)</label>
            <input
              id="new-password"
              className="input"
              type="password"
              autoComplete="new-password"
              required
              minLength={12}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button type="submit" className="btn" disabled={busy}>Change password</button>
        </form>
        {error && <p className="error" role="alert">{error}</p>}
      </section>

      <p className="muted">
        Remembered it? <Link to="/login" className="link">Sign in</Link>.
      </p>
    </Layout>
  );
}