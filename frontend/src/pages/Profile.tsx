import { useEffect, useState, type FormEvent } from "react";
import { changePassword, errorMessage, getProfile, updateProfile, type Profile } from "../api/client";
import Layout from "../components/Layout";

export default function ProfilePage() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [displayName, setDisplayName] = useState("");
  const [phone, setPhone] = useState("");
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    getProfile()
      .then((p) => {
        setProfile(p);
        setDisplayName(p.display_name ?? "");
        setPhone(p.phone ?? "");
      })
      .catch((err) => setError(errorMessage(err)));
  }, []);

  async function onSaveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setNotice(null);
    try {
      const saved = await updateProfile(displayName.trim(), phone.trim());
      setProfile(saved);
      setNotice("Profile saved.");
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function onChangePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await changePassword(currentPassword, newPassword);
      setCurrentPassword("");
      setNewPassword("");
      setNotice("Password changed.");
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Layout>
      <h1>Profile</h1>

      {error && <p className="error" role="alert">{error}</p>}
      {notice && <p className="notice" role="status">{notice}</p>}

      {profile && (
        <>
          <section className="panel">
            <h2>Your details</h2>
            <p className="muted">
              Email: {profile.email} · Role: {profile.role}
            </p>
            <p className="muted">Your email and role are set by the system and cannot be changed here.</p>

            <form className="row mt" onSubmit={onSaveProfile}>
              <div>
                <label htmlFor="display-name">Full name</label>
                <input id="display-name" className="input" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
              </div>
              <div>
                <label htmlFor="phone">Phone</label>
                <input id="phone" className="input" placeholder="+91 …" value={phone} onChange={(e) => setPhone(e.target.value)} />
              </div>
              <button type="submit" className="btn">Save</button>
            </form>
          </section>

          <section className="panel">
            <h2>Change password</h2>
            <p className="muted">You need your current password. Use at least 12 characters.</p>
            <form className="row mt" onSubmit={onChangePassword}>
              <div>
                <label htmlFor="current-password">Current password</label>
                <input id="current-password" className="input" type="password" autoComplete="current-password" required value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} />
              </div>
              <div>
                <label htmlFor="new-password">New password</label>
                <input id="new-password" className="input" type="password" autoComplete="new-password" required minLength={12} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
              </div>
              <button type="submit" className="btn" disabled={busy}>Change password</button>
            </form>
          </section>
        </>
      )}
    </Layout>
  );
}