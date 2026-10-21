// Builds the 8 Play Store screenshot pages (1080x1920) as HTML; render.sh turns them into PNGs.
const fs = require("fs");
const path = require("path");
const dir = __dirname;
const A = n => `assets/${n}.png`;

const css = `
@font-face { font-family: SG; src: url("fonts/space_grotesk.ttf"); }
@font-face { font-family: JB; src: url("fonts/jetbrains_mono.ttf"); font-weight: 400 800; }
* { box-sizing: border-box; margin: 0; padding: 0; }
html, body { width: 1080px; height: 1920px; overflow: hidden; }
body {
  font-family: SG, sans-serif; color: #fff; position: relative;
  background: radial-gradient(1200px 900px at 50% 30%, #2a2a2a 0%, #171717 55%, #0d0d0d 100%);
}
body::after { /* faint dot grid, like the widgets */
  content: ""; position: absolute; inset: 0; pointer-events: none;
  background-image: radial-gradient(rgba(255,255,255,0.05) 1.6px, transparent 1.7px);
  background-size: 28px 28px;
}
.head { position: absolute; left: 80px; right: 80px; top: 120px; z-index: 5; }
.kicker { font-family: JB; font-weight: 700; font-size: 30px; letter-spacing: 6px; color: #e02b2b; margin-bottom: 28px; }
.title { font-family: JB; font-weight: 800; font-size: 82px; line-height: 1.04; letter-spacing: 1px; text-transform: uppercase; }
.title em { font-style: normal; color: #e02b2b; }
.sub { font-family: SG; font-size: 36px; line-height: 1.35; color: rgba(255,255,255,0.62); margin-top: 26px; max-width: 900px; }
.tile { position: absolute; border-radius: 34px; box-shadow: 0 24px 50px rgba(0,0,0,0.55); }
.pill {
  display: inline-flex; align-items: center; gap: 14px; padding: 20px 34px; border-radius: 999px;
  font-family: JB; font-weight: 700; font-size: 30px; letter-spacing: 3px; text-transform: uppercase;
  background: #1f1f1f; color: #fff; border: 1.5px solid rgba(255,255,255,0.14);
}
.pill.red { background: #e02b2b; border-color: #e02b2b; }
.pill.light { background: #f2f2f2; color: #111; border-color: #f2f2f2; }
.pill.green { background: #62d9a0; color: #0b2b1e; border-color: #62d9a0; }
.dot { width: 16px; height: 16px; border-radius: 50%; background: currentColor; }
.phone {
  position: absolute; border-radius: 78px; padding: 18px; background: #050505;
  box-shadow: 0 0 0 3px #303030, 0 50px 120px rgba(0,0,0,0.7);
}
.phone .screen { width: 100%; height: 100%; border-radius: 62px; overflow: hidden; position: relative; background: #000; }
.phone .screen img { width: 100%; display: block; }
.phone .bar { /* clean status bar over the real one */
  position: absolute; left: 0; right: 0; top: 0; height: 4.7%; background: #000;
  display: flex; align-items: center; justify-content: space-between; padding: 0 9%;
  font-family: SG; font-weight: 700; color: #fff;
}
.phone .bar .bat { width: 1.9em; height: 0.95em; border-radius: 0.25em; border: 2px solid #fff; position: relative; }
.phone .bar .bat::after { content: ""; position: absolute; inset: 2px; right: 24%; background: #fff; border-radius: 0.12em; }
.phone .cam { position: absolute; top: 2.1%; left: 50%; width: 3.2%; aspect-ratio: 1; border-radius: 50%; background: #111; transform: translateX(-50%); z-index: 3; box-shadow: 0 0 0 2px #1d1d1d; }
.card {
  position: absolute; border-radius: 40px; background: #1d1d1d; padding: 40px;
  border: 1.5px solid rgba(255,255,255,0.08); box-shadow: 0 30px 70px rgba(0,0,0,0.45);
}
.card.light { background: #f2f2f2; color: #111; border-color: #f2f2f2; }
.card.red { background: #e02b2b; border-color: #e02b2b; }
.card .big { font-family: JB; font-weight: 800; font-size: 112px; line-height: 1; letter-spacing: -2px; }
.card .big sup { font-size: 56px; vertical-align: top; }
.card .lbl { font-family: JB; font-weight: 700; font-size: 32px; line-height: 1.25; letter-spacing: 2px; text-transform: uppercase; margin-top: 18px; }
.card .ic { font-family: JB; font-size: 46px; opacity: 0.55; margin-bottom: 18px; }
.foot { position: absolute; left: 0; right: 0; bottom: 110px; display: flex; justify-content: center; gap: 22px; z-index: 6; }
.fadeb { position: absolute; left: 0; right: 0; bottom: 0; height: 260px; z-index: 5; background: linear-gradient(rgba(13,13,13,0), #0d0d0d 70%); }
.fadet { position: absolute; left: 0; right: 0; top: 0; z-index: 4; }
.brand { position: absolute; left: 80px; bottom: 60px; font-family: JB; font-weight: 700; font-size: 26px; letter-spacing: 5px; color: rgba(255,255,255,0.4); z-index: 6; }
.brand b { color: #e02b2b; }
`;

function page(body) {
  return `<!doctype html><html><head><meta charset="utf-8"><style>${css}</style></head><body>${body}</body></html>`;
}

function phone(shot, x, y, w, rot = 0, z = 2) {
  const h = Math.round(w * 2392 / 1080) + 36;
  const fs = Math.round(w * 0.045);
  return `<div class="phone" style="left:${x}px;top:${y}px;width:${w + 36}px;height:${h}px;transform:rotate(${rot}deg);z-index:${z}">
    <div class="screen"><img src="shots/${shot}.png"><div class="bar" style="font-size:${fs}px"><span>10:08</span><span class="bat"></span></div></div>
    <div class="cam"></div></div>`;
}

/** A tilted wall of widget tiles (tiles are sized from their own aspect ratio). */
function collage(names, { rot = -32, cols = 6, cell = 250, gap = 34, x = -260, y = 520, scale = 1, opacity = 1, sizes = {} } = {}) {
  let html = `<div style="position:absolute;left:${x}px;top:${y}px;transform:rotate(${rot}deg) scale(${scale});transform-origin:0 0;opacity:${opacity}">`;
  const colY = new Array(cols).fill(0);
  names.forEach((n) => {
    const c = colY.indexOf(Math.min(...colY));
    const span = sizes[n] || 1;
    const w = cell * span + gap * (span - 1);
    html += `<img class="tile" src="${A(n)}" style="left:${c * (cell + gap)}px;top:${colY[c]}px;width:${w}px">`;
    const img = assetSize(n);
    const h = Math.round(w * img.h / img.w);
    for (let k = 0; k < span && c + k < cols; k++) colY[c + k] = colY[c] + h + gap;
  });
  return html + `</div>`;
}

function assetSize(n) {
  const buf = fs.readFileSync(path.join(dir, "assets", n + ".png"));
  return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20) };
}

const brand = `<div class="fadeb"></div><div class="brand"><b>●</b> DOT WIDGETS</div>`;

const darkTiles = ["d_clock.dotdigital.l", "d_battery.ring", "d_clock.text.words", "d_calendar.life.dots", "d_clock.stacked.hhmm",
  "l_clock.analog.ring", "d_clock.glyph.ring", "d_weather.moon.phase", "l_toggle.wifi", "d_clock.seconds.bar", "d_fun.dice.roll",
  "l_calendar.day.date", "d_learn.pomodoro.ring", "d_clock.binary", "d_battery.fuel.gauge", "l_clock.dotdigital.m", "d_toggle.darkmode",
  "d_calendar.year.progress", "d_clock.minimal.pill", "l_battery.ring", "d_fun.pattern.generator", "d_clock.world.tokyo",
  "d_sensor.lunar.moon", "l_weather.moon.phase", "d_clock.radial.24", "d_toggle.grid.2x2", "d_prod.week.dots", "d_clock.tally.marks",
  "l_clock.minimal.pill", "d_battery.segmented", "d_calendar.month.grid", "d_clock.analog.ring", "l_prod.week.dots", "d_sensor.ram.meter",
  "d_clock.flip.style", "l_clock.world.london", "d_calendar.day.date", "d_sensor.world.dual", "d_prod.search.bar", "l_calendar.month.grid"];

const pages = {
  // 1 — hero wall of widgets
  "01_hero": page(`
    <div class="head"><div class="kicker">DOT-MATRIX WIDGET PACK</div>
      <div class="title">223 widgets.<br><em>One</em> clean<br>home screen.</div></div>
    ${collage(darkTiles, { x: -330, y: 800, cols: 6, cell: 230, gap: 30 })}
    <div class="fadet" style="height:820px;background:linear-gradient(#1b1b1b 0, #1b1b1b 500px, rgba(27,27,27,0))"></div>
    <div class="foot"><span class="pill red">No ads</span><span class="pill">Android 7 and up</span></div>${brand}`),

  // 2 — bento + home-screen phone
  "02_bento": page(`
    <div class="head"><div class="kicker">EVERYTHING INCLUDED</div><div class="title">Just add<br><em>widgets.</em></div></div>
    <div class="card light" style="left:70px;top:540px;width:430px;height:380px"><div class="big">223</div><div class="lbl">dot-matrix<br>widgets</div></div>
    <div class="card" style="left:580px;top:540px;width:430px;height:380px"><div class="big">8</div><div class="lbl">skins for<br>every widget</div></div>
    <div class="card red" style="left:70px;top:970px;width:430px;height:380px"><div class="big">10</div><div class="lbl">one-tap<br>looks</div></div>
    <div class="card" style="left:580px;top:970px;width:430px;height:380px"><div class="big">112</div><div class="lbl">matching<br>wallpapers</div></div>
    <div class="card" style="left:70px;top:1400px;width:940px;height:280px;display:flex;align-items:center;gap:40px">
      <img src="${A("hero_clock_m")}" style="width:430px;border-radius:28px">
      <div class="lbl" style="margin:0">No extra app<br>or widget maker.<br><span style="color:#e02b2b">Pick. Place. Done.</span></div></div>
    ${brand}`),

  // 3 — skins
  "03_skins": page(`
    <div class="head"><div class="kicker">8 SKINS</div><div class="title">One widget.<br><em>Eight</em> looks.</div>
      <div class="sub">Dot, light strip, segment, pixel, terminal, paper, neon and native dot. Every widget supports every skin.</div></div>
    ${["dot", "glyph", "segment", "pixel", "terminal", "paper", "neon", "nothing"].map((s, i) => {
      const label = { dot: "Dot", glyph: "Light strip", segment: "Segment", pixel: "Pixel", terminal: "Terminal", paper: "Paper", neon: "Neon", nothing: "Native dot" }[s];
      const x = 70 + (i % 2) * 480, y = 720 + Math.floor(i / 2) * 270;
      return `<img class="tile" src="${A("skin_" + s)}" style="left:${x}px;top:${y}px;width:450px">
        <div class="pill" style="position:absolute;left:${x + 18}px;top:${y + 160}px;padding:10px 20px;font-size:22px;background:rgba(20,20,20,0.85)">${label}</div>`;
    }).join("")}
    ${brand}`),

  // 4 — customise
  "04_customise": page(`
    <div class="head"><div class="kicker">LIVE CUSTOMISATION</div><div class="title">Make every<br>widget <em>yours.</em></div></div>
    ${phone("09_customize_size_accent", 270, 520, 520, 0)}
    <div class="card" style="left:40px;top:700px;width:270px;padding:26px;z-index:4">
      <div class="lbl" style="margin:0 0 16px;font-size:24px">Accent</div>
      <div style="display:flex;flex-wrap:wrap;gap:14px">${["#e02b2b", "#f2a516", "#b8e000", "#00c2d1", "#ffffff", "#8a5cf6"].map(c => `<span style="width:56px;height:56px;border-radius:50%;background:${c};display:inline-block"></span>`).join("")}</div></div>
    <div class="card light" style="left:780px;top:640px;width:260px;padding:26px;z-index:4">
      <div class="lbl" style="margin:0;font-size:24px">Frames<br>Corners<br>Opacity<br>Theme</div></div>
    <img class="tile" src="${A("frame_glow")}" style="left:40px;top:1180px;width:250px;z-index:4">
    <img class="tile" src="${A("accent_cyan")}" style="left:800px;top:1120px;width:230px;z-index:4">
    ${brand}`),

  // 5 — looks
  "05_looks": page(`
    <div class="head"><div class="kicker">ONE-TAP LOOKS</div><div class="title">Restyle your<br>whole home<br>screen <em>at once.</em></div></div>
    ${phone("23_settings_looks", 300, 760, 520, 0)}
    <img class="tile" src="${A("skin_paper")}" style="left:30px;top:900px;width:360px;z-index:4;transform:rotate(-6deg)">
    <img class="tile" src="${A("skin_neon")}" style="left:720px;top:1080px;width:330px;z-index:4;transform:rotate(5deg)">
    <img class="tile" src="${A("skin_segment")}" style="left:40px;top:1380px;width:340px;z-index:4;transform:rotate(4deg)">
    ${brand}`),

  // 6 — setups + wallpapers
  "06_setups": page(`
    <div class="head"><div class="kicker">SETUPS &amp; WALLPAPERS</div><div class="title">Curated setups.<br><em>112</em> wallpapers.</div>
      <div class="sub">Wallpaper, widgets and accent that match, in one tap.</div></div>
    ${phone("16_setups_minimal_red", 40, 760, 470, -6, 2)}
    ${phone("19_wallpapers_top", 560, 700, 470, 6, 3)}
    ${brand}`),

  // 7 — themes
  "07_themes": page(`
    <div class="head"><div class="kicker">THEMES</div><div class="title">Dark. Light.<br><em>Material You.</em></div>
      <div class="sub">Each widget follows your theme, or choose its own. The accent can follow your wallpaper.</div></div>
    ${["dark", "light", "you"].map((t, c) => {
      const ids = ["clock.dotdigital.m", "clock.analog.ring", "battery.ring", "calendar.year.progress"];
      const x = 50 + c * 335;
      let y = 600, out = "";
      ids.forEach(id => {
        const n = `theme_${t}_${id}`; const s = assetSize(n); const w = 310; const h = Math.round(w * s.h / s.w);
        out += `<img class="tile" src="${A(n)}" style="left:${x}px;top:${y}px;width:${w}px">`; y += h + 22;
      });
      return out;
    }).join("")}
    <div class="foot" style="bottom:150px"><span class="pill">◐ Dark</span><span class="pill light">◑ Light</span><span class="pill green">Material You</span></div>
    ${brand}`),

  // 8 — privacy
  "08_private": page(`
    ${collage(darkTiles.slice().reverse(), { x: -300, y: 1140, cols: 6, cell: 210, gap: 28, opacity: 0.35 })}
    <div class="head"><div class="kicker">PRIVATE BY DESIGN</div><div class="title">No ads.<br>No accounts.<br><em>No tracking.</em></div></div>
    <div class="card" style="left:70px;top:820px;width:450px;height:250px;display:flex;align-items:center"><div class="lbl" style="margin:0">No analytics<br>or crash reports</div></div>
    <div class="card light" style="left:560px;top:820px;width:450px;height:250px;display:flex;align-items:center"><div class="lbl" style="margin:0">Every permission<br>is optional</div></div>
    <div class="card light" style="left:70px;top:1110px;width:450px;height:250px;display:flex;align-items:center"><div class="lbl" style="margin:0">Most widgets<br>work offline</div></div>
    <div class="card red" style="left:560px;top:1110px;width:450px;height:250px;display:flex;align-items:center"><div class="lbl" style="margin:0">Only redraws<br>what changed</div></div>
    ${brand}`),
};

for (const [name, html] of Object.entries(pages)) fs.writeFileSync(path.join(dir, name + ".html"), html);
console.log(Object.keys(pages).join("\n"));
