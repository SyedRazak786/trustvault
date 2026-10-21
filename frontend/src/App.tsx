import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import "./app.css";
import "./roles.css";
import RequireAuth from "./components/RequireAuth";
import { getSession } from "./auth/session";
import About from "./pages/About";
import Admin from "./pages/Admin";
import AdminDocuments from "./pages/AdminDocuments";
import Contact from "./pages/Contact";
import DocumentDetail from "./pages/DocumentDetail";
import Documents from "./pages/Documents";
import Help from "./pages/Help";
import HowItWorks from "./pages/HowItWorks";
import Login from "./pages/Login";
import Privacy from "./pages/Privacy";
import Profile from "./pages/Profile";
import Security from "./pages/Security";
import SharedWithMe from "./pages/SharedWithMe";
import SignUp from "./pages/SignUp";
import Terms from "./pages/Terms";
import Welcome from "./pages/Welcome";

// Visitors see the welcome page. Signed-in users go to their documents; admins go to the metadata list.
function HomeRoute() {
  const session = getSession();
  if (!session) return <Navigate to="/welcome" replace />;
  return session.role === "ADMIN" ? <Navigate to="/admin/documents" replace /> : <Documents />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public pages: no sign-in needed */}
        <Route path="/welcome" element={<Welcome />} />
        <Route path="/how-it-works" element={<HowItWorks />} />
        <Route path="/security" element={<Security />} />
        <Route path="/about" element={<About />} />
        <Route path="/contact" element={<Contact />} />
        <Route path="/help" element={<Help />} />
        <Route path="/privacy" element={<Privacy />} />
        <Route path="/terms" element={<Terms />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<SignUp />} />

        {/* Home decides between the welcome page and the signed-in home */}
        <Route path="/" element={<HomeRoute />} />

        {/* Signed-in pages */}
        <Route path="/shared" element={<RequireAuth><SharedWithMe /></RequireAuth>} />
        <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />
        <Route path="/documents/:id" element={<RequireAuth><DocumentDetail /></RequireAuth>} />

        {/* Admin pages */}
        <Route path="/admin" element={<RequireAuth role="ADMIN"><Admin /></RequireAuth>} />
        <Route path="/admin/documents" element={<RequireAuth role="ADMIN"><AdminDocuments /></RequireAuth>} />

        <Route path="*" element={<Navigate to="/welcome" replace />} />
      </Routes>
    </BrowserRouter>
  );
}