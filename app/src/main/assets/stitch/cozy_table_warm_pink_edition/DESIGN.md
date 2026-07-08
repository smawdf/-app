---
name: Cozy Table (Warm Pink Edition)
colors:
  surface: '#fef8f2'
  surface-dim: '#dfd9d3'
  surface-bright: '#fef8f2'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f8f3ed'
  surface-container: '#f3ede7'
  surface-container-high: '#ede7e1'
  surface-container-highest: '#e7e2dc'
  on-surface: '#1d1b18'
  on-surface-variant: '#524346'
  inverse-surface: '#32302c'
  inverse-on-surface: '#f6f0ea'
  outline: '#847376'
  outline-variant: '#d6c1c5'
  surface-tint: '#894c5c'
  primary: '#894c5c'
  on-primary: '#ffffff'
  primary-container: '#f4a7b9'
  on-primary-container: '#733949'
  inverse-primary: '#ffb1c3'
  secondary: '#78555e'
  on-secondary: '#ffffff'
  secondary-container: '#ffd1dc'
  on-secondary-container: '#7a5761'
  tertiary: '#8b4e38'
  on-tertiary: '#ffffff'
  tertiary-container: '#f8a98e'
  on-tertiary-container: '#753c27'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffd9e0'
  primary-fixed-dim: '#ffb1c3'
  on-primary-fixed: '#380a1a'
  on-primary-fixed-variant: '#6e3545'
  secondary-fixed: '#ffd9e2'
  secondary-fixed-dim: '#e7bbc6'
  on-secondary-fixed: '#2d141c'
  on-secondary-fixed-variant: '#5e3e47'
  tertiary-fixed: '#ffdbcf'
  tertiary-fixed-dim: '#ffb59c'
  on-tertiary-fixed: '#380d01'
  on-tertiary-fixed-variant: '#6f3723'
  background: '#fef8f2'
  on-background: '#1d1b18'
  surface-variant: '#e7e2dc'
typography:
  headline-lg:
    fontFamily: Be Vietnam Pro
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Be Vietnam Pro
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-bold:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 20px
  headline-lg-mobile:
    fontFamily: Be Vietnam Pro
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 40px
  margin-mobile: 20px
  margin-desktop: 64px
  gutter: 16px
---

## Brand & Style
This design system is built around the concept of "Soft Comfort." It targets users seeking a sanctuary—specifically in the food, lifestyle, or social journaling space—where the interface feels like a warm hug. The aesthetic is heavily inspired by hand-drawn illustrations and a "kawaii" minimalist sensibility.

The design style is **Tactile Minimalism**. It avoids the coldness of standard digital interfaces by using soft, organic shapes and a texture-rich color palette. It incorporates subtle skeuomorphic elements like "squishy" buttons and thick, hand-drawn-style strokes to create an interface that feels physically present and emotionally approachable.

## Colors
The palette is a carefully curated selection of warm, dusty pinks and creamy whites, designed to evoke the feeling of a sun-drenched breakfast nook.

- **Primary (#F4A7B9):** A soft, dusty rose used for call-to-action elements and key brand moments.
- **Secondary (#FFD1DC):** A pale cherry blossom pink used for highlights and softer interactive states.
- **Tertiary (#E89B81):** A warm terracotta pink that provides earthy contrast for accents and iconography.
- **Surface & Background (#FFF9F3):** A warm, milky cream that replaces clinical whites to maintain a cozy atmosphere.
- **Text & Stroke (#4A3732):** A deep cocoa brown is used instead of pure black to maintain softness while ensuring high legibility.

## Typography
The typography strategy prioritizes approachability and warmth. **Be Vietnam Pro** is utilized for headlines to provide a contemporary yet friendly voice with its slightly rounded terminals. 

For body copy and functional labels, **Plus Jakarta Sans** offers a soft, geometric clarity that remains highly readable even at smaller sizes. The scale is generous, with increased line heights to promote a relaxed reading pace. All text should be rendered in the cocoa brown stroke color to avoid harsh contrast.

## Layout & Spacing
This design system uses a **Fluid-Fixed Hybrid Grid**. Content is housed within a central container that caps at 1200px for desktop, while margins expand fluidly. 

The spacing rhythm is based on a 4px baseline, but defaults to larger "breathable" increments (16px and 24px) to reinforce the airy, relaxed brand personality. Layouts should favor asymmetrical compositions and generous white space (or "cream space") to prevent the UI from feeling cluttered. On mobile, margins are tightened slightly but padding within components remains high to facilitate easy tapping.

## Elevation & Depth
Depth in this design system is achieved through **Tonal Layering** and **Soft Shadows**. Unlike traditional material design, shadows here are highly diffused and tinted with the primary pink or cocoa brown hues to avoid "dirty" grey blurs.

- **Level 0 (Base):** The Cream surface (#FFF9F3).
- **Level 1 (Cards):** Slightly lighter cream or secondary pink with a 2px cocoa brown border (hand-drawn style).
- **Level 2 (Interaction):** Elements use a "squishy" shadow—a 4px offset shadow with 0px blur to simulate a physical, stamped effect.
- **Backdrop Blurs:** Used sparingly for overlays, with a light pink tint to maintain the warm atmosphere.

## Shapes
Shapes are organic and "cloud-like." Every corner is rounded to remove any sense of sharpness or danger. 

Standard components use a 0.5rem radius, but primary buttons and decorative containers often use a **Pill-shape** or a "super-ellipse" (squircle) to mimic the aesthetic of the reference illustration. High-level containers (like main app cards) should use the `rounded-xl` (1.5rem) setting to feel soft and toy-like.

## Components
- **Buttons:** Primary buttons are pill-shaped, filled with Primary Pink, and feature a subtle 2px bottom "lip" (darker pink) to appear pressable and tactile.
- **Cards:** Cards should use a secondary-pink background with a thin cocoa brown border. Internal padding should be at least `lg` (24px).
- **Input Fields:** Soft cream backgrounds with a Primary Pink focus border. The cursor and placeholder text should utilize the cocoa brown palette.
- **Chips:** Small, highly rounded (pill) tags used for categories. Use Tertiary pink for high-contrast labels.
- **Icons:** Icons must have a consistent "hand-drawn" weight—use 2px or 3px stroke widths with rounded caps and joins.
- **Checkboxes/Radios:** Large, "bouncy" toggle states that use the primary pink color when active.