---
name: 暖 · Warmart
description: Dark, warm e-commerce platform for quality-conscious young Chinese consumers
colors:
  primary: "#ff6b35"
  primary-dark: "#e55a2b"
  primary-light: "#ff8a5c"
  bg: "#0f0f11"
  bg-card: "#1a1b1f"
  bg-elevated: "#222327"
  text: "#ffffff"
  text-soft: "#b0b1b6"
  text-muted: "#6b6c72"
  border: "#2a2b30"
  border-light: "#1e1f23"
  success: "#34c759"
  warning: "#ff9f0a"
  danger: "#ff453a"
typography:
  display:
    fontFamily: "'Noto Serif SC', Georgia, serif"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "0.02em"
  headline:
    fontFamily: "'Noto Serif SC', Georgia, serif"
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: "0.02em"
  title:
    fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, 'Inter', sans-serif"
    fontWeight: 600
    lineHeight: 1.3
  body:
    fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, 'Inter', sans-serif"
    fontWeight: 400
    fontSize: "15px"
    lineHeight: 1.6
  label:
    fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, 'Inter', sans-serif"
    fontWeight: 500
    fontSize: "12px"
    letterSpacing: "0.08em"
rounded:
  sm: "6px"
  md: "12px"
  lg: "20px"
  xl: "28px"
  full: "9999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "16px"
  lg: "24px"
  xl: "32px"
  2xl: "48px"
  3xl: "64px"
  4xl: "96px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "#ffffff"
    rounded: "10px"
    padding: "14px 34px"
    typography: "{typography.body}"
    fontWeight: 600
  button-primary-hover:
    backgroundColor: "{colors.primary-dark}"
    boxShadow: "0 8px 28px rgba(255,107,53,0.35)"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "#ffffff"
    border: "1px solid rgba(255,255,255,0.5)"
    rounded: "10px"
    padding: "14px 34px"
  button-ghost-hover:
    borderColor: "{colors.primary}"
    textColor: "{colors.primary}"
  input-default:
    backgroundColor: "{colors.bg-card}"
    textColor: "{colors.text}"
    rounded: "{rounded.sm}"
    border: "1px solid {colors.border}"
    height: "44px"
    padding: "0 14px"
  input-focus:
    borderColor: "{colors.primary}"
  card-default:
    backgroundColor: "{colors.bg-card}"
    rounded: "{rounded.md}"
    border: "1px solid {colors.border}"
    padding: "0"
  chip-default:
    backgroundColor: "transparent"
    textColor: "{colors.text-soft}"
    border: "1px solid {colors.border}"
    rounded: "8px"
    padding: "7px 18px"
  chip-active:
    backgroundColor: "rgba(255,107,53,0.12)"
    textColor: "{colors.primary}"
    borderColor: "{colors.primary}"
---

# Design System: 暖 · Warmart

## 1. Overview

**Creative North Star: "The Ember Market"**

A dark e-commerce ground where warm orange glows like embers in a hearth. The interface recedes into the deep charcoal background, letting product imagery command the surface. Every visual decision — the generous spacing, the serif headings, the tonal layering of surfaces — serves one goal: a shopping experience that feels substantial, trustworthy, and quietly elevated.

This system is built for evening browsing on desktop: young design-aware shoppers scrolling after work or on weekends, in low ambient light, looking for quality goods. The dark ground reduces visual fatigue; the warm orange accent provides wayfinding energy without noise. The palette is restrained — one accent, three surface tones, a tinted neutral hierarchy. Motion is purposeful: hover lifts, focus glows, transitions are slow and smooth. Nothing decorative, nothing that interrupts the task.

**Key Characteristics:**

- **Dark by default**, grounded in deep warm charcoal (#0f0f11). Surfaces layer through luminance, not shadows.
- **Warm single-accent** — Ember Orange (oklch(0.62 0.22 35)) is the only saturated color, used sparingly on CTAs, prices, active states, and scrollbar thumbs. Its rarity is the point.
- **Editorial-sans pairing** — Noto Serif SC for display hierarchy, system-ui sans for body. Respects Chinese typographic tradition while staying performant.
- **Tonal depth over shadow** — hierarchy through color value, not box-shadow. Cards are flat at rest; shadows appear only as interaction feedback.
- **Tactile interactions** — buttons lift, inputs glow, chips snap to active states. Every touchpoint has weight.

## 2. Colors: The Ember Palette

A dark-warm palette anchored by orange and built from tinted charcoal neutrals. The orange carries wayfinding and emphasis; the background and surface tones create the spatial hierarchy.

### Primary

- **Ember Orange** (#ff6b35 / oklch(0.62 0.22 35)): The sole accent. CTAs, prices, active tab indicators, focus rings, scrollbar thumbs, badge backgrounds, category selection. Used at approximately 8-12% of any given screen.
- **Ember Dark** (#e55a2b / oklch(0.55 0.20 35)): Hover and active states for primary buttons and links.
- **Ember Glow** (#ff8a5c / oklch(0.72 0.18 35)): Lighter accent for secondary highlights and subtle emphasis.
- **Ember Tint** (rgba(255, 107, 53, 0.12) / oklch(0.62 0.22 35 / 0.12)): Subtle background tint for price blocks, active spec chips, selected categories, hover overlays on icon buttons.

### Neutral

- **Obsidian** (#0f0f11 / oklch(0.10 0.008 35)): Page background. The ground of the entire interface. Deep, slightly warm to avoid a cold "tech dark mode" feel.
- **Emberstone** (#1a1b1f / oklch(0.14 0.008 35)): Card and surface background. One step above the page ground. Used for all container surfaces.
- **Basalt** (#222327 / oklch(0.17 0.008 35)): Elevated surface. Used for hover card states, skeleton highlights, and the category browse section.
- **Hover Wash** (rgba(255, 255, 255, 0.04)): The hover overlay applied on icon buttons, menu items, and interactive elements.
- **Warm White** (#ffffff / oklch(1 0 35)): Primary text. Pure white with warm-tinted context from the surrounding palette.
- **Silver Ash** (#b0b1b6 / oklch(0.72 0.006 35)): Secondary text. Metadata, navigation labels, muted information.
- **Dim Ash** (#6b6c72 / oklch(0.45 0.008 35)): Placeholder text, disabled states, subdued footnotes, captions.

### Border

- **Iron** (#2a2b30 / oklch(0.20 0.008 35)): Standard border. Subtle separation between surfaces.
- **Shadow Iron** (#1e1f23 / oklch(0.16 0.008 35)): Lighter border for subtle dividers.

### Semantic

- **Leaf** (#34c759 / oklch(0.65 0.22 145)): Success states.
- **Amber** (#ff9f0a / oklch(0.75 0.18 75)): Warning states.
- **Rose** (#ff453a / oklch(0.55 0.24 30)): Error and danger states.

### Named Rules

**The One Accent Rule.** Ember Orange is the only saturated color on any screen. Success green, warning amber, and danger rose appear only in their semantic context. No secondary accent, no gradient decorations, no rainbow badges. The orange's rarity is its power.

**The Warm Ground Rule.** Neutrals are tinted warm (oklch hue ~35). The dark is not a neutral gray — it leans toward brown-warm, avoiding the cold blue-gray that dominates "dark mode" defaults. This warmth is subtle (chroma 0.008) but essential to the brand feeling.

## 3. Typography

**Display Font:** Noto Serif SC (with Georgia fallback)
**Body Font:** system-ui stack (-apple-system, Segoe UI, Roboto, Inter)
**Mono Font:** JetBrains Mono (Fira Code fallback, used only in code contexts)

**Character:** The pairing is Song-style serif for warmth and authority, combined with a clean system sans for readability. Noto Serif SC carries the brand's Chinese identity — its even stroke weight and generous apertures make it legible at both display and small sizes. The sans body is familiar, performant, and recedes into the background.

### Hierarchy

- **Display** (700, clamp(24px, 5vw, 44px), 1.2): Hero titles and major section headings. Uses Noto Serif SC with 0.02em letter-spacing. Tracks at the top of the page only.
- **Headline** (600, 20-28px, 1.3): Section titles (zone headers, PDP product name, auth headline). Noto Serif SC.
- **Title** (600, 16-18px, 1.3): Card titles, modal headers, cart total labels. System sans-serif.
- **Body** (400, 15px, 1.6): All reading text. System sans. Max line length 75ch. The primary reading size.
- **Label** (500, 12px, uppercase with 0.08em letter-spacing): Metadata, category headers, tab labels, hints, footnotes. System sans.
- **Small** (500, 11-13px, 1.3): Product tags, badges, badges, price old-values, timestamps. Tight but readable.

## 4. Elevation

Depth is conveyed primarily through tonal layering — moving from Obsidian (page bg) to Emberstone (card) to Basalt (elevated surface) creates a clear spatial hierarchy without relying on shadows. This approach keeps the interface clean and avoids the visual clutter of multiple shadow depths competing for attention.

Shadows exist but are reserved for interaction feedback: a card lifts on hover, a button rises on rollover, a dropdown menu separates from the page. At rest, surfaces are flat.

### Shadow Vocabulary

- **Ambient Low** (`0 1px 3px rgba(0, 0, 0, 0.3)`): Default card shadow. Barely perceptible — defines the card edge against the page.
- **Ambient Mid** (`0 4px 16px rgba(0, 0, 0, 0.4)`): Card hover state, toast notifications, dropdown menus. Enough depth to lift a surface one level.
- **Ambient High** (`0 12px 40px rgba(0, 0, 0, 0.5)`): Large overlays, flyout panels, the auth card container.
- **Ambient Max** (`0 20px 60px rgba(0, 0, 0, 0.6)`): Modal backdrops, the highest z-order surfaces.
- **Ember Glow** (`0 0 24px rgba(255, 107, 53, 0.25)`): Hover glow on primary CTAs. The only colored shadow in the system.

### Named Rules

**The Flat-By-Default Rule.** Surfaces are flat at rest. Shadows appear only as a response to interaction (hover, focus, open state). A card at rest should not look lifted — it sits on its surface, distinguished by tonal value, not by shadow.

## 5. Components

Every component shares the same vocabulary: dark surface, orange accent on active states, gentle hover feedback, rounded corners at 6-12px.

### Buttons

- **Shape:** Slightly rounded (10-12px radius). Full-width primary buttons use pill shape (23px / 25px radius) on standalone CTAs.
- **Primary (Ember Orange):** Background `var(--wz-orange)`, white text, font-weight 600. Hover darkens to `var(--wz-orange-dark)` and lifts 1px with the Ember Glow shadow. Contains a subtle shine sweep animation on hover (auth button).
- **Ghost / Outline:** Transparent background, white/soft border (1px solid rgba(255,255,255,0.5)). Hover shifts border and text to Ember Orange. Used for secondary actions in hero and comment sections.
- **Spec Chips:** Low-profile toggle buttons (13px font, 7px 18px padding, 8px radius). Default: transparent bg with Iron border. Active: Ember Tint background + Ember Orange border + orange text. Hover: orange border without the fill.
- **Icon Buttons:** 36px square, transparent. Hover shows Ember Tint background + orange icon. Used for cart, close, search clear.

### Inputs / Fields

- **Style:** Dark surface background (`var(--wz-bg-card)` or `var(--wz-bg)`), Iron border (1px solid `var(--wz-border)`), 6px radius (auth uses 12px). 44px height standard.
- **Focus:** Border shifts to Ember Orange with a 3px orange-tinted box-shadow glow (`rgba(255,107,53,0.1-0.12)`). Transition is 200ms ease-out.
- **Icon:** Fields with leading icons (auth forms) have the icon at 13px left, text padded to 40px left. Icon color is Dim Ash.
- **Checkbox:** Custom 20px square with Iron border. Checked state fills with Ember Orange, shows white checkmark. Used in cart and settings.
- **Quantity Control:** Inline flex with bordered buttons (36px wide) flanking a centered value. Bordered with Iron, 6-8px radius. Disabled state at 0.25 opacity.

### Navigation (Header)

- **Structure:** Fixed top bar, 60px height, full-width with `max-width: 1280px` inner. Scroll-down hides; scroll-up reveals.
- **Brand:** Logo character "暖" in Noto Serif SC (24px, 700 weight, Ember Orange) + "Warmart" in same face (18px, white). Gap 6px.
- **Search Bar:** Pill-shaped (9999px radius), Emberstone background, Iron border. Contains category dropdown, divider, text input, and orange circular search button (34px). Focus state: orange border + 3px orange-tinted glow.
- **Actions:** Cart icon (with badge counter), user avatar/menu, login/register links. CTA button uses pill shape with orange background.
- **Dropdowns:** Element Plus overrides — dark card background, Iron border, soft text, hover highlight via Hover Wash.

### Cards / Containers

- **Corner Style:** 10-12px radius (product cards 8-10px). Auth card container uses 40px outer radius.
- **Background:** Emberstone (`var(--wz-bg-card)`) with 1px Iron border.
- **Shadow Strategy:** Flat at rest (Ambient Low). On hover: Ambient Mid shadow + border stays or subtly shifts. Product cards lift on hover with slight translateY(-2 to -3px).
- **Hover Effect:** Image zoom (scale 1.06, 400ms ease-out), favorite heart fades in (opacity 0→1), overlay gradient appears from bottom showing product info.

### Chips / Tags

- **Badges:** Absolute-positioned pill on product images. 10-11px font, 700 weight, white text. Orange background by default. Hotsale zone uses Rose (danger red) with pulse animation.
- **Category Tags:** Subcategory chips in flyout panel — dark page background, 9999px radius. Hover fills with Ember Orange.

### Product Feed Cards

- 4-column desktop grid, transitions to 3-column at 900px, 2-column at 640px.
- Square aspect-ratio image area with Emberstone fallback.
- Info panel below image: product name (13px, 500 weight), price row with Ember Orange current and struck-through original.
- Favorite heart button with scale-beat animation and ripple ring effect on click.
- Loading skeleton: shimmer animation with Emberstone-Basalt gradient sweep.

### The Flyout Panel

- Signature component. Full-width overlay panel below the category browse bar. Left sidebar (170px) with category buttons; right panel with subcategory chips (4-column) and product previews (4-column grid).
- Smooth appearance: opacity + translateY transition, 180ms ease-out.
- Shadow: Ambient High, creating clear separation from the page.

### Footer

- Multi-column grid (2fr 1fr 1fr 1fr on desktop, single column on mobile).
- Brand column with logo, description, social icon circles (hover fills orange).
- Link columns with muted text, orange hover.

## 6. Do's and Don'ts

### Do:

- **Do** use Ember Orange as the single accent across the interface — for primary CTAs, prices, active states, focus rings, and scrollbar thumbs. Its consistency builds trust.
- **Do** use tonal layering (Obsidian → Emberstone → Basalt) for surface hierarchy. Let the three dark values do the structural work.
- **Do** keep surface at rest flat and shadow-free. Lift only on interaction.
- **Do** use Noto Serif SC for all display and headline text. The serif warmth is the brand's typographic anchor.
- **Do** use system-ui sans for body text, labels, and data — performance and familiarity matter at reading sizes.
- **Do** use generous spacing (16px/24px/32px grid gaps, 48px section padding). Crowded layouts feel cheap.
- **Do** use the Ember Glow shadow on hovered CTAs as the only colored shadow. It's the system's signature.
- **Do** use the shimmer skeleton pattern (Emberstone → Basalt gradient) for loading states. It fits the dark ground naturally.

### Don't:

- **Don't** introduce a second accent color. One accent. No purple gradients, no neon, no glassmorphism.
- **Don't** use pure `#000` or `#fff` without context. Obsidian (#0f0f11) is the black; Warm White is the white.
- **Don't** use aggressive discount badges, flashing sale banners, or cluttered grids. The interface earns trust through restraint, not urgency.
- **Don't** nest cards. A card on a card surface is always wrong.
- **Don't** use side-stripe borders (border-left/right greater than 1px as colored accents). Use full borders, background tints, or nothing.
- **Don't** apply gradient text via background-clip. Single solid colors only.
- **Don't** use em dashes in UI copy. Use commas, colons, or periods.
- **Don't** animate CSS layout properties (width, height, top, left). Use transforms and opacity.
- **Don't** use a hero-metric layout (big number, small label, supporting stats, gradient accent). That pattern belongs to B2B SaaS, not e-commerce.
- **Don't** open a modal as a first-resort interaction. Exhaust inline and progressive alternatives first.
