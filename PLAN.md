# CalSnap — подробный план миграции (PWA → Kotlin/Compose)

Источники:
- **Оригинал**: `CalSnap/` (index.html, assets/css/*, assets/js/*, sw.js, sounds/*) в корне workspace `/home/rjv/Downloads/CalSnaps/CalSnap/`
- **Порт**: `android-calsnap/` (git: `rjv-vi/android-calsnap`)
- **Дизайн-референсы (UI/цвета)**: `/home/rjv/Downloads/CalSnaps/скриншоты с оригинального проекта/` и `.../НОВЫЕ/`
- **Правило логики и анимаций**: смотреть `assets/js/*.js` оригинала
- **Правило UI/цветов/размеров**: смотреть скриншоты оригинала, дополнительно CSS tokens в `assets/css/base.css`

Легенда статуса: ✅ готово • 🟡 частично • ❌ нет

---

## 0. Фундамент (design tokens + инфра)

| Пункт | Статус | Детали |
|---|---|---|
| Warm light theme tokens (`#F2F0EB`, `#FFFFFF`, `#141210`, `#FF5500`) | ✅ | `ui/theme/Color.kt` |
| Dark theme tokens (`#0F0E0C`, `#1A1916`, `#F4F2EE`) | ✅ | `Color.kt`, `Theme.kt` |
| Radii scale `10 / 16 / 22 / 28 / 36` | ✅ | `Shape.kt` |
| Shadow scale s1–s4 | 🟡 | Нет точного соответствия multi-layer shadow из `--s2/s3`, сейчас `shadow(elevation)` даёт один слой |
| Font DM Sans | ❌ | Используется системный; нужно добавить `assets/fonts/DMSans-*.ttf` + `FontFamily` |
| Spring press анимация (scale .96) | ✅ | `calSnapClickable` |
| Staggered card-enter анимация | ✅ | `AnimatedSection` |
| Haptic feedback (HFX.light/medium/success/error/tick) | ❌ | Нужно обернуть в `HFX` утилиту поверх `HapticFeedback` / `Vibrator` |
| Sound effects (`sounds/*.mp3`) | ❌ | Скопировать `CalSnap/sounds/*.mp3` в `app/src/main/res/raw/` + `SoundPool` менеджер `SFX` |
| Tab pill sliding animation (`.tabs-pill`) | 🟡 | В Add tabs есть, но без анимированной «pill» под активной вкладкой как в оригинале |
| Nav pill sliding animation (`#nav-pill`) | 🟡 | В `CalSnapBottomBar.kt` сейчас статичный highlight, нужно плавающую pill |
| Toast snackbar (`showToast`) | ❌ | Нужен глобальный host + compose API |
| Confirm dialog (`showConfirm`) | 🟡 | Есть только DeleteConfirm на Home, нужна общая компонента |

---

## 1. Онбординг (5 шагов)

Логика: `assets/js/state.js` (`on1…onFin`, `pickGender`, `togglePref`), `assets/js/drum.js`.
UI ref: скрин `15-54-05.jpg` (Food preferences), `15-54-07.jpg` (Your stats).

| Шаг | Статус | Что нужно |
|---|---|---|
| 1. Имя | ✅ | |
| 2. Gender (m/f карточки) | ✅ | |
| 2. **DOB drum picker** (day/month/year 3-колоночный скролл) | 🟡 | Сейчас стандартный wheel. Оригинал — кастомный drum-picker из `drum.js`. Нужно перенести 3 wheel-колонки в стиле iOS, с `snap` и bold для активного |
| 2. Рост + Вес (рядом) | ✅ | |
| 3. Activity cards | ✅ | |
| 4. Goal cards | ✅ | |
| 5. Prefs chips (8 штук: No meat / Gluten-free / Lactose-free / No sugar / Vegan / Keto / Halal / No eggs) | ✅ | |
| 5. Allergies input | ✅ | |
| Back/Continue buttons | ✅ | |
| Прогресс dots сверху (5 линий) | 🟡 | Нужно проверить точную анимацию заполнения |

---

## 2. Home screen

Логика: `assets/js/app.js` (`rH`, `selectDay`, `openFd`, `emo`), `state.js` (`streak`, `_getFreezes`), `water.js` (`_updateMiniWater`).
UI ref: `15-53-39.jpg` (light), `15-53-40.jpg` (dark), `20-22-14.jpg` (empty state).

| Элемент | Статус | Детали |
|---|---|---|
| Greeting by time of day | ✅ | Ночь/утро/день/вечер |
| Big name `RJV!` | ✅ | |
| Streak pill (оранжевый gradient + 🔥 + число + «days») | ✅ | |
| «Gemini API key needed» warm bar | ✅ | |
| Горизонтальный 7-дневный календарь (с точками: ok/over) | ✅ | `CalendarStrip` |
| **Calorie hero card**: big eaten `/ goal` + кольцо % + «kcal left» pill | ✅ | |
| Macro row (Protein / Carbs / Fat) с текущим/целью + прогресс-бар | ✅ | |
| Mini water row (💧 + bar + `x / y ml`) | ✅ | |
| Заголовок «Today» / дата | ✅ | |
| **Группы приёмов**: Breakfast/Lunch/Snack/Dinner | ✅ | `MealGroup` |
| Food row: emoji + название + kcal + макросы + star + trash | ✅ | Макросы P/C/F уже убрали из цветных pill |
| Servings badge `×0.5 / ×2` | ✅ | |
| Empty state «Tap + to add your first meal» + 🥗 | ✅ | |

**Streak-логика**: оригинал имеет «freeze» (1 авто-заморозка в неделю). Порт: `domain/StreakCalculator.kt` — нужно сверить с `state.js → streak() + _getFreezes() + _isFreezeUsedThisWeek`. Статус: 🟡.

---

## 3. Progress screen

Логика: `assets/js/app.js` (`rP`, `rWater`, `rBMI`), `bmi.js`, `water.js`, `daily-ai.js`, `daily.js`.
UI ref: `15-53-44.jpg` (top), `15-54-18.jpg` (heat + weight + weekly AI), `15-54-02.jpg` (weight chart + Saved! badge).

| Элемент | Статус | Детали |
|---|---|---|
| Header `Progress` + месяц справа | ✅ | |
| Streak big card (оранжевый `linear-gradient` + 🔥 + число + week dots Mon–Sun) | ✅ | |
| BMI card + needle + категории + цветная шкала | ✅ | |
| Stats grid (Avg calories / Best day / Entries / Days logged) | ✅ | |
| Water card: ring + big num + wave bar | ✅ | |
| **Water drink buttons** (6): 💧 🍵 ☕ 🧃 🥛 🫗 с ml | ✅ | Но нет custom slider модалки (`openWaterCustom`) |
| Caloric напитки также добавляются в food log | ❌ | `water.js:55 addWater` добавляет в `log` если `kcal>0` |
| Water salt hint (автомат) | 🟡 | Бейдж есть, но детект соли по сегодняшней еде (`saltWords`) нет |
| Water goal ×1.2 при соли | ❌ | `getWaterGoal` в `water.js:43` |
| Water timeline (лента ивентов) + Undo | ✅ | |
| Heatmap 28 дней + легенда (Нет/Мало/Норма/Цель/Перебор) | ✅ | |
| **Daily AI summary card** | ❌ | `daily-ai.js` + `dailyAiCard` в `index.html:431` — нужен порт: Gemini prompt с сегодняшним diary → короткий отчёт |
| Weight chart (SVG линия + точки) | ✅ | |
| Weight log modal + шагатель | ✅ | |
| «+ Записать» badge pill | ✅ | |
| Pace card (↗️/↘️/→ + delta) | ✅ | |
| **Weekly AI analysis card** («Analyse» button → Gemini за 7 дней) | ❌ | `daily.js:8 loadWeekAnalysis` |

---

## 4. Add food sheet

Логика: `app.js` (`openAdd`, `swTab`, `onPhoto`, `doPhoto`, `doText`, `doBarcode*`, `renderFavs`, `addFavToLog`, `addRes`).
UI ref: `20-22-13.jpg` (Photo), `20-22-18.jpg` (Text), `20-22-24.jpg` (Barcode), `15-53-43.jpg` (Favourites).

Общее:
- Bottom sheet с handle, заголовок «Add food» + ✕ ✅
- 4 таба с анимированной pill 🟡 (pill под активной вкладкой как в оригинале — проверить)

### 4.1 Photo tab
| Пункт | Статус |
|---|---|
| Upzone «Add a photo / Pick a source» с пунктирной рамкой + 📷 icon | 🟡 (есть кнопки Camera/Gallery, но upzone стиля нет) |
| Camera (system intent via FileProvider) | ✅ |
| Gallery picker | ✅ |
| Preview выбранного фото + кнопка сброса | 🟡 (только имя файла) |
| Textarea «Необязательно: опиши блюдо» | ✅ |
| Loading indicator `sp` + «Gemini анализирует…» | ✅ |
| Result card: image + name + portion + kcal + macros + description + **ingredients chips** + «+ Add to diary» | 🟡 (нет image, нет ingredients) |
| Native CameraX preview вместо системного Intent (long-term) | ❌ |

### 4.2 Text tab
| Пункт | Статус |
|---|---|
| **Example chips** (1 яблоко / тарелка гречки / 2 яйца варёных / 200г творога / кофе с молоком / бутерброд с сыром) | ❌ |
| Textarea | ✅ |
| «🔍 Рассчитать» кнопка (Analyze with AI) | ✅ (текст «Analyze with AI» — поменять на «🔍 Рассчитать» или оставить) |
| Result card → `+ Add to diary` | ✅ |

### 4.3 Barcode tab
| Пункт | Статус |
|---|---|
| Иконка «Scan barcode» + «OpenFoodFacts + AI lookup» | 🟡 |
| Кнопки Камера / Галерея | ❌ (сейчас только manual EAN) |
| Manual EAN input + стрелка | ✅ |
| OCR через Gemini → OpenFoodFacts fallback | 🟡 (сейчас только manual + OpenFoodFacts) |
| **ML Kit live barcode scan** (Android) | ❌ |
| Result card → `+ Add to diary` | ✅ |

### 4.4 Favourites tab
| Пункт | Статус |
|---|---|
| Список карточек: emoji + name + kcal + portion + ✕ + `+ Add` | ✅ |
| Тап `+ Add` → выбор порции (0.5× / 1× / 1.5× / 2× + stepper) | ✅ (в порте даже лучше оригинала) |
| Empty state «No favourite foods yet» | ✅ |
| Сохранение независимо от diary entry (удаление из diary не ломает favourite) | ✅ (commit `9c47d5a`) |

---

## 5. Food detail + edit

Логика: `app.js` (`openFd`, `changeQty`, `_qtyHold`, `editFd`, `saveEditFd`, `delFd`, `delL`).
UI ref: `15-53-37.jpg` (Яблоко detail).

| Пункт | Статус |
|---|---|
| Bottom sheet с blurred food-emoji header (размытое яблоко) | 🟡 (сейчас серый фон + emoji) |
| Big kcal число + portion + 3 macro tiles (P/C/F) | ✅ |
| Description | ✅ |
| **Ingredients chips** block | ✅ |
| «Добавлено: Sun May 03 2026 в 20:19» | ✅ |
| **Quantity row**: «Количество порций» + −/число/+ с hold-to-repeat (`_qtyHold`) | 🟡 (есть, но вместо qty (int) у нас servings (float .5 step)) |
| Кнопки «✏️ Изменить» + «🗑 Удалить» в ряд | ✅ |
| Edit sheet: name / portion / kcal / macros / time / meal chips | ✅ |
| Delete confirm dialog (🗑 + «Remove from diary?» + Delete/Отмена) | ✅ |

**Важный архитектурный вопрос**: в оригинале quantity — целочисленный множитель (1,2,3…) и kcal=baseKcal×qty; в порте это `servings` с 0.5 шагом. Нужно решить, что оставить — второе гибче.

---

## 6. AI trainer

Логика: `app.js` (`initAi`, `aiSend`, `aiSug`, `aiVoiceToggle`, `clearAiChat`, `_aiThinkStart`, `aiMsg`).
UI ref: `15-54-04.jpg` (Clear chat confirm), dialogue bubbles на разных скринах.

| Пункт | Статус |
|---|---|
| Header: back-arrow + 🤖 avatar + «AI Нутрициолог» + «● Онлайн» + reset icon | 🟡 |
| Quick suggestion chips (6 штук: Моя норма? / Что съесть? / Мой рацион / Набор массы / Быстрый перекус / Дефицит калорий) | ✅ |
| **Welcome card** с контекстом (сегодня kcal/water/goal/left) | 🟡 (есть, но надо свериться с форматом из `initAi`) |
| Message bubble (user/ai) с разными углами | ✅ |
| Input bar: textfield + 🎤 mic + ➤ send | ✅ (mic кнопка есть, но no-op) |
| **Voice input** (SpeechRecognizer) | ❌ |
| **Clear chat confirm** dialog | ❌ |
| Typing bubble (три точки) | ✅ |
| **Context builder** (profile + today diary + water + timeline) перед отправкой в Gemini | 🟡 (нужно сверить `aiSend` в `app.js:853`) |
| Dynamic model picker (38 моделей Gemini) | ✅ (есть в Settings) |
| Fallback model chain | ❌ |
| Error cards (bad key / 403 / rate limit) | 🟡 (общий ErrorCard) |

---

## 7. Settings

Логика: `ui.js` (`rSet`, `ed`, `saveEd`, `toggleTheme`, `toggleSfx`, `toggleHfx`, `toggleLang`, `exportCSV`, `exportJSON`, `importJSON`, `clrAll`), `notif.js`, `about.js`.
UI ref: `15-53-48.jpg` (полный settings).

| Секция | Пункт | Статус |
|---|---|---|
| **Appearance** | Dark theme toggle | ✅ |
| | Sounds toggle (`toggleSfx`) | ❌ (SFX системы нет) |
| | Haptics toggle (`toggleHfx`) | ❌ (HFX системы нет) |
| | Language (RU/EN) | ✅ |
| **Profile** | Name | ❌ (ed modal) |
| | Goal (Lose/Maintain/Gain) | ❌ |
| | Activity | ❌ |
| | Age/Height/Weight | ❌ |
| | Kcal norm (auto + override) | ❌ |
| | Prefs + Allergies | ❌ |
| **API** | Gemini API key | ✅ |
| | Gemini model picker | ✅ |
| **Notifications** | Reminders: master toggle + breakfast/lunch/dinner time + water interval | ❌ (нужны native Android notifications + WorkManager/AlarmManager) |
| **Data** | Export CSV | ❌ |
| | Export JSON (формат совместимый с оригиналом: `version`, `exported`, `user`, `log`, `wts`, `key`, `model`, `theme`, `cal`, `hfx`, `sfx`, `notif`, `notifCfg`, `water`) | ❌ |
| | Import JSON | ❌ |
| | Reset all data | ❌ |
| **About** | Widgets (nice-to-have) | ❌ |
| | CalSnap authors modal (RJV + Rizan + dev panel по tap на 🍎) | ❌ |

---

## 8. Платформенные фичи (native only)

| Пункт | Статус | Источник |
|---|---|---|
| CameraX preview screen (photo flow) | ❌ | AGENTS.md milestone 7 |
| ML Kit barcode live scanner | ❌ | |
| Local notifications через WorkManager/AlarmManager | ❌ | Заменяет SW `_sendNotif` из `notif.js` |
| File-based JSON/CSV export/import через SAF | ❌ | |
| Share target (получить фото из других приложений) | ❌ | `share.js:_handleShareTarget` |
| App-link deep linking на `https://rjv-vi.github.io/CalSnap/…` | ✅ | Manifest intent-filter есть |
| Native sound assets (`raw/*.mp3`) | ❌ | |

---

## 9. Milestone roadmap (в порядке приоритета)

Каждый milestone = 1–3 коммита, потом push + проверка GitHub Actions.

### M1 — Design foundation polish
1. Добавить DM Sans font + прописать `FontFamily`
2. Добавить HFX утилиту (Vibrator / HapticFeedback) + точки вызова (tap/tick/medium/success/error)
3. Добавить SFX утилиту (SoundPool + raw resources `btn_tap`, `sheet_open`, `sheet_close`, `add_food`, `error`, `ai_send`, `save`, `delete`, `select`, `water_add`, `water_goal`, `water_undo`, `toggle`, `scan_success`, `barcode_scan`, `notif_ring`, `drum_tick`)
4. Сделать плавающую nav-pill в `CalSnapBottomBar`
5. Сделать tabs-pill анимацию в Add sheet tabs
6. Общий Toast host + `showToast(msg)`
7. Общий ConfirmDialog `showConfirm(icon, title, body, actionLabel, onConfirm, cancelLabel)`

### M2 — Settings parity
1. Profile section: Name/Goal/Activity/DOB/Height/Weight/Kcal norm/Prefs/Allergies + edit modals (повторить UI онбординга внутри sheet)
2. Sound + Haptic toggles
3. Notifications sheet с master toggle + 3 meal times + water interval + native scheduling
4. Data section: Export CSV, Export JSON, Import JSON (совместимый формат), Reset all
5. About modal (authors) + dev panel на 10 тапов по иконке

### M3 — Progress daily/weekly AI
1. Daily AI summary card в Progress (Gemini prompt = сегодняшний diary + профиль)
2. Weekly AI analysis card с кнопкой «Analyse» (7-дневный рацион → feedback от Gemini)
3. Кеш результатов на день/неделю
4. Error states

### M4 — AI chat parity
1. Context builder (full profile + tlog + water) перед отправкой
2. Welcome card с живым контекстом
3. Clear chat confirm
4. Back navigation на home
5. Voice input через SpeechRecognizer
6. Gemini fallback chain
7. API error cards (bad key / 403 / 429)

### M5 — Add sheet parity
1. Text tab: example chips (6 штук, tap = prefill textarea)
2. Photo tab: upzone styled block + preview после выбора + reset + ingredients chips в result
3. Barcode tab: camera/gallery inputs + OCR fallback
4. Photo result card: отображение превью + ingredients

### M6 — Food detail parity
1. Blurred food-emoji header как в оригинале (большой emoji + blur backdrop)
2. Quantity model решение (int qty vs float servings — выбрать одно и зафиксировать)
3. Hold-to-repeat на −/+
4. Drink detection + авто-water event при добавлении напитка

### M7 — Water polish
1. Caloric drink → also added to food log
2. Salt detection on today's diary → badge + goal ×1.2
3. Custom water amount bottom sheet со слайдером + preset chips (100/200/300/500/750)

### M8 — Native camera + barcode
1. CameraX preview screen для фото + снять/retake/analyze
2. ML Kit live barcode scanner
3. Ресайз фото перед отправкой в Gemini

### M9 — Streak freeze logic
1. Port `_getFreezes`, `_isFreezeUsedThisWeek` в `StreakCalculator.kt`
2. Автозаморозка раз в неделю если пропуск 1 день
3. Streak не рвётся при заморозке

### M10 — DOB drum picker
1. Кастомный 3-колоночный drum picker (day/month/year) как в `drum.js`
2. Snap on release, haptic tick на изменение
3. Заменить системный date picker в онбординге

---

## 10. Правила разработки (для ИИ)

1. **Парсинг логики** — смотреть `CalSnap/assets/js/*.js` (источник истины поведения), не доверять index.html больше чем JS.
2. **Парсинг UI/цветов** — смотреть **скриншоты первым**, CSS как вторичный источник (`base.css` содержит tokens `--streak: #FF5500`, `--bg0: #F2F0EB` и т.п.).
3. **Правила коммитов**:
   - 1 milestone ≠ 1 коммит; разбивать на 1–3 логических
   - не пушить без явного подтверждения пользователя
   - сообщения по шаблону `Verb phrase.`
4. **Проверка** — локальный gradle без JDK не работает; проверяем через GitHub Actions workflow `.github/workflows/android-debug.yml`. После каждого push получить SHA, run URL, artifact size.
5. **Не добавлять вложенные прямоугольники** — плоский black/white стиль, иконки без border, тайлы без extra background внутри карточек.
6. **Пути**:
   - worktree: `/tmp/android-calsnap-remote-check/` (клон GitHub)
   - visible copy: `/home/rjv/Downloads/CalSnaps/android-calsnap/` (только sync, не git)
7. **Никогда не коммитить оранжевые акценты в result cards Add sheet** — это уже один раз откатывалось.
8. **Favourite identity** — стабильна между изменениями servings (`FavouriteFood.withServingMultiplier`).
