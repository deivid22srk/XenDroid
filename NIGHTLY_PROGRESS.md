# XenDroid Overnight Emulator-Core Improvement Log

Session context: continuous improvement loop on the emulator core (game
compatibility + emulation performance). All changes land on feature branches
off `main`, validated by the existing `.github/workflows/XenDroid.yml` build
(green = compile-valid for arm64, NDEBUG). Evidence is recorded here per branch.

Host: aarch64 proot (no local Android SDK/NDK) -> CI-only compile validation.

---

## Session start state
- `main` HEAD: `9b4dbd1` "Fix settings rows: reflow long text, keep switches on-screen"
- CI green (run 30725229122), release `XenDroid-9b4dbd1` APK shipped.
- Core snapshot: xenia-edge rebased on xenia-canary `dc561e4df` (2026-04-25).

---

## Phase 1 - Diagnosis (read-only audits)
Sub-agent findings:
- Active stack: a64-only JIT (PPC->HIR->ARM64, xbyak_aarch64, 7 allocatable
  GPRs x22-x28), classic `gpu/vulkan` (SPIR-V translator), AAudio audio
  (cvar `apu=aaudio`), AndroidInputDriver via JNI.
- Snapshot identity confirmed: xenia-edge @ canary `dc561e4df`.

### Bug confirmed (HIGH confidence): XMP audio broken over AAudio
Files/evidence:
- `xe_aaudio_audio_system.cpp` `CreateDriver(sem, freq, ch, need_conv)` was a
  `//FIXME` stub ignoring both `channels` and `need_format_conversion`.
- AAudio callback unconditionally ran
  `conversion::sequential_6_BE_to_interleaved_2_LE` (guest 5.1 big-endian) on
  every queued block, including XMP blocks, which are interleaved little-endian
  floats at the file's channel count (2 typically) ->
  XMP music decoded as 6ch BE = garbled.
- Root cause class: AAudio driver hardcoded guest-6ch-BE semantics; SDL
  backend already honors `need_format_conversion_`/`channels`.

## Branch 1: `fix/aaudio-xmp-format`
Goal: XMP (Xbox Music Player) audio plays correctly over AAudio, mirroring the
SDL backend's format semantics without touching the main render path.

Changes:
- `xe_aaudio_audio_driver.h`: constructor takes `(frequency, channels,
  need_format_conversion)` with defaults preserving the main path (48000, 6,
  true). Per-instance source block sizing, cursor state, `last_block_` grown to
  the largest block (3072 floats).
- `xe_aaudio_audio_driver.cpp`:
  - Constructor sanitizes channel counts (asserts are no-ops under NDEBUG).
  - `AudioCallback` dispatches to new `XmpAudioCallback` when
    `!need_format_conversion_`.
  - `XmpAudioCallback`: cursor-based drain (a 1536-sample block spans multiple
    AAudio callbacks), copies/folds LE source to stereo, reuses gain, fade-in,
    gap concealment, stats.
  - `CopyToLastBlock`: 1ch (duplicate), 2ch (memcpy), 6ch (fold, same
    coefficients as the BE converter minus byte swap).
  - `BuildStream` opens at the source's own sample rate.
  - `Shutdown` frees the drain cursor block.
- `xe_aaudio_audio_system.cpp`: `CreateDriver` stub now forwards the real args.

Validation: XenDroid.yml CI build (compile-valid). Main render path untouched
(byte-identical conversion branch).
Status: PENDING CI.

## Branch 1 - follow-up (independent critical review)
Reviewer verdict on b36e565: REWORK. Confirmed main-path regression risk NONE
(conversion branch byte-identical vs parent). Found MAJOR: semaphore released
once per callback while a stereo block spans 3 callbacks (mono: 6) -> producer
over-submits 3-6x, pre-buffers the whole song, truncates it when the playlist
advances. Fix (48f1eab): release once per queued block fully drained; keep the
gap-path wakeup release. Also logged unsupported channel counts, fixed the
'played' stat to use the per-driver output block, zeroed last_block_ tail to the
full output block, documented Shutdown cursor safety.
CI green on b36e565; 48f1eab CI pending.

## Branch 2: perf/batch-perf-map-writes
The a64 perf-map (cvar a64_perf_map, default ON) called fflush per placed
function -> one write() syscall per JIT translation. Batching flushes every 512
entries (kPerfMapFlushBatch) cuts syscalls ~512x; fclose at teardown flushes the
tail; simpleperf polls the map so live attribution is preserved.
Reviewed independently: SHIP (no races; counter only under perf_map_mutex_).
CI green (54186c3). Added explicit <cstdint> include.

## Branch 3: gpu/android-present-mode-mailbox
Vulkan swapchain created with IMMEDIATE present mode (no vsync) as 1st
priority on all platforms -> visible tearing on Android's fixed-refresh
displays. On the Android build (XE_PLATFORM_xendroid) mailbox is now preferred
(1st priority); desktop keeps immediate-first for VRR/latency. The immediate
cvar still allows it as a fallback. Cvar descriptions updated to match.
CI green (578abc7).

## GPU audit summary (from Vulkan extension audit)
Vulkan path is likely to boot on typical 2024-2026 Adreno/Mali: only hard
requirements are independentBlend + VK_KHR_swapchain + VK_KHR_android_surface
(all universal); every other extension/feature is optional with in-renderer
fallbacks (audit list in session notes).
- HIGH: sparse residency absent on proprietary Adreno/Mali -> dense 512MB
  shared buffer fallback (vulkan_shared_memory.cc:95-235); on low-RAM devices
  an allocation failure hard-aborts.
- MED: no graceful null-GPU fallback; init/device-loss = FatalError
  (vulkan_graphics_system.cc:32-42). Change is opinionated (silent black
  screen) -> NOT PR'd, journaled.
- Present-mode default fixed by Branch 3.

## Fronts evaluated and deferred (evidence recorded, not PR'd)
- a64 GPR pool (7 allocatable) enlargement: high-risk structural change, needs
  on-device benchmarking.
- GPU graceful fallback on init failure: masks real GPU problems.
- Memory M1/M2/M3/M4/M5: M1 dead code on Android (no save states in app);
  M2 subtle protection semantics; M3/M4/M5 low-impact. All journaled with
  file:line for future work.

## Memory audit findings (from 16KB-page audit) - journaled, not PR'd
- HIGH H1: single-attempt MAP_FIXED 32GB guest window, no retry, silent clobber
  (memory.cc:228-250, memory_posix.cc:215/508). No MAP_FIXED_NOREPLACE in tree.
- MED M1: BaseHeap::Save/Restore calls raw 4KB mprotect
  (memory.cc:990-999, 1028-1043) -> EINVAL on 16KB hosts. BUT the Android
  frontend (xendroid_emu.cpp) never invokes Save/Restore -> dead path on
  Android. Fix deferred; revisit if save states are added.
- MED M2: ShouldSkipHostCommit (memory.cc:119-126) skips ALL commits when
  page_size()>4KB, leaving NoAccess/ReadOnly guest pages host-RW. Real
  protection-semantics hole, but risky to change without on-device validation.
  Deferred.
- MED M3/M4/M5: trampoline scan uses destructive MAP_FIXED probe
  (a64_backend.cc:744-762); MMIO commits need 16KB-aligned bases (current
  ranges are 64KB-aligned, fine); release-time protect skipped. Deferred.
- Verified safe: BaseHeap::Protect aggregation is host-page aware
  (memory.cc:1605-1650); physical-heap 0xE0000000 alias works on both page
  sizes; guest stacks aligned to heap page size; ASharedMemory used.

