import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError, login } from "../api/client";
import { setSession } from "../auth/session";

const RULES = [
  "Every request is checked on the server.",
  "Refused attempts are recorded, not hidden.",
  "Access ends the moment it is revoked.",
];

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);

    try {
      const session = await login(email, password);
      setSession(session);
      navigate("/", { replace: true });
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        // Same message for an unknown email and a wrong password, so the form cannot be used to find accounts.
        setError("The email or password you entered is not correct.");
      } else if (err instanceof ApiError && err.status === 0) {
        setError("The server cannot be reached. Check your connection and try again.");
      } else {
        setError("Sign-in did not complete. Try again.");
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="shell">
      <aside className="brand-panel">
        <p className="brand-name">TrustVault</p>

        <p className="brand-statement">
          Documents open only for the people they were shared with.
        </p>

        <ul className="rules">
          {RULES.map((rule) => (
            <li key={rule}>{rule}</li>
          ))}
        </ul>
      </aside>

      <main className="form-panel">
        <div className="form-inner">
          <h1 className="form-title">Sign in</h1>
          <p className="form-note">
            Use the email address and password your administrator gave you.
          </p>

          <form onSubmit={onSubmit} noValidate>
            <div className="field">
              <label htmlFor="email">Email address</label>
              <input
                id="email"
                type="email"
                autoComplete="username"
                required
                aria-invalid={error ? true : undefined}
                aria-describedby={error ? "login-error" : undefined}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>

            <div className="field">
              <label htmlFor="password">Password</label>
              <div className="password-row">
                <input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  required
                  aria-invalid={error ? true : undefined}
                  aria-describedby={error ? "login-error" : undefined}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                <button
                  type="button"
                  className="toggle"
                  onClick={() => setShowPassword((shown) => !shown)}
                  aria-pressed={showPassword}
                >
                  {showPassword ? "Hide" : "Show"}
                </button>
              </div>
            </div>

            {error && (
              <p id="login-error" className="error" role="alert">
                {error}
              </p>
            )}

            <button type="submit" className="submit" disabled={busy}>
              {busy ? "Signing in" : "Sign in"}
            </button>
          </form>

          <div className="form-footer">
            <p>Every sign-in attempt is recorded. <a className="link" href="/signup">Create an account</a></p>
          </div>
        </div>
      </main>
    </div>
  );
}