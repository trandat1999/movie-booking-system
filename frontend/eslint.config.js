// @ts-check
const eslint = require("@eslint/js");
const tseslint = require("typescript-eslint");
const angular = require("angular-eslint");

module.exports = tseslint.config(
  {
    files: ["**/*.ts"],
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommended,
      ...tseslint.configs.stylistic,
      ...angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      // Component gắn route (trang) đặt hậu tố Page, còn lại hậu tố Component (PLAN.md 2A.3)
      "@angular-eslint/component-class-suffix": ["error", { suffixes: ["Component", "Page"] }],
      "@angular-eslint/directive-selector": [
        "error",
        {
          type: "attribute",
          prefix: "app",
          style: "camelCase",
        },
      ],
      "@angular-eslint/component-selector": [
        "error",
        {
          type: "element",
          prefix: "app",
          style: "kebab-case",
        },
      ],
    },
  },
  {
    files: ["**/*.html"],
    extends: [
      ...angular.configs.templateRecommended,
      ...angular.configs.templateAccessibility,
    ],
    rules: {
      // Các control của NG-ZORRO được coi là phần tử form khi nằm trong <label>
      '@angular-eslint/template/label-has-associated-control': [
        'error',
        {
          controlComponents: ['nz-input-number', 'nz-select', 'nz-date-picker', 'nz-time-picker', 'nz-switch'],
        },
      ],
    },
  }
);
