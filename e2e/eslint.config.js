import js from '@eslint/js'
import globals from 'globals'
import sonarjs from 'eslint-plugin-sonarjs'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

// The e2e workspace's quality gate, the sibling of web/eslint.config.js:
// zero findings, no baseline, one commented override per deliberate idiom.
export default defineConfig([
  globalIgnores(['node_modules', 'playwright-report', 'test-results']),
  {
    files: ['**/*.ts'],
    extends: [js.configs.recommended, tseslint.configs.recommended, sonarjs.configs.recommended],
    languageOptions: {
      globals: globals.node,
    },
    rules: {
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_', caughtErrorsIgnorePattern: '^_' },
      ],
      // Flat `kind === 'a' ? x : kind === 'b' ? y : z` chains pick one label or body per scenario
      // (accessibility.spec.ts, practice-support.spec.ts); a lookup table would hide the mapping.
      'sonarjs/no-nested-conditional': 'off',
    },
  },
])
