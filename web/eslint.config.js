import js from "@eslint/js";
import globals from "globals";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import sonarjs from "eslint-plugin-sonarjs";
import tseslint from "typescript-eslint";
import { defineConfig, globalIgnores } from "eslint/config";

export default defineConfig([
  // schema.ts is generated (npm run gen:api) — type-checked by tsc, not style-linted.
  globalIgnores(["dist", "coverage", "src/api/schema.ts"]),
  {
    files: ["**/*.{ts,tsx}"],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
      sonarjs.configs.recommended,
    ],
    languageOptions: { globals: globals.browser },
    rules: {
      // Allow intentionally-unused bindings prefixed with `_` (e.g. destructuring a
      // field out of an object to omit it from a payload).
      "@typescript-eslint/no-unused-vars": [
        "error",
        { argsIgnorePattern: "^_", varsIgnorePattern: "^_", caughtErrorsIgnorePattern: "^_" },
      ],

      // ── Quality gates (the frontend counterpart of config/detekt/detekt.yml) ─────────
      // Policy: fix findings in code first; tune a rule here ONLY for a deliberate repo
      // idiom, with a comment naming it. No inline eslint-disable without a comment.

      // Flat selection and fallback chains (`a ? x : b ? y : z`) are idiomatic here: error-detail
      // mapping in api/client.ts, status wording in pages/practice and the inspector panel, and the
      // commit-stamp fallback in vite.config.ts read best top to bottom.
      "sonarjs/no-nested-conditional": "off",

      // Core size/complexity backstops — generous by design (page components are single
      // functions); they only catch future monsters. Today's max (re-measured 2026-11-08, after the
      // pages/ split): cyclomatic complexity 35 (Playground), 252 lines (Playground; Practice 248),
      // 5 parameters.
      complexity: ["error", 40],
      "max-lines-per-function": ["error", { "max": 260, "skipBlankLines": true, "skipComments": true }],
      "max-params": ["error", 6],
    },
  },
]);
