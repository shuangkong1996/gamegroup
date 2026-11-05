import js from "@eslint/js";
import tseslint from "typescript-eslint";

export default tseslint.config(
  { ignores: ["dist", "node_modules"] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ["src/sim/**/*.ts"],
    rules: {
      "no-restricted-globals": ["error", "document", "window"],
      "no-restricted-properties": ["error", { object: "Math", property: "random" }],
      "no-restricted-imports": ["error", { paths: ["phaser"] }],
    },
  },
);
