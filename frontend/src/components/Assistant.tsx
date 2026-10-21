import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { askAssistant, errorMessage } from "../api/client";

type Turn = { from: "you" | "assistant"; text: string; links?: string[] };

const LINK_LABELS: Record<string, string> = {
  "/forgot-password": "Forgot password",
  "/signup": "Create account",
  "/shared": "Shared with me",
  "/help": "Help",
  "/contact": "Contact",
  "/security": "Security",
  "/": "Documents",
};

const OPENED_KEY = "tv-assistant-opened";
const GREETING =
  "Hi, I'm the TrustVault Assistant. Ask me how to share, upload, reset a password, or stay secure.";

export default function Assistant() {
  const [open, setOpen] = useState(false);
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);
  const [turns, setTurns] = useState<Turn[]>([{ from: "assistant", text: GREETING }]);
  const logRef = useRef<HTMLDivElement>(null);

  // Opens by itself once per browser session, after a short pause.
  useEffect(() => {
    if (sessionStorage.getItem(OPENED_KEY)) return;
    const timer = setTimeout(() => {
      setOpen(true);
      sessionStorage.setItem(OPENED_KEY, "1");
    }, 1500);
    return () => clearTimeout(timer);
  }, []);

  // Keep the newest message in view.
  useEffect(() => {
    if (logRef.current) logRef.current.scrollTop = logRef.current.scrollHeight;
  }, [turns, open]);

  async function onSend(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const question = text.trim();
    if (!question) return;
    setTurns((t) => [...t, { from: "you", text: question }]);
    setText("");
    setBusy(true);
    try {
      const r = await askAssistant(question);
      setTurns((t) => [...t, { from: "assistant", text: r.answer, links: r.links }]);
    } catch (err) {
      setTurns((t) => [...t, { from: "assistant", text: errorMessage(err) }]);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="assistant">
      {open && (
        <div className="assistant-panel" role="dialog" aria-label="TrustVault Assistant">
          <div className="assistant-head">
            <span>TrustVault Assistant</span>
            <button type="button" className="link-button" onClick={() => setOpen(false)}>Close</button>
          </div>

          <div className="assistant-log" ref={logRef} aria-live="polite">
            {turns.map((t, i) => (
              <div key={i} className={`bubble ${t.from}`}>
                <p>{t.text}</p>
                {t.links && t.links.length > 0 && (
                  <div className="bubble-links">
                    {t.links.map((href) => (
                      <Link key={href} to={href} className="link">{LINK_LABELS[href] ?? href}</Link>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>

          <form className="assistant-form" onSubmit={onSend}>
            <input
              className="input"
              placeholder="Ask about sharing, uploads, passwords…"
              aria-label="Message the assistant"
              value={text}
              onChange={(e) => setText(e.target.value)}
              maxLength={500}
              disabled={busy}
            />
            <button type="submit" className="btn" disabled={busy}>Send</button>
          </form>
          <p className="assistant-note">
            The assistant never asks for passwords or tokens, and it cannot open documents for you.
          </p>
        </div>
      )}

      <button
        type="button"
        className="assistant-fab"
        aria-expanded={open}
        aria-label={open ? "Close the assistant" : "Open the TrustVault Assistant"}
        onClick={() => setOpen((o) => !o)}
      >
        {open ? "×" : "Help"}
      </button>
    </div>
  );
}