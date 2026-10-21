import { useEffect, useState } from "react";
import { errorMessage, getTrail, type Trail } from "../api/client";
import { formatDate } from "../lib/format";

const LABELS: Record<string, string> = {
  UPLOAD: "Uploaded",
  DOWNLOAD: "Download",
  GRANT: "Access granted",
  REVOKE: "Access removed",
  DELETE: "Deleted",
  VIEW_TRAIL: "Trail viewed",
  LOGIN: "Signed in",
};

export default function TrustTrail({ documentId }: { documentId: number }) {
  const [trail, setTrail] = useState<Trail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getTrail(documentId)
      .then(setTrail)
      .catch((err) => setError(errorMessage(err)));
  }, [documentId]);

  if (error) {
    return <p className="muted">The trail is not available to you.</p>;
  }
  if (!trail) {
    return <p className="muted">Loading the trail</p>;
  }

  return (
    <section className="panel trail">
      <h2>Trust Trail</h2>

      <p className={trail.chainIntact ? "notice" : "error"} role="status">
        {trail.chainIntact
          ? "The record for this document is intact."
          : "The record for this document has been altered. Entries from the point of change cannot be trusted."}
      </p>

      <ol className="trail-list">
        {trail.events.map((e) => (
          <li key={e.id} className={e.outcome === "DENIED" ? "trail-denied" : "trail-allowed"}>
            <span className="trail-when">{formatDate(e.created_at)}</span>
            <span className="trail-what">
              <strong>{LABELS[e.action] ?? e.action}</strong>
              {e.outcome === "DENIED" && <span className="tag-denied"> · refused</span>}
            </span>
            <span className="trail-who">{e.actor ?? "Unknown"}</span>
            {e.detail && <span className="trail-detail">{e.detail}</span>}
          </li>
        ))}
      </ol>
    </section>
  );
}