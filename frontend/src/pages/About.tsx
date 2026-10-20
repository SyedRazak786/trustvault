import Layout from "../components/Layout";

export default function About() {
  return (
    <Layout>
      <h1>About TrustVault</h1>
      <section className="panel">
        <p>
          TrustVault is a document platform where a file is reachable only by the people it was shared with,
          and where every upload, download, change, deletion and refused attempt is recorded.
        </p>
        <p>
          Every decision is made on the server, for every request. Access can be removed at once, and
          the audit trail is protected both by database rules and by a hash chain that detects tampering.
        </p>
      </section>
      <section className="panel">
        <h2>Who does what</h2>
        <p><strong>Owners</strong> upload documents, share them as read-only or editable, remove access, and delete their own documents.</p>
        <p><strong>Viewers</strong> see and download only what has been shared with them.</p>
        <p><strong>Administrators</strong> review activity, permission changes and the audit chain, and see document details. They cannot read document contents.</p>
      </section>
    </Layout>
  );
}