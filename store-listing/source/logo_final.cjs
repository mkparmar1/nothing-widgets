// Final "D." logo: Play icon, feature graphic, adaptive launcher icon + legacy PNG pages.
const fs = require("fs");
const path = require("path");
const out = path.join(__dirname, "logo");
fs.mkdirSync(out, { recursive: true });

const RED = "#E02B2B", INK = "#0A0A0A", WHITE = "#F5F5F5";
const D = ["11110", "10001", "10001", "10001", "10001", "10001", "11110"].map(r => r.split("").map(c => c === "1"));

/**
 * The mark in a unit box: dots for the 5x7 "D" plus the red stop, centred on (0,0).
 * pitch = 1; returns circles in mark units so every output can scale it.
 */
function mark({ offDots = true } = {}) {
  const r = 0.4, stopR = 0.48, gap = 1.32; // stop sits one gap right of the D, on the baseline
  const width = 4 + gap, height = 6;
  const ox = -width / 2, oy = -height / 2;
  const c = [];
  D.forEach((row, y) => row.forEach((lit, x) => {
    if (lit) c.push({ x: ox + x, y: oy + y, r, fill: WHITE });
    else if (offDots) c.push({ x: ox + x, y: oy + y, r, fill: "rgba(255,255,255,0.09)", off: true });
  }));
  c.push({ x: ox + 4 + gap, y: oy + 6, r: stopR, fill: RED, stop: true });
  return { circles: c, width: width + 2 * stopR, height: height + 2 * r };
}

function svgMark({ size, scale, cx, cy, offDots = true }) {
  return mark({ offDots }).circles.map(k =>
    `<circle cx="${(cx + k.x * scale).toFixed(2)}" cy="${(cy + k.y * scale).toFixed(2)}" r="${(k.r * scale).toFixed(2)}" fill="${k.fill}"/>`).join("");
}

// 1) Play Store icon 512x512 (Play applies its own rounded mask; art is full-bleed).
const playIcon = `<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">
  <rect width="512" height="512" fill="${INK}"/>${svgMark({ size: 512, scale: 50, cx: 256, cy: 256 })}</svg>`;

// 2) Feature graphic 1024x500.
function featureGraphic() {
  const tiles = ["d_clock.dotdigital.l", "l_clock.analog.ring", "d_battery.ring", "d_clock.text.words", "l_calendar.day.date",
    "d_clock.glyph.ring", "d_weather.moon.phase", "l_clock.dotdigital.m", "d_clock.stacked.hhmm", "d_calendar.life.dots",
    "d_clock.minimal.pill", "l_battery.ring", "d_fun.dice.roll", "d_clock.binary", "d_toggle.grid.2x2", "l_toggle.wifi"];
  let wall = "";
  tiles.forEach((t, i) => {
    const col = i % 4, row = Math.floor(i / 4);
    wall += `<img src="../assets/${t}.png" style="position:absolute;left:${col * 190}px;top:${row * 190 + (col % 2) * 60}px;width:170px;border-radius:24px;box-shadow:0 14px 30px rgba(0,0,0,.55)">`;
  });
  return `<!doctype html><html><head><meta charset="utf-8"><style>
    @font-face{font-family:JB;src:url("../fonts/jetbrains_mono.ttf");font-weight:400 800}
    @font-face{font-family:SG;src:url("../fonts/space_grotesk.ttf")}
    html,body{margin:0;width:1024px;height:500px;overflow:hidden}
    body{position:relative;background:radial-gradient(700px 500px at 30% 50%,#242424,#0d0d0d);color:#fff}
    body::after{content:"";position:absolute;inset:0;background-image:radial-gradient(rgba(255,255,255,.05) 1.5px,transparent 1.6px);background-size:24px 24px;pointer-events:none}
    .wall{position:absolute;left:560px;top:-120px;transform:rotate(-24deg);transform-origin:0 0}
    .fade{position:absolute;inset:0;background:linear-gradient(90deg,#141414 0,#141414 470px,rgba(20,20,20,0) 640px);z-index:2}
    .txt{position:absolute;left:70px;top:0;bottom:0;display:flex;flex-direction:column;justify-content:center;z-index:3}
    .name{font-family:JB;font-weight:800;font-size:58px;letter-spacing:6px;margin-top:22px}
    .name b{color:${RED}}
    .tag{font-family:SG;font-size:26px;color:rgba(255,255,255,.65);margin-top:10px;letter-spacing:.5px}
  </style></head><body>
    <div class="wall">${wall}</div><div class="fade"></div>
    <div class="txt">
      <svg width="150" height="150" viewBox="0 0 512 512" style="border-radius:34px">${playIcon.replace(/^<svg[^>]*>/, "").replace(/<\/svg>$/, "")}</svg>
      <div class="name">DOT WIDGETS <b>●</b></div>
      <div class="tag">223 dot-matrix widgets for your home screen</div>
    </div></body></html>`;
}

const page = (svg, w, h) => `<!doctype html><html><head><meta charset="utf-8"><style>html,body{margin:0;width:${w}px;height:${h}px;overflow:hidden;background:transparent}svg{display:block}</style></head><body>${svg}</body></html>`;

fs.writeFileSync(path.join(out, "play_icon.svg"), playIcon);
fs.writeFileSync(path.join(out, "play_icon.html"), page(playIcon, 512, 512));
fs.writeFileSync(path.join(out, "feature_graphic.html"), featureGraphic());

// 3) Legacy launcher PNG pages (API < 26): rounded square and round, at each density.
const legacy = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
for (const [d, px] of Object.entries(legacy)) {
  for (const round of [false, true]) {
    const inner = `<svg xmlns="http://www.w3.org/2000/svg" width="${px}" height="${px}" viewBox="0 0 512 512">
      <defs><clipPath id="m">${round ? `<circle cx="256" cy="256" r="256"/>` : `<rect width="512" height="512" rx="112"/>`}</clipPath></defs>
      <g clip-path="url(#m)"><rect width="512" height="512" fill="${INK}"/>${svgMark({ scale: 42, cx: 256, cy: 256 })}</g></svg>`;
    fs.writeFileSync(path.join(out, `legacy_${d}_${round ? "round" : "square"}.html`), page(inner, px, px));
  }
}

// 4) Adaptive icon vectors (108dp canvas; keep the mark inside the 66dp safe circle).
function vectorCircle(cx, cy, r, color) {
  const f = n => +n.toFixed(2);
  return `    <path android:fillColor="${color}" android:pathData="M${f(cx - r)},${f(cy)}a${f(r)},${f(r)} 0 1,0 ${f(2 * r)},0a${f(r)},${f(r)} 0 1,0 ${f(-2 * r)},0z"/>`;
}
function vector(circles) {
  return `<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by store-listing/source/logo_final.cjs: dot-matrix "D" with a red stop ("D."). -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
${circles.join("\n")}
</vector>
`;
}
const argb = c => c === WHITE ? "#FFF5F5F5" : c === RED ? "#FFE02B2B" : "#17FFFFFF";
// Scale 6.4dp per dot: the D's farthest corner sits ~28dp from centre, inside the 33dp safe radius.
const S = 6.4;
const fg = mark().circles.map(k => vectorCircle(54 + k.x * S, 54 + k.y * S, k.r * S, argb(k.fill.startsWith("rgba") ? "off" : k.fill)));
const mono = mark({ offDots: false }).circles.map(k => vectorCircle(54 + k.x * S, 54 + k.y * S, k.r * S, "#FFFFFFFF"));
fs.writeFileSync(path.join(out, "ic_launcher_foreground.xml"), vector(fg));
fs.writeFileSync(path.join(out, "ic_launcher_monochrome.xml"), vector(mono));
fs.writeFileSync(path.join(out, "ic_launcher_background.xml"), `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#FF0A0A0A" android:pathData="M0,0h108v108h-108z"/>
</vector>
`);
const far = Math.max(...mark().circles.map(k => Math.hypot(k.x * S, k.y * S) + k.r * S));
console.log("farthest dot edge from centre (dp):", far.toFixed(1), "(safe radius 33)");
