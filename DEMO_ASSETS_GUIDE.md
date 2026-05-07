# 🎥 Demo Assets Creation Guide

> **Goal:** Show developers the value in under 30 seconds

---

## 📋 Required Assets Checklist

- [ ] Demo GIF (30-45 seconds) — for README and landing page
- [ ] 5 Screenshots — for marketplace and documentation
- [ ] 2-minute demo video — for Product Hunt and social media
- [ ] Logo variations — for branding consistency
- [ ] Social media preview images — for link sharing

---

## 🎬 1. Demo GIF (CRITICAL)

### What to Show

**Perfect 30-second flow:**

```
1. [0-5s]   Open a Java/JS file with issues
2. [5-10s]  Right-click → "Analyze with AESTHENIXAI" or click toolbar button
3. [10-15s] Loading indicator → Results appear
4. [15-20s] Inline highlights on problematic lines
5. [20-25s] Click issue → Problems panel shows details
6. [25-30s] Click "Apply fix" → Code updates automatically
```

### Recording Tools

**Windows:**

- **ScreenToGif** (recommended) — free, easy, small files
  - Download: https://www.screentogif.com/
  - Settings: 15 FPS, 1280x720

**Mac:**

- **Kap** (recommended) — free, beautiful output
  - Download: https://getkap.co/
  - Settings: 60 FPS → export as GIF at 15 FPS

**Cross-platform:**

- **LICEcap** — simple, lightweight
  - Download: https://www.cockos.com/licecap/

### Recording Settings

```
Resolution: 1280x720 (or 1920x1080 scaled down)
Frame rate: 10-15 FPS (smaller file size)
Duration: 30-45 seconds max
File size: < 5 MB for GitHub, < 10 MB for landing page
Format: GIF or MP4 (GIF for GitHub, MP4 for landing)
```

### Recording Tips

1. **Clean workspace:**
   - Close unnecessary tabs
   - Hide personal info
   - Use dark theme (matches branding)
   - Increase font size (14-16pt)

2. **Smooth movements:**
   - Move cursor slowly
   - Pause 1-2 seconds on key moments
   - No sudden jumps

3. **Clear demonstration:**
   - Use obvious code issues (nested loops, long methods)
   - Show severity indicators (red/yellow/blue)
   - Highlight the "wow moment" (instant results)

### Example Code for Demo

**Java (shows multiple issue types):**

```java
public class UserService {
    // Long method + nested loops + no exception handling
    public String generateReport(List<User> users) {
        String result = "";  // Performance issue
        for (User user : users) {
            for (Order order : user.getOrders()) {
                result += order.toString();  // String concatenation in loop
            }
        }
        return result;
    }
}
```

**JavaScript (shows common issues):**

```javascript
function processData(data) {
  // No input validation
  for (let i = 0; i < data.length; i++) {
    for (let j = 0; j < data[i].items.length; j++) {
      console.log(data[i].items[j]); // Nested loop
    }
  }
}
```

### Post-Processing

**Optimize GIF size:**

```bash
# Using gifsicle (install via brew/apt)
gifsicle -O3 --colors 256 input.gif -o output.gif

# Or use online tools:
# - https://ezgif.com/optimize
# - https://gifcompressor.com/
```

**Convert to MP4 (smaller, better quality):**

```bash
# Using ffmpeg
ffmpeg -i input.gif -movflags faststart -pix_fmt yuv420p -vf "scale=trunc(iw/2)*2:trunc(ih/2)*2" output.mp4
```

---

## 📸 2. Screenshots (Minimum 5)

### Required Screenshots

#### 1. VS Code Analysis View

**Shows:** Code editor with inline issue highlights

**Setup:**

- Open file with 3-4 issues
- Run analysis
- Show inline decorations (red/yellow squiggles)
- Show hover tooltip with issue details
- Capture at 1920x1080

**Annotations to add:**

- Arrow pointing to inline highlight: "Issues highlighted in real-time"
- Arrow pointing to severity icon: "Color-coded by severity"

#### 2. GitHub PR Review

**Shows:** GitHub PR with check run and annotations

**Setup:**

- Open a PR with AESTHENIXAI review
- Show "Checks" tab with pass/fail status
- Show "Files changed" tab with inline annotations
- Capture full browser window

**Annotations to add:**

- Arrow pointing to check status: "Automatic quality gate"
- Arrow pointing to annotation: "Inline suggestions on changed lines"

#### 3. Problems Panel

**Shows:** VS Code Problems panel with categorized issues

**Setup:**

- Show Problems panel at bottom
- Group by severity (Errors, Warnings, Info)
- Show file path and line numbers
- Capture at 1920x1080

**Annotations to add:**

- Arrow pointing to severity grouping: "Organized by priority"
- Arrow pointing to issue count: "Track progress"

#### 4. Score Dashboard

**Shows:** Quality score visualization

**Setup:**

- Show score card with 74/100 or similar
- Show issue breakdown (2 Critical, 3 Warnings, 1 Suggestion)
- Show trend graph if available
- Capture at 1920x1080

**Annotations to add:**

- Arrow pointing to score: "Blended AI + static analysis"
- Arrow pointing to breakdown: "Actionable insights"

#### 5. Diff View (Before/After)

**Shows:** Side-by-side comparison with suggested fixes

**Setup:**

- Show original code on left
- Show improved code on right
- Highlight changed lines
- Capture at 1920x1080

**Annotations to add:**

- Arrow pointing to fix: "AI-suggested improvements"
- Arrow pointing to diff: "See exactly what changed"

### Screenshot Tools

**Windows:**

- **ShareX** (recommended) — free, powerful
  - Download: https://getsharex.com/
  - Built-in annotation tools

**Mac:**

- **CleanShot X** (paid, best quality)
  - Download: https://cleanshot.com/
- **Cmd+Shift+4** (built-in, free)

**Cross-platform:**

- **Flameshot** — free, good annotation tools
  - Download: https://flameshot.org/

### Screenshot Specs

```
Resolution: 1920x1080 or 2560x1440
Format: PNG (better quality than JPG)
Theme: Dark (matches branding)
Font size: 14-16pt (readable)
Annotations: Arrows, labels, highlights
File size: < 500 KB each (optimize with TinyPNG)
```

### Annotation Tips

1. **Use consistent styling:**
   - Arrow color: #7f5af0 (brand accent)
   - Text color: #ffffff
   - Background: semi-transparent dark overlay
   - Font: Inter or system default

2. **Keep it minimal:**
   - 1-2 annotations per screenshot
   - Short labels (3-5 words)
   - Don't clutter the image

3. **Highlight key features:**
   - Circle important UI elements
   - Draw arrows to show flow
   - Add subtle glow to focal points

---

## 🎥 3. Demo Video (2 minutes)

### Script Structure

**[0:00-0:15] Hook + Problem**

```
Visual: Slow PR review process, waiting, frustrated developer
Voiceover: "Code review is slow. PRs sit for days. Issues get missed until production."
```

**[0:15-0:30] Solution**

```
Visual: AESTHENIXAI logo, tagline
Voiceover: "AESTHENIXAI reviews your code in under 5 seconds. Catch risky code before it hits review."
```

**[0:30-1:00] VS Code Demo**

```
Visual: Screen recording of VS Code workflow
Voiceover: "Open any file. Click Analyze. See results instantly. Issues highlighted inline. One-click fixes."
```

**[1:00-1:30] GitHub Integration**

```
Visual: Screen recording of GitHub PR workflow
Voiceover: "Every PR gets an automatic review. Inline annotations. Quality scores. Pass/fail gates."
```

**[1:30-1:45] Social Proof**

```
Visual: Stats, testimonials, GitHub stars
Voiceover: "Built for fast-moving dev teams. Free during early access."
```

**[1:45-2:00] Call to Action**

```
Visual: Landing page with install buttons
Voiceover: "Install the VS Code extension. Add the GitHub App. Start shipping cleaner PRs today."
```

### Recording Tools

**Easy (no editing):**

- **Loom** — record + share instantly
  - https://www.loom.com/
  - Free tier: 5 min videos

**Professional (requires editing):**

- **OBS Studio** — free, powerful
  - https://obsproject.com/
  - Record screen + webcam + audio
- **ScreenFlow** (Mac, paid)
  - https://www.telestream.net/screenflow/
  - Best for polished demos

### Video Specs

```
Resolution: 1920x1080 (1080p)
Frame rate: 30 FPS
Format: MP4 (H.264)
Audio: 128 kbps AAC
Duration: 1:30 - 2:00
File size: < 50 MB
Aspect ratio: 16:9
```

### Recording Tips

1. **Audio quality matters:**
   - Use external mic (Blue Yeti, Rode NT-USB)
   - Record in quiet room
   - Speak clearly and slowly
   - Add background music (low volume)

2. **Visual polish:**
   - Clean desktop
   - Hide notifications
   - Use dark theme
   - Increase font size
   - Smooth cursor movements

3. **Pacing:**
   - Pause 1-2 seconds between sections
   - Don't rush
   - Let viewers absorb information
   - Use transitions (fade, slide)

### Editing Tools

**Free:**

- **DaVinci Resolve** — professional-grade
- **Shotcut** — simple, cross-platform
- **iMovie** (Mac) — easy to use

**Paid:**

- **Adobe Premiere Pro** — industry standard
- **Final Cut Pro** (Mac) — professional
- **Camtasia** — screen recording + editing

### Background Music

**Free sources:**

- YouTube Audio Library
- Incompetech (Kevin MacLeod)
- Bensound
- Free Music Archive

**Paid sources:**

- Epidemic Sound
- Artlist
- AudioJungle

**Tips:**

- Choose upbeat, modern, tech-focused tracks
- Keep volume low (background only)
- Fade in/out smoothly
- Match energy to visuals

---

## 🎨 4. Logo Variations

### Required Formats

```
1. Full logo (icon + text)
   - Light background: logo-light.svg
   - Dark background: logo-dark.svg
   - Transparent: logo-transparent.png

2. Icon only (for favicons, app icons)
   - 512x512: icon-512.png
   - 256x256: icon-256.png
   - 128x128: icon-128.png
   - 64x64: icon-64.png
   - 32x32: icon-32.png
   - 16x16: icon-16.png

3. Social media
   - Twitter: 400x400
   - LinkedIn: 300x300
   - GitHub: 200x200
```

### Current Logo

Your current logo is a gradient "A" in a rounded square. This is good! Keep it consistent.

**Gradient:**

```css
background: linear-gradient(135deg, #7f5af0, #2cb67d);
```

### Export Settings

**SVG (vector, scalable):**

- Use for web, print, any size
- Keep text as paths (not fonts)
- Optimize with SVGO

**PNG (raster, fixed size):**

- Use for social media, favicons
- Export at 2x resolution (retina)
- Optimize with TinyPNG

---

## 🌐 5. Social Media Preview Images

### Open Graph Images (for link sharing)

**Specs:**

```
Size: 1200x630 px
Format: PNG or JPG
File size: < 300 KB
Aspect ratio: 1.91:1
```

**Content:**

- Logo in top-left
- Tagline: "Ship cleaner pull requests with AI-powered review"
- Key visual: Screenshot or illustration
- Brand colors: dark background, accent highlights

**Tools:**

- Figma (free, web-based)
- Canva (templates available)
- Photoshop (if you have it)

### Twitter Card

**Specs:**

```
Size: 1200x675 px (or 1200x628 for summary_large_image)
Format: PNG or JPG
File size: < 5 MB
```

**Content:**

- Similar to Open Graph
- More horizontal space
- Focus on key message

### LinkedIn Preview

**Specs:**

```
Size: 1200x627 px
Format: PNG or JPG
File size: < 5 MB
```

**Content:**

- Professional look
- Clear value proposition
- Call to action

---

## 📦 Asset Organization

### Folder Structure

```
assets/
├── demo/
│   ├── demo.gif (< 5 MB, for GitHub)
│   ├── demo.mp4 (< 10 MB, for landing page)
│   └── demo-full.mp4 (2 min video)
├── screenshots/
│   ├── 01-vscode-analysis.png
│   ├── 02-github-pr-review.png
│   ├── 03-problems-panel.png
│   ├── 04-score-dashboard.png
│   └── 05-diff-view.png
├── logo/
│   ├── logo-light.svg
│   ├── logo-dark.svg
│   ├── logo-transparent.png
│   └── icons/
│       ├── icon-512.png
│       ├── icon-256.png
│       ├── icon-128.png
│       ├── icon-64.png
│       ├── icon-32.png
│       └── icon-16.png
└── social/
    ├── og-image.png (1200x630)
    ├── twitter-card.png (1200x675)
    └── linkedin-preview.png (1200x627)
```

---

## ✅ Quality Checklist

Before publishing any asset:

### Demo GIF

- [ ] Shows complete workflow (open → analyze → fix)
- [ ] Duration: 30-45 seconds
- [ ] File size: < 5 MB
- [ ] Resolution: 1280x720 or higher
- [ ] Frame rate: 10-15 FPS
- [ ] No personal information visible
- [ ] Dark theme for consistency
- [ ] Smooth cursor movements

### Screenshots

- [ ] High resolution (1920x1080+)
- [ ] PNG format
- [ ] Dark theme
- [ ] Readable font size (14-16pt)
- [ ] Annotated with arrows/labels
- [ ] No personal information
- [ ] Optimized file size (< 500 KB)
- [ ] Consistent styling

### Demo Video

- [ ] 1:30 - 2:00 duration
- [ ] 1080p resolution
- [ ] Clear audio (no background noise)
- [ ] Background music (low volume)
- [ ] Smooth transitions
- [ ] Clear call to action
- [ ] Captions/subtitles (optional but recommended)
- [ ] File size: < 50 MB

### Logo

- [ ] SVG and PNG formats
- [ ] Multiple sizes (16px to 512px)
- [ ] Light and dark versions
- [ ] Transparent background
- [ ] Consistent with brand colors
- [ ] Optimized file sizes

### Social Media Images

- [ ] Correct dimensions (1200x630, etc.)
- [ ] Clear value proposition
- [ ] Brand colors and logo
- [ ] Readable text
- [ ] File size: < 300 KB
- [ ] Tested on actual platforms

---

## 🚀 Quick Start (Do This First)

**Priority 1 (30 minutes):**

1. Record demo GIF showing VS Code workflow
2. Take 2 screenshots (VS Code + GitHub PR)
3. Export logo as PNG (512x512)

**Priority 2 (1 hour):** 4. Take remaining 3 screenshots 5. Annotate all screenshots with arrows/labels 6. Optimize all images (TinyPNG)

**Priority 3 (2 hours):** 7. Record 2-minute demo video 8. Edit with transitions and music 9. Create social media preview images

**Total time: 3-4 hours to create all assets**

---

## 📚 Resources

### Free Stock Assets

- **Icons:** Heroicons, Lucide, Feather Icons
- **Illustrations:** unDraw, Storyset, Blush
- **Photos:** Unsplash, Pexels, Pixabay
- **Music:** YouTube Audio Library, Incompetech

### Design Tools

- **Figma** — UI design, mockups
- **Canva** — social media graphics
- **Photopea** — Photoshop alternative (web-based)
- **GIMP** — free Photoshop alternative

### Optimization Tools

- **TinyPNG** — compress PNG/JPG
- **SVGOMG** — optimize SVG
- **Squoosh** — image compression (Google)
- **HandBrake** — video compression

---

## 💡 Pro Tips

1. **Consistency is key:**
   - Use same theme (dark) across all assets
   - Use brand colors (#7f5af0, #2cb67d)
   - Use same font (Inter or system default)

2. **Show, don't tell:**
   - Real code examples
   - Actual results
   - Concrete numbers (< 5 seconds, 82/100 score)

3. **Focus on the "wow moment":**
   - Instant results
   - Inline highlights
   - One-click fixes
   - GitHub integration

4. **Test on actual platforms:**
   - GitHub README (does GIF load?)
   - Landing page (is video smooth?)
   - Social media (does preview look good?)
   - VS Code Marketplace (are screenshots clear?)

5. **Iterate based on feedback:**
   - Ask early users what's confusing
   - A/B test different GIFs
   - Update assets as product evolves

---

**Remember:** Your demo assets are often the first impression. Invest time to make them great. A 30-second GIF can be more valuable than 1000 words of documentation. 🎬
