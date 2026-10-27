# NobiTerminal v22 — full source snapshot

This repo is the **complete patched app source tree for NobiTerminal v22**
(the version shipped as the v22 APK on 2026-10-27). It exists so the app's
source can be browsed, understood, and edited by hand in one place.

> This snapshot is for viewing and editing. It is **not** the build route.
> The real build route is the generator repo
> (`tale92266-hash/nobiterminal`): it clones upstream, applies the Nobi
> patches, renames the package, builds the bootstrap and the app in GitHub
> Actions, and produces the APK. Edits made here do **not** flow into
> generator builds automatically — to ship an edit, it must become a patch
> in the generator repo.

> **Single-repo rule (Sam's order, 2026-10-27):** this ONE repo is updated
> in place for every version — no new repo per version. `main` always
> holds the latest shipped version's tree; each version is also kept as a
> lightweight tag (`v20`, `v21`, …). This file describes the tree at
> `main` (v22). The v20 and v21 trees are tags `v20` and `v21`.

## What this snapshot was made from (NEW pattern — plugin + base patches)

- **Base:** upstream `termux/termux-app`
  `@8629e632fcb95da272221be327db653fb24befe9` (2026-09-26).
- **Frozen base:** v22 is built on the frozen V19 base — generator branch
  `v22-about-page` `@60e1823cccd9b762701c8baf775446dd69097f43`, branched
  from `v21-theme-batch` `@7885be2f41c114b256f0d95b17bfefc030c8a8c9`
  (itself from `v20-ui-refresh` `@d5f6cdd0770f3353afb4edd9c8e6817ee83cf95b`,
  from `frozen-v19-base` `@5df75ae247bdde23a3732afaaafbb69b7a4c4c1b`),
  which pins termux-app `8629e632` and termux-packages `f8f7bf3d488a`
  (see below). No latest-upstream tracking: the freeze order stands
  until Sam says the app is final.
- **Patches, in generator order:** first all 54 Nobi plugin patches from
  generator branch `v22-about-page`
  `@60e1823cccd9b762701c8baf775446dd69097f43`
  (v21's 53 + one new About-page patch, w45 — there is NO w21
  patch, see below), then the **base patches** from the generator repo's
  `f-droid-patches/app-patches/` dir — the same order the generator
  uses (`install_plugin` first, then `patch_apps`). All applied with GNU
  `patch -p1`. Plugin phase: 0 `.rej`, 0 `.orig`. Base phase: 0 `.rej`;
  `.orig` files from offset/fuzz were logged and deleted, exactly like
  the generator's `apply_patches` does.
- **Shipped v22 APK for cross-reference:** 35,520,586 bytes,
  SHA-256 `0e1d93bef95bd4c928cd3fe6ae0519f4a279652121e63f3df7c49a6868035221`,
  generator run `40505228665` (cache HIT, 10 min 38 s — the dc47 seed
  published by the v20 build; no bootstrap patch changed, so the cache
  key did not move and no new seed was published),
  signer
  SHA-256 fingerprint
  `B6:DA:01:48:0E:EF:D5:FB:F2:CD:37:71:B8:D1:02:1E:C7:91:30:4B:DD:6C:4B:F4:1D:3F:AA:BA:D4:8E:E5:E1`.

### The 54 plugin patches, in apply order

1. `nobiterminal-app-name.patch`
2. `nobiterminal-drawer-default.patch`
3. `nobiterminal-neon-default.patch`
4. `nobiterminal-settings-suite.patch`
5. `nobiterminal-tabs-bar.patch`
6. `nobiterminal-v5-core.patch`
7. `nobiterminal-v5-drawer.patch`
8. `nobiterminal-v5-icon-splash.patch`
9. `nobiterminal-v5-nobicode-icons-a.patch`
10. `nobiterminal-v5-nobicode-icons-b.patch`
11. `nobiterminal-v5-nobicode-res.patch`
12. `nobiterminal-v5-nobicode-src.patch`
13. `nobiterminal-v5-tabs-rename.patch`
14. `nobiterminal-v6-batch.patch`
15. `nobiterminal-v7-batch.patch`
16. `nobiterminal-v7-fix.patch`
17. `nobiterminal-v7-fix2.patch`
18. `nobiterminal-v8-extrakeys.patch`
19. `nobiterminal-v9-drawer-notify.patch`
20. `nobiterminal-w10-editor-permissions.patch`
21. `nobiterminal-w11-followup.patch`
22. `nobiterminal-w12-fix-duplicate-permissions.patch`
23. `nobiterminal-w13-fix-vectors-kotlin.patch`
24. `nobiterminal-w14-autorepo.patch`
25. `nobiterminal-w15-keys-settings-theme.patch`
26. `nobiterminal-w16-settings-follow-terminal.patch`
27. `nobiterminal-w17-settings-toolbar-identity.patch`
28. `nobiterminal-w18-drawer-hides-keyboard.patch` (2,377 bytes)
29. `nobiterminal-w19-dialog-hides-keyboard.patch` (5,902 bytes)
30. `nobiterminal-w20-gutter-tokens.patch` (1,434 bytes)
31. `nobiterminal-w22-file-breadcrumb.patch` (3,672 bytes)
32. `nobiterminal-w23-run-output-errors.patch` (2,218 bytes)
33. `nobiterminal-w24-secondary-cyan.patch` (1,589 bytes)
34. `nobiterminal-w25-current-line-offset.patch` (1,037 bytes)
35. `nobiterminal-w26-word-highlight-offset.patch` (1,070 bytes)
36. `nobiterminal-w27-extrakeys-navy.patch` (2,494 bytes)
37. `nobiterminal-w28-drawer-ripple-icons.patch` (9,529 bytes)
38. `nobiterminal-w29-name-nobita.patch` (1,944 bytes)
39. `nobiterminal-w30-settings-controls-cyan.patch` (1,378 bytes)
40. `nobiterminal-w31-settings-body-tint.patch` (4,026 bytes)
41. `nobiterminal-w32-settings-row-icons.patch` (36,107 bytes)
42. `nobiterminal-w33-toolbar-title-popup.patch` (3,555 bytes)
43. `nobiterminal-w34-dialogs-navy-cyan.patch` (4,561 bytes)
44. `nobiterminal-w35-message-dialogs-daynight.patch` (3,454 bytes)
45. `nobiterminal-w36-drawer-navy-day.patch` (878 bytes)
46. `nobiterminal-w37-terminal-accents-cyan.patch` (2,048 bytes)
47. `nobiterminal-w38-textinput-cursor-cyan.patch` (2,009 bytes)
48. `nobiterminal-w39-scrollbar-thumb.patch` (1,194 bytes)
49. `nobiterminal-w40-bootstrap-progress.patch` (3,220 bytes)
50. `nobiterminal-w41-report-toolbar-navy.patch` (1,969 bytes)
51. `nobiterminal-w42-help-navy-cyan.patch` (3,316 bytes)
52. `nobiterminal-w43-playback-terminal-like.patch` (1,918 bytes)
53. `nobiterminal-w44-hubs-match-settings.patch` (12,133 bytes)
54. `nobiterminal-w45-about-page.patch` (8,896 bytes)

### The 7 base patches — 5 applied, 2 out of scope (read this)

The generator applies 7 base patches after the plugin patches. Five of
them touch files inside this `termux-app` tree and ARE applied here, in
lexical order:

1. `auto-hide-extra-keys.patch` — extra-keys toolbar hides when a
   hardware keyboard is connected (new preference
   `shouldExtraKeysBeVisible` / `isExtraKeysOnlyIfNoHardware`).
2. `increase-gradle-memory.patch` — `gradle.properties` JVM heap
   2048M → 8192M (build-only setting).
3. `local-bootstraps.patch` — removes the `app` module's
   `externalNativeBuild` ndkBuild block (and APK splits) from
   `app/build.gradle`, plus a `TermuxInstaller.java` change. This is why
   the shipped APK contains NO `libtermux-bootstrap.so`: the CI tree
   does not build it either.
4. `local-termux-am-library.patch` — `termux-shared/build.gradle` uses
   `project(":termux-am-library")` and `settings.gradle` includes
   `:termux-am-library`. NOTE: the module's sources themselves are NOT
   in this tree — CI moves them into `termux-app/` during its download
   step, before patching. So `settings.gradle` references a module
   whose sources arrive via CI, not via this snapshot.
5. `multi-window.patch` — multi-window support: `TerminalView`
   `attachSession` now returns the previous session (was boolean),
   `detachSession()` added, manifest + activity/service/session
   changes. NOTE: this patch is a concatenation of several upstream
   commits, including an add-then-remove pair for a `mAttached` field
   in `TerminalSession.java` — net effect on that file is zero, which
   is the correct CI result, not a missing change.

Two base patches are NOT applied here, and that is deliberate, not an
oversight — they contain ZERO `termux-app/` targets, so there is nothing
in this tree for them to patch:

6. `local-maven.patch` — touches only `termux-tasker/`, `termux-float/`,
   `termux-api/`, `termux-widget/` build files (sibling apps in the
   generator's `termux-apps-main` dir). Those apps are not built in
   NobiTerminal builds (all disabled) and are not part of this
   `termux-app`-only snapshot.
7. `local-termux-gui.patch` — touches only `termux-gui/app/build.gradle`
   (sibling app, also not built here).

So this tree is the full CI tree **for the `termux-app` module**: every
base-patch effect that lands inside `termux-app` is present; the two
sibling-app-only patches have no effect on this module by construction.

## What's new in v22 (vs v21) — About page rebuild

Sam's order: the About page should be about NobiTerminal itself, with a
clear credit to Termux and its developers, Termux links, and a nice page
with smileys — plus a back arrow in the header. One patch, w45, 4 files.

- **About header (new, About-only content):** built in code in
  `SettingsActivity.configureAboutPreference` (the About content was
  already 100% code-built; no `strings.xml` churn), placed before the
  unchanged App Info / Device Info / Important Links blocks, so the
  version info stays exactly as already reported:
  - `# NobiTerminal ✨` — H1 uses `TermuxConstants.TERMUX_APP_NAME`
    (`"NobiTerminal"`); the sparkle is the Java escape `\u2728`.
  - `NobiTerminal is a terminal app based on Termux, with its own apt
    repositories and the Nobi theme.` — grounded in the tree: upstream
    fork @ 8629e632, the three `nobiterminal-repo[-2/-3]` deb lines in
    `NobiRepoSetup.java`, and the `nobi_navy`/`nobi_cyan` theme tokens.
    No feature invented.
  - `## Thanks 🙏` — the folded-hands sign is the Java surrogate-pair
    escape `\uD83D\uDE4F`.
  - `Thanks, and all credit goes to Termux and its developers.` —
    Sam's wording, verbatim, no emoji inside the sentence.
  - `## Termux Links` + 6 markdown links. Every URL is a
    `TermuxConstants` constant already present in the tree (the same
    constants `getImportantLinksMarkdownString` uses) — the patch
    references the constant, never a literal URL: Termux website
    (`TERMUX_SITE_URL`), Termux on GitHub
    (`TERMUX_GITHUB_REPO_URL`), Termux packages on GitHub
    (`TERMUX_PACKAGES_GITHUB_REPO_URL`), Termux Wiki
    (`TERMUX_WIKI_URL`), Termux support email
    (`TERMUX_SUPPORT_EMAIL_MAILTO_URL`), r/termux on Reddit
    (`TERMUX_REDDIT_SUBREDDIT_URL`). Links open via Markwon's default
    `ACTION_VIEW` handling (no new link code). Deliberately NOT
    included: `TERMUX_DONATE_URL` — the Donate row is hidden for
    Play-signed builds (Play policy gate in
    `SettingsActivity.configureDonatePreference`); surfacing it
    unconditionally in About would bypass that gate. The legacy
    Important Links block (all 8 GitHub repos + email + Reddit +
    wikis) is kept unchanged below the new section, so no existing
    link was dropped.
- **Header back arrow (Sam's follow-up order):** `ReportActivity`
  shows the real ActionBar up drawable (white title, white nav-icon
  tint, both modes) via the shared helper
  `AppCompatActivityUtils.setShowBackButtonInActionBar` — the exact
  mechanism SettingsActivity uses (v17) — plus `onSupportNavigateUp`
  → `onBackPressed`. This applies to ALL report screens (About,
  transcript issue reports, crash reports, plugin reports), the same
  scope w41's navy toolbar already took.
- **Body tint:** `Theme.NobiReport.DayNight` gains the w31 tokens
  (body surface `#FFF4F6FC` day / `#FF101730` night, secondary text
  navy day / periwinkle night), plus a new night twin with cyan links
  at night (9.79:1 on the night body; day keeps the inherited blue
  `#0969DA`, 4.81:1 — cyan on the day body is 1.67:1 and was rejected
  on contrast grounds). White on navy 18.98:1; navy on day body
  17.56:1; periwinkle on night body 7.38:1. Solid theme-controlled
  colors only; no gradients, no blur, no grey secondary text. The
  header is built on the existing background thread — no new
  UI-thread work, no new dependencies.
- **Preserved byte-for-byte in construction:** the Share / Copy /
  Save-to-file menu, the save-file path (`NobiTerminal-about.log`),
  the system-back behaviour (`finishAndRemoveTask`), and the full App
  Info / Device Info / Important Links blocks. Nothing removed.
  Report body tint follows the app's night mode
  (`ReportActivity.setNightMode`), not Settings' terminal-luminance
  rule — pre-existing behaviour, untouched.
- **Everything else is v21 as-is:** the w30–w44 theme batch (Settings
  controls/body/icons, toolbar title + popup, dialogs navy/cyan,
  drawer navy in day mode, terminal accents, text-input cursor,
  scrollbar, bootstrap progress dialog, Report/Help/Playback themes,
  hubs), w27 extra-keys navy, w28 drawer ripple + cyan icons, w29
  Nobita name strings, w26 word-highlight fix, w25 current-line fix,
  w20 gutter tokens, w22 breadcrumb, w23 run-output errors, w24 cyan
  secondary, and the verified pre-existing save/dirty chain.
- **NO w21 patch:** the save/dirty indicator chain (the would-be w21
  item) was verified to already exist, so no patch was made for it —
  the numbering skips w21 deliberately, it is not a missing file.
- **Bootstrap patch unchanged:** no bootstrap patch was modified in
  v22, so the cache key did not move (dc47) and the v22 build was a
  cache HIT. The greeting caveat stands: existing installs keep their
  first-install bootstrap greeting files.
- **Base patches (above)** — same 5 applied, same 2 sibling-only
  patches out of scope, as in v16–v21.

## Dead PNG nuance (same as v11–v20)

The 11 legacy launcher PNGs (`mipmap-*/ic_launcher.png`,
`mipmap-*/ic_launcher_round.png`) plus `drawable/banner.png` are
**present in this tree** (upstream files, 11/11 counted) but **absent
from the shipped v22 APK** — the generator deletes them at build time
(`remove_dead_nobi_assets()`, before Gradle). Do NOT claim tree == APK
for these 11 files.

## Honest limitations (same as v11–v20)

- **Package name:** the tree is `com.termux`, not `com.nobiterminal`.
  The rename is a generator build-time step (after patching). Edit
  `com.termux` paths. Building this tree directly produces a `com.termux`
  app.
- **No bootstrap:** `app/src/main/assets/` is absent. CI builds the
  bootstrap from `termux-packages` (v22 build used the frozen HEAD
  `f8f7bf3d488a`, cache HIT on the dc47 seed published by the v20
  build).
- **Signing:** `app/testkey_untrusted.jks` is in the tree; its cert
  fingerprint matches the shipped v22 APK signer exactly, so a debug
  build from this tree installs over the shipped v22.
- **ACS:** `main` will NOT build directly in a phone IDE. With the base
  patches, TWO modules still wire `externalNativeBuild` ndkBuild
  (`terminal-emulator` → `libtermux`, `termux-shared` →
  `liblocal-socket`) — the `app` module no longer does (see
  `local-bootstraps` above). The phone IDE cannot run the inner
  `ndk-build` (v4 failed at `:termux-shared:configureNdkBuildDebug`,
  exit 126). No `acs-prebuilt` branch exists for v22. Gradle wrapper jar IS in
  the tree, so ACS import does not hang on the wrapper.
- **Upstream README:** upstream's `README.md` was renamed to
  `README_UPSTREAM.md` (content byte-identical); this file is the
  snapshot doc.
- **Binary hunks:** GNU `patch` strips git binary hunks, exactly like
  CI — patch-intended binary files are absent here, as in the CI tree.
- **Day-mode rendering is inferred:** Sam's screenshots so far are
  night-mode; day twins follow the `values/` vs `values-night/` attrs
  + the v16 luminance rule. On-device day-mode check still needed.
- **Edits here don't ship:** to ship an edit, it must become a patch
  in the generator repo.
