import { useEffect, useState, type ReactNode } from "react";
import Layout from "../components/Layout";
import { Link } from "react-router-dom";

// Your contact and profile links.
const GMAIL = "mailto:syedrazak337@gmail.com";
const WHATSAPP = "https://wa.me/917842233786";
const LINKEDIN = "https://www.linkedin.com/in/syed-razak-43aa0a241/";
const GITHUB = "https://github.com/SyedRazak786";
const PORTFOLIO = "https://syedrazakportfolio.netlify.app/";
const MAP_URL =
  "https://www.openstreetmap.org/export/embed.html?bbox=80.4065%2C16.2867%2C80.4665%2C16.3267&layer=mapnik&marker=16.3067%2C80.4365";

const SLIDES = [
  "Share documents with the people they're meant for, and prove who opened them.",
  "Access ends the moment you remove it.",
  "Every action leaves a record you can check.",
];

const SECTIONS = [
  { id: "how-it-works", label: "How it works" },
  { id: "why", label: "Why it's different" },
  { id: "about", label: "About" },
  { id: "security", label: "Security" },
  { id: "contact", label: "Contact" },
  { id: "help", label: "Help" },
  { id: "legal", label: "Privacy and terms" },
];

/* ---------- Icons (drawn in code, no image files) ---------- */

function Icon({ children }: { children: ReactNode }) {
  return (
    <svg className="icon" viewBox="0 0 48 48" fill="none" stroke="currentColor" strokeWidth="2"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {children}
    </svg>
  );
}

const IconAccount = () => (
  <Icon><circle cx="24" cy="18" r="7" /><path d="M10 40c2-7 7-10 14-10s12 3 14 10" /></Icon>
);
const IconUpload = () => (
  <Icon><path d="M24 32V12M16 20l8-8 8 8" /><path d="M12 36v4h24v-4" /></Icon>
);
const IconShare = () => (
  <Icon>
    <circle cx="14" cy="24" r="4" /><circle cx="34" cy="14" r="4" /><circle cx="34" cy="34" r="4" />
    <path d="M18 22l12-6M18 26l12 6" />
  </Icon>
);
const IconRemove = () => (
  <Icon><circle cx="24" cy="24" r="14" /><path d="M17 31L31 17" /></Icon>
);
const IconRecord = () => (
  <Icon><rect x="12" y="10" width="24" height="28" rx="2" /><path d="M18 18h12M18 24h12M18 30h8" /></Icon>
);
const IconShield = () => (
  <Icon><path d="M24 6l14 5v11c0 9-6 16-14 20-8-4-14-11-14-20V11z" /><path d="M18 24l4 4 8-8" /></Icon>
);
const IconLock = () => (
  <Icon><rect x="12" y="22" width="24" height="18" rx="2" /><path d="M17 22v-6a7 7 0 0 1 14 0v6" /></Icon>
);
const IconFile = () => (
  <Icon><path d="M14 6h14l8 8v28H14z" /><path d="M28 6v8h8" /></Icon>
);
const IconFolder = () => (
  <Icon><path d="M6 14h12l4 4h20v20H6z" /></Icon>
);
const IconEye = () => (
  <Icon><path d="M6 24s7-10 18-10 18 10 18 10-7 10-18 10S6 24 6 24z" /><circle cx="24" cy="24" r="4" /></Icon>
);
const IconBoard = () => (
  <Icon><rect x="10" y="8" width="28" height="32" rx="2" /><path d="M18 8V5h12v3M18 26l4 4 8-8" /></Icon>
);
const IconPin = () => (
  <Icon><path d="M24 42s12-12 12-22a12 12 0 0 0-24 0c0 10 12 22 12 22z" /><circle cx="24" cy="20" r="4" /></Icon>
);
const IconMail = () => (
  <Icon><rect x="6" y="12" width="36" height="24" rx="2" /><path d="M6 14l18 12 18-12" /></Icon>
);
const IconHelp = () => (
  <Icon><circle cx="24" cy="24" r="16" /><path d="M19 19a5 5 0 1 1 7 4.6c-1.3.6-2 1.5-2 2.9V28" /><path d="M24 35v.01" /></Icon>
);
const IconLegal = () => (
  <Icon><rect x="12" y="8" width="24" height="32" rx="2" /><path d="M18 16h12M18 22h12M18 28h8" /></Icon>
);
const IconInfo = () => (
  <Icon><circle cx="24" cy="24" r="16" /><path d="M24 22v10M24 16v.01" /></Icon>
);

/* ---------- Content ---------- */

const HOW_STEPS = [
  {
    icon: <IconAccount />,
    title: "Create an account",
    text: "Sign up with your email and a password of at least 12 characters. New accounts are owners, so you can upload, share and receive documents. You can add your name and phone number under Profile.",
  },
  {
    icon: <IconUpload />,
    title: "Upload a document",
    text: "Upload a PDF, PNG, JPEG or plain text file up to 10 MB. The system checks the file's content, not its name, so a file cannot be disguised as something it is not.",
  },
  {
    icon: <IconShare />,
    title: "Share it with a named person",
    text: "Enter the recipient's email and choose Read or Edit. Only that person can see the document. They find it under Shared with me and can download it.",
  },
  {
    icon: <IconRemove />,
    title: "Remove access when it's no longer needed",
    text: "Click Remove next to a person's name. Their next request is refused straight away, even with the same sign-in. Owners can also delete a document.",
  },
  {
    icon: <IconRecord />,
    title: "Every action is recorded",
    text: "Uploads, downloads, sharing, removals, deletions, sign-ins and refused attempts are logged with who did it, when, and the outcome. Administrators can verify that the log has not been altered.",
  },
];

const WHY = [
  { title: "Checked on every request", text: "The server decides who may open a document each time. Hiding a button is never the protection." },
  { title: "Access ends immediately", text: "When an owner removes someone's access, that person's next request is refused, even with the same sign-in." },
  { title: "A record that can be checked", text: "Every action is logged. Administrators can verify the log, and any edit that bypasses the protections is detected." },
];

const ROLES = [
  { icon: <IconFolder />, title: "Owners", text: "Upload documents, share them as Read or Edit, remove access, and delete their own documents." },
  { icon: <IconEye />, title: "Viewers", text: "See and download only the documents that have been shared with them." },
  { icon: <IconBoard />, title: "Administrators", text: "Review activity, permission changes and the audit chain, and see document details. They cannot read document contents." },
];

const SECURITY = [
  { icon: <IconShield />, title: "Who can open a document", text: "Every request is checked against the database at the moment it is made. Access that is removed stops working at once." },
  { icon: <IconLock />, title: "Passwords", text: "Stored only as BCrypt hashes, which are deliberately slow to crack. Nobody, including administrators, can read a stored password." },
  { icon: <IconFile />, title: "Files", text: "Stored under random names, never in a web-accessible folder. The file type comes from the content, so an executable cannot pass as a PDF." },
  { icon: <IconRecord />, title: "The activity record", text: "The database refuses to change or delete records, and each record is linked to the one before it. Any edit is detected and located." },
];

const FAQ = [
  { q: "How do I share a document?", a: "Open the document, then under Who has access enter the person's email, choose Read or Edit, and click Share." },
  { q: "Why can't I see a document someone sent me?", a: "You can only see documents shared with your account. Check the email the owner used, and make sure you're signed in to that account." },
  { q: "What file types can I upload?", a: "PDF, PNG, JPEG and plain text files up to 10 MB. The system checks the file's content, not its name." },
  { q: "How do I remove someone's access?", a: "Open the document you own, find the person under Who has access, and click Remove. Their access stops immediately." },
  { q: "I forgot my password. What do I do?", a: "Email support with the address on your account. Password reset by email is not available yet." },
  { q: "Who can see the activity log?", a: "Only administrators can see activity and permission changes. They cannot open document contents." },
];

const LINKS = [
  {
    label: "Gmail", href: GMAIL,
    icon: (<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 7l9 6 9-6" /></svg>),
  },
  {
    label: "WhatsApp", href: WHATSAPP,
    icon: (<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><path d="M3 21l1.4-4.2A9 9 0 1 1 7.2 19.6L3 21z" /><path d="M9 10c0 3 2 5 5 5l1-1.5-1.8-.8-.8.8c-.9-.4-1.8-1.3-2.2-2.2l.8-.8L10.5 9 9 10z" /></svg>),
  },
  {
    label: "LinkedIn", href: LINKEDIN,
    icon: (<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="2" /><path d="M7.5 10v7M7.5 7.5v.01M11 17v-7M11 13c0-2 1.5-3 3-3s2.5 1 2.5 3v4" /></svg>),
  },
  {
    label: "GitHub", href: GITHUB,
    icon: (<svg viewBox="0 0 16 16" fill="currentColor" aria-hidden="true"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z" /></svg>),
  },
  {
    label: "Portfolio", href: PORTFOLIO,
    icon: (<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3c2.5 3 2.5 15 0 18M12 3c-2.5 3-2.5 15 0 18" /></svg>),
  },
];

/* ---------- Page ---------- */

export default function Welcome() {
  const [index, setIndex] = useState(0);

  // Move to the next headline every 4.5 seconds.
  useEffect(() => {
    const timer = setInterval(() => setIndex((i) => (i + 1) % SLIDES.length), 4500);
    return () => clearInterval(timer);
  }, []);

  return (
    <Layout>
      {/* Hero */}
      <section className="hero-grid">
        <div className="hero-text">
          {/* key changes each slide, which replays the fade-in animation */}
          <h1 key={index} className="slider-title">{SLIDES[index]}</h1>

          <div className="dots" role="group" aria-label="Headlines">
            {SLIDES.map((_, i) => (
              <button
                key={i}
                type="button"
                aria-label={`Show headline ${i + 1}`}
                aria-current={i === index}
                onClick={() => setIndex(i)}
              />
            ))}
          </div>

          <p className="lead-text">
            TrustVault is a secure document platform. A file can only be reached by someone it was shared with,
            and every upload, download, change and refused attempt is recorded.
          </p>

          <div className="cta-row">
            <Link to="/signup" className="btn">Create an account</Link>
            <Link to="/login" className="btn btn-quiet">Sign in</Link>
          </div>
        </div>

        <div className="hero-art">
          <img src="/logo.svg" alt="TrustVault logo" width={220} height={220} />
        </div>
      </section>

      {/* On-page index */}
      <nav className="section-index" aria-label="On this page">
        {SECTIONS.map((s) => (
          <a key={s.id} href={`#${s.id}`}>{s.label}</a>
        ))}
      </nav>

      {/* 1. How it works */}
      <section id="how-it-works" className="site-section">
        <header className="section-head">
          <span className="section-icon"><IconUpload /></span>
          <div>
            <h2>How it works</h2>
            <p className="section-intro">
              Five steps, from creating an account to removing access. Each one is enforced by the server.
            </p>
          </div>
        </header>

        <ol className="timeline">
          {HOW_STEPS.map((step, i) => (
            <li key={step.title}>
              <span className="timeline-num">{i + 1}</span>
              <span className="timeline-icon">{step.icon}</span>
              <div>
                <h3>{step.title}</h3>
                <p>{step.text}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      {/* 2. Why it's different */}
      <section id="why" className="site-section">
        <header className="section-head">
          <span className="section-icon"><IconShield /></span>
          <div>
            <h2>Why it's different</h2>
            <p className="section-intro">Three properties that most document tools do not guarantee.</p>
          </div>
        </header>

        <div className="card-grid">
          {WHY.map((w) => (
            <div key={w.title} className="card">
              <h3>{w.title}</h3>
              <p>{w.text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* 3. About */}
      <section id="about" className="site-section">
        <header className="section-head">
          <span className="section-icon"><IconInfo /></span>
          <div>
            <h2>About TrustVault</h2>
            <p className="section-intro">
              A platform where a file is reachable only by the people it was shared with, and where every
              decision is made on the server, for every request.
            </p>
          </div>
        </header>

        <div className="card-grid">
          {ROLES.map((r) => (
            <div key={r.title} className="card">
              <span className="card-icon">{r.icon}</span>
              <h3>{r.title}</h3>
              <p>{r.text}</p>
            </div>
          ))}
        </div>
      </section>

      {/* 4. Security */}
      <section id="security" className="site-section">
        <header className="section-head">
          <span className="section-icon"><IconShield /></span>
          <div>
            <h2>Security</h2>
            <p className="section-intro">The protections we have tested, explained in plain language.</p>
          </div>
        </header>

        <div className="card-grid">
          {SECURITY.map((s) => (
            <div key={s.title} className="card">
              <span className="card-icon">{s.icon}</span>
              <h3>{s.title}</h3>
              <p>{s.text}</p>
            </div>
          ))}
        </div>

        <p className="section-foot">
          Not yet in place: login rate limiting, email verification at sign-up, and content editing for Edit access.
          The full list is in the project's threat model.
        </p>
      </section>

      {/* 5. Contact */}
      <section id="contact" className="site-section">
        <header className="section-head">
          <span className="section-icon"><IconPin /></span>
          <div>
            <h2>Contact</h2>
            <p className="section-intro">Get in touch, or find us on the map.</p>
          </div>
        </header>

        <ul className="contact-list">
          <li><IconMail /><a href="mailto:syedrazak337@gmail.com">syedrazak337@gmail.com</a></li>
          <li><IconPin /><span>Anandapet, 7th Line, Guntur, Andhra Pradesh, India</span></li>
        </ul>

        <iframe
          className="map-frame"
          title="Map of Guntur, Andhra Pradesh"
          src={MAP_URL}
          loading="lazy"
          referrerPolicy="no-referrer"
        />
        <p className="section-foot">
          The map is provided by OpenStreetMap. Loading it sends your IP address to OpenStreetMap.
        </p>
      </section>

      {/* 6. Help */}
      <section id="help" className="site-section">
        <header className="section-head">
          <span className="section-icon"><IconHelp /></span>
          <div>
            <h2>Help and support</h2>
            <p className="section-intro">Answers to the questions we are asked most often.</p>
          </div>
        </header>

        <div className="faq-list">
          {FAQ.map((item) => (
            <details key={item.q} className="faq">
              <summary>{item.q}</summary>
              <p>{item.a}</p>
            </details>
          ))}
        </div>

        <p className="section-foot">
          Still stuck? Email{" "}
          <a className="link" href="mailto:syedrazak337@gmail.com?subject=TrustVault%20support">
            syedrazak337@gmail.com
          </a>{" "}
          with your account email and a short description. Please don't include your password.
        </p>
      </section>

      {/* 7. Privacy and terms */}
      <section id="legal" className="site-section">
        <header className="section-head">
          <span className="section-icon"><IconLegal /></span>
          <div>
            <h2>Privacy and terms</h2>
            <p className="section-intro">A summary. The full pages are linked below each summary.</p>
          </div>
        </header>

        <div className="legal-grid">
          <div className="card">
            <span className="card-icon"><IconLegal /></span>
            <h3>Privacy</h3>
            <p>
              We store your email, optional name and phone, a hash of your password, your documents, and an activity
              record. We do not use advertising or analytics trackers, and we do not sell personal information.
            </p>
            <Link to="/privacy" className="link">Read the full privacy page</Link>
          </div>
          <div className="card">
            <span className="card-icon"><IconLegal /></span>
            <h3>Terms of use</h3>
            <p>
              Keep your password private and take responsibility for what you upload and share. Do not try to reach
              documents you have not been granted.
            </p>
            <Link to="/terms" className="link">Read the full terms</Link>
          </div>
        </div>
        <p className="section-foot">These pages are drafts and must be reviewed before real customers use the service.</p>
      </section>

      {/* Connect */}
      <section id="connect" className="site-section">
        <header className="section-head">
          <span className="section-icon"><IconAccount /></span>
          <div>
            <h2>Connect</h2>
            <p className="section-intro">Find me on these channels.</p>
          </div>
        </header>

        <div className="social">
          {LINKS.map((link) => (
            <a
              key={link.label}
              href={link.href}
              aria-label={link.label}
              target={link.href.startsWith("mailto:") ? undefined : "_blank"}
              rel={link.href.startsWith("mailto:") ? undefined : "noopener noreferrer"}
            >
              {link.icon}
              <span>{link.label}</span>
            </a>
          ))}
        </div>
      </section>
    </Layout>
  );
}