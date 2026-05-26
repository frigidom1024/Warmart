---
target: 首页 (HomeView.vue)
total_score: 24
p0_count: 0
p1_count: 4
p2_count: 3
timestamp: 2026-05-25T11-37-31Z
slug: src-views-homeview-vue
---
## Design Health Score

> *Based on Nielsen's 10 Usability Heuristics (0-4 scale)*

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2/4 | No loading skeleton; silent error fallback on API failures |
| 2 | Match System + Real World | 4/4 | Natural Chinese e-commerce language; familiar terms |
| 3 | User Control & Freedom | 2/4 | Favorites have no undo; no way to clear/reset visible state |
| 4 | Consistency & Standards | 3/4 | Category nav duplicated (tiles + flyout rail); otherwise coherent |
| 5 | Error Prevention | 2/4 | Pure silent `catch {}` on all API calls prevents error visibility |
| 6 | Recognition vs Recall | 3/4 | Everything visible on surface; flyout hidden until hover; heart icon unlabeled |
| 7 | Flexibility & Efficiency | 2/4 | Flyout panel is good accelerator; no keyboard nav, no bulk actions |
| 8 | Aesthetic & Minimalist | 3/4 | Clean dark theme, good spacing; pulsing badge and ornament lines are clutter |
| 9 | Error Recovery | 1/4 | All `catch` blocks silent; no error toast, empty state, or retry |
| 10 | Help & Documentation | 2/4 | Footer FAQ links exist; zero contextual help or tooltips |
| **Total** | | **24/40** | **Acceptable (20-27)** |

---

## Anti-Patterns Verdict

**AI Slop Signal: MODERATE**

The page avoids the worst offenses (no gradient text, no glassmorphism, no modals), but four patterns flag it:

1. **picsum.photos placeholder images** — Hero carousel and both banners ship with Lorem Picsum URLs. Strongest single AI-slop signal.
2. **Hero metrics strip** — "100W+ / 5000+ / 99%" is the textbook hero-metrics template, generic and unearned.
3. **Side-stripe on zone titles** — 3px orange left border on every section heading mirrors the banned side-stripe pattern.
4. **Decorative ornament lines** — Two 60px lines flanking the category slogan serve no structural purpose; pure decoration.

**Detector scan**: Unavailable (detect.mjs submodule missing from installation).

**Browser evidence**: Page renders with real data, no JS errors. But one code defect was caught: `ref="onFeedSentinel"` uses string-ref syntax (`ref="onFeedSentinel"`) which conflicts with the function-based callback ref, producing repeated Vue warnings. The IntersectionObserver likely never attaches.

---

## Overall Impression

The homepage has ambition. The dark theme + orange accent combination is a legitimate aesthetic choice for "质感轻奢," and the section structure (hero → categories → product zones → feed → footer) follows a logical narrative arc. But the execution reveals itself as a template that was customized rather than designed from first principles: placeholder images, silent error handling, and a `shuffleArray` that randomizes products every load undermine the premium trust the brand promises. The single biggest opportunity is **replacing placeholder visuals with bespoke assets**, which would immediately lift perceived quality by a full tier.

---

## What's Working

1. **Hero autoplay with hover pause** — The carousel stops when the user engages and resumes after they leave. A rare moment where the interface reduces cognitive load rather than adding to it.

2. **Consistent design token system** — Every color, shadow, radius, and transition references `var(--wz-*)` tokens. The orange accent is used with discipline, never appearing where it doesn't indicate interactivity. Principle #4 ("一致即专业") is well-executed at the code level.

3. **Flyout panel progressive disclosure** — Subcategories appear only on hover, with a 300ms hide timer preventing accidental dismissal. The initial category grid stays clean while rich detail is available on demand. Textbook progressive disclosure.

---

## Priority Issues

### P1 — Silent error handling across all data fetching

- **What**: Every API call uses `catch { /* keep empty */ }` or `catch { /* handled */ }`. When `getCategoryList` or `getProductList` fails, the page renders with zero feedback — no toast, no error banner, no empty state.
- **Why matters**: Heuristic 9 at 1/4. If the backend is down, users see a hero carousel with broken picsum images and nothing else. They cannot tell whether the site is broken, their connection is bad, or the store is empty. Destroys trust immediately, violating principle #3 ("信任即货币").
- **Fix**: Show skeleton states during load (the `loading` ref exists but is never bound in the template). Add error state with retry button. Add `onerror` handlers on product images with gradient placeholders.

### P1 — Hover-only favorite buttons invisible on touch devices

- **What**: `.zone__product-fav` and `.feed__card-fav` use `opacity: 0` with `:hover { opacity: 1 }`. On touch devices there is no hover, so the heart icon is never shown.
- **Why matters**: Heuristic 3 at 2/4. The primary interaction for saving products is literally invisible to mobile users. The target audience (18-35 Chinese consumers) shops heavily on mobile.
- **Fix**: Show the favorite button at reduced opacity (0.5) by default. Ensure minimum 44×44px tap target.

### P1 — Duplicate API calls waste bandwidth

- **What**: Two separate `onMounted` hooks both fetch `getProductList({ size: 200 })`. The first call builds product zones, the second builds the hotsale section. Network tab confirms two identical requests.
- **Why matters**: Doubles load time for the primary data fetch on a slow connection. Violates principle #5 ("工具即消失").
- **Fix**: Merge the two `onMounted` hooks. Fetch once, derive both `hotsaleProducts` and `zones` from the same response.

### P1 — `ref="onFeedSentinel"` binding is broken

- **What** (caught by Assessment B): Line 567 uses `ref="onFeedSentinel"` (string-ref syntax), but `onFeedSentinel` is a function (line 277) intended as a callback ref. Vue 3 interprets string refs as lookups for `ref()` variables, not functions. Repeated console warning: "Template ref 'onFeedSentinel' used on a non-ref value."
- **Why matters**: The IntersectionObserver never properly attaches to the sentinel. Infinite scroll feed may not trigger `loadMore()` in production builds, silently cutting off the product feed after the first page.
- **Fix**: Change to `:ref="onFeedSentinel"` (dynamic ref binding), or refactor to use a proper `ref<HTMLElement | null>()` variable.

### P2 — Random shuffle destroys spatial memory

- **What**: `shuffleArray` randomly reorders products every page load. Each visit shows different products in different positions.
- **Why matters**: Heuristic 6. Users develop spatial memory for product placement — returning visitors expect consistency. Random shuffling forces re-scanning every time.
- **Fix**: Sort by a stable key (newest, highest-rated, or API sort order). Reserve shuffle for explicit "surprise me" features.

### P2 — Placeholder image URLs in production template

- **What**: Hero carousel and both banners use `https://picsum.photos/id/...` URLs. These are generic random photography, not Warmart inventory.
- **Why matters**: Heuristic 8. A "质感轻奢" brand shipping with stock photo placeholders contradicts the brand directly. More critically, picsum may be rate-limited or deprecated in production.
- **Fix**: Replace with actual product or campaign imagery. Add `@error` fallback handlers for broken images.

### P2 — No loading skeleton for initial page load

- **What**: The `loading` ref (line 18) is set and toggled but never referenced in the template. During the 1-3 seconds while data loads, the page shows empty dark space.
- **Why matters**: Heuristic 1. Users staring at a blank page will assume the site is broken. This is the user's first impression.
- **Fix**: Conditionally render shimmer skeleton grids matching final layout dimensions while `loading` is true.

---

## Persona Red Flags

### Jordan (Confused First-Timer)

- **No starting point guidance**: Four hero slides with different CTAs ("即刻选购", "抢购新品", "立即抢购", "开通会员") all route to the same `/product/list` page. Jordan cannot distinguish them.
- **Hidden flyout invisible**: The `.cat-trigger__panel` appears only on hover. Jordan, who relies on visible affordances, will never discover subcategory browsing exists.
- **No error state guidance**: If the API fails, Jordan sees empty dark space with no indication of what happened, no retry, no help link. Abandonment is the only path.

### Riley (Deliberate Stress Tester)

- **Silent failures everywhere**: Every `catch` block discards the error. Riley would throttle the network and watch the page degrade to nothing with zero user feedback.
- **Shuffle inconsistency**: Reloading 3 times shows different product order each time. Flags the interface as unpredictable.
- **Broken-image cascade**: Picsum URLs returning 404 would ripple through hero, banners, and product images. Zero `onerror` handlers anywhere.
- **Duplicate API calls**: Network tab shows two identical `getProductList({ size: 200 })` requests. Signals code quality issues.

### Casey (Distracted Mobile User)

- **Favorite hearts always hidden**: The hover-only heart button is never visible on touch. Casey can never favorite a product from the homepage.
- **Flyout doesn't work on touch**: Relies on `mouseenter`/`mouseleave`. On mobile the panel never opens.
- **Small touch targets**: At 640px, category tiles may be ~70px wide with 22×22px icons. Below WCAG 44×44pt recommendation.
- **Large hero on mobile**: At 460px height, the hero takes ~60% of viewport on a typical phone, pushing all products below the fold.

---

## Minor Observations

- Line 348: `{ value: '99%', label: '好评率' }` — trust metric that invites skepticism without a source link or verification badge.
- `.cat-browse__slogan` has `white-space: nowrap` with 0.3em letter-spacing — may overflow on very narrow viewports.
- `.zone__product-badge` has per-child color rules (`:nth-child(2)` = green, `:nth-child(3)` = purple, `:nth-child(4)` = blue) — arbitrary, no semantic meaning. Inconsistent when zones have fewer than 4 products.
- The `feed__card` shows no "add to cart" button — browse-only with no direct purchase path from the feed. May be intentional, may be an omission.

---

## Questions to Consider

1. **The hero carousel has four slides with different promises ("精选全球好物", "夏季新品全面上架", "数码家电超级品类日", "开通年度会员") but every CTA routes to the same `/product/list` page. If the content promises different destinations, why does the interaction deliver the same one? Would a single hero image with one clear CTA be stronger?**

2. **The category section shows all categories as equal-weight tiles, but the zone grid immediately below applies a hard priority split (first 2 get 8 products, rest get 4). Tile grid says "all equal" while zone grid says "some matter more." Should the category display also communicate priority, or should the zone grid drop the distinction?**

3. **`shuffleArray` randomizes products every load. In a physical store, merchandise moves rarely and deliberately. What assumption does random shuffling serve — and is it compatible with "信任即货币" (trust is currency)?**
