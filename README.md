# Compose UI Lab

A growing collection of custom Jetpack Compose components — built from scratch, with no
Material components underneath. Every component is driven by a small design-token system
(color, type, spacing, motion), so the whole library moves with one consistent personality.

Built by a senior mobile engineer (8 years, fintech background) who cares about the small
details that make an interface feel premium — spring physics, micro-interactions, haptics.

![demo](docs/demo.gif)
<!-- Record the running app (5–10s), export a light GIF, and drop it at docs/demo.gif -->

## Component 1 — Animated Bottom Navigation

A bottom navigation bar with a morphing, spring-driven indicator.

**What it demonstrates**
- Spring physics with a subtle overshoot (no linear tweens)
- A pill indicator that slides between destinations
- Per-icon color transition + a bouncy settle on selection
- Label revealed only for the active destination (expanding-pill feel)
- Custom press feedback (no Material ripple) + haptics on select
- Semantics for accessibility, works with 3–5 items, light & dark

**Built on a design-token layer**
`designsystem/token` defines `LabColors`, `LabTypography`, `LabSpacing`, `LabShapes`
and a central `LabMotion`. Components reference roles (e.g. `LabTheme.colors.accent`),
never raw values — the same approach used in production design systems.

## Component 2 — Canvas Charts

A line chart and a donut chart, both drawn from scratch on `Canvas` — no charting
library underneath.

**Line chart**
- Smooth Catmull-Rom curve through the points (no jagged joints)
- Draws itself in left-to-right whenever the data changes
- Gradient area fill under the line
- Touch to scrub: drag across to read any point — a guide line, focus dot and value
  tooltip follow the finger, with a light haptic tick per step

**Donut chart**
- Sweeps in clockwise from the top on data change
- Tap a slice to select it: the slice springs outward, the rest dim back, and the
  center switches to that slice's share; tap again to deselect
- Slice colors come from the token palette — callers pass only label + value

Both expose an accessible summary and a live selected-value through semantics, and read
their colors, motion and type from the same token layer as everything else.

## Tech

Jetpack Compose · Kotlin 2.0 · Material3 (icons/text only) · AGP 8.5 · minSdk 24

## Run

1. Open the project root in Android Studio (Koala or newer).
2. Let it sync — Android Studio brings its own JDK 17 and downloads Gradle 8.9.
3. Run the `app` configuration on a device or emulator.

> CLI builds: the project uses the Gradle wrapper. If `./gradlew` reports a missing
> `gradle-wrapper.jar`, generate it once with `gradle wrapper` (Android Studio's IDE
> sync does not need it).

## Project structure

```
app/src/main/java/com/uilab/showcase/
├── MainActivity.kt
├── designsystem/
│   ├── token/        (Color, Typography, Spacing, Shapes, Motion)
│   └── theme/        (LabTheme + CompositionLocals)
├── components/
│   ├── bottomnav/    (LabBottomNav — component 1)
│   └── chart/        (LabLineChart, LabDonutChart — component 2)
└── catalog/          (host + interactive demo screens)
```

## Roadmap

- [x] Animated bottom navigation
- [x] Custom Canvas chart (line / donut)
- [ ] Swipeable card stack
- [ ] Morphing FAB
- [ ] Shimmer skeleton loaders

## License

MIT
