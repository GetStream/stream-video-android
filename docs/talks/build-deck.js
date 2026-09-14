const pptxgen = require("pptxgenjs");
const fs = require("fs");
const path = require("path");

const assetsDir = path.join(__dirname, "build-assets");
const BG_SECTION = "image/png;base64," + fs.readFileSync(path.join(assetsDir, "bg_section.b64"), "utf8").trim();
const BG_START   = "image/png;base64," + fs.readFileSync(path.join(assetsDir, "bg_start.b64"),   "utf8").trim();
const BG_CONTENT = "image/png;base64," + fs.readFileSync(path.join(assetsDir, "bg_content.b64"), "utf8").trim();
const LOGO_WHITE = "image/svg+xml;base64," + fs.readFileSync(path.join(assetsDir, "logo_white.b64"), "utf8").trim();
const LOGO_GRAD  = "image/svg+xml;base64," + fs.readFileSync(path.join(assetsDir, "logo_gradient.b64"), "utf8").trim();

const C = {
  blue:       "005FFF",
  white:      "FFFFFF",
  navy:       "051733",
  kicker:     "A8C7FA",
  kickerDim:  "6FA0E8",
  divLine:    "1E3A6E",
  cardBg:     "0B2347",
  cardBorder: "1A3D7A",
  green:      "22C55E",
  red:        "EF4444",
  yellow:     "F59E0B",
};

const FONT = "Geist";
const DATE = "Aug 2026";

function addBrandmark(slide) {
  slide.addImage({ data: LOGO_WHITE, x: 0.25, y: 5.15, w: 0.28, h: 0.28 });
}
function addDateStamp(slide) {
  slide.addText(DATE, { x: 8.8, y: 0.18, w: 1.0, h: 0.25, fontFace: FONT, fontSize: 9, color: C.kicker, align: "right", margin: 0 });
}
function addKicker(slide, text) {
  slide.addText(text.toUpperCase(), { x: 0.45, y: 0.85, w: 6, h: 0.28, fontFace: FONT, fontSize: 10, color: C.kicker, charSpacing: 1.5, margin: 0 });
}
function addTitle(slide, text, opts = {}) {
  slide.addText(text, { x: 0.45, y: 1.1, w: opts.w || 9.1, h: opts.h || 0.82, fontFace: FONT, fontSize: opts.fontSize || 34, color: C.white, margin: 0, ...opts });
}
function addRule(slide, y = 1.95) {
  slide.addShape("rect", { x: 0.45, y, w: 9.1, h: 0.012, fill: { color: C.divLine }, line: { color: C.divLine } });
}
function addChrome(slide, kicker, title, titleOpts = {}) {
  addBrandmark(slide); addDateStamp(slide); addKicker(slide, kicker); addTitle(slide, title, titleOpts); addRule(slide);
}
function addCard(slide, x, y, w, h, opts = {}) {
  slide.addShape("rect", { x, y, w, h, fill: { color: opts.fill || C.cardBg }, line: { color: opts.line || C.cardBorder, pt: 0.75 } });
}
function addAccentCard(slide, x, y, w, h) {
  slide.addShape("rect", { x, y, w, h, fill: { color: C.blue }, line: { color: C.blue } });
}
function addLeftAccent(slide, x, y, h) {
  slide.addShape("rect", { x, y, w: 0.04, h, fill: { color: C.blue }, line: { color: C.blue } });
}
function makeBullets(items, opts = {}) {
  return items.map((item, i) => ({
    text: item,
    options: { bullet: { type: "bullet", indent: 12 }, breakLine: i < items.length - 1, fontFace: FONT, fontSize: opts.fontSize || 15, color: opts.color || C.white, paraSpaceAfter: opts.spacing !== undefined ? opts.spacing : 6 },
  }));
}

let pres = new pptxgen();
pres.layout = "LAYOUT_16x9";
pres.title = "SVC on Android, Debugged";

// ═══ 1. TITLE SLIDE ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_START };
  s.addImage({ data: LOGO_GRAD, x: 0.45, y: 0.38, w: 0.55, h: 0.55 });
  s.addText("stream", { x: 1.1, y: 0.38, w: 2.5, h: 0.55, fontFace: FONT, fontSize: 22, color: C.white, margin: 0, valign: "middle" });
  s.addText("SVC on Android, Debugged", { x: 0.45, y: 1.7, w: 7.5, h: 1.4, fontFace: FONT, fontSize: 52, color: C.white, margin: 0, valign: "top" });
  s.addText("A hardware encoder debugging story", { x: 0.45, y: 3.12, w: 7, h: 0.5, fontFace: FONT, fontSize: 16, color: C.kicker, margin: 0 });
  s.addText("Pratim Mallick  ·  Android SDK @ Stream", { x: 0.45, y: 3.62, w: 5, h: 0.38, fontFace: FONT, fontSize: 13, color: "8AABB8", margin: 0 });
  s.addText("RTC.ON 2026  ·  Kraków", { x: 0.45, y: 4.02, w: 5, h: 0.32, fontFace: FONT, fontSize: 12, color: "6A8A9A", margin: 0 });
}

// ═══ 2. STREAM PRODUCTS ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addBrandmark(s);
  addDateStamp(s);
  s.addText("STREAM", { x: 0.4, y: 0.22, w: 4, h: 0.22, fontFace: FONT, fontSize: 10, color: C.kicker, charSpacing: 1.5, margin: 0 });
  s.addText("Stream Products", { x: 0.4, y: 0.44, w: 9, h: 0.5, fontFace: FONT, fontSize: 28, color: C.white, margin: 0 });

  const products = [
    { icon: "icon-chat.png", shot: "product-chat.png", label: "CHAT MESSAGING", aspect: 607 / 1024 },
    { icon: "icon-video.png", shot: "product-video.png", label: "VIDEO & AUDIO", aspect: 508 / 1024 },
    { icon: "icon-feeds.png", shot: "product-feeds.png", label: "ACTIVITY FEEDS", aspect: 472 / 1024 },
    { icon: "icon-mod.png", shot: "product-mod.png", label: "AUTO MODERATION", aspect: 978 / 1024 },
  ];
  const colW = 2.25, gap = 0.15, startX = 0.4, maxShotH = 3.35, maxShotW = 2.15;
  products.forEach(({ icon, shot, label, aspect }, i) => {
    const x = startX + i * (colW + gap);
    s.addImage({ path: path.join(assetsDir, icon), x: x + (colW - 0.42) / 2, y: 1.05, w: 0.42, h: 0.42 });
    s.addText(label, { x, y: 1.52, w: colW, h: 0.28, fontFace: FONT, fontSize: 10, color: C.kicker, align: "center", charSpacing: 0.6, margin: 0 });
    let shotW = maxShotW, shotH = shotW / aspect;
    if (shotH > maxShotH) { shotH = maxShotH; shotW = shotH * aspect; }
    const sx = x + (colW - shotW) / 2;
    s.addImage({ path: path.join(assetsDir, shot), x: sx, y: 1.88, w: shotW, h: shotH });
  });
}

// ═══ 3. AGENDA ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "Stream  ·  RTC.ON 2026", "Agenda");
  const topics = [
    ["01", "VP9 SVC 101",          "What it is, why we need it"],
    ["02", "The Debugging Story",  "Symptom → Root cause"],
    ["03", "Hardware Capabilities", "Can we query the system?"],
    ["04", "Way Forward",          "Practical solutions"],
  ];
  topics.forEach(([num, label, desc], i) => {
    const y = 2.2 + i * 0.82;
    addAccentCard(s, 0.45, y, 0.44, 0.44);
    s.addText(num, { x: 0.45, y, w: 0.44, h: 0.44, fontFace: FONT, fontSize: 13, color: C.white, align: "center", valign: "middle", margin: 0 });
    s.addText(label, { x: 1.05, y: y + 0.02, w: 5, h: 0.25, fontFace: FONT, fontSize: 14, color: C.white, margin: 0 });
    s.addText(desc, { x: 1.05, y: y + 0.24, w: 5, h: 0.2, fontFace: FONT, fontSize: 11, color: C.kicker, margin: 0 });
  });
}

// ═══ 4. SECTION: VP9 SVC 101 ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_SECTION };
  addBrandmark(s); addDateStamp(s);
  addAccentCard(s, 0.45, 1.8, 0.55, 0.55);
  s.addText("01", { x: 0.45, y: 1.8, w: 0.55, h: 0.55, fontFace: FONT, fontSize: 16, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("VP9 SVC 101", { x: 0.45, y: 2.45, w: 7, h: 1.2, fontFace: FONT, fontSize: 54, color: C.white, margin: 0 });
}

// ═══ 5. WHAT IS SVC? ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "VP9 SVC 101", "What is Scalable Video Coding?");

  addCard(s, 0.35, 2.15, 1.35, 1.75);
  s.addText("01", { x: 0.45, y: 2.25, w: 1.15, h: 0.2, fontFace: FONT, fontSize: 11, color: C.kicker, margin: 0 });
  s.addText("Encode\nonce", { x: 0.45, y: 2.5, w: 1.15, h: 0.7, fontFace: FONT, fontSize: 16, color: C.white, margin: 0 });
  s.addText("1 encoder\n1 stream", { x: 0.45, y: 3.25, w: 1.15, h: 0.5, fontFace: FONT, fontSize: 11, color: "8AABB8", margin: 0 });

  s.addText("→", { x: 1.7, y: 2.8, w: 0.28, h: 0.4, fontFace: FONT, fontSize: 20, color: C.blue, align: "center", valign: "middle", margin: 0 });

  s.addText("L3T3  ·  3 spatial × 3 temporal", { x: 1.98, y: 2.08, w: 3.55, h: 0.2, fontFace: FONT, fontSize: 10, color: C.blue, align: "center", charSpacing: 0.4, margin: 0 });
  const temps = [
    { label: "T0", fps: "7.5" },
    { label: "T1", fps: "15" },
    { label: "T2", fps: "30" },
  ];
  temps.forEach(({ label, fps }, i) => {
    const x = 2.72 + i * 0.88;
    s.addText(label, { x, y: 2.28, w: 0.8, h: 0.16, fontFace: FONT, fontSize: 10, color: C.kicker, align: "center", margin: 0 });
    s.addText(fps, { x, y: 2.42, w: 0.8, h: 0.14, fontFace: FONT, fontSize: 9, color: C.kickerDim, align: "center", margin: 0 });
  });
  const spatials = [
    { id: "S2", res: "720p" },
    { id: "S1", res: "360p" },
    { id: "S0", res: "180p" },
  ];
  spatials.forEach(({ id, res }, r) => {
    const y = 2.6 + r * 0.42;
    s.addText(id, { x: 1.98, y, w: 0.38, h: 0.38, fontFace: FONT, fontSize: 11, color: C.white, align: "right", valign: "middle", margin: 0 });
    temps.forEach((_, c) => {
      const x = 2.72 + c * 0.88;
      addCard(s, x, y, 0.8, 0.38, { fill: C.cardBg, line: C.blue });
      s.addText(res, { x, y, w: 0.8, h: 0.38, fontFace: FONT, fontSize: 10, color: C.white, align: "center", valign: "middle", margin: 0 });
    });
  });

  s.addText("→", { x: 5.4, y: 2.8, w: 0.28, h: 0.4, fontFace: FONT, fontSize: 20, color: C.blue, align: "center", valign: "middle", margin: 0 });

  addCard(s, 5.68, 2.15, 1.55, 1.75, { fill: C.cardBg, line: C.blue });
  s.addText("02", { x: 5.8, y: 2.25, w: 1.3, h: 0.2, fontFace: FONT, fontSize: 11, color: C.kicker, margin: 0 });
  s.addText("SFU drops\nlayers", { x: 5.8, y: 2.5, w: 1.3, h: 0.7, fontFace: FONT, fontSize: 15, color: C.white, margin: 0 });
  s.addText("No decode\nNo re-encode", { x: 5.8, y: 3.25, w: 1.3, h: 0.5, fontFace: FONT, fontSize: 11, color: "8AABB8", margin: 0 });

  s.addText("→", { x: 7.23, y: 2.8, w: 0.28, h: 0.4, fontFace: FONT, fontSize: 20, color: C.blue, align: "center", valign: "middle", margin: 0 });

  addCard(s, 7.52, 2.15, 2.03, 0.8);
  s.addText("S2T2 viewer", { x: 7.52, y: 2.2, w: 2.03, h: 0.38, fontFace: FONT, fontSize: 13, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("720p  ·  30 fps", { x: 7.52, y: 2.52, w: 2.03, h: 0.32, fontFace: FONT, fontSize: 11, color: C.kicker, align: "center", margin: 0 });
  addCard(s, 7.52, 3.1, 2.03, 0.8);
  s.addText("S0T0 viewer", { x: 7.52, y: 3.15, w: 2.03, h: 0.38, fontFace: FONT, fontSize: 13, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("180p  ·  7.5 fps", { x: 7.52, y: 3.47, w: 2.03, h: 0.32, fontFace: FONT, fontSize: 11, color: C.kicker, align: "center", margin: 0 });

  addCard(s, 0.35, 4.1, 4.55, 1.05);
  s.addText("SPATIAL", { x: 0.5, y: 4.2, w: 4.25, h: 0.22, fontFace: FONT, fontSize: 11, color: C.kicker, charSpacing: 1, margin: 0 });
  s.addText("S0 / S1 / S2  ·  180p → 360p → 720p", { x: 0.5, y: 4.48, w: 4.25, h: 0.45, fontFace: FONT, fontSize: 14, color: C.white, margin: 0 });

  addCard(s, 5.1, 4.1, 4.45, 1.05);
  s.addText("TEMPORAL", { x: 5.25, y: 4.2, w: 4.15, h: 0.22, fontFace: FONT, fontSize: 11, color: C.kicker, charSpacing: 1, margin: 0 });
  s.addText("T0 / T1 / T2 on every S  ·  7.5 → 15 → 30 fps", { x: 5.25, y: 4.48, w: 4.15, h: 0.45, fontFace: FONT, fontSize: 14, color: C.white, margin: 0 });
}

// ═══ 6. WHY SVC vs SIMULCAST ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "VP9 SVC 101", "Why SVC? (vs Simulcast)");

  s.addText("SVC", { x: 0.45, y: 2.08, w: 4.2, h: 0.28, fontFace: FONT, fontSize: 16, color: C.green, margin: 0 });
  addCard(s, 0.45, 2.42, 1.35, 0.85);
  s.addText("Phone\n1 encode", { x: 0.45, y: 2.42, w: 1.35, h: 0.85, fontFace: FONT, fontSize: 12, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("→", { x: 1.85, y: 2.6, w: 0.35, h: 0.5, fontFace: FONT, fontSize: 18, color: C.green, align: "center", valign: "middle", margin: 0 });
  addCard(s, 2.25, 2.42, 1.05, 0.85, { fill: C.cardBg, line: C.green });
  s.addText("720p\n360p\n180p", { x: 2.25, y: 2.44, w: 1.05, h: 0.82, fontFace: FONT, fontSize: 11, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("→", { x: 3.35, y: 2.6, w: 0.3, h: 0.5, fontFace: FONT, fontSize: 18, color: C.green, align: "center", valign: "middle", margin: 0 });
  addCard(s, 3.7, 2.42, 0.95, 0.85);
  s.addText("SFU", { x: 3.7, y: 2.42, w: 0.95, h: 0.85, fontFace: FONT, fontSize: 14, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("1 stream  ·  1 encoder  ·  SFU drops layers", { x: 0.45, y: 3.35, w: 4.2, h: 0.28, fontFace: FONT, fontSize: 12, color: C.kicker, margin: 0 });

  s.addText("SIMULCAST", { x: 5.35, y: 2.08, w: 4.2, h: 0.28, fontFace: FONT, fontSize: 16, color: C.kicker, margin: 0 });
  addCard(s, 5.35, 2.42, 1.35, 0.85);
  s.addText("Phone\n3 encodes", { x: 5.35, y: 2.42, w: 1.35, h: 0.85, fontFace: FONT, fontSize: 12, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("→", { x: 6.75, y: 2.42, w: 0.28, h: 0.28, fontFace: FONT, fontSize: 14, color: C.kicker, align: "center", margin: 0 });
  s.addText("→", { x: 6.75, y: 2.7, w: 0.28, h: 0.28, fontFace: FONT, fontSize: 14, color: C.kicker, align: "center", margin: 0 });
  s.addText("→", { x: 6.75, y: 2.98, w: 0.28, h: 0.28, fontFace: FONT, fontSize: 14, color: C.kicker, align: "center", margin: 0 });
  addCard(s, 7.1, 2.38, 0.85, 0.28);
  s.addText("720p", { x: 7.1, y: 2.38, w: 0.85, h: 0.28, fontFace: FONT, fontSize: 11, color: C.white, align: "center", valign: "middle", margin: 0 });
  addCard(s, 7.1, 2.7, 0.85, 0.28);
  s.addText("360p", { x: 7.1, y: 2.7, w: 0.85, h: 0.28, fontFace: FONT, fontSize: 11, color: C.white, align: "center", valign: "middle", margin: 0 });
  addCard(s, 7.1, 3.02, 0.85, 0.28);
  s.addText("180p", { x: 7.1, y: 3.02, w: 0.85, h: 0.28, fontFace: FONT, fontSize: 11, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("→", { x: 8.0, y: 2.6, w: 0.28, h: 0.5, fontFace: FONT, fontSize: 18, color: C.kicker, align: "center", valign: "middle", margin: 0 });
  addCard(s, 8.32, 2.42, 0.95, 0.85);
  s.addText("SFU", { x: 8.32, y: 2.42, w: 0.95, h: 0.85, fontFace: FONT, fontSize: 14, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("3 streams  ·  3 encoders  ·  more CPU", { x: 5.35, y: 3.35, w: 4.2, h: 0.28, fontFace: FONT, fontSize: 12, color: C.kicker, margin: 0 });

  s.addShape("rect", { x: 5.02, y: 2.08, w: 0.012, h: 1.55, fill: { color: C.divLine }, line: { color: C.divLine } });

  addCard(s, 0.45, 3.85, 9.1, 1.05, { fill: C.cardBg, line: C.blue });
  s.addText("On mobile, CPU and battery win. SVC is the better fit — if the encoder can actually produce the layers.", {
    x: 0.65, y: 3.85, w: 8.7, h: 1.05, fontFace: FONT, fontSize: 15, color: C.white, margin: 0, valign: "middle",
  });
}

// ═══ 7. SECTION: THE DEBUGGING STORY ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_SECTION };
  addBrandmark(s); addDateStamp(s);
  addAccentCard(s, 0.45, 1.8, 0.55, 0.55);
  s.addText("02", { x: 0.45, y: 1.8, w: 0.55, h: 0.55, fontFace: FONT, fontSize: 16, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("The Debugging Story", { x: 0.45, y: 2.45, w: 7, h: 1.2, fontFace: FONT, fontSize: 54, color: C.white, margin: 0 });
}

// ═══ 8. SETUP — WORKS ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "The Debugging Story", "The Setup — Everything Works");

  addCard(s, 0.45, 2.2, 2.8, 1.6);
  s.addText("Android\nPublisher\n(VP9 HW)", { x: 0.55, y: 2.35, w: 2.6, h: 1.3, fontFace: FONT, fontSize: 13, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("→  L1T3  →", { x: 3.4, y: 2.75, w: 1.6, h: 0.5, fontFace: FONT, fontSize: 14, color: C.green, align: "center", valign: "middle", margin: 0 });
  addCard(s, 5.15, 2.2, 1.8, 1.6);
  s.addText("SFU", { x: 5.15, y: 2.2, w: 1.8, h: 1.6, fontFace: FONT, fontSize: 16, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("→", { x: 7.1, y: 2.75, w: 0.5, h: 0.5, fontFace: FONT, fontSize: 18, color: C.green, align: "center", valign: "middle", margin: 0 });
  addCard(s, 7.7, 2.2, 1.8, 1.6);
  s.addText("Subscriber\n(1)", { x: 7.7, y: 2.2, w: 1.8, h: 1.6, fontFace: FONT, fontSize: 13, color: C.white, align: "center", valign: "middle", margin: 0 });

  s.addText(makeBullets([
    "1 subscriber → SFU requests L1T3 (1 spatial, 3 temporal)",
    "Hardware VP9 encoder handles this fine",
    "Network quality: excellent",
  ], { fontSize: 14, spacing: 8 }), { x: 0.45, y: 4.1, w: 9.1, h: 1.3, margin: 0, valign: "top" });
}

// ═══ 9. SETUP — BREAKS ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "The Debugging Story", "3rd Participant Joins — Quality Drops");

  addCard(s, 0.45, 2.2, 2.8, 1.6);
  s.addText("Android\nPublisher\n(VP9 HW)", { x: 0.55, y: 2.35, w: 2.6, h: 1.3, fontFace: FONT, fontSize: 13, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("→ L3T3_KEY →", { x: 3.4, y: 2.75, w: 1.7, h: 0.5, fontFace: FONT, fontSize: 13, color: C.red, align: "center", valign: "middle", margin: 0 });
  addCard(s, 5.25, 2.2, 1.6, 1.6);
  s.addText("SFU", { x: 5.25, y: 2.2, w: 1.6, h: 1.6, fontFace: FONT, fontSize: 16, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("→", { x: 7.0, y: 2.3, w: 0.4, h: 0.4, fontFace: FONT, fontSize: 16, color: C.red, align: "center", margin: 0 });
  s.addText("→", { x: 7.0, y: 3.1, w: 0.4, h: 0.4, fontFace: FONT, fontSize: 16, color: C.red, align: "center", margin: 0 });
  addCard(s, 7.5, 2.1, 1.7, 0.7);
  s.addText("Sub 1", { x: 7.5, y: 2.1, w: 1.7, h: 0.7, fontFace: FONT, fontSize: 12, color: C.white, align: "center", valign: "middle", margin: 0 });
  addCard(s, 7.5, 2.95, 1.7, 0.7);
  s.addText("Sub 2", { x: 7.5, y: 2.95, w: 1.7, h: 0.7, fontFace: FONT, fontSize: 12, color: C.white, align: "center", valign: "middle", margin: 0 });

  s.addText(makeBullets([
    "Multiple subscribers → SFU switches to L3T3_KEY (3 spatial, 3 temporal)",
    "SFU quality score drops immediately and never recovers",
  ], { fontSize: 14, spacing: 8 }), { x: 0.45, y: 4.1, w: 9.1, h: 1.3, margin: 0, valign: "top" });
}

// ═══ 10. SYMPTOMS ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "The Debugging Story", "The Symptoms — Hardware VP9 Only");

  s.addText("Observed only on hardware VP9. Software VP9 (libvpx) showed none of these.", {
    x: 0.45, y: 2.05, w: 9.1, h: 0.32, fontFace: FONT, fontSize: 13, color: C.kicker, margin: 0,
  });

  const symptoms = [
    { num: "01", title: "SFU marked the stream as poor", body: "Score fell when the 3rd participant joined — and never recovered" },
    { num: "02", title: "Encoder re-created in a loop", body: "Hardware encoder destroyed and recreated ~every 200ms" },
    { num: "03", title: "Stats looked starved", body: "targetBitrate reported ~71 kbps at 720×1280" },
  ];
  symptoms.forEach(({ num, title, body }, i) => {
    const y = 2.5 + i * 0.95;
    addCard(s, 0.45, y, 9.1, 0.85);
    addAccentCard(s, 0.45, y, 0.7, 0.85);
    s.addText(num, { x: 0.45, y, w: 0.7, h: 0.85, fontFace: FONT, fontSize: 16, color: C.white, align: "center", valign: "middle", margin: 0 });
    s.addText(title, { x: 1.35, y: y + 0.1, w: 7.9, h: 0.32, fontFace: FONT, fontSize: 16, color: C.white, margin: 0 });
    s.addText(body, { x: 1.35, y: y + 0.44, w: 7.9, h: 0.28, fontFace: FONT, fontSize: 13, color: C.kicker, margin: 0 });
  });
}

// ═══ 11. ENCODER THRASHING LOOP — THE PROBLEM ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "Debugging", "The Encoder Thrashing Loop");

  s.addText("QualityScaler flips resolution. Hardware VP9 cannot scale in-place — each flip tears the encoder down twice.", {
    x: 0.45, y: 2.05, w: 9.1, h: 0.32, fontFace: FONT, fontSize: 13, color: C.kicker, margin: 0,
  });

  const steps = [
    { num: "01", tag: "QUALITYSCALER", title: "720p → 540p", body: "QP too high" },
    { num: "02", tag: "HW ENCODER", title: "STOP / START ×2", body: "Surface → ByteBuffer" },
    { num: "03", tag: "GCC / BWE", title: "Bitrate drops", body: "Huge frames" },
    { num: "04", tag: "QUALITYSCALER", title: "540p → 720p", body: "QP too low" },
  ];
  steps.forEach(({ num, tag, title, body }, i) => {
    const x = 0.4 + i * 2.4;
    addCard(s, x, 2.48, 2.15, 1.5);
    addAccentCard(s, x, 2.48, 2.15, 0.08);
    s.addText(num, { x: x + 0.1, y: 2.62, w: 1.95, h: 0.22, fontFace: FONT, fontSize: 11, color: C.kicker, margin: 0 });
    s.addText(tag, { x: x + 0.1, y: 2.84, w: 1.95, h: 0.2, fontFace: FONT, fontSize: 9, color: C.kickerDim, charSpacing: 0.8, margin: 0 });
    s.addText(title, { x: x + 0.1, y: 3.06, w: 1.95, h: 0.42, fontFace: FONT, fontSize: 14, color: C.white, margin: 0 });
    s.addText(body, { x: x + 0.1, y: 3.5, w: 1.95, h: 0.3, fontFace: FONT, fontSize: 12, color: "8AABB8", margin: 0 });
    if (i < 3) {
      s.addText("→", { x: x + 2.08, y: 3.01, w: 0.38, h: 0.4, fontFace: FONT, fontSize: 20, color: C.blue, align: "center", valign: "middle", margin: 0 });
    }
  });

  s.addShape("rect", { x: 1.42, y: 4.14, w: 0.03, h: 0.22, fill: { color: C.blue }, line: { color: C.blue } });
  s.addShape("rect", { x: 8.62, y: 3.98, w: 0.03, h: 0.38, fill: { color: C.blue }, line: { color: C.blue } });
  s.addShape("rect", { x: 1.42, y: 4.34, w: 7.23, h: 0.03, fill: { color: C.blue }, line: { color: C.blue } });
  s.addText("←   back to 720p   ·   repeats every 5–10 seconds", {
    x: 1.7, y: 4.4, w: 6.7, h: 0.26, fontFace: FONT, fontSize: 12, color: C.blue, align: "center", margin: 0,
  });

  addCard(s, 0.45, 4.78, 9.1, 0.52, { fill: C.cardBg, line: C.cardBorder });
  s.addText("Software VP9 scales in-place. Hardware cannot — so QualityScaler turns a QP adjustment into a full encoder recreate loop.", {
    x: 0.65, y: 4.78, w: 8.7, h: 0.52, fontFace: FONT, fontSize: 13, color: C.white, margin: 0, valign: "middle",
  });
}

// ═══ 12. QUALITYSCALER OFF — STOPPED THE LOOP, NOT THE PROBLEM ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "Debugging", "QualityScaler Off Stopped the Loop");

  addCard(s, 0.45, 2.15, 4.45, 2.35);
  addAccentCard(s, 0.45, 2.15, 0.08, 2.35);
  s.addText("WHAT WE DID", { x: 0.7, y: 2.28, w: 4.0, h: 0.24, fontFace: FONT, fontSize: 11, color: C.green, charSpacing: 1.2, margin: 0 });
  s.addText("Turned QualityScaler off", { x: 0.7, y: 2.56, w: 4.0, h: 0.36, fontFace: FONT, fontSize: 18, color: C.white, margin: 0 });
  s.addText(makeBullets([
    "degradationPreference disabled the scaler",
    "720p ↔ 540p flipping stopped",
    "Encoder recreate loop stopped",
  ], { fontSize: 14, spacing: 6 }), { x: 0.7, y: 3.05, w: 4.0, h: 1.25, margin: 0, valign: "top" });

  addCard(s, 5.1, 2.15, 4.45, 2.35);
  addAccentCard(s, 5.1, 2.15, 0.08, 2.35);
  s.addText("WHAT WAS LEFT", { x: 5.35, y: 2.28, w: 4.0, h: 0.24, fontFace: FONT, fontSize: 11, color: C.red, charSpacing: 1.2, margin: 0 });
  s.addText("SFU still said poor", { x: 5.35, y: 2.56, w: 4.0, h: 0.36, fontFace: FONT, fontSize: 18, color: C.white, margin: 0 });
  s.addText(makeBullets([
    "Quality score never recovered",
    "Resolution switch was gone",
    "The stream was still broken",
  ], { fontSize: 14, spacing: 6 }), { x: 5.35, y: 3.05, w: 4.0, h: 1.25, margin: 0, valign: "top" });

  addCard(s, 0.45, 4.7, 9.1, 0.6, { fill: C.cardBg, line: C.blue });
  s.addText("The oscillation was real — and we stopped it. It was not the reason the SFU marked this stream as poor.", {
    x: 0.65, y: 4.7, w: 8.7, h: 0.6, fontFace: FONT, fontSize: 15, color: C.white, margin: 0, valign: "middle",
  });
}

// ═══ 13. TARGETBITRATE WAS ALSO NOT THE PROBLEM ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "Debugging", "71 kbps Was a WebRTC Stats Bug");

  addCard(s, 0.45, 2.15, 4.45, 2.2);
  s.addText("WHAT STATS SAID", { x: 0.65, y: 2.28, w: 4.05, h: 0.22, fontFace: FONT, fontSize: 11, color: C.red, charSpacing: 1.2, margin: 0 });
  s.addText("71 kbps", { x: 0.65, y: 2.55, w: 4.05, h: 0.7, fontFace: FONT, fontSize: 40, color: C.white, margin: 0 });
  s.addText("targetBitrate at 720×1280\nLooked like the encoder was starved", { x: 0.65, y: 3.3, w: 4.05, h: 0.8, fontFace: FONT, fontSize: 14, color: C.kicker, margin: 0 });

  addCard(s, 5.1, 2.15, 4.45, 2.2, { fill: C.cardBg, line: C.blue });
  s.addText("WHAT WAS ACTUALLY SENT", { x: 5.3, y: 2.28, w: 4.05, h: 0.22, fontFace: FONT, fontSize: 11, color: C.green, charSpacing: 1.2, margin: 0 });
  s.addText("~1.4 Mbps", { x: 5.3, y: 2.55, w: 4.05, h: 0.7, fontFace: FONT, fontSize: 40, color: C.white, margin: 0 });
  s.addText("bytesSent  ·  SFU cap 1.5 Mbps\nBWE ~3 Mbps  —  not starved", { x: 5.3, y: 3.3, w: 4.05, h: 0.8, fontFace: FONT, fontSize: 14, color: C.kicker, margin: 0 });

  addCard(s, 0.45, 4.5, 9.1, 0.85, { fill: C.cardBg, line: C.blue });
  s.addText("send_statistics_proxy.cc reports GetSpatialLayerSum(*simulcast_index) — the base spatial layer only, on single-SSRC VP9 SVC. The 71 kbps figure was a reporting artifact. Also not the real problem.", {
    x: 0.65, y: 4.5, w: 8.7, h: 0.85, fontFace: FONT, fontSize: 14, color: C.white, margin: 0, valign: "middle",
  });
}

// ═══ 14. ROOT CAUSE ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "Debugging", "Asked for 3 Layers — Encoder Sent 1");

  s.addText("ASKED FOR", { x: 0.45, y: 2.08, w: 4.2, h: 0.22, fontFace: FONT, fontSize: 11, color: C.green, charSpacing: 1.2, margin: 0 });
  s.addText("L3T3_KEY", { x: 0.45, y: 2.28, w: 4.2, h: 0.32, fontFace: FONT, fontSize: 16, color: C.white, margin: 0 });

  const asked = [
    { y: 2.68, w: 4.2, label: "S2  ·  720p", sub: "top layer" },
    { y: 3.38, w: 3.2, label: "S1  ·  360p", sub: "mid layer" },
    { y: 4.08, w: 2.2, label: "S0  ·  180p", sub: "base layer" },
  ];
  asked.forEach(({ y, w, label, sub }) => {
    const x = 0.45 + (4.2 - w) / 2;
    addCard(s, x, y, w, 0.6, { fill: C.cardBg, line: C.green });
    s.addText(label, { x, y: y + 0.04, w, h: 0.3, fontFace: FONT, fontSize: 14, color: C.white, align: "center", margin: 0 });
    s.addText(sub, { x, y: y + 0.32, w, h: 0.22, fontFace: FONT, fontSize: 11, color: C.kicker, align: "center", margin: 0 });
  });

  s.addText("→", { x: 4.7, y: 3.35, w: 0.6, h: 0.5, fontFace: FONT, fontSize: 28, color: C.blue, align: "center", valign: "middle", margin: 0 });

  s.addText("ENCODER PRODUCED", { x: 5.35, y: 2.08, w: 4.2, h: 0.22, fontFace: FONT, fontSize: 11, color: C.red, charSpacing: 1.2, margin: 0 });
  s.addText("1 flat stream", { x: 5.35, y: 2.28, w: 4.2, h: 0.32, fontFace: FONT, fontSize: 16, color: C.white, margin: 0 });

  addCard(s, 5.35, 2.68, 4.2, 2.0, { fill: "3A1A1A", line: C.red });
  s.addText("720p only", { x: 5.35, y: 3.15, w: 4.2, h: 0.45, fontFace: FONT, fontSize: 28, color: C.white, align: "center", margin: 0 });
  s.addText("No S1  ·  No S0  ·  Nothing to split", { x: 5.55, y: 3.65, w: 3.8, h: 0.35, fontFace: FONT, fontSize: 13, color: C.kicker, align: "center", margin: 0 });
  s.addText("~1.4 Mbps sent  —  not starved", { x: 5.55, y: 4.05, w: 3.8, h: 0.3, fontFace: FONT, fontSize: 12, color: "8AABB8", align: "center", margin: 0 });

  s.addText("SFU expected three layers. Hardware sent one. Stream marked poor.", {
    x: 0.45, y: 4.82, w: 9.1, h: 0.35, fontFace: FONT, fontSize: 14, color: C.kicker, margin: 0,
  });
}

// ═══ 15. THE REAL PROBLEM ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "Debugging", "The Real Problem");

  s.addText("We stopped the thrashing loop. The 71 kbps figure was a stats bug. The SFU was still right — the stream was broken.", {
    x: 0.45, y: 2.05, w: 9.1, h: 0.32, fontFace: FONT, fontSize: 13, color: C.kicker, margin: 0,
  });

  addCard(s, 0.45, 2.5, 4.45, 2.15, { fill: C.cardBg, line: C.red });
  addAccentCard(s, 0.45, 2.5, 0.08, 2.15);
  s.addText("HARDWARE VP9", { x: 0.7, y: 2.62, w: 4.0, h: 0.22, fontFace: FONT, fontSize: 11, color: C.red, charSpacing: 1.2, margin: 0 });
  s.addText("Temporal layers only", { x: 0.7, y: 2.88, w: 4.0, h: 0.32, fontFace: FONT, fontSize: 18, color: C.white, margin: 0 });
  s.addText("Asked L3T3_KEY. Encoder sent one flat 720p stream. Wrapper hardcodes num_spatial_layers = 1. SFU expected three layers and marked the stream poor.", {
    x: 0.7, y: 3.28, w: 4.0, h: 1.2, fontFace: FONT, fontSize: 14, color: "8AABB8", margin: 0,
  });

  addCard(s, 5.1, 2.5, 4.45, 2.15);
  addAccentCard(s, 5.1, 2.5, 0.08, 2.15);
  s.addText("SOFTWARE VP9", { x: 5.35, y: 2.62, w: 4.0, h: 0.22, fontFace: FONT, fontSize: 11, color: C.green, charSpacing: 1.2, margin: 0 });
  s.addText("This never happens", { x: 5.35, y: 2.88, w: 4.0, h: 0.32, fontFace: FONT, fontSize: 18, color: C.white, margin: 0 });
  s.addText("libvpx produces real L3T3 — all three spatial layers. It scales in-place. No encoder teardown. SFU quality stays stable.", {
    x: 5.35, y: 3.28, w: 4.0, h: 1.2, fontFace: FONT, fontSize: 14, color: "8AABB8", margin: 0,
  });

  addCard(s, 0.45, 4.85, 9.1, 0.5, { fill: C.cardBg, line: C.blue });
  s.addText("The encoder cannot produce spatial layers. That is what the SFU was scoring. Everything else was noise.", {
    x: 0.65, y: 4.85, w: 8.7, h: 0.5, fontFace: FONT, fontSize: 14, color: C.white, margin: 0, valign: "middle",
  });
}

// ═══ 16. SECTION: QUERYING HARDWARE ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_SECTION };
  addBrandmark(s); addDateStamp(s);
  addAccentCard(s, 0.45, 1.8, 0.55, 0.55);
  s.addText("03", { x: 0.45, y: 1.8, w: 0.55, h: 0.55, fontFace: FONT, fontSize: 16, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("Querying Hardware\nCapabilities", { x: 0.45, y: 2.45, w: 7, h: 1.4, fontFace: FONT, fontSize: 48, color: C.white, margin: 0 });
}

// ═══ 17. MEDIACODEC SVC SUPPORT ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "Hardware Capabilities", "MediaCodec SVC Support — Temporal Only");

  const cols = [
    { head: "What You Can Query", items: ["Supported resolutions & bitrates", "Profile levels", "Temporal layering schemas (KEY_TEMPORAL_LAYERING)"] },
    { head: "What You Can't", items: ["Spatial layer support", "\"Does this encoder do L3T3?\"", "Multi-resolution SVC capability"] },
  ];
  cols.forEach(({ head, items }, i) => {
    const x = 0.45 + i * 4.7;
    addLeftAccent(s, x, 2.08, 0.38);
    s.addText(head, { x: x + 0.15, y: 2.08, w: 4.3, h: 0.38, fontFace: FONT, fontSize: 14, color: i === 0 ? C.green : C.red, margin: 0 });
    s.addText(makeBullets(items, { fontSize: 14, spacing: 7 }), { x: x + 0.15, y: 2.55, w: 4.3, h: 1.8, margin: 0, valign: "top" });
    if (i === 0) s.addShape("rect", { x: 5.02, y: 2.05, w: 0.012, h: 2.5, fill: { color: C.divLine }, line: { color: C.divLine } });
  });

  addCard(s, 0.45, 4.4, 9.1, 0.9, { fill: C.cardBg, line: C.blue });
  s.addText("Android 17 (API 37) adds getSupportedLayeringSchemas() — but only returns temporal schemas like webrtc.svc.l1tN. The platform confirms: hardware SVC = temporal only.", {
    x: 0.65, y: 4.5, w: 8.7, h: 0.7, fontFace: FONT, fontSize: 12.5, color: C.kicker, margin: 0, valign: "middle",
  });
}

// ═══ 18. SECTION: WAY FORWARD ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_SECTION };
  addBrandmark(s); addDateStamp(s);
  addAccentCard(s, 0.45, 1.8, 0.55, 0.55);
  s.addText("04", { x: 0.45, y: 1.8, w: 0.55, h: 0.55, fontFace: FONT, fontSize: 16, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("Way Forward", { x: 0.45, y: 2.45, w: 7, h: 1.2, fontFace: FONT, fontSize: 54, color: C.white, margin: 0 });
}

// ═══ 19. SOFTWARE VP9 PROS & CONS ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "Way Forward", "Software VP9 (libvpx) — Pros & Cons");

  const cols = [
    { head: "✓ Pros", accent: C.green, items: ["True L3T3 — all 3 spatial layers", "No encoder re-creation issues", "SFU quality stays stable"] },
    { head: "✗ Cons", accent: C.red, items: ["High CPU → qualityLimitationReason: cpu", "Battery drains significantly faster", "Frame rate may drop under load"] },
  ];
  cols.forEach(({ head, accent, items }, i) => {
    const x = 0.45 + i * 4.7;
    addLeftAccent(s, x, 2.08, 0.38);
    s.addText(head, { x: x + 0.15, y: 2.08, w: 4.3, h: 0.38, fontFace: FONT, fontSize: 14, color: accent, margin: 0 });
    s.addText(makeBullets(items, { fontSize: 14, spacing: 8 }), { x: x + 0.15, y: 2.55, w: 4.3, h: 2.0, margin: 0, valign: "top" });
    if (i === 0) s.addShape("rect", { x: 5.02, y: 2.05, w: 0.012, h: 2.5, fill: { color: C.divLine }, line: { color: C.divLine } });
  });

  s.addText("Today's only complete fix for spatial SVC — but the CPU/battery cost is real on mobile.", { x: 0.45, y: 4.8, w: 9.1, h: 0.4, fontFace: FONT, fontSize: 13, color: C.kicker, margin: 0 });
}

// ═══ 20. PRODUCTION DATA ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "Way Forward", "Production Data — Android Is the Outlier");

  s.addText("190 Android livestreams from a livestreaming customer — July 2026", { x: 0.45, y: 2.05, w: 9.1, h: 0.35, fontFace: FONT, fontSize: 13, color: C.kicker, margin: 0 });

  const rows = [
    [{ text: "Host Type", options: { color: C.kicker, fill: { color: "0B2347" } } },
     { text: "Median Quality", options: { color: C.kicker, fill: { color: "0B2347" } } },
     { text: "Poor (<70)", options: { color: C.kicker, fill: { color: "0B2347" } } }],
    ["Android app", "89", { text: "41%", options: { color: C.red } }],
    ["iOS app", "98", { text: "7%", options: { color: C.green } }],
    ["Web browser", "98", { text: "6%", options: { color: C.green } }],
    ["OBS / external", "99", { text: "0.4%", options: { color: C.green } }],
  ];
  s.addTable(rows, {
    x: 0.45, y: 2.45, w: 5.5, h: 2.0,
    fontFace: FONT, fontSize: 13, color: C.white,
    border: { pt: 0.5, color: C.cardBorder },
    fill: { color: C.cardBg },
    rowH: 0.4,
    colW: [2.2, 1.8, 1.5],
  });

  addCard(s, 6.2, 2.45, 3.35, 2.0, { fill: C.cardBg, line: C.red });
  s.addShape("rect", { x: 6.2, y: 2.45, w: 3.35, h: 0.06, fill: { color: C.red }, line: { color: C.red } });
  s.addText("Android VP9 breakdown", { x: 6.38, y: 2.58, w: 3.0, h: 0.25, fontFace: FONT, fontSize: 11, color: C.red, margin: 0 });
  s.addText("HW VP9 (Samsung)", { x: 6.38, y: 2.88, w: 2.2, h: 0.22, fontFace: FONT, fontSize: 12, color: C.white, margin: 0 });
  s.addText("100% poor", { x: 8.5, y: 2.88, w: 1.0, h: 0.22, fontFace: FONT, fontSize: 12, color: C.red, align: "right", margin: 0 });
  s.addText("Median quality: 30", { x: 6.38, y: 3.12, w: 3.0, h: 0.2, fontFace: FONT, fontSize: 11, color: C.kickerDim, margin: 0 });
  s.addText("SW VP9 (libvpx)", { x: 6.38, y: 3.45, w: 2.2, h: 0.22, fontFace: FONT, fontSize: 12, color: C.white, margin: 0 });
  s.addText("28% poor", { x: 8.5, y: 3.45, w: 1.0, h: 0.22, fontFace: FONT, fontSize: 12, color: C.yellow, align: "right", margin: 0 });
  s.addText("CPU/battery limited", { x: 6.38, y: 3.69, w: 3.0, h: 0.2, fontFace: FONT, fontSize: 11, color: C.kickerDim, margin: 0 });

  addCard(s, 0.45, 4.65, 9.1, 0.7, { fill: C.cardBg, line: C.blue });
  s.addText("Neither VP9 path works reliably on Android. Hardware can't do SVC; software drains the device. A 720p cap doesn't help — the encoder is the bottleneck, not resolution.", {
    x: 0.65, y: 4.72, w: 8.7, h: 0.55, fontFace: FONT, fontSize: 12.5, color: C.white, margin: 0, valign: "middle",
  });
}

// ═══ 21. BROADCASTER MODE ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "Way Forward", "Broadcaster Mode — Server-Side Quality Ladder");

  addCard(s, 0.45, 2.2, 2.4, 1.4);
  s.addText("Android\nPublisher", { x: 0.55, y: 2.35, w: 2.2, h: 0.6, fontFace: FONT, fontSize: 13, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("1 clean HW stream\n(H.264 or VP8)", { x: 0.55, y: 2.95, w: 2.2, h: 0.5, fontFace: FONT, fontSize: 11, color: C.kicker, align: "center", valign: "middle", margin: 0 });

  s.addText("→", { x: 3.0, y: 2.7, w: 0.5, h: 0.5, fontFace: FONT, fontSize: 22, color: C.blue, align: "center", valign: "middle", margin: 0 });

  addCard(s, 3.6, 2.2, 2.8, 1.4, { fill: C.cardBg, line: C.blue });
  s.addShape("rect", { x: 3.6, y: 2.2, w: 2.8, h: 0.06, fill: { color: C.blue }, line: { color: C.blue } });
  s.addText("Stream Server\n(transcodes)", { x: 3.7, y: 2.4, w: 2.6, h: 0.6, fontFace: FONT, fontSize: 13, color: C.white, align: "center", valign: "middle", margin: 0 });
  s.addText("Generates quality\nladder server-side", { x: 3.7, y: 3.0, w: 2.6, h: 0.45, fontFace: FONT, fontSize: 11, color: C.kicker, align: "center", valign: "middle", margin: 0 });

  s.addText("→", { x: 6.55, y: 2.4, w: 0.4, h: 0.3, fontFace: FONT, fontSize: 16, color: C.green, align: "center", margin: 0 });
  s.addText("→", { x: 6.55, y: 2.75, w: 0.4, h: 0.3, fontFace: FONT, fontSize: 16, color: C.green, align: "center", margin: 0 });
  s.addText("→", { x: 6.55, y: 3.1, w: 0.4, h: 0.3, fontFace: FONT, fontSize: 16, color: C.green, align: "center", margin: 0 });

  addCard(s, 7.1, 2.15, 2.4, 0.4);
  s.addText("720p viewer", { x: 7.2, y: 2.15, w: 2.2, h: 0.4, fontFace: FONT, fontSize: 11.5, color: C.white, align: "center", valign: "middle", margin: 0 });
  addCard(s, 7.1, 2.65, 2.4, 0.4);
  s.addText("360p viewer", { x: 7.2, y: 2.65, w: 2.2, h: 0.4, fontFace: FONT, fontSize: 11.5, color: C.white, align: "center", valign: "middle", margin: 0 });
  addCard(s, 7.1, 3.15, 2.4, 0.4);
  s.addText("180p viewer", { x: 7.2, y: 3.15, w: 2.2, h: 0.4, fontFace: FONT, fontSize: 11.5, color: C.white, align: "center", valign: "middle", margin: 0 });

  const benefits = [
    "Phone publishes one stream on its best hardware codec — no SVC needed",
    "Server generates the adaptive quality ladder viewers need",
    "No CPU drain, no missing layers, no encoder re-creation",
    "SDK option — opt-in for livestream / broadcast use cases",
  ];
  benefits.forEach((text, i) => {
    const y = 3.75 + i * 0.38;
    s.addText("•", { x: 0.45, y, w: 0.25, h: 0.34, fontFace: FONT, fontSize: 16, color: C.blue, margin: 0, valign: "middle" });
    s.addText(text, { x: 0.75, y, w: 8.8, h: 0.34, fontFace: FONT, fontSize: 14, color: C.white, margin: 0, valign: "middle" });
  });
}

// ═══ 22. TAKEAWAYS ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_CONTENT };
  addChrome(s, "SVC on Android, Debugged", "Takeaways");
  const takeaways = [
    "HW VP9 → temporal SVC only, spatial layers silently missing",
    "SW VP9 → real SVC, but CPU/battery cost too high on mobile",
    "Neither path works — 100% HW failure, 28% SW failure in prod",
    "Broadcaster mode = phone sends 1 stream, server makes the ladder",
    "No API to query spatial SVC — you find out from the bitstream",
  ];
  takeaways.forEach((text, i) => {
    const y = 2.15 + i * 0.58;
    s.addText("•", { x: 0.45, y, w: 0.3, h: 0.5, fontFace: FONT, fontSize: 22, color: C.blue, margin: 0, valign: "middle" });
    s.addText(text, { x: 0.85, y, w: 8.7, h: 0.5, fontFace: FONT, fontSize: 16, color: C.white, margin: 0, valign: "middle" });
  });
}

// ═══ 23. THANK YOU ═══
{
  let s = pres.addSlide();
  s.background = { data: BG_SECTION };
  s.addImage({ data: LOGO_GRAD, x: 6.4, y: 2.2, w: 3.35, h: 3.35, transparency: 18 });
  s.addText("Thank you", { x: 0.55, y: 1.65, w: 6.0, h: 1.5, fontFace: FONT, fontSize: 64, color: C.white, margin: 0 });
  s.addText("Questions?", { x: 0.55, y: 3.18, w: 5.0, h: 0.55, fontFace: FONT, fontSize: 18, color: C.kicker, margin: 0 });
  s.addText("Pratim Mallick  ·  Stream", { x: 0.55, y: 3.82, w: 5.0, h: 0.4, fontFace: FONT, fontSize: 14, color: "8AABB8", margin: 0 });
  s.addImage({ data: LOGO_WHITE, x: 0.4, y: 5.12, w: 0.28, h: 0.28 });
  addDateStamp(s);
}

const outPath = path.join(__dirname, "SVC_on_Android_Debugged_RTC_ON_2026.pptx");
pres.writeFile({ fileName: outPath })
  .then(() => console.log("✅ " + outPath))
  .catch(e => { console.error(e); process.exit(1); });
