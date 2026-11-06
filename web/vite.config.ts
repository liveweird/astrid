import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  server: {
    host: "127.0.0.1",
    port: 5180,
    strictPort: true,
    proxy: { "/api": "http://127.0.0.1:8088" },
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
      // Full-source baseline after prompt, navigation, recovery and theme regressions (2026-11-07): statements
      // 86.12, branches 79.18, functions 82.08, lines 89.62. Floors sit at the
      // integer below each result and must move upward as coverage grows.
      thresholds: {
        statements: 86,
        branches: 79,
        functions: 82,
        lines: 89,
      },
    },
  },
});
