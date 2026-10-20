import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import "./app.css";
import "./roles.css";
import RequireAuth from "./components/RequireAuth";
import { getSession } from "./auth/session";
import Admin from "./pages/Admin";
import AdminDocuments from "./pages/AdminDocuments";
import DocumentDetail from "./pages/DocumentDetail";
import Documents from "./pages/Documents";
import Login from "./pages/Login";

// Admins own no documents, so their home is the metadata page rather than an empty list.
function HomeRoute() {
  return getSession()?.role === "ADMIN" ? <Navigate to="/admin/documents" replace /> : <Documents />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <HomeRoute />
            </RequireAuth>
          }
        />
        <Route
          path="/documents/:id"
          element={
            <RequireAuth>
              <DocumentDetail />
            </RequireAuth>
          }
        />
        <Route
          path="/admin"
          element={
            <RequireAuth role="ADMIN">
              <Admin />
            </RequireAuth>
          }
        />
        <Route
          path="/admin/documents"
          element={
            <RequireAuth role="ADMIN">
              <AdminDocuments />
            </RequireAuth>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}