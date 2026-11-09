import react from "@vitejs/plugin-react";
import { execFileSync } from "node:child_process";
import { defineConfig } from "vitest/config";

function git(...args: string[]) {
  try {
    return execFileSync("/usr/bin/git", args, {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"],
    }).trim();
  } catch {
    return "";
  }
}

const localCommit = git("rev-parse", "--short", "HEAD");
const commit = process.env.GIT_SHA
  || (localCommit ? `${localCommit}${git("status", "--porcelain") ? "+dirty" : ""}` : "unknown");
const commitTime = process.env.GIT_COMMIT_TIME || git("log", "-1", "--format=%cI");

export default defineConfig({
  plugins: [react()],
  define: {
    __APP_COMMIT__: JSON.stringify(commit),
    __APP_COMMIT_TIME__: JSON.stringify(commitTime),
  },
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
    modulePreload: {
      resolveDependencies(filename, deps, { hostType }) {
        // WebKit can retain a failed modulepreload across reloads (bugs.webkit.org/show_bug.cgi?id=270357).
        // Let the lazy import fetch this entry itself; retain CSS, shared dependencies and HTML preloads.
        return hostType === "js" && /(?:^|\/)Changelog-[^/]+\.js$/.test(filename)
          ? deps.filter((dependency) => dependency !== filename)
          : deps;
      },
    },
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
      // Full-source baseline after guided multiple-gap practice
      // (2026-11-09): statements 95.06, branches 89.11, functions 97.73, lines 97.73. Floors sit at the
      // integer below each result and must move upward as coverage grows.
      thresholds: {
        statements: 95,
        branches: 89,
        functions: 97,
        lines: 97,
      },
    },
  },
});
