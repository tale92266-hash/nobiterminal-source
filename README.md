# NobiTerminal v20 — full source snapshot

This repo is the **complete patched app source tree for NobiTerminal v20**
(the version shipped as the v20 APK on 2026-10-27). It exists so the app's
source can be browsed, understood, and edited by hand in one place.

> This snapshot is for viewing and editing. It is **not** the build route.
> The real build route is the generator repo
> (`tale92266-hash/nobiterminal`): it clones upstream, applies the Nobi
> patches, renames the package, builds the bootstrap and the app in GitHub
> Actions, and produces the APK. Edits made here do **not** flow into
> generator builds automatically — to ship an edit, it must become a patch
> in the generator repo.

## What this snapshot was made from (NEW pattern — plugin + base patches)

- **Base:** upstream `termux/termux-app`
  `@8629e632fcb95da272221be327db653fb24befe9` (2026-09-26).
- **Frozen base:** v20 is built on the frozen V19 base — generator branch
  `v20-ui-refresh` `@d5f6cdd0770f3353afb4edd9c8e6817ee83cf95b`, branched
  from `frozen-v19-base` `@5df75ae247bdde23a3732afaaafbb69b7a4c4c1b`,
  which pins termux-app `8629e632` and termux-packages `f8f7bf3d488a`
  (see below). No latest-upstream tracking: the freeze order stands
  until Sam says the app is final.
- **Patches, in generator order:** first all 38 Nobi plugin patches from
  generator branch `v20-ui-refresh`
  `@d5f6cdd0770f3353afb4edd9c8e6817ee83cf95b`
  (v19's 35 + three new app patches, w27/w28/w29 — there is NO w21
  patch, see below), then the **base patches** from the generator repo's
  `f-droid-patches/app-patches/` dir — the same order the generator
  uses (`install_plugin` first, then `patch_apps`). All applied with GNU
  `patch -p1`. Plugin phase: 0 `.rej`, 0 `.orig`. Base phase: 0 `.rej`;
  `.orig` files from offset/fuzz were logged and deleted, exactly like
  the generator's `apply_patches` does.
- **Shipped v20 APK for cross-reference:** 35,502,044 bytes,
  SHA-256 `01e08215b485384cb7f35fb995d9f87ba1ff9dfb42955f234ffc56c90ccdb6c7`,
  generator run `40476932922` (cache MISS build, ~76 min — the
  bootstrap patch changed, so the cache key moved and this run
  published the new seed asset itself; see bootstrap note below),
  signer
  SHA-256 fingerprint
  `B6:DA:01:48:0E:EF:D5:FB:F2:CD:37:71:B8:D1:02:1E:C7:91:30:4B:DD:6C:4B:F4:1D:3F:AA:BA:D4:8E:E5:E1`.

### The 38 plugin patches, in apply order

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

## What's new in v20 (vs v19)

- **w27 — extra-keys navy (terminal):** the bottom extra-keys buttons
  are no longer black. `values/themes.xml` and `values-night/themes.xml`
  now use `@color/nobi_navy` (`#FF0A0F24`, same navy as the tabs bar)
  for `extraKeysButtonBackgroundColor`, `@color/nobi_cyan` (`#FF22D3EE`)
  for the active text color, and the new `@color/nobi_key_pressed`
  (`#FF1E2C55`) for the pressed background — in both light and dark
  themes. `nobi_cyan` and `nobi_key_pressed` are defined in
  `values/colors.xml`.
- **w28 — drawer ripple + cyan icons (terminal):** all 11 drawer menu
  rows in `layout/activity_termux.xml` (`nobi_menu_new_window`,
  `nobi_menu_nobicode`, `nobi_menu_new_session`,
  `nobi_menu_quick_commands`, `nobi_menu_find`, `nobi_menu_server`,
  `nobi_menu_session_tools`, `nobi_menu_output_editor`,
  `nobi_menu_permissions`, `nobi_menu_settings`, `nobi_menu_exit`) now
  have `android:background="?attr/selectableItemBackground"`, so a
  visible ripple/press effect shows on tap; their icons are tinted
  `@color/nobi_cyan` instead of the theme drawer tint.
- **w29 — name Nobita (NobiCode examples):** the three user-visible
  example strings that said "Sam" now say "Nobita": the JSON hint in
  `MainActivity.kt` (`{"name": "Nobita"}`) and the Python cheat-sheet
  lines in `FollowupCheatSheets.kt` (`name = "Nobita"`,
  `user = {"name": "Nobita", "age": 20}`). The remaining "Sam"
  occurrences in this tree are code comments only (developer notes
  such as "keys Sam asked to remove") — no user-visible string still
  says "Sam".
- **Bootstrap greeting Sam → Nobita (not in this tree, recorded
  honestly):** the terminal's "Welcome back, Sam." / "Good morning,
  Sam." greeting is printed by the bootstrap login scripts, not by
  app code — it lives in the bootstrap patch
  `f-droid-patches/bootstrap-patches/name-change-helper.patch`
  (7,226 bytes, sha256 `3088c616…`), which patches only
  `termux-packages` files (`packages/...` targets, zero `termux-app/`
  targets). That patch now prints "Welcome back, Nobita." and the
  time-aware greeting with "Nobita". Because it changed, the bootstrap
  cache key's PATCH_HASH moved (`0ca27c36767e` → `dc474e2e8a5e`), so
  the v20 build was a cache MISS and published the new seed asset
  `bootstrap-aarch64-f8f7bf3d488a-8629e632fcb9-dc474e2e8a5e-59a8c6e3-com.nobiterminal.tar.gz`
  (21,554,576 bytes, 2026-10-27T10:10:55Z). The bootstrap greeting
  change takes effect on fresh installs (existing installs keep their
  first-install bootstrap files — same caveat as the v20 freeze note).
- **Everything else is v19 as-is:** w26 word-highlight fix, w25
  current-line fix, w20 gutter tokens, w22 breadcrumb, w23 run-output
  errors, w24 cyan secondary, and the verified pre-existing save/dirty
  chain.
- **NO w21 patch:** the save/dirty indicator chain (the would-be w21
  item) was verified to already exist, so no patch was made for it —
  the numbering skips w21 deliberately, it is not a missing file.
- **Base patches (above)** — same 5 applied, same 2 sibling-only
  patches out of scope, as in v16–v19.

## Dead PNG nuance (same as v11–v19)

The 11 legacy launcher PNGs (`mipmap-*/ic_launcher.png`,
`mipmap-*/ic_launcher_round.png`) plus `drawable/banner.png` are
**present in this tree** (upstream files, 11/11 counted) but **absent
from the shipped v20 APK** — the generator deletes them at build time
(`remove_dead_nobi_assets()`, before Gradle). Do NOT claim tree == APK
for these 11 files.

## Honest limitations (same as v11–v19)

- **Package name:** the tree is `com.termux`, not `com.nobiterminal`.
  The rename is a generator build-time step (after patching). Edit
  `com.termux` paths. Building this tree directly produces a `com.termux`
  app.
- **No bootstrap:** `app/src/main/assets/` is absent. CI builds the
  bootstrap from `termux-packages` (v20 build used the frozen HEAD
  `f8f7bf3d488a`, cache MISS — this run published the new dc47 seed
  asset itself, see above).
- **Signing:** `app/testkey_untrusted.jks` is in the tree; its cert
  fingerprint matches the shipped v20 APK signer exactly, so a debug
  build from this tree installs over the shipped v20.
- **ACS:** `main` will NOT build directly in a phone IDE. With the base
  patches, TWO modules still wire `externalNativeBuild` ndkBuild
  (`terminal-emulator` → `libtermux`, `termux-shared` →
  `liblocal-socket`) — the `app` module no longer does (see
  `local-bootstraps` above). The phone IDE cannot run the inner
  `ndk-build` (v4 failed at `:termux-shared:configureNdkBuildDebug`,
  exit 126). No `acs-prebuilt` branch exists for v20. The shipped v20
  APK contains exactly `libtermux.so` + `liblocal-socket.so`
  (× 4 ABIs), matching the two wired modules. Gradle wrapper jar IS in
  the tree, so ACS import does not hang on the wrapper.
- **Upstream README:** upstream's `README.md` was renamed to
  `README_UPSTREAM.md` (content byte-identical); this file is the
  snapshot doc.
- **Binary hunks:** GNU `patch` strips git binary hunks, exactly like
  CI — patch-intended binary files are absent here, as in the CI tree.
- **Edits here don't ship:** to ship an edit, it must become a patch
  in the generator repo.
