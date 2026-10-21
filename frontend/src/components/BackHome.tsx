import { Link } from "react-router-dom";

export default function BackHome() {
  return (
    <Link to="/welcome" className="back-home">← Back to home</Link>
  );
}