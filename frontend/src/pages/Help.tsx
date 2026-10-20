import Layout from "../components/Layout";

const FAQ = [
  {
    q: "How do I share a document?",
    a: "Open the document, then under Who has access enter the person's email, choose Read or Edit, and click Share.",
  },
  {
    q: "Why can't I see a document someone sent me?",
    a: "You can only see documents that have been shared with your account. Check the email address the owner used, and make sure you're signed in to that account.",
  },
  {
    q: "What file types can I upload?",
    a: "PDF, PNG, JPEG and plain text files up to 10 MB. The system checks the file's content, not its name.",
  },
  {
    q: "How do I remove someone's access?",
    a: "Open the document you own, find the person under Who has access, and click Remove. Their access stops immediately.",
  },
  {
    q: "I forgot my password. What do I do?",
    a: "Contact support with the email address on your account. Password reset by email is not available yet.",
  },
  {
    q: "Who can see the activity log?",
    a: "Only administrators can see activity and permission changes. Administrators cannot open document contents.",
  },
];

export default function Help() {
  return (
    <Layout>
      <h1>Help and support</h1>

      <section className="panel">
        <h2>Common questions</h2>
        {FAQ.map((item) => (
          <details key={item.q} className="faq">
            <summary>{item.q}</summary>
            <p>{item.a}</p>
          </details>
        ))}
      </section>

      <section className="panel">
        <h2>Contact support</h2>
        <p>
          Email{" "}
          <a className="link" href="mailto:syedrazak337@gmail.com?subject=TrustVault%20support">
            syedrazak337@gmail.com
          </a>{" "}
          with your account email and a short description of the problem. Please don't include your password.
        </p>
        <p className="muted">Support replies are sent within two working days.</p>
      </section>
    </Layout>
  );
}