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
import Login from "./pages/Login";
import Profile from "./pages/Profile";
import SharedWithMe from "./pages/SharedWithMe";
import Help from "./pages/Help";
import SignUp from "./pages/SignUp";

// Admins own no documents, so their home is the metadata page rather than an empty list.
function HomeRoute() {
  return getSession()?.role === "ADMIN" ? <Navigate to="/admin/documents" replace /> : <Documents />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<SignUp />} />
        <Route path="/about" element={<About />} />
        <Route path="/contact" element={<Contact />} />
        <Route path="/" element={<RequireAuth><HomeRoute /></RequireAuth>} />
        <Route path="/shared" element={<RequireAuth><SharedWithMe /></RequireAuth>} />
        <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />
        <Route path="/documents/:id" element={<RequireAuth><DocumentDetail /></RequireAuth>} />
        <Route path="/admin" element={<RequireAuth role="ADMIN"><Admin /></RequireAuth>} />
        <Route path="/admin/documents" element={<RequireAuth role="ADMIN"><AdminDocuments /></RequireAuth>} />
        <Route path="*" element={<Navigate to="/" replace />} />
       <Route path="/help" element={<Help />} />
      </Routes>
    </BrowserRouter>
  );
}