import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    chunkSizeWarningLimit: 2000,
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    proxy: {
      "/xrpc": {
        target: "https://projectfalcon-production.up.railway.app",
        changeOrigin: true,
      },
      "/ws": {
        target: "https://projectfalcon-production.up.railway.app",
        ws: true,
        changeOrigin: true,
      },
      "/api": {
        target: "https://projectfalcon-production.up.railway.app",
        changeOrigin: true,
      },
    },
  },
});
