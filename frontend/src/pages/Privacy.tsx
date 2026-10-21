import Layout from "../components/Layout";

export default function Privacy() {
  return (
    <Layout>
      <div className="prose">
        <h1>Privacy</h1>
        <p className="draft">
          Draft for review. This page describes the product as built. It must be reviewed before real customers
          use the service.
        </p>

        <section className="panel">
          <h2>What we store</h2>
          <ul>
            <li>Your email address, used to sign in and to receive grants.</li>
            <li>Your name and phone number, if you add them. Both are optional.</li>
            <li>A hash of your password. We cannot read the password itself.</li>
            <li>The documents you upload, and their name, category, size and date.</li>
            <li>An activity record of sign-ins, uploads, downloads, sharing, removals, deletions and refused attempts.</li>
          </ul>
          <p>The activity record stores the account that acted, not the IP address or device of the person.</p>
        </section>

        <section className="panel">
          <h2>What we do not do</h2>
          <p>TrustVault does not use advertising or analytics trackers, and does not sell personal information.</p>
        </section>

        <section className="panel">
          <h2>Third-party map</h2>
          <p>
            The Contact page loads its map from OpenStreetMap. Loading it sends your IP address to OpenStreetMap, as
            any map on a website does. No account details or documents are sent.
          </p>
        </section>

        <section className="panel">
          <h2>Retention and deletion</h2>
          <p>
            Deleted documents are hidden from every list and download, but their files are not yet removed from
            storage. A retention period and purge process are planned and will be stated here when they are built.
          </p>
        </section>

        <section className="panel">
          <h2>Your requests</h2>
          <p>
            To ask what we hold about you, or to request a change or removal, email{" "}
            <a className="link" href="mailto:syedrazak337@gmail.com">syedrazak337@gmail.com</a> from the address on
            your account.
          </p>
        </section>
      </div>
    </Layout>
  );
}