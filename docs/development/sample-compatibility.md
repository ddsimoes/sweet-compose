# Sample Compatibility Modules

This document describes the refactored sample layout used to validate API compatibility between Sweet and Compose-style APIs.

## Modules

- `:sample-sweet`
  - Holds all shared sample composables, data models, and small app trees.
  - Contains SWT / AutoSWT based tests that exercise the shared samples.
  - Source is under `sample-sweet/src/main/kotlin/io/github/ddsimoes/sweet/sample`.
  - Test source is under `sample-sweet/src/test/kotlin`.
  - Only uses `androidx.compose.*` and standard Kotlin APIs (plus a few Sweet debug helpers where needed).
  - Depends on `:sweet` for its Compose API surface.
  - Gradle task: `./gradlew :sample-sweet:test`.

- `:sample-jetbrains`
  - Module for testing the same samples but with actual JetBrains Compose Desktop
  - Use hardlinks from :sample-sweet to compile check API and to compare behavior

The old `:sample` module has been replaced by these two modules and is no longer part of the Gradle build.

## Hardlink Pattern for Cross-Runtime Sources

Some sample sources (e.g. `DrawingKitchenSink.kt`) exist as **hardlinks** between
`sample-sweet/src/main/kotlin/` and `sample-jetbrains/src/main/kotlin/`. Both paths
point to the same inode — the identical source file is compiled against two different
Compose runtimes in two different Gradle modules:

- `sample-sweet` compiles against `:sweet` (Sweet's Compose implementation).
- `sample-jetbrains` compiles against `org.jetbrains.compose.desktop` (JetBrains
  Compose Desktop / MPP).

This guarantees that the exact same code builds and behaves correctly under both
implementations, validating API parity from a single source of truth. Changes made
through either path automatically stay in sync because they are the same file.

When adding a new cross-runtime sample that should compile against both runtimes,
create the source in one module and hardlink it at the corresponding path in the
other:

```bash
ln sample-sweet/src/main/kotlin/io/github/ddsimoes/sweet/sample/MySample.kt \
    sample-jetbrains/src/main/kotlin/io/github/ddsimoes/sweet/sample/MySample.kt
```


## Adding a New Cross‑Runtime Sample

1. **Add shared composables to `sample-sweet`:**
   - Create or extend a file under `sample-sweet/src/main/kotlin/io/github/ddsimoes/sweet/sample`.
   - Define your composables and any small data models there.
   - Keep imports limited to:
     - `androidx.compose.runtime.*`
     - `androidx.compose.ui.*`
     - `androidx.compose.foundation.*`
     - `androidx.compose.material3.*`
     - standard Kotlin / Java libraries.

2. **Add Sweet-backed tests to `sample-sweet`:**
   - Create tests in `sample-sweet/src/test/kotlin` using AutoSWT patterns already used by existing tests.
   - Reuse the composables from `sample-sweet` (e.g. `MySampleApp()`).
   - Prefer layout and interaction assertions over screenshot-only checks.

3. **(Optional) Add a JetBrains runner:**
   - Once `:sample-jetbrains` is wired to JetBrains Compose Desktop, add simple `main` entry points that call the same shared composables from `sample-sweet`.

## Notes

- Ktlint continues to **exclude** all sample modules (`sample`, `sample-sweet`, `sample-jetbrains`) to keep sample code flexible.
- Tests that read internal state from samples (e.g. `canvasSampleDrawCount`, `imageSampleBitmapSize`) now access them via public top‑level properties in `sample-sweet`.

