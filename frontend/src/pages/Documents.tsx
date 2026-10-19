import { useNavigate } from "react-router-dom";
import { setSession } from "../auth/session";

export default function Documents() {
  const navigate = useNavigate();

  function signOut() {
    setSession(null);
    navigate("/login", { replace: true });
  }

  return (
    <main className="page">
      <h1>Documents</h1>
      <p className="lead">No documents yet.</p>
      <button type="button" onClick={signOut}>Sign out</button>
    </main>
  );
}