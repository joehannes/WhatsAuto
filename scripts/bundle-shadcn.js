const esbuild = require("esbuild");
const path = require("path");
const fs = require("fs");

const root = path.join(__dirname, "..");
const entry = path.join(root, "src/main/bridge/shadcn.js");
const outdir = path.join(root, "dist/js");
const outfile = path.join(outdir, "shadcn-ui.js");

fs.mkdirSync(outdir, {recursive: true});

esbuild
  .build({
    entryPoints: [entry],
    bundle: true,
    format: "iife",
    globalName: "WhatsAutoShadcn",
    outfile,
    platform: "browser",
    target: "es2020",
    jsx: "automatic",
    loader: {
      ".tsx": "tsx",
      ".ts": "ts",
    },
    logLevel: "info",
  })
  .catch(() => process.exit(1));
