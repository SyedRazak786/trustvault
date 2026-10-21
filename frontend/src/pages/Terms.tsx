import Layout from "../components/Layout";
import BackHome from "../components/BackHome";
export default function Terms() {
  return (
    <Layout>
      <div className="prose">
        <h1>Terms of use</h1>
        <p className="draft">
          Draft for review. These terms must be reviewed before real customers use the service.
        </p>

        <section className="panel">
          <h2>Your account</h2>
          <p>
            You are responsible for keeping your password private and for everything done with your account. Tell us
            at once if you believe someone else has used it.
          </p>
        </section>

        <section className="panel">
          <h2>What you upload</h2>
          <p>
            You are responsible for the files you upload and for the people you share them with. Do not upload content
            you have no right to share, or content that is unlawful.
          </p>
        </section>

        <section className="panel">
          <h2>Acceptable use</h2>
          <p>
            Do not attempt to reach documents you have not been granted, probe the service for weaknesses without
            permission, or use it to send unsolicited messages. Report any weakness you find to the contact address
            below.
          </p>
        </section>

        <section className="panel">
          <h2>Availability</h2>
          <p>
            TrustVault is provided as it is, for demonstration and evaluation. We may change or pause features, and we
            will say so on this page when we do.
          </p>
        </section>

        <section className="panel">
          <h2>Contact</h2>
          <p>
            Questions about these terms: <a className="link" href="mailto:syedrazak337@gmail.com">syedrazak337@gmail.com</a>.
          </p>
        </section>
      </div>
      <BackHome />
    </Layout>
  );
}