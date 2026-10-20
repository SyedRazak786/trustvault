import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Forward API calls to the backend, so the browser sees one origin and no CORS setup is needed.
    proxy: { "/api": "http://localhost:8080" },
  },
});