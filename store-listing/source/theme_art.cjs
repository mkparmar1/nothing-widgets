// Generates the starter theme packs into app/src/main/assets/themes/<id>/ as WebP.
// Original art, drawn with SVG lighting filters in headless Chrome. Any file can later be replaced
// by final art with the same name and proportions.
// Usage: node theme_art.cjs
const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");

const CHROME = "C:/Program Files/Google/Chrome/Application/chrome.exe";
const ASSETS = path.resolve(__dirname, "../../app/src/main/assets/themes");
const TMP = path.join(__dirname, "theme_art_tmp");

const PACKS = [
  {
    id: "clay_red", name: "Clay Red", description: "Puffy 3D clay in red and charcoal",
    rim: "#E5322E", inner: "#3A3534", well: "#2A2625", on: "#E5322E", off: "#2C2827",
    bgTop: "#5A1512", bgBottom: "#2A0B0A", onTile: "#FFFFFF", onButtonOn: "#FFFFFF", onButtonOff: "#FFFFFF",
    muted: "#C9BFBD", accent: "#FF4A44",
  },
  {
    id: "clay_blue", name: "Clay Blue", description: "Puffy 3D clay in blue and ink",
    rim: "#2F7BE6", inner: "#23262E", well: "#191B21", on: "#3A86EE", off: "#1E2028",
    bgTop: "#123A80", bgBottom: "#071B42", onTile: "#FFFFFF", onButtonOn: "#FFFFFF", onButtonOff: "#FFFFFF",
    muted: "#B6C2D6", accent: "#5AA2FF",
  },
];

const filter = (id, blur, scale) => `
<filter id="${id}" x="-15%" y="-15%" width="130%" height="130%" color-interpolation-filters="sRGB">
  <feGaussianBlur in="SourceAlpha" stdDeviation="${blur}" result="b"/>
  <feDiffuseLighting in="b" surfaceScale="${scale}" diffuseConstant="0.98" lighting-color="#fff" result="d0">
    <feDistantLight azimuth="235" elevation="62"/></feDiffuseLighting>
  <feGaussianBlur in="d0" stdDeviation="5" result="d"/>
  <feComposite in="SourceGraphic" in2="d" operator="arithmetic" k1="1" k2="0" k3="0" k4="0" result="lit"/>
  <feSpecularLighting in="b" surfaceScale="${scale}" specularConstant="0.55" specularExponent="26" lighting-color="#fff" result="s0">
    <feDistantLight azimuth="235" elevation="52"/></feSpecularLighting>
  <feGaussianBlur in="s0" stdDeviation="5" result="s"/>
  <feComposite in="s" in2="SourceAlpha" operator="in" result="sp"/>
  <feComposite in="lit" in2="SourceAlpha" operator="in" result="base"/>
  <feMerge result="m"><feMergeNode in="base"/><feMergeNode in="sp"/></feMerge>
</filter>
<filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
  <feDropShadow dx="6" dy="10" stdDeviation="12" flood-color="#000" flood-opacity="0.45"/>
</filter>`;

const svg = (w, h, body) => `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">
<defs>${filter("big", 22, 26)}${filter("mid", 30, 34)}${filter("well", 12, -14)}${filter("ball", 44, 62)}</defs>${body}</svg>`;

// A rim + inner tile; m = margin around the shape for its shadow.
function tile(p, w, h, r, { m = 0.05 } = {}) {
  const mm = Math.round(Math.min(w, h) * m), rim = Math.round(Math.min(w, h) * 0.035);
  const x = mm, y = mm, W = w - 2 * mm, H = h - 2 * mm;
  return `<g filter="url(#shadow)"><rect x="${x}" y="${y}" width="${W}" height="${H}" rx="${r}" fill="${p.rim}" filter="url(#big)"/></g>
  <rect x="${x + rim}" y="${y + rim}" width="${W - 2 * rim}" height="${H - 2 * rim}" rx="${Math.max(r - rim, 8)}" fill="${p.inner}" filter="url(#mid)"/>`;
}

const well = (p, cx, cy, r) => `<circle cx="${cx}" cy="${cy}" r="${r}" fill="${p.well}" filter="url(#well)"/>`;

function button(p, fill) {
  return svg(320, 320, `<g filter="url(#shadow)"><circle cx="160" cy="160" r="136" fill="${fill}" filter="url(#ball)"/></g>`);
}

function assets(p) {
  const S = 640;
  const quad = [0.30, 0.70].flatMap(y => [0.30, 0.70].map(x => well(p, S * x, S * y, S * 0.165))).join("");
  return {
    tile_square: [S, S, svg(S, S, tile(p, S, S, 120))],
    tile_quad: [S, S, svg(S, S, tile(p, S, S, 120) + quad)],
    tile_wide: [S * 2, S, svg(S * 2, S, tile(p, S * 2, S, 120))],
    tile_pill: [S * 2, Math.round(S * 0.68), svg(S * 2, Math.round(S * 0.68), tile(p, S * 2, Math.round(S * 0.68), 190))],
    tile_tall: [300, S, svg(300, S, tile(p, 300, S, 120, { m: 0.09 }))],
    tile_tall_fill: [300, S, svg(300, S, tile({ ...p, inner: p.on }, 300, S, 120, { m: 0.09 }))],
    button_on: [320, 320, button(p, p.on)],
    button_off: [320, 320, button(p, p.off)],
    wallpaper: [1080, 2400, svg(1080, 2400, `
      <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${p.bgTop}"/><stop offset="1" stop-color="${p.bgBottom}"/></linearGradient>
      <filter id="grain"><feTurbulence type="fractalNoise" baseFrequency="0.004" numOctaves="3" seed="4"/><feColorMatrix values="0 0 0 0 1  0 0 0 0 1  0 0 0 0 1  0.05 0 0 0 -0.01"/></filter></defs>
      <rect width="1080" height="2400" fill="url(#g)"/><rect width="1080" height="2400" filter="url(#grain)"/>`)],
  };
}

// One page turns every SVG into WebP and prints the data URLs as JSON.
function build() {
  fs.mkdirSync(TMP, { recursive: true });
  const jobs = [];
  for (const p of PACKS) for (const [name, [w, h, s]] of Object.entries(assets(p))) {
    jobs.push({ pack: p.id, name, w, h, src: "data:image/svg+xml;base64," + Buffer.from(s).toString("base64"), q: name === "wallpaper" ? 0.82 : 0.9 });
  }
  const html = `<!doctype html><body><pre id="out"></pre><script>
  (async()=>{const jobs=${JSON.stringify(jobs)};const res=[];
  for(const j of jobs){const i=new Image();await new Promise((ok,no)=>{i.onload=ok;i.onerror=no;i.src=j.src});
    const c=document.createElement('canvas');c.width=j.w;c.height=j.h;c.getContext('2d').drawImage(i,0,0);
    res.push({pack:j.pack,name:j.name,url:c.toDataURL('image/webp',j.q)})}
  const M='@'+'@';document.getElementById('out').textContent=M+JSON.stringify(res)+M;})();</script></body>`;
  const page = path.join(TMP, "page.html");
  fs.writeFileSync(page, html);
  const dom = execFileSync(CHROME, ["--headless=new", "--disable-gpu", "--virtual-time-budget=120000", "--dump-dom", "file:///" + page.replace(/\\/g, "/")],
    { maxBuffer: 1 << 29, encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] });
  const m = dom.match(/@@(.*)@@/s);
  if (!m) throw new Error("no output from Chrome");
  for (const r of JSON.parse(m[1])) {
    const dir = path.join(ASSETS, r.pack);
    fs.mkdirSync(dir, { recursive: true });
    if (!r.url.startsWith("data:image/webp")) throw new Error("not webp: " + r.name);
    fs.writeFileSync(path.join(dir, r.name + ".webp"), Buffer.from(r.url.split(",")[1], "base64"));
  }
  for (const p of PACKS) {
    const { id, name, description, onTile, onButtonOn, onButtonOff, muted, accent } = p;
    fs.writeFileSync(path.join(ASSETS, id, "theme.json"), JSON.stringify({ id, name, description, onTile, onButtonOn, onButtonOff, muted, accent }, null, 2) + "\n");
  }
  fs.rmSync(TMP, { recursive: true, force: true });
}

build();
for (const p of PACKS) {
  const dir = path.join(ASSETS, p.id);
  const kb = fs.readdirSync(dir).map(f => fs.statSync(path.join(dir, f)).size).reduce((a, b) => a + b, 0) >> 10;
  console.log(p.id, fs.readdirSync(dir).length, "files", kb + " KB");
}
