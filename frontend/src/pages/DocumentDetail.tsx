import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, downloadDocument, errorMessage } from "../api/client";
import { getSession } from "../auth/session";
import Layout from "../components/Layout";
import TrustTrail from "../components/TrustTrail";
import { formatDate, formatSize } from "../lib/format";
import type { DocRow, Grant } from "../lib/types";

export default function DocumentDetail() {
  const { id } = useParams();
  const docId = Number(id);
  const navigate = useNavigate();
  const session = getSession();
  const isOwnerRole = session?.role === "OWNER";

  const [doc, setDoc] = useState<DocRow | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  // The grants list only loads for the owner of this document, so a successful load means "you own it".
  const [grants, setGrants] = useState<Grant[] | null>(null);
  const [sharingNote, setSharingNote] = useState<string | null>(null);
  const isOwner = isOwnerRole && grants !== null;

  const [email, setEmail] = useState("");
  const [permission, setPermission] = useState<"READ" | "EDIT">("READ");
  const [busy, setBusy] = useState(false);

  async function loadGrants() {
    try {
      setGrants(await api<Grant[]>(`/api/documents/${docId}/grants`));
      setSharingNote(null);
    } catch {
      setGrants(null);
      setSharingNote("Only the owner of this document can manage who has access.");
    }
  }

  async function load() {
    setLoading(true);
    setError(null);
    try {
      // There is no single-document endpoint yet, so the document is found in the visible list.
      const all = await api<DocRow[]>("/api/documents");
      const found = all.find((d) => d.id === docId) ?? null;
      setDoc(found);
      if (found && isOwnerRole) {
        await loadGrants();
      }
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [docId]);

  async function onDownload() {
    if (!doc) return;
    setError(null);
    try {
      await downloadDocument(doc.id, doc.name);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function onShare(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const target = email.trim();
      await api(`/api/documents/${docId}/grants`, {
        method: "POST",
        body: JSON.stringify({ email: target, permission }),
      });
      setNotice(`Shared with ${target} (${permission}).`);
      setEmail("");
      await loadGrants();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function onRevoke(userId: number, userEmail: string) {
    setError(null);
    setNotice(null);
    try {
      await api(`/api/documents/${docId}/grants/${userId}`, { method: "DELETE" });
      setNotice(`Removed access for ${userEmail}.`);
      await loadGrants();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function onDelete() {
    if (!window.confirm("Delete this document? It disappears for everyone, and the deletion is recorded.")) {
      return;
    }
    setError(null);
    try {
      await api(`/api/documents/${docId}`, { method: "DELETE" });
      navigate("/", { replace: true });
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <Layout>
      <p><Link to="/" className="link">All documents</Link></p>

      {loading ? (
        <p className="muted">Loading</p>
      ) : !doc ? (
        <p className="error" role="alert">This document is not available to you.</p>
      ) : (
        <>
          <div className="page-head">
            <h1>{doc.name}</h1>
            <div className="row">
              <button type="button" className="btn" onClick={() => void onDownload()}>Download</button>
              {isOwner && (
                <button type="button" className="btn btn-danger" onClick={() => void onDelete()}>
                  Delete
                </button>
              )}
            </div>
          </div>

          {error && <p className="error" role="alert">{error}</p>}
          {notice && <p className="notice" role="status">{notice}</p>}

          <section className="panel">
            <p className="muted">
              Category: {doc.category} · {formatSize(doc.size_bytes)} · Uploaded {formatDate(doc.created_at)}
            </p>
          </section>

          {isOwner && (
            <section className="panel">
              <h2>Who has access</h2>
              {grants && grants.length === 0 ? (
                <p className="muted">Nobody else has access yet.</p>
              ) : (
                <div className="scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>Email</th>
                        <th>Access</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {grants?.map((g) => (
                        <tr key={g.user_id}>
                          <td>{g.email}</td>
                          <td>{g.permission}</td>
                          <td>
                            <button
                              type="button"
                              className="btn btn-danger"
                              onClick={() => void onRevoke(g.user_id, g.email)}
                            >
                              Remove
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              <form className="row mt" onSubmit={onShare}>
                <div>
                  <label htmlFor="share-email">Email</label>
                  <input
                    id="share-email"
                    className="input"
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                  />
                </div>
                <div>
                  <label htmlFor="share-permission">Access</label>
                  <select
                    id="share-permission"
                    className="select"
                    value={permission}
                    onChange={(e) => setPermission(e.target.value as "READ" | "EDIT")}
                  >
                    <option value="READ">Read</option>
                    <option value="EDIT">Edit</option>
                  </select>
                </div>
                <button type="submit" className="btn" disabled={busy}>Share</button>
              </form>
            </section>
          )}

          {isOwner && <TrustTrail documentId={doc.id} />}

          {isOwnerRole && !isOwner && sharingNote && <p className="muted">{sharingNote}</p>}
        </>
      )}
    </Layout>
  );
}