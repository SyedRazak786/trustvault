import Layout from "../components/Layout";

export default function Security() {
  return (
    <Layout>
      <div className="prose">
        <h1>Security</h1>
        <p>
          TrustVault is built so that protection does not depend on the screen you see. The server checks every
          request, and the checks below are the ones we have tested.
        </p>

        <section className="panel">
          <h2>Who can open a document</h2>
          <p>
            Every request is checked against the database at the moment it is made. A person can open a document only
            if they own it or hold a grant for it. If access is removed, the same sign-in stops working immediately.
            A document that a person cannot open returns the same "not available" response as one that does not exist,
            so the system does not reveal which documents exist.
          </p>
        </section>

        <section className="panel">
          <h2>Passwords</h2>
          <p>
            Passwords are stored only as BCrypt hashes, which are deliberately slow to crack. Nobody, including the
            administrators, can read a stored password.
          </p>
        </section>

        <section className="panel">
          <h2>Files</h2>
          <p>
            Files are stored under random names, not the names they were uploaded with, and never in a web-accessible
            folder. The file type is taken from the content, so an executable cannot be uploaded as a PDF.
          </p>
        </section>

        <section className="panel">
          <h2>The activity record</h2>
          <p>
            The database itself refuses to change or delete activity records, and each record is linked to the one
            before it. If someone bypasses the database rules and edits a record, the administrator's verification
            reports the exact record where the change begins.
          </p>
        </section>

        <section className="panel">
          <h2>What is not yet in place</h2>
          <p>
            We state these openly. Login attempts are not yet rate-limited, so repeated guessing is possible though it
            is fully logged. Sign-up does not yet verify email addresses. Edit access does not yet change file contents.
            Someone with full database administrator access could rewrite the whole activity record, which is why
            publishing the latest record hash to an independent store is planned.
          </p>
        </section>
      </div>
    </Layout>
  );
}