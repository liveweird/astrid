import js from "@eslint/js";
import globals from "globals";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import sonarjs from "eslint-plugin-sonarjs";
import tseslint from "typescript-eslint";
import { defineConfig, globalIgnores } from "eslint/config";

export default defineConfig([
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
      "sonarjs/no-nested-conditional": "off",
      "sonarjs/parameterized-tests": "off",
      "sonarjs/no-hardcoded-passwords": "off",
      complexity: ["error", 40],
      "max-lines-per-function": ["error", { "max": 260, "skipBlankLines": true, "skipComments": true }],
      "max-params": ["error", 6]
    },
  },
]);
