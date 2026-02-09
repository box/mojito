New Frontend Tooling (ESLint/Prettier/Vitest)
=============================================

Context
- The new Vite/React/TS frontend (`webapp/new-frontend`) is currently bare-bones with no lint/format/test guardrails.
- We want lightweight defaults before building pages so conventions stick and CI can enforce them.

Goals
- Add ESLint + Prettier with sensible defaults for React + TypeScript and keep configs minimal (now includes simple-import-sort for imports/exports).
- Add Vitest + @testing-library/react for unit/component tests.
- Provide npm scripts that match repo conventions (`lint`, `lint:fix`, `format`, `format:check`, `test`, `test:watch`).
- Keep noise low: ignore build output/node_modules; align with Maven-managed Node/npm notes.

Non-goals
- No CI wiring yet; no strict style bikeshedding. Just defaults that can evolve.
- No storybook/e2e yet.

Plan (narrow)
- Install dev deps: eslint, @typescript-eslint/parser/plugin, eslint-plugin-react(+hooks), eslint-plugin-react-refresh, eslint-config-prettier; prettier + plugins for json/yaml/markdown; vitest + @testing-library/react + jsdom; @types/node for tests.
- Add `eslint.config.js` (flat config) and `.prettierrc.cjs` + `.prettierignore`.
- Create basic `vitest.config.ts` aligned with Vite, jsdom environment, and path setup.
- Add a starter React component test to verify wiring.
- Add npm scripts and update docs if needed.

Open questions
- Do we enforce import sorting? (decided yes, using simple-import-sort, auto-fixed by eslint --fix.)
- JSX runtime: use React 17 classic vs. automatic (Vite default uses automatic). Align ESLint config with Vite default.
