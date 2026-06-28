# OrderDisk Icon System Redesign Design

## Goal

Redraw and standardize the OrderDisk icon system without requiring Figma or third-party paid assets.

The first implementation pass should make the app feel more coherent and release-ready by replacing the most visible inconsistent icons while keeping the current screen layouts and product behavior intact.

## Design Direction

Use the approved "warm bowl with paired chopsticks" direction:

- Warm, daily-use, two-person meal decision feeling.
- Rounded strokes and soft geometry.
- Cream, warm brown, muted red, and restrained fresh green accents.
- Product UI should stay familiar and calm, not cute to the point of feeling childish.

The app icon can be more branded. Page icons should stay simple, recognizable, and close to Material 3 conventions.

## Current State

OrderDisk currently mixes several icon treatments:

- Launcher icon uses generated `mipmap-*` webp files plus vector foreground/background resources.
- Bottom navigation and several actions use Material Icons.
- Many screens use emoji or text symbols for actions and visual markers, including camera, gallery, delete, edit, food categories, random again, wishlist, history, profile pairing, and meal result states.
- `CategoryDisplay` returns emoji strings for dish category fallback imagery.

This creates an inconsistent visual language. Some emoji also render differently across Android devices, which makes the app look less controlled.

## Non-Goals

- Do not redesign navigation or screen layout in this pass.
- Do not depend on Figma, paid icon packs, or downloaded stock icons.
- Do not add a new icon library dependency.
- Do not replace every category emoji in one risky sweep if it would touch too many screens.
- Do not change feature behavior, routing, repositories, or sync logic.
- Do not alter app name, package name, signing, or versioning.

## Scope

### Phase 1: Branded App Icon

Replace the launcher foreground/background vectors with a custom Android vector design:

- Rounded cream tile.
- Bowl shape as the main mark.
- Two crossing chopsticks as the paired-person cue.
- Small food accent dot or leaf accent for warmth.

Then regenerate or update launcher resources as needed so Android uses the new icon consistently.

Acceptance:

- App launcher icon is visibly different from the current mark.
- It remains legible at small sizes.
- It builds in debug and release.

### Phase 2: Shared Icon Vocabulary

Create a small local icon vocabulary using existing Compose Material icons and custom vector drawables where needed.

Initial action icons:

- Add
- Back
- Search
- Refresh
- Delete
- Edit
- Camera
- Gallery
- Link
- Upload image
- Favorite or wishlist
- Pair or partner
- Meal or bowl
- History
- Random or shuffle

Use Material icons when the existing library already provides a good match. Use local vector drawables only where Material icons are not expressive enough for OrderDisk.

Acceptance:

- No new dependency is added.
- Icon stroke/weight and color usage feel consistent.
- Icons use accessible content descriptions where they are meaningful actions.

### Phase 3: Replace High-Impact Emoji And Text Symbols

Replace the most visible emoji/text-icon usages first:

- `AddDishScreen`: camera/gallery choices, empty image placeholder, add ingredient, remove ingredient, and delete step. Difficulty stars may stay as text stars in the first pass because they represent rating state rather than an action control.
- `DishDetailScreen`: back, edit, ingredient section, cook steps, notes.
- `DishLibraryScreen`: delete menu text icon and category fallback imagery where practical.
- `WishlistScreen`: title icon, action buttons, delete menu.
- `ProfileScreen`: avatar source choices, paired/unpaired indicators, pair management icon, plus button.
- `AboutScreen` and `AuthScreen`: large food emoji brand mark should use the app icon mark or a local bowl icon.

Keep category food emoji fallback for cards only if replacing it would require a larger illustration system. If kept, document it as a later Phase 2 category illustration task.

Acceptance:

- The most visible emoji no longer appear in action controls.
- Text-only symbols such as `+`, `x`, and trash emoji are replaced with actual icons where they function as controls.
- Food category fallback visuals remain visually acceptable and do not block release.

### Phase 4: Verification

Run:

- `./gradlew.bat compileDebugKotlin`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

If app icon resources are regenerated, inspect the final APK output paths:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

## Implementation Strategy

Prefer small, reversible edits:

1. Add local vector drawable assets under `app/src/main/res/drawable`.
2. Add a small Compose helper if repeated painter resource usage becomes noisy.
3. Replace screen icons file by file.
4. Compile after the first representative screen.
5. Build both APK variants after all replacements.

Do not commit generated preview files from `.superpowers/brainstorm`.

## Testing Strategy

Icon changes are mostly resource and UI compile work. Verification should focus on:

- Kotlin/Compose compilation.
- Android resource packaging.
- Debug and release APK generation.
- Manual smoke review of high-impact screens if a device or emulator is available.

No repository or business logic tests are required unless implementation unexpectedly touches non-UI logic.

## Risks

- Over-replacing all category emoji may create too much churn. Keep that as a controlled second pass if needed.
- Vector drawables may look different at tiny launcher sizes if the design is too detailed. Keep the launcher mark simple.
- Some Material icons may not exist in the imported icon set. Use available equivalents rather than adding dependencies.
- Android launcher icons can be cached by the device launcher. Users may need reinstall or launcher cache refresh to see the new icon.
