import globals from "globals";
import esLintPluginReact from "eslint-plugin-react";
import esLintPluginReactHooks from "eslint-plugin-react-hooks";

export default [
  {
    languageOptions: {
        ecmaVersion: 2022,
        sourceType: "module",
        globals: {
            ...globals.browser,
            ...globals.node,
        }
    },
    plugins: {
      react: esLintPluginReact,
      reactHooks: esLintPluginReactHooks,
    },
    rules: {
      'react/prop-types': 'off',
      'object-curly-spacing': ['error', 'always'],
      'no-unused-vars': ['error', {
            'vars': 'all',
            'args': 'after-used',
            'caughtErrors': 'all',
            'ignoreRestSiblings': false,
            'reportUsedIgnorePattern': false
        }],
      'no-trailing-spaces': 'error',
      "prefer-const": ["error"],
      "eqeqeq": ["error", "always"]
    },
    settings: {
      react: {
        version: 'detect', // Automatically detect the React version
      },
    },
  }
];