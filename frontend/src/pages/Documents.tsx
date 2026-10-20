import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { api, errorMessage, uploadDocument } from "../api/client";
import { getSession } from "../auth/session";
import Layout from "../components/Layout";
import { formatDate, formatSize } from "../lib/format";
import type { DocRow } from "../lib/types";

const ACCEPTED = ".pdf,.png,.jpg,.jpeg,.txt";

export default function Documents() {
  const session = getSession();
  const canUpload = session?.role === "OWNER";

  const [docs, setDocs] = useState<DocRow[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [file, setFile] = useState<File | null>(null);
  const [category, setCategory] = useState("");
  const [uploading, setUploading] = useState(false);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      setDocs(await api<DocRow[]>("/api/documents"));
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function onUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file) return;
    // Keep a reference to the form now. React clears the event's currentTarget after the first await.
    const form = event.currentTarget;
    setUploading(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await uploadDocument(file, category.trim());
      setNotice(`Uploaded ${saved.name}.`);
      setFile(null);
      setCategory("");
      form.reset();
      await load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setUploading(false);
    }
  }

  const q = query.trim().toLowerCase();
  const visible = docs.filter(
    (d) => !q || d.name.toLowerCase().includes(q) || d.category.toLowerCase().includes(q)
  );

  return (
    <Layout>
      <div className="page-head">
        <h1>Documents</h1>
        <input
          className="input"
          type="search"
          placeholder="Search name or category"
          aria-label="Search documents"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>

      {error && <p className="error" role="alert">{error}</p>}
      {notice && <p className="notice" role="status">{notice}</p>}

      {canUpload && (
        <section className="panel">
          <h2>Upload a document</h2>
          <form className="row" onSubmit={onUpload}>
            <div>
              <label htmlFor="file">File</label>
              <input
                id="file"
                type="file"
                accept={ACCEPTED}
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
            </div>
            <div>
              <label htmlFor="category">Category</label>
              <input
                id="category"
                className="input"
                placeholder="e.g. HR"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              />
            </div>
            <button type="submit" className="btn" disabled={!file || uploading}>
              {uploading ? "Uploading" : "Upload"}
            </button>
          </form>
          <p className="muted">
            PDF, PNG, JPEG or plain text, up to 10 MB. The file's content decides its type, not its name.
          </p>
        </section>
      )}

      {loading ? (
        <p className="muted">Loading</p>
      ) : visible.length === 0 ? (
        <p className="empty">
          {docs.length === 0 ? "No documents are available to you." : "No documents match your search."}
        </p>
      ) : (
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Category</th>
                <th>Size</th>
                <th>Uploaded</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((d) => (
                <tr key={d.id}>
                  <td><Link className="name" to={`/documents/${d.id}`}>{d.name}</Link></td>
                  <td>{d.category}</td>
                  <td>{formatSize(d.size_bytes)}</td>
                  <td>{formatDate(d.created_at)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Layout>
  );
}