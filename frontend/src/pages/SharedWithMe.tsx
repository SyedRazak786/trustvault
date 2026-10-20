import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, errorMessage } from "../api/client";
import Layout from "../components/Layout";
import { formatDate, formatSize } from "../lib/format";

type SharedDoc = {
  id: number;
  name: string;
  category: string;
  size_bytes: number;
  created_at: string;
  owner_email: string;
  permission: "READ" | "EDIT";
};

export default function SharedWithMe() {
  const [docs, setDocs] = useState<SharedDoc[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<SharedDoc[]>("/api/shares")
      .then(setDocs)
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  return (
    <Layout>
      <div className="page-head">
        <h1>Shared with me</h1>
      </div>

      {error && <p className="error" role="alert">{error}</p>}

      {loading ? (
        <p className="muted">Loading</p>
      ) : docs.length === 0 ? (
        <p className="empty">Nobody has shared a document with you yet.</p>
      ) : (
        <div className="scroll">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>From</th>
                <th>Access</th>
                <th>Size</th>
                <th>Shared</th>
              </tr>
            </thead>
            <tbody>
              {docs.map((d) => (
                <tr key={d.id}>
                  <td><Link className="name" to={`/documents/${d.id}`}>{d.name}</Link></td>
                  <td>{d.owner_email}</td>
                  <td>{d.permission}</td>
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