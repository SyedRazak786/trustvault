import { useEffect, useState } from "react";
import { api, errorMessage } from "../api/client";
import Layout from "../components/Layout";
import { formatDate } from "../lib/format";
import type { AuditRow, ChainResult } from "../lib/types";

export default function Admin() {
  const [activity, setActivity] = useState<AuditRow[]>([]);
  const [permissions, setPermissions] = useState<AuditRow[]>([]);
  const [chain, setChain] = useState<ChainResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const [a, p] = await Promise.all([
        api<AuditRow[]>("/api/admin/activity?limit=200"),
        api<AuditRow[]>("/api/admin/permissions?limit=200"),
      ]);
      setActivity(a);
      setPermissions(p);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  async function verify() {
    setError(null);
    try {
      setChain(await api<ChainResult>("/api/admin/audit/verify"));
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <Layout>
      <div className="page-head">
        <h1>Activity</h1>
        <button type="button" className="btn btn-quiet" onClick={() => void load()}>Refresh</button>
      </div>

      {error && <p className="error" role="alert">{error}</p>}

      <section className="panel">
        <h2>Audit integrity</h2>
        <p className="muted">Recomputes the whole hash chain and reports the first record that does not match.</p>
        <div className="row mt">
          <button type="button" className="btn" onClick={() => void verify()}>Verify chain</button>
          {chain && (
            <p className={chain.intact ? "notice" : "error"} role="status">
              {chain.intact
                ? `Chain intact across ${chain.rowsChecked} records.`
                : `Chain broken at record ${chain.firstBadId}. Records from that point cannot be trusted.`}
            </p>
          )}
        </div>
      </section>

      <section className="panel">
        <h2>Permission changes</h2>
        {loading ? <p className="muted">Loading</p> : <AuditTable rows={permissions} empty="No grants or revocations yet." />}
      </section>

      <section className="panel">
        <h2>Recent activity</h2>
        {loading ? <p className="muted">Loading</p> : <AuditTable rows={activity} empty="No activity yet." />}
      </section>
    </Layout>
  );
}

function AuditTable({ rows, empty }: { rows: AuditRow[]; empty: string }) {
  if (rows.length === 0) {
    return <p className="muted">{empty}</p>;
  }
  return (
    <div className="scroll">
      <table>
        <thead>
          <tr>
            <th>When</th>
            <th>Who</th>
            <th>Action</th>
            <th>Document</th>
            <th>Outcome</th>
            <th>Detail</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.id}>
              <td>{formatDate(r.created_at)}</td>
              <td>{r.actor ?? "—"}</td>
              <td>{r.action}</td>
              <td>{r.document_id ?? "—"}</td>
              <td className={r.outcome === "DENIED" ? "tag-denied" : "tag-allowed"}>{r.outcome}</td>
              <td>{r.detail ?? ""}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}