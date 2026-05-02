# Project notes

- Verify the Android app with `./gradlew :app:assembleDebug`.
- Gradle needs JDK 17+ available through `JAVA_HOME` or `PATH`.
- For weak local machines, rely on GitHub Actions workflow `.github/workflows/android-debug.yml`.

## Product direction: full native CalSnap port

The requested final product is a **full native Kotlin/Jetpack Compose Android app** that matches the original CalSnap PWA as closely as practical.

Important hard requirements from the user:

- **No WebView / TWA / browser-wrapper final implementation.**
  - WebView was temporarily used in commit `96901c6fc6eca39916e989f7854ebbc146e0556f` to prove exact parity with the original PWA.
  - That is **not acceptable as the final product**.
  - The final app must implement UI, logic, storage, camera, import/export, notifications, haptics/sounds, and AI flows natively in Kotlin/Compose.
- Source of truth is the latest `main` of the original repo: `rjv-vi/CalSnap`.
  - Reference files previously inspected under `/tmp/calsnap_refs.VbSyAF/CalSnap-main`.
  - If that temp directory is gone, re-fetch the original repo and use:
    - `index.html`
    - `assets/css/base.css`
    - `assets/css/components.css`
    - `assets/css/screens.css`
    - `assets/css/polish.css`
    - `assets/js/*.js`
    - `README.md`
    - `ANDROID.md`
- Preserve compatibility with original PWA data import/export.
  - JSON exported from the PWA should restore profile, diary, weights, water, settings, Gemini key/model, notification config where applicable.
  - Native export should be readable by the original PWA when possible.
- UI should use original CSS tokens as a design spec:
  - warm background `#F2F0EB`;
  - dark text/accent `#141210`;
  - streak orange `#FF5500`;
  - original radii scale `10 / 16 / 22 / 28 / 36`;
  - card depth/shadows;
  - spring/tactile press animations;
  - active tab pill animation;
  - bottom sheets and modal shapes;
  - screen transition timing from the PWA.
- Pixel-perfect parity is expected to require device/APK feedback iterations. Do not claim exact parity without visual testing.

## Original CalSnap feature map to port

The original PWA is a single-page app with these main screens and overlays:

### Screens / navigation

- Onboarding (`#ob`)
  - 5 steps:
    1. name;
    2. gender + DOB drum picker + height + weight;
    3. activity;
    4. goal;
    5. dietary preferences + allergies.
- Home (`#home`)
  - top greeting based on time of day;
  - streak pill;
  - API missing bar;
  - horizontal 7-day calendar strip;
  - calorie hero/ring;
  - macro cards/progress;
  - mini water row;
  - diary timeline grouped by meals:
    - breakfast;
    - lunch;
    - snack;
    - dinner.
- Progress (`#prog`)
  - progress header;
  - heatmap / calendar;
  - BMI card;
  - water balance card with drink buttons;
  - water event timeline;
  - daily AI summary;
  - weight pace card;
  - weekly AI analysis;
  - weight chart.
- AI trainer/chat (`#ai`)
  - header;
  - quick suggestion chips;
  - chat messages;
  - input bar;
  - Gemini context includes profile, today’s diary, water.
- Settings (`#sett`)
  - profile/settings rows;
  - API key/model;
  - language;
  - theme;
  - notifications;
  - import/export JSON/CSV;
  - sound/haptic toggles;
  - about/authors/dev panels as applicable.
- Bottom nav:
  - Home;
  - Progress;
  - central Add button;
  - AI;
  - Settings.

### Add food sheet

Original Add modal has 4 tabs:

1. Photo
   - camera and gallery pickers;
   - preview;
   - optional description;
   - Gemini photo analysis;
   - result card with image, food, portion, calories, macros, description, ingredients;
   - add to diary.
2. Text
   - example chips;
   - text area;
   - Gemini text analysis;
   - result card;
   - add to diary.
3. Barcode
   - camera/gallery barcode image input;
   - manual EAN input;
   - OpenFoodFacts lookup;
   - Gemini/OCR fallback in original JS;
   - result card;
   - add to diary.
4. Favourites
   - list saved favourite foods;
   - add favourite to diary;
   - remove favourite.

### Food diary interactions

- Food detail bottom sheet.
- Edit food sheet:
  - food name;
  - portion;
  - kcal;
  - protein/carbs/fat;
  - meal type chips;
  - quantity controls.
- Delete confirmation.
- Favourite toggle.
- Drink detection can add water events.

### Progress / water / weight

- Water:
  - daily water goal derived from profile weight;
  - drink buttons with drink types;
  - event timeline;
  - undo/remove events;
  - reminders every configured interval.
- Weight:
  - weight logging modal;
  - weight chart;
  - pace card;
  - BMI calculation.
- Heatmap:
  - last 28 days.
- Streak:
  - original has streak with auto-freeze behavior; port the actual logic from JS.

### AI / Gemini

- Gemini REST API, not SDK-dependent UI logic.
- Dynamic model listing/selection.
- Fallback model chain.
- API error handling for bad key/forbidden/rate limit.
- Food JSON sanitization from markdown/code fences.
- Photo analysis.
- Text analysis.
- AI chat.
- Daily AI summary.
- Weekly AI analysis.
- Recents where original stores them.

### Import/export

- CSV export matching original columns:
  - date;
  - time;
  - food;
  - portion;
  - kcal;
  - protein;
  - carbs;
  - fats;
  - water section.
- JSON export/import matching original keys as closely as possible:
  - `version`;
  - `exported`;
  - `user`;
  - `log`;
  - `wts`;
  - `key`;
  - `model`;
  - `theme`;
  - `cal`;
  - `hfx`;
  - `sfx`;
  - `notif`;
  - `notifCfg`;
  - `water`.

### Notifications / offline / platform

- Native Android notifications/reminders should replace Service Worker notifications.
- Offline state should be represented natively; AI requires internet, diary/settings/progress should work offline.
- Haptics should use Android vibration/haptic feedback.
- Sounds should be native assets or equivalent; original repo has `sounds/*.mp3`.
- Camera should be native:
  - CameraX for photo/barcode where applicable;
  - ML Kit barcode scanning.

## Milestone plan

Do not try to port everything in one commit. Use milestone commits and verify each with GitHub Actions.

### Milestone 1 — restore native entry and exact app shell

- Remove WebView as the main entry point.
- Restore native Compose navigation as the app entry.
- Keep/repair 5-tab bottom nav matching original:
  - Home;
  - Progress;
  - Add center button;
  - AI;
  - Settings.
- Implement original-style screen background, typography, cards, buttons, sheets, tab pills, and press animations as shared Compose components.
- Rework onboarding to 5 original steps including preferences/allergies and DOB drum picker.

### Milestone 2 — Home + Add sheet parity

- Home:
  - greeting by time of day;
  - streak pill;
  - API missing bar;
  - horizontal date calendar strip;
  - calorie hero/ring;
  - macro cards;
  - mini water row;
  - meal-grouped diary timeline;
  - food row favourite/delete/actions.
- Add sheet:
  - convert Add screen to bottom sheet/modal matching original;
  - 4 tabs: Photo/Text/Barcode/Favourites;
  - local food database port from `FOOD_DB` in original `assets/js/app.js`;
  - favourites storage and UI;
  - result cards matching original;
  - add-to-diary source tracking.

### Milestone 3 — Food detail/edit + local data compatibility

- Food detail bottom sheet.
- Edit food sheet.
- Quantity controls.
- Delete confirm modal.
- Favourite toggle.
- Drink detection and water side effects.
- Data model migration if needed to support all original fields.

### Milestone 4 — Progress parity

- Water card with drink buttons and timeline.
- Water log/undo.
- BMI card.
- Weight log modal.
- Weight chart.
- 28-day heatmap.
- Streak logic including original freeze behavior.
- Daily AI summary card.
- Weekly AI analysis card.

### Milestone 5 — AI parity

- AI chat UI matching original.
- Quick suggestions.
- Context builder matching original.
- Dynamic Gemini model picker.
- Fallback chain and REST errors.
- Recents/history as in original.

### Milestone 6 — Settings/import/export/notifications

- Settings rows and sheets matching original.
- API key modal.
- Model picker modal.
- Language/theme toggles.
- Sound/haptic toggles.
- Import JSON from original PWA.
- Export JSON/CSV compatible with original.
- Native reminders/notifications for meals and water.
- About/authors panel.

### Milestone 7 — Native camera/barcode/sounds polish

- CameraX photo flow.
- ML Kit live barcode scanner.
- Gallery import fallback.
- Native sound assets from original `sounds/*.mp3`.
- Final animation/spacing parity pass after installing APK.

## Verification workflow

- Local environment currently lacks Java/JDK, so `./gradlew` fails locally with `JAVA_HOME is not set`.
- Use GitHub Actions for compile verification unless JDK 17+ is installed locally.
- Before pushing:
  - run static resource checks for missing strings;
  - inspect Kotlin files for obvious syntax/import issues;
  - keep commits small enough to debug CI failures.
- After each milestone:
  - push only with user approval;
  - verify `.github/workflows/android-debug.yml`;
  - report commit SHA, Actions URL, status, and artifact size.
