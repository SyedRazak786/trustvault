import { Link } from "react-router-dom";
import Layout from "../components/Layout";

export default function HowItWorks() {
  return (
    <Layout>
      <div className="prose">
        <h1>How it works</h1>
        <p>TrustVault has three roles. Each one can do only what its role and its grants allow.</p>

        <section className="panel">
          <h2>1. Create an account</h2>
          <p>
            Sign up with your email and a password of at least 12 characters. New accounts are owners, so you can
            upload, share and receive documents. You can edit your name and phone number under Profile.
          </p>
        </section>

        <section className="panel">
          <h2>2. Upload a document</h2>
          <p>
            Owners upload a PDF, PNG, JPEG or plain text file up to 10 MB. The system checks the file's content, not its
            name, so a file cannot be disguised as something it is not.
          </p>
        </section>

        <section className="panel">
          <h2>3. Share it with a named person</h2>
          <p>
            Enter the recipient's email and choose Read or Edit. Only that person can see the document. The recipient
            finds it under Shared with me and can download it.
          </p>
        </section>

        <section className="panel">
          <h2>4. Remove access when it is no longer needed</h2>
          <p>
            Click Remove next to a person's name. Their next request is refused straight away. Owners can also delete
            a document, which removes it from every list and download.
          </p>
        </section>

        <section className="panel">
          <h2>5. Every action is recorded</h2>
          <p>
            Uploads, downloads, sharing, removals, deletions, sign-ins and refused attempts are all logged with who did
            it, when and the outcome. Administrators review this log and can verify that it has not been altered.
          </p>
        </section>

        <p>
          <Link to="/signup" className="link">Create an account</Link> or <Link to="/security" className="link">read how security works</Link>.
        </p>
      </div>
    </Layout>
  );
}