// Dot Widgets logo concepts → SVG/HTML; render.sh turns them into PNGs.
const fs = require("fs");
const path = require("path");
const out = path.join(__dirname, "logo");
fs.mkdirSync(out, { recursive: true });

const RED = "#E02B2B", INK = "#0A0A0A", WHITE = "#F5F5F5";

// 7x7 dot-matrix "D" (1 = lit).
const D = [
  "111110.",
  "1....11",
  "1.....1",
  "1.....1",
  "1.....1",
  "1....11",
  "111110.",
].map(r => r.split("").map(c => c === "1"));

function dots(grid, { x0, y0, pitch, r, on = WHITE, off = "rgba(255,255,255,0.10)", accent = null }) {
  let s = "";
  grid.forEach((row, y) => row.forEach((lit, x) => {
    const cx = x0 + x * pitch, cy = y0 + y * pitch;
    const isAccent = accent && accent[0] === x && accent[1] === y;
    s += `<circle cx="${cx}" cy="${cy}" r="${r}" fill="${isAccent ? RED : lit ? on : off}"/>`;
  }));
  return s;
}

// A — dot-matrix D, red signature dot in the corner (like every widget's corner dot).
function conceptA(size = 512) {
  const pitch = 46, r = 17, w = 6 * pitch, x0 = (512 - w) / 2 - 8, y0 = (512 - w) / 2;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 512 512">
    <rect width="512" height="512" fill="${INK}"/>
    ${dots(D, { x0, y0, pitch, r })}
    <circle cx="${x0 + 6 * pitch + 30}" cy="${y0 - 30}" r="26" fill="${RED}"/>
  </svg>`;
}

// B — a 3x3 widget grid of dots, one of them red: "dot widgets".
function conceptB(size = 512) {
  const pitch = 128, r = 44, x0 = 256 - pitch, y0 = 256 - pitch;
  let s = "";
  for (let y = 0; y < 3; y++) for (let x = 0; x < 3; x++) {
    const red = x === 2 && y === 0;
    s += `<circle cx="${x0 + x * pitch}" cy="${y0 + y * pitch}" r="${r}" fill="${red ? RED : WHITE}" opacity="${red ? 1 : (x + y) % 2 ? 0.92 : 1}"/>`;
  }
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 512 512">
    <rect width="512" height="512" fill="${INK}"/>${s}</svg>`;
}

// C — a widget tile (rounded square) drawn in dots, with the red dot inside: a widget, in dots.
function conceptC(size = 512) {
  const n = 7, pitch = 44, r = 12, x0 = 256 - 3 * pitch, y0 = 256 - 3 * pitch;
  let s = "";
  for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) {
    const edge = x === 0 || y === 0 || x === n - 1 || y === n - 1;
    const corner = (x === 0 || x === n - 1) && (y === 0 || y === n - 1);
    if (corner) continue; // rounded corners
    s += `<circle cx="${x0 + x * pitch}" cy="${y0 + y * pitch}" r="${edge ? r : r * 0.55}" fill="${edge ? WHITE : "rgba(255,255,255,0.12)"}"/>`;
  }
  s += `<circle cx="${x0 + 4 * pitch}" cy="${y0 + 2 * pitch}" r="36" fill="${RED}"/>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 512 512">
    <rect width="512" height="512" fill="${INK}"/>${s}</svg>`;
}

const page = svg => `<!doctype html><html><head><meta charset="utf-8"><style>html,body{margin:0;width:512px;height:512px;overflow:hidden;background:${INK}}</style></head><body>${svg}</body></html>`;
for (const [k, f] of Object.entries({ A: conceptA, B: conceptB, C: conceptC })) {
  fs.writeFileSync(path.join(out, `concept_${k}.svg`), f());
  fs.writeFileSync(path.join(out, `concept_${k}.html`), page(f()));
}
// Preview sheet: each concept at icon size, masked like a launcher (circle) and at 48px.
const sheet = `<!doctype html><html><head><meta charset="utf-8"><style>
body{margin:0;background:#3a3a3a;font:20px monospace;color:#ddd;display:flex;gap:40px;padding:30px}
.c{display:flex;flex-direction:column;gap:16px;align-items:center}
.m{width:220px;height:220px;border-radius:50%;overflow:hidden}.s{width:220px;height:220px;border-radius:48px;overflow:hidden}
.t{width:48px;height:48px;border-radius:50%;overflow:hidden}
svg{width:100%;height:100%;display:block}</style></head><body>
${["A", "B", "C"].map(k => {
  const svg = fs.readFileSync(path.join(out, `concept_${k}.svg`), "utf8");
  return `<div class="c"><div class="s">${svg}</div><div class="m">${svg}</div><div class="t">${svg}</div>${k}</div>`;
}).join("")}</body></html>`;
fs.writeFileSync(path.join(out, "concepts.html"), sheet);
console.log("concepts written");

// A2 — 5x7 dot-matrix "D" + a red full stop: "D." (as in the "DOT WIDGETS ●" mark).
const D57 = ["11110", "10001", "10001", "10001", "10001", "10001", "11110"].map(r => r.split("").map(c => c === "1"));
function conceptA2(size = 512, { bg = true, off = true } = {}) {
  const pitch = 50, r = 19.5;
  const groupW = 4 * pitch + 70; // D plus the stop
  const x0 = 256 - groupW / 2 + 4, y0 = 256 - 3 * pitch;
  const lit = dots(D57, { x0, y0, pitch, r, off: off ? "rgba(255,255,255,0.09)" : "none" });
  const stop = `<circle cx="${x0 + 4 * pitch + 66}" cy="${y0 + 6 * pitch}" r="24" fill="${RED}"/>`;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 512 512">
    ${bg ? `<rect width="512" height="512" fill="${INK}"/>` : ""}${lit}${stop}</svg>`;
}
// A3 — same D, red dot as the D's own top-right "corner dot".
function conceptA3(size = 512) {
  const pitch = 52, r = 20, x0 = 256 - 2 * pitch, y0 = 256 - 3 * pitch;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 512 512">
    <rect width="512" height="512" fill="${INK}"/>${dots(D57, { x0, y0, pitch, r, off: "rgba(255,255,255,0.09)" })}
    <circle cx="${x0 + 4 * pitch + 58}" cy="${y0 - 52}" r="22" fill="${RED}"/></svg>`;
}
module.exports = { conceptA2, D57, dots, RED, INK, WHITE };
for (const [k, f] of Object.entries({ A2: conceptA2, A3: conceptA3, B: conceptB })) {
  fs.writeFileSync(path.join(out, `concept_${k}.svg`), f());
}
const sheet2 = `<!doctype html><html><head><meta charset="utf-8"><style>
body{margin:0;background:#3a3a3a;font:20px monospace;color:#ddd;display:flex;gap:40px;padding:30px}
.c{display:flex;flex-direction:column;gap:16px;align-items:center}
.m{width:220px;height:220px;border-radius:50%;overflow:hidden}.s{width:220px;height:220px;border-radius:48px;overflow:hidden}
.t{width:48px;height:48px;border-radius:50%;overflow:hidden}.w{width:220px;height:220px;border-radius:48px;overflow:hidden;background:#fff;padding:0}
svg{width:100%;height:100%;display:block}</style></head><body>
${["A2", "A3", "B"].map(k => {
  const svg = fs.readFileSync(path.join(out, `concept_${k}.svg`), "utf8");
  return `<div class="c"><div class="s">${svg}</div><div class="m">${svg}</div><div class="t">${svg}</div>${k}</div>`;
}).join("")}</body></html>`;
fs.writeFileSync(path.join(out, "concepts2.html"), sheet2);
