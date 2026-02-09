import { fileURLToPath } from 'url';
import path from 'path';
import js from '@eslint/js';
import globals from 'globals';
import reactPlugin from 'eslint-plugin-react';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import eslintConfigPrettier from 'eslint-config-prettier';
import tseslint from 'typescript-eslint';
import simpleImportSort from 'eslint-plugin-simple-import-sort';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const typeAwareConfigs = tseslint.configs.recommendedTypeChecked.map((config) => ({
  ...config,
  files: ['src/**/*.{ts,tsx}'],
  ignores: ['vite.config.ts'],
  languageOptions: {
    ...config.languageOptions,
    globals: {
      ...globals.browser,
      ...globals.vitest,
    },
    parserOptions: {
      project: ['./tsconfig.app.json'],
      tsconfigRootDir: __dirname,
    },
  },
}));

export default tseslint.config(
  {
    ignores: ['dist', 'node_modules', 'coverage', '**/*.cjs', 'eslint.config.js'],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...typeAwareConfigs,
  {
    files: ['src/**/*.{ts,tsx}'],
    plugins: {
      react: reactPlugin,
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
      'simple-import-sort': simpleImportSort,
    },
    rules: {
      'react/react-in-jsx-scope': 'off',
      'react/jsx-uses-react': 'off',
      'react-refresh/only-export-components': 'warn',
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',
      '@typescript-eslint/consistent-type-imports': 'warn',
      'simple-import-sort/imports': 'warn',
      'simple-import-sort/exports': 'warn',
    },
    settings: {
      react: { version: 'detect' },
    },
  },
  {
    files: ['vite.config.ts'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: globals.node,
    },
  },
  {
    files: ['**/*.{ts,tsx,js,jsx}'],
    rules: {
      'no-console': 'warn',
    },
  },
  eslintConfigPrettier,
);
