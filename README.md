# NobiTerminal v23 — full source snapshot

This repo is the **complete patched app source tree for NobiTerminal v23**
(the version shipped as the v23 APK on 2026-11-08). It exists so the app's
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
> `main` (v23). The v20, v21 and v22 trees are tags `v20`, `v21` and `v22`.

## What this snapshot was made from (NEW pattern — plugin + base patches)

- **Base:** upstream `termux/termux-app`
  `@8629e632fcb95da272221be327db653fb24befe9` (2026-09-26).
- **Frozen base:** v23 is built on the frozen V19 base — generator branch
  `v23-autorepo-all-repos` `@a99980eece17c1fb9ef98eec8112e95548b23dc1`, branched
  from `v22-about-page` `@60e1823cccd9b762701c8baf775446dd69097f43`
  (itself from `v21-theme-batch` `@7885be2f41c114b256f0d95b17bfefc030c8a8c9`,
  from `v20-ui-refresh` `@d5f6cdd0770f3353afb4edd9c8e6817ee83cf95b`,
  from `frozen-v19-base` `@5df75ae247bdde23a3732afaaafbb69b7a4c4c1b`),
  which pins termux-app `8629e632` and termux-packages `f8f7bf3d488a`
  (see below). No latest-upstream tracking: the freeze order stands
  until Sam says the app is final.
- **Patches, in generator order:** first all 55 Nobi plugin patches from
  generator branch `v23-autorepo-all-repos`
  `@a99980eece17c1fb9ef98eec8112e95548b23dc1`
  (v22's 54 + one new autorepo patch, w46 — there is NO w21
  patch, see below), then the **base patches** from the generator repo's
  `f-droid-patches/app-patches/` dir — the same order the generator
  uses (`install_plugin` first, then `patch_apps`). All applied with GNU
  `patch -p1`. Plugin phase: 0 `.rej`, 0 `.orig`. Base phase: 0 `.rej`;
  `.orig` files from offset/fuzz were logged and deleted, exactly like
  the generator's `apply_patches` does.
- **Shipped v23 APK for cross-reference:** 35,524,908 bytes,
  SHA-256 `d4f8c3ac684a35957b8c483c54f2e5d06a845e25651d44d2b9476ee32f1f498c`,
  generator run `42479632772` (cache HIT, 10 min 59 s — the dc47 seed
  published by the v20 build; no bootstrap patch changed, so the cache
  key did not move and no new seed was published). Runs 1–4 failed
  pre-compile on a degraded runner-egress path to services.gradle.org
  (Gradle 9.8.2 download timeout); run 5 succeeded after egress
  recovered — no code change between runs 1 and 5,
  signer
  SHA-256 fingerprint
  `B6:DA:01:48:0E:EF:D5:FB:F2:CD:37:71:B8:D1:02:1E:C7:91:30:4B:DD:6C:4B:F4:1D:3F:AA:BA:D4:8E:E5:E1`.

### The 55 plugin patches, in apply order

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
55. `nobiterminal-w46-autorepo-all-repos.patch` (24,470 bytes)

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

## What's new in v23 (vs v22) — autorepo: all repos auto-setup

Sam's order: first time the app opens, ALL Nobi apt repos should be set up
automatically — hardcoded, like Termux's own defaults — with the `apt
update` visible in the terminal and a retry on failure, so nothing of old
Termux remains. One patch, w46, 2 files (`NobiRepoSetup.java` +
`strings.xml`).

- **10 hardcoded repo lists (was 3):** `NobiRepoSetup` now writes
  `nobi1.list` … `nobi10.list` into
  `$PREFIX/etc/apt/sources.list.d/` on first open — `nobi1` is
  `deb [trusted=yes] https://tale92266-hash.github.io/nobiterminal-repo ./`
  and `nobi2` … `nobi10` follow the same pattern (`nobiterminal-repo-N`).
  Repo-9 and repo-10 do not exist yet; their lists are written anyway,
  so the day those repos are created `apt update` picks them up with no
  app update (Sam's order 2026-11-08). Until then their 404s are
  expected and are NOT treated as failure.
- **Old Termux fully cut off:** official Termux lines are commented out
  in `sources.list` (backup rule intact: `sources.list.nobi-backup` is
  written once, never overwritten) AND in any non-nobi `*.list` file
  under `sources.list.d`. After setup, `apt` only ever talks to Nobi
  repos.
- **Visible `apt update`:** setup runs in a new "Nobi Repo Setup"
  terminal session (via `TermuxService`), so the user watches the
  progress live — step lines plus the real `apt update` output. It only
  runs when setup actually changed something (first open, or self-heal);
  later opens with everything already correct do nothing.
- **Retry on failure (two layers):** the setup script auto-retries a
  failed update 3 times (waits 5s, 15s, 30s); success means a
  `Hit:`/`Get:` line for a Nobi repo (`grep -qE
  "^(Hit|Get):.*tale92266-hash.github.io"`), so repo-9/10 404s never
  trigger a retry. If all retries fail, a Retry / Later dialog appears
  (strings `nobi_apt_update_retry` / `nobi_apt_update_later`); Retry
  re-runs setup, Later dismisses, and a failed-update marker in private
  prefs makes the next open retry instead of silently forgetting.
- **Idempotent + self-heal:** later opens skip when all 10 lists are
  correct and no official line remains; a deleted list is recreated on
  the next open; a wrong list is overwritten with the canonical line.
  All file work is off the UI thread; the terminal is never blocked.
- **Everything else is v22 as-is:** the About page rebuild (w45), the
  w30–w44 theme batch, w27 extra-keys navy, w28 drawer ripple + cyan
  icons, w29 Nobita name strings, w26 word-highlight fix, w25
  current-line fix, w20 gutter tokens, w22 breadcrumb, w23 run-output
  errors, w24 cyan secondary, and the verified pre-existing save/dirty
  chain.
- **NO w21 patch:** the save/dirty indicator chain (the would-be w21
  item) was verified to already exist, so no patch was made for it —
  the numbering skips w21 deliberately, it is not a missing file.
- **Bootstrap patch unchanged:** no bootstrap patch was modified in
  v23, so the cache key did not move (dc47) and the v23 build was a
  cache HIT. The greeting caveat stands: existing installs keep their
  first-install bootstrap greeting files.
- **Base patches (above)** — same 5 applied, same 2 sibling-only
  patches out of scope, as in v16–v22.

## Dead PNG nuance (same as v11–v20)

The 11 legacy launcher PNGs (`mipmap-*/ic_launcher.png`,
`mipmap-*/ic_launcher_round.png`) plus `drawable/banner.png` are
**present in this tree** (upstream files, 11/11 counted) but **absent
from the shipped v23 APK** — the generator deletes them at build time
(`remove_dead_nobi_assets()`, before Gradle). Do NOT claim tree == APK
for these 11 files.

## Honest limitations (same as v11–v20)

- **Package name:** the tree is `com.termux`, not `com.nobiterminal`.
  The rename is a generator build-time step (after patching). Edit
  `com.termux` paths. Building this tree directly produces a `com.termux`
  app.
- **No bootstrap:** `app/src/main/assets/` is absent. CI builds the
  bootstrap from `termux-packages` (v23 build used the frozen HEAD
  `f8f7bf3d488a`, cache HIT on the dc47 seed published by the v20
  build).
- **Signing:** `app/testkey_untrusted.jks` is in the tree; its cert
  fingerprint matches the shipped v23 APK signer exactly, so a debug
  build from this tree installs over the shipped v23.
- **ACS:** `main` will NOT build directly in a phone IDE. With the base
  patches, TWO modules still wire `externalNativeBuild` ndkBuild
  (`terminal-emulator` → `libtermux`, `termux-shared` →
  `liblocal-socket`) — the `app` module no longer does (see
  `local-bootstraps` above). The phone IDE cannot run the inner
  `ndk-build` (v4 failed at `:termux-shared:configureNdkBuildDebug`,
  exit 126). No `acs-prebuilt` branch exists for v23. Gradle wrapper jar IS in
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
