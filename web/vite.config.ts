import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  server: {
    host: "127.0.0.1",
    port: 5180,
    strictPort: true,
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8088",
        configure(proxy) {
          proxy.on("proxyReq", (proxyRequest, request) => {
            const origin = request.headers.origin;
            if (origin === "http://127.0.0.1:5180" || origin === "http://localhost:5180") {
              proxyRequest.setHeader("Origin", "http://127.0.0.1:8088");
            }
          });
        },
      },
    },
  },
  build: {
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            { name: "react", test: /node_modules[\\/](?:react|react-dom|scheduler)[\\/]/, priority: 10 },
            { name: "mantine", test: /node_modules[\\/]@mantine[\\/]/, priority: 5 },
          ],
        },
      },
    },
  },
  test: {
    globals: true,
    environment: "happy-dom",
    setupFiles: ["./src/test/setup.ts"],
    coverage: {
      provider: "v8",
      reporter: ["text", "html"],
      include: ["src/**/*.{ts,tsx}"],
      exclude: [
        "src/**/*.test.{ts,tsx}",
        "src/api/schema.ts",
        "src/test/**",
        "src/main.tsx",
        "src/vite-env.d.ts",
      ],
      // Full-source baseline after keyboard, accessibility and request-deadline regressions
      // (2026-11-07): statements 89.26, branches 82.15, functions 89.74, lines 91.23. Floors sit at the
      // integer below each result and must move upward as coverage grows.
      thresholds: {
        statements: 89,
        branches: 82,
        functions: 89,
        lines: 91,
      },
    },
  },
});
