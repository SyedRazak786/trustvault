import { useEffect, useState } from "react";
import { api, errorMessage } from "../api/client";
import Layout from "../components/Layout";
import { formatDate, formatSize } from "../lib/format";

type AdminDoc = {
  id: number;
  name: string;
  category: string;
  content_type: string;
  size_bytes: number;
  created_at: string;
  deleted: boolean | number;
  owner: string;
};

export default function AdminDocuments() {
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [docs, setDocs] = useState<AdminDoc[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Waits until typing pauses, so each search is one audited request rather than one per keystroke.
  useEffect(() => {
    const timer = setTimeout(() => setQuery(search.trim()), 350);
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    const path = "/api/admin/documents" + (query ? `?q=${encodeURIComponent(query)}` : "");
    api<AdminDoc[]>(path)
      .then((rows) => {
        if (!cancelled) setDocs(rows);
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [query]);

  return (
    <Layout>
      <div className="page-head">
        <h1>Documents</h1>
        <input
          className="input"
          type="search"
          placeholder="Search name or category"
          aria-label="Search documents"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <p className="muted">
        Administrators see document details only. Contents, downloads and sharing belong to the owner and the people they grant access to.
      </p>

      {error && <p className="error" role="alert">{error}</p>}

      {loading ? (
        <p className="muted">Loading</p>
      ) : docs.length === 0 ? (
        <p className="empty">No documents match your search.</p>
      ) : (
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Owner</th>
                <th>Category</th>
                <th>Size</th>
                <th>Uploaded</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {docs.map((d) => {
                const deleted = Boolean(d.deleted);
                return (
                  <tr key={d.id}>
                    <td>{d.name}</td>
                    <td>{d.owner}</td>
                    <td>{d.category}</td>
                    <td>{formatSize(d.size_bytes)}</td>
                    <td>{formatDate(d.created_at)}</td>
                    <td className={deleted ? "tag-denied" : "tag-allowed"}>
                      {deleted ? "Deleted" : "Active"}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </Layout>
  );
}