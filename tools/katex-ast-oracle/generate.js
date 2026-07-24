const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const katex = require("katex");

const projectRoot = path.resolve(__dirname, "../..");
const corpusRoot = path.join(
  projectRoot,
  "shared/src/jvmTest/resources/zhihu-formula-corpus",
);
const formulas = JSON.parse(
  fs.readFileSync(path.join(corpusRoot, "formulas.json"), "utf8"),
);

katex.__defineFunction({
  type: "bbox",
  names: ["\\bbox"],
  numArgs: 1,
  numOptionalArgs: 1,
  allowedInText: true,
  argTypes: ["raw", "original"],
  handler({ parser }, args, optionalArgs) {
    return {
      type: "bbox",
      mode: parser.mode,
      body: args[0],
      options: optionalArgs[0],
    };
  },
});

function normalizeInput(latex) {
  const trailingBackslashes = latex.match(/\\+$/)?.[0].length ?? 0;
  return trailingBackslashes % 2 === 1 ? latex.slice(0, -1) : latex;
}

function emptyProfile() {
  return {
    fractions: 0,
    rulelessFractions: 0,
    binomials: 0,
    roots: 0,
    indexedRoots: 0,
    superscripts: 0,
    subscripts: 0,
    delimiters: [],
    tables: [],
    rowGapPatterns: [],
    textSegments: [],
    accents: 0,
    extensibleArrows: 0,
    operators: 0,
    colors: 0,
    boxes: 0,
  };
}

function textContent(node) {
  if (Array.isArray(node)) {
    return node.map(textContent).join("");
  }
  if (!node || typeof node !== "object") {
    return "";
  }
  if (
    typeof node.text === "string" &&
    (node.type === "mathord" ||
      node.type === "textord" ||
      node.type === "atom")
  ) {
    return node.text;
  }
  return Object.entries(node)
    .filter(([key]) => key !== "loc")
    .map(([, value]) => textContent(value))
    .join("");
}

function comparableText(value) {
  return value.normalize("NFD").replace(/\p{M}/gu, "");
}

function comparableDelimiter(value) {
  const delimiters = {
    ".": "",
    "\\{": "{",
    "\\}": "}",
    "\\langle": "⟨",
    "\\rangle": "⟩",
    "\\lvert": "|",
    "\\rvert": "|",
    "\\lVert": "‖",
    "\\rVert": "‖",
  };
  return delimiters[value] ?? value;
}

function visit(node, profile) {
  if (Array.isArray(node)) {
    node.forEach((child) => visit(child, profile));
    return;
  }
  if (!node || typeof node !== "object") {
    return;
  }

  switch (node.type) {
    case "genfrac":
      if (node.leftDelim != null || node.rightDelim != null) {
        profile.binomials++;
      } else {
        profile.fractions++;
        if (!node.hasBarLine) {
          profile.rulelessFractions++;
        }
      }
      break;
    case "sqrt":
      profile.roots++;
      if (node.index != null) {
        profile.indexedRoots++;
      }
      break;
    case "supsub":
      if (node.sup != null) {
        profile.superscripts++;
      }
      if (node.sub != null) {
        profile.subscripts++;
      }
      break;
    case "leftright":
      profile.delimiters.push(
        `${comparableDelimiter(node.left ?? "")}|${comparableDelimiter(
          node.right ?? "",
        )}`,
      );
      if (
        comparableDelimiter(node.left ?? "") === "{" &&
        comparableDelimiter(node.right ?? "") === "" &&
        Array.isArray(node.body) &&
        node.body.length === 1 &&
        node.body[0]?.type === "array" &&
        node.body[0].arraystretch === 1.2 &&
        node.body[0].colSeparationType === undefined
      ) {
        profile.tables.push(`cases:${node.body[0].body.length}`);
        profile.rowGapPatterns.push(
          (node.body[0].rowGaps ?? [])
            .map((gap) => (gap == null ? "null" : `${gap.number}${gap.unit}`))
            .join(","),
        );
        node.body[0].body.forEach((row) => visit(row, profile));
        return;
      }
      break;
    case "array":
    case "align":
      if (
        node.cols !== undefined &&
        Array.isArray(node.body) &&
        node.body.every(Array.isArray)
      ) {
        profile.rowGapPatterns.push(
          (node.rowGaps ?? [])
            .map((gap) => (gap == null ? "null" : `${gap.number}${gap.unit}`))
            .join(","),
        );
        profile.tables.push(
          `table:${node.body.length}x${Math.max(
            0,
            ...node.body.map((row) => row.length),
          )}`,
        );
      }
      break;
    case "text":
      {
        const text = comparableText(textContent(node.body)).trim();
        if (text !== "") {
          profile.textSegments.push(text);
        }
      }
      return;
    case "accent":
    case "overline":
    case "horizBrace":
      profile.accents++;
      break;
    case "xArrow":
      profile.extensibleArrows++;
      break;
    case "op":
    case "operatorname":
      if (
        !Array.isArray(node.body) ||
        !node.body.some(
          (child) => child?.type === "op" || child?.type === "operatorname",
        )
      ) {
        profile.operators++;
      }
      if (Array.isArray(node.body)) {
        node.body
          .filter((child) => child?.type !== "text")
          .forEach((child) => visit(child, profile));
      }
      return;
    case "tag":
      visit(node.body, profile);
      return;
    case "color":
      profile.colors++;
      break;
    case "bbox":
    case "enclose":
      profile.boxes++;
      break;
  }

  for (const [key, value] of Object.entries(node)) {
    if (key !== "loc" && key !== "type" && key !== "tags") {
      visit(value, profile);
    }
  }
}

function errorCategory(error) {
  const message = String(error.message);
  if (message.includes("function '$'")) {
    return "malformed-dollar";
  }
  if (message.includes("symbol group type") && message.includes("type cr")) {
    return "invalid-array";
  }
  return "unexpected";
}

const entries = formulas.map(({ latex }) => {
  const normalized = normalizeInput(latex);
  const hash = crypto.createHash("sha256").update(latex).digest("hex");
  try {
    const ast = katex.__parse(normalized, {
      displayMode: true,
      strict: "ignore",
      trust: false,
      maxExpand: 1000,
    });
    const profile = emptyProfile();
    visit(ast, profile);
    profile.delimiters.sort();
    profile.tables.sort();
    profile.rowGapPatterns.sort();
    profile.textSegments.sort();
    return { sha256: hash, status: "parsed", profile };
  } catch (error) {
    return {
      sha256: hash,
      status: "rejected",
      errorCategory: errorCategory(error),
    };
  }
});

const output = {
  schemaVersion: 1,
  katexVersion: katex.version,
  settings: {
    displayMode: true,
    strict: "ignore",
    trust: false,
    maxExpand: 1000,
    oddTrailingBackslash: "remove one before KaTeX parsing",
    bbox: "parse-only MathJax-compatible extension",
  },
  parsedCount: entries.filter((entry) => entry.status === "parsed").length,
  rejectedCount: entries.filter((entry) => entry.status === "rejected").length,
  entries,
};

fs.writeFileSync(
  path.join(corpusRoot, "katex-ast-oracle.json"),
  `${JSON.stringify(output, null, 2)}\n`,
);
