# Size Step Foundation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate SizeCraft's player scale system from raw scale values (0.001–100.0) to a base-6 logarithmic step system (`steps: Double`, where `scale = 6^steps`), enabling fractional steps, cleaner bounds, and a grid-tier concept for future microblock features.

**Architecture:** `SizeData` stores a `steps: Double` field; `scale` and `gridTier` are derived computed properties (never stored). Config, commands, and `SizeEvents` are updated to operate in step-space. No data migration shim needed — the mod has not been released.

**Tech Stack:** Kotlin 2.3, NeoForge 1.21.5, Mojang `Codec`/`RecordCodecBuilder`, Cloth Config, JUnit 5

---

## File Map

| Status | File                                                            | What changes                                                                              |
| ------ | --------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| Modify | `build.gradle`                                                  | Add JUnit 5 `testImplementation` + `useJUnitPlatform()`                                   |
| Create | `src/test/kotlin/dev/sizecraft/player/SizeDataTest.kt`          | Step ↔ scale math + gridTier unit tests                                                  |
| Create | `src/test/kotlin/dev/sizecraft/player/SizeEventsTest.kt`        | `clampSteps` per-player bounds unit tests                                                 |
| Modify | `src/main/kotlin/dev/sizecraft/player/SizeData.kt`              | `steps` field, computed `scale`/`gridTier`, nullable `minSteps`/`maxSteps`, updated codec |
| Modify | `src/main/kotlin/dev/sizecraft/player/SizeEvents.kt`            | `clampSteps`, `getEffectiveMinSteps/MaxSteps`; remove scale equivalents                   |
| Modify | `src/main/kotlin/dev/sizecraft/config/SizeCraftConfig.kt`       | `defaultSteps`, `globalMinSteps`, `globalMaxSteps` replace scale counterparts             |
| Modify | `src/main/kotlin/dev/sizecraft/config/SizeCraftConfigScreen.kt` | Updated field names, bounds (−10 to +10), new lang keys                                   |
| Modify | `src/main/kotlin/dev/sizecraft/command/SizeCraftCommand.kt`     | Step args (−10 to +10), step clamping, two-value get display                              |
| Modify | `src/main/resources/assets/sizecraft/lang/en_us.json`           | Config and command key renames/updates                                                    |

---

### Task 1: Wire JUnit 5

**Files:**

- Modify: `build.gradle`

- [ ] **Step 1: Add JUnit 5 dependency and configure test task**

  In `build.gradle`, add inside the `dependencies {}` block and add a new `test` block:

  ```groovy
  dependencies {
      implementation "net.neoforged:neoforge:${neo_version}"
      implementation "thedarkcolour:kotlinforforge-neoforge:${kotlin_for_forge_version}"
      implementation "me.shedaniel.cloth:cloth-config-neoforge:${cloth_config_version}"

      testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
      testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
  }

  tasks.named('test', Test) {
      useJUnitPlatform()
  }
  ```

- [ ] **Step 2: Verify Gradle syncs cleanly**

  ```bash
  ./gradlew dependencies --configuration testRuntimeClasspath 2>&1 | grep -i junit | head -10
  ```

  Expected: lines containing `junit-jupiter` and `junit-platform-launcher`.

- [ ] **Step 3: Commit**

  ```bash
  git add build.gradle
  git commit -m "build: add JUnit 5 for unit tests"
  ```

---

### Task 2: SizeData math tests (red)

**Files:**

- Create: `src/test/kotlin/dev/sizecraft/player/SizeDataTest.kt`

> These tests reference `SizeData(steps = ...)`, `data.scale`, and `data.gridTier` — none of which exist yet. The file will not compile until Task 3. That compile failure is the "red" state.

- [ ] **Step 1: Create the test file**

  ```kotlin
  package dev.sizecraft.player

  import org.junit.jupiter.api.Assertions.*
  import org.junit.jupiter.api.Test
  import kotlin.math.pow
  import kotlin.math.sqrt

  class SizeDataTest {

      @Test
      fun `steps 0 gives scale 1`() {
          assertEquals(1.0, SizeData(steps = 0.0).scale, 1e-9)
      }

      @Test
      fun `steps 1 gives scale 6`() {
          assertEquals(6.0, SizeData(steps = 1.0).scale, 1e-9)
      }

      @Test
      fun `steps negative 1 gives scale one-sixth`() {
          assertEquals(1.0 / 6.0, SizeData(steps = -1.0).scale, 1e-9)
      }

      @Test
      fun `steps 0_5 gives scale sqrt-6`() {
          assertEquals(sqrt(6.0), SizeData(steps = 0.5).scale, 1e-9)
      }

      @Test
      fun `steps 3 gives scale 216`() {
          assertEquals(216.0, SizeData(steps = 3.0).scale, 1e-6)
      }

      @Test
      fun `gridTier 0 for steps 0`() {
          assertEquals(0, SizeData(steps = 0.0).gridTier)
      }

      @Test
      fun `gridTier snaps up for positive fractional steps`() {
          assertEquals(1, SizeData(steps = 0.01).gridTier)
          assertEquals(1, SizeData(steps = 0.5).gridTier)
          assertEquals(1, SizeData(steps = 1.0).gridTier)
          assertEquals(2, SizeData(steps = 1.01).gridTier)
          assertEquals(2, SizeData(steps = 1.5).gridTier)
      }

      @Test
      fun `gridTier rounds toward zero for negative fractional steps`() {
          // ceil(-0.5) = 0, ceil(-1.0) = -1, ceil(-0.01) = 0
          assertEquals(0, SizeData(steps = -0.01).gridTier)
          assertEquals(0, SizeData(steps = -0.5).gridTier)
          assertEquals(-1, SizeData(steps = -1.0).gridTier)
          assertEquals(-1, SizeData(steps = -1.5).gridTier)
          assertEquals(-2, SizeData(steps = -2.0).gridTier)
      }

      @Test
      fun `default SizeData has steps 0`() {
          assertEquals(0.0, SizeData().steps, 0.0)
      }

      @Test
      fun `default SizeData has null min and max steps`() {
          assertNull(SizeData().minSteps)
          assertNull(SizeData().maxSteps)
      }
  }
  ```

  Save to `src/test/kotlin/dev/sizecraft/player/SizeDataTest.kt`.

- [ ] **Step 2: Confirm compile failure (red)**

  ```bash
  ./gradlew compileTestKotlin 2>&1 | grep -i "unresolved\|error" | head -20
  ```

  Expected: errors about unresolved references to `steps`, `scale`, `gridTier`, `minSteps`, `maxSteps`.

---

### Task 3: SizeData step model (green)

**Files:**

- Modify: `src/main/kotlin/dev/sizecraft/player/SizeData.kt`
- Modify: `src/main/kotlin/dev/sizecraft/player/SizeEvents.kt` (minimal compile fix — full refactor in Task 5)

- [ ] **Step 1: Replace SizeData.kt**

  ```kotlin
  package dev.sizecraft.player

  import com.mojang.serialization.Codec
  import com.mojang.serialization.codecs.RecordCodecBuilder
  import dev.sizecraft.SizeCraftMod
  import net.neoforged.bus.api.IEventBus
  import net.neoforged.neoforge.attachment.AttachmentType
  import net.neoforged.neoforge.registries.DeferredRegister
  import net.neoforged.neoforge.registries.NeoForgeRegistries
  import java.util.Optional
  import java.util.function.Supplier
  import kotlin.math.ceil
  import kotlin.math.pow

  /**
   * Per-player size data stored as a NeoForge attachment.
   * Persisted to NBT automatically via codec, copied on death.
   *
   * Size is stored as [steps] on a base-6 logarithmic scale:
   *   scale = 6^steps
   * Each integer step multiplies or divides size by 6.
   * Fractional steps are supported (e.g. 0.5 = √6 ≈ 2.45x).
   */
  data class SizeData(
      var steps: Double = 0.0,
      var minSteps: Double? = null,  // null = use global config value
      var maxSteps: Double? = null,  // null = use global config value
      var isPredator: Boolean = false,
      var isPrey: Boolean = false,
      var hammerspaceEscapable: Boolean = true,
      var escapeDelayTicks: Int = -1,             // -1 = use global config value
      var previousGameMode: Int = 0,
      var stomachSlot: String = "stomach",
      var returnDimension: String = "",
      var returnX: Double = 0.0,
      var returnY: Double = 0.0,
      var returnZ: Double = 0.0,
      var returnYaw: Float = 0f,
      var returnPitch: Float = 0f,
  ) {
      /** Minecraft SCALE attribute value derived from steps: 6^steps. */
      val scale: Double get() = 6.0.pow(steps)

      /**
       * Block-interaction grid tier for this player.
       * Fractional steps snap upward to the next tier (ceil).
       * Examples: steps=0.0 → 0, steps=0.5 → 1, steps=1.0 → 1, steps=-0.5 → 0, steps=-1.0 → -1
       */
      val gridTier: Int get() = ceil(steps).toInt()

      companion object {

          val CODEC: Codec<SizeData> = RecordCodecBuilder.create { builder ->
              builder.group(
                  Codec.DOUBLE.fieldOf("steps").forGetter(SizeData::steps),
                  Codec.DOUBLE.optionalFieldOf("min_steps")
                      .forGetter { Optional.ofNullable(it.minSteps) },
                  Codec.DOUBLE.optionalFieldOf("max_steps")
                      .forGetter { Optional.ofNullable(it.maxSteps) },
                  Codec.BOOL.optionalFieldOf("is_predator", false).forGetter(SizeData::isPredator),
                  Codec.BOOL.optionalFieldOf("is_prey", false).forGetter(SizeData::isPrey),
                  Codec.BOOL.optionalFieldOf("hammerspace_escapable", true).forGetter(SizeData::hammerspaceEscapable),
                  Codec.INT.optionalFieldOf("escape_delay_ticks", -1).forGetter(SizeData::escapeDelayTicks),
                  Codec.INT.optionalFieldOf("previous_game_mode", 0).forGetter(SizeData::previousGameMode),
                  Codec.STRING.optionalFieldOf("stomach_slot", "stomach").forGetter(SizeData::stomachSlot),
                  Codec.STRING.optionalFieldOf("return_dimension", "").forGetter(SizeData::returnDimension),
                  Codec.DOUBLE.optionalFieldOf("return_x", 0.0).forGetter(SizeData::returnX),
                  Codec.DOUBLE.optionalFieldOf("return_y", 0.0).forGetter(SizeData::returnY),
                  Codec.DOUBLE.optionalFieldOf("return_z", 0.0).forGetter(SizeData::returnZ),
                  Codec.FLOAT.optionalFieldOf("return_yaw", 0f).forGetter(SizeData::returnYaw),
                  Codec.FLOAT.optionalFieldOf("return_pitch", 0f).forGetter(SizeData::returnPitch),
              ).apply(builder, ::fromCodec)
          }

          private fun fromCodec(
              steps: Double,
              minSteps: Optional<Double>,
              maxSteps: Optional<Double>,
              isPredator: Boolean,
              isPrey: Boolean,
              hammerspaceEscapable: Boolean,
              escapeDelayTicks: Int,
              previousGameMode: Int,
              stomachSlot: String,
              returnDimension: String,
              returnX: Double,
              returnY: Double,
              returnZ: Double,
              returnYaw: Float,
              returnPitch: Float,
          ): SizeData = SizeData(
              steps = steps,
              minSteps = minSteps.orElse(null),
              maxSteps = maxSteps.orElse(null),
              isPredator = isPredator,
              isPrey = isPrey,
              hammerspaceEscapable = hammerspaceEscapable,
              escapeDelayTicks = escapeDelayTicks,
              previousGameMode = previousGameMode,
              stomachSlot = stomachSlot,
              returnDimension = returnDimension,
              returnX = returnX,
              returnY = returnY,
              returnZ = returnZ,
              returnYaw = returnYaw,
              returnPitch = returnPitch,
          )

          private val ATTACHMENT_TYPES: DeferredRegister<AttachmentType<*>> =
              DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SizeCraftMod.MOD_ID)

          val SIZE_DATA: Supplier<AttachmentType<SizeData>> = ATTACHMENT_TYPES.register("size_data") { ->
              AttachmentType.builder(Supplier { SizeData() })
                  .serialize(CODEC)
                  .copyOnDeath()
                  .build()
          }

          fun register(modBus: IEventBus) {
              ATTACHMENT_TYPES.register(modBus)
          }
      }
  }
  ```

- [ ] **Step 2: Fix the compile errors SizeData change introduced in SizeEvents.kt**

  The old `data.minScale` / `data.maxScale` sentinel checks no longer exist. Replace the three functions in `SizeEvents.kt` with temporary null-based versions (full step refactor comes in Task 5):

  In `SizeEvents.kt`, replace the three functions `getEffectiveMinScale`, `getEffectiveMaxScale`, `clampScale`:

  ```kotlin
  fun getEffectiveMinScale(data: SizeData): Double {
      val minSteps = data.minSteps ?: SizeCraftConfig.globalMinSteps
      return 6.0.pow(minSteps)
  }

  fun getEffectiveMaxScale(data: SizeData): Double {
      val maxSteps = data.maxSteps ?: SizeCraftConfig.globalMaxSteps
      return 6.0.pow(maxSteps)
  }

  fun clampScale(scale: Double, data: SizeData): Double {
      val min = getEffectiveMinScale(data)
      val max = getEffectiveMaxScale(data)
      return scale.coerceIn(min, max)
  }
  ```

  Also add the import at the top of `SizeEvents.kt`:

  ```kotlin
  import kotlin.math.pow
  ```

  And update the two event handler calls that read `data.scale` — these now work automatically because `scale` is a computed property on `SizeData`. No changes needed there.

  Finally, fix `SizeCraftCommand.kt` line 314, which references `SizeCraftConfig.defaultScale`. Add a temporary compile shim: in `SizeCraftConfig.kt`, add a deprecated alias after the `defaultSteps` property is added in Task 6. For now, leave it as-is and just verify nothing else breaks on compile.

  Actually, compile the project to see all remaining errors:

  ```bash
  ./gradlew compileKotlin 2>&1 | grep -i "error" | head -30
  ```

  If errors appear only in `SizeCraftCommand.kt` at the `defaultScale` reference, that's expected — it gets fixed in Task 6. Everything else should be clean.

- [ ] **Step 3: Run SizeData math tests (green)**

  ```bash
  ./gradlew test --tests "dev.sizecraft.player.SizeDataTest" 2>&1 | tail -20
  ```

  Expected: `BUILD SUCCESSFUL` with all 11 tests passing.

- [ ] **Step 4: Commit**

  ```bash
  git add src/main/kotlin/dev/sizecraft/player/SizeData.kt \
          src/main/kotlin/dev/sizecraft/player/SizeEvents.kt \
          src/test/kotlin/dev/sizecraft/player/SizeDataTest.kt
  git commit -m "feat: migrate SizeData to logarithmic step model (scale = 6^steps)"
  ```

---

### Task 4: SizeEvents clamp tests

**Files:**

- Create: `src/test/kotlin/dev/sizecraft/player/SizeEventsTest.kt`

- [ ] **Step 1: Create the test file**

  ```kotlin
  package dev.sizecraft.player

  import org.junit.jupiter.api.Assertions.*
  import org.junit.jupiter.api.Test

  class SizeEventsTest {

      @Test
      fun `clampSteps returns value unchanged when within per-player bounds`() {
          val data = SizeData(steps = 0.0, minSteps = -2.0, maxSteps = 2.0)
          assertEquals(1.0, SizeEvents.clampSteps(1.0, data), 1e-9)
          assertEquals(-1.0, SizeEvents.clampSteps(-1.0, data), 1e-9)
          assertEquals(0.0, SizeEvents.clampSteps(0.0, data), 1e-9)
      }

      @Test
      fun `clampSteps clamps to max when over per-player bound`() {
          val data = SizeData(steps = 0.0, minSteps = -2.0, maxSteps = 2.0)
          assertEquals(2.0, SizeEvents.clampSteps(5.0, data), 1e-9)
          assertEquals(2.0, SizeEvents.clampSteps(2.0001, data), 1e-9)
      }

      @Test
      fun `clampSteps clamps to min when under per-player bound`() {
          val data = SizeData(steps = 0.0, minSteps = -2.0, maxSteps = 2.0)
          assertEquals(-2.0, SizeEvents.clampSteps(-5.0, data), 1e-9)
          assertEquals(-2.0, SizeEvents.clampSteps(-2.0001, data), 1e-9)
      }

      @Test
      fun `getEffectiveMinSteps returns per-player min when set`() {
          val data = SizeData(minSteps = -1.5)
          assertEquals(-1.5, SizeEvents.getEffectiveMinSteps(data), 1e-9)
      }

      @Test
      fun `getEffectiveMaxSteps returns per-player max when set`() {
          val data = SizeData(maxSteps = 1.5)
          assertEquals(1.5, SizeEvents.getEffectiveMaxSteps(data), 1e-9)
      }
  }
  ```

  Save to `src/test/kotlin/dev/sizecraft/player/SizeEventsTest.kt`.

- [ ] **Step 2: Run tests — expect 3 failures (clampSteps not yet on SizeEvents)**

  ```bash
  ./gradlew test --tests "dev.sizecraft.player.SizeEventsTest" 2>&1 | tail -20
  ```

  Expected: `getEffectiveMinSteps` and `getEffectiveMaxSteps` tests may fail if those methods don't exist yet; the `clampSteps` tests may fail if `SizeEvents.clampSteps` isn't exposed. Note failures — they guide Task 5.

---

### Task 5: SizeEvents step helpers (green)

**Files:**

- Modify: `src/main/kotlin/dev/sizecraft/player/SizeEvents.kt`

- [ ] **Step 1: Replace all scale helper functions with step equivalents**

  In `SizeEvents.kt`, replace the three functions added in Task 3's compile fix with the proper step-based versions. The full replacement block (lines ~49–67):

  ```kotlin
  /**
   * Resolves the effective minimum steps for a player (per-player override or global config).
   */
  fun getEffectiveMinSteps(data: SizeData): Double {
      return data.minSteps ?: SizeCraftConfig.globalMinSteps
  }

  /**
   * Resolves the effective maximum steps for a player (per-player override or global config).
   */
  fun getEffectiveMaxSteps(data: SizeData): Double {
      return data.maxSteps ?: SizeCraftConfig.globalMaxSteps
  }

  /**
   * Clamps a step value within the effective bounds for a player.
   */
  fun clampSteps(steps: Double, data: SizeData): Double {
      val min = getEffectiveMinSteps(data)
      val max = getEffectiveMaxSteps(data)
      return steps.coerceIn(min, max)
  }
  ```

  Also remove the old `getEffectiveMinScale`, `getEffectiveMaxScale`, `clampScale` functions entirely.

  Also remove the `import kotlin.math.pow` added in Task 3 (no longer needed here).

  The updated event handler calls still work because `data.scale` is a computed property.

- [ ] **Step 2: Run SizeEvents tests**

  ```bash
  ./gradlew test --tests "dev.sizecraft.player.SizeEventsTest" 2>&1 | tail -20
  ```

  Expected: `BUILD SUCCESSFUL`, all 5 tests pass.

- [ ] **Step 3: Run full test suite**

  ```bash
  ./gradlew test 2>&1 | tail -10
  ```

  Expected: all 16 tests pass.

- [ ] **Step 4: Commit**

  ```bash
  git add src/main/kotlin/dev/sizecraft/player/SizeEvents.kt \
          src/test/kotlin/dev/sizecraft/player/SizeEventsTest.kt
  git commit -m "feat: add clampSteps/getEffectiveMin/MaxSteps to SizeEvents"
  ```

---

### Task 6: Config step migration

**Files:**

- Modify: `src/main/kotlin/dev/sizecraft/config/SizeCraftConfig.kt`
- Modify: `src/main/resources/assets/sizecraft/lang/en_us.json`

- [ ] **Step 1: Replace scale config properties with step equivalents in SizeCraftConfig.kt**

  Replace the three internal scale values and their public accessors. The full `size` section in `init {}`:

  ```kotlin
  internal val _defaultSteps: ModConfigSpec.DoubleValue
  internal val _globalMinSteps: ModConfigSpec.DoubleValue
  internal val _globalMaxSteps: ModConfigSpec.DoubleValue
  ```

  ```kotlin
  val defaultSteps: Double get() = _defaultSteps.get()
  val globalMinSteps: Double get() = _globalMinSteps.get()
  val globalMaxSteps: Double get() = _globalMaxSteps.get()
  ```

  ```kotlin
  serverBuilder.push("size")
  _defaultSteps = serverBuilder
      .comment("Default steps for new players (scale = 6^steps). 0 = normal size.")
      .defineInRange("defaultSteps", 0.0, -10.0, 10.0)
  _globalMinSteps = serverBuilder
      .comment("Global minimum steps (unless per-player override is set). -3 ≈ 1/216 scale.")
      .defineInRange("globalMinSteps", -3.0, -10.0, 10.0)
  _globalMaxSteps = serverBuilder
      .comment("Global maximum steps (unless per-player override is set). 3 = 216x scale.")
      .defineInRange("globalMaxSteps", 3.0, -10.0, 10.0)
  serverBuilder.pop()
  ```

  Remove the old `_defaultScale`, `_globalMinScale`, `_globalMaxScale` declarations, accessors, and builder calls entirely.

- [ ] **Step 2: Update the config lang keys in en_us.json**

  Replace these three key-value pairs:

  ```json
  "sizecraft.config.default_scale": "Default Scale",
  "sizecraft.config.default_scale.tooltip": "Scale applied to new players or on reset.",
  "sizecraft.config.global_min": "Global Minimum Scale",
  "sizecraft.config.global_min.tooltip": "Minimum scale any player can be set to (unless overridden per-player).",
  "sizecraft.config.global_max": "Global Maximum Scale",
  "sizecraft.config.global_max.tooltip": "Maximum scale any player can be set to (unless overridden per-player).",
  ```

  With:

  ```json
  "sizecraft.config.default_steps": "Default Steps",
  "sizecraft.config.default_steps.tooltip": "Step value applied to new players or on reset. scale = 6^steps, so 0 = normal size.",
  "sizecraft.config.global_min_steps": "Global Minimum Steps",
  "sizecraft.config.global_min_steps.tooltip": "Minimum steps any player can be set to. e.g. -2 = 1/36 scale, -3 = 1/216 scale.",
  "sizecraft.config.global_max_steps": "Global Maximum Steps",
  "sizecraft.config.global_max_steps.tooltip": "Maximum steps any player can be set to. e.g. 2 = 36x scale, 3 = 216x scale.",
  ```

- [ ] **Step 3: Compile to catch remaining references**

  ```bash
  ./gradlew compileKotlin 2>&1 | grep "error" | head -20
  ```

  Expected errors: `SizeCraftConfigScreen.kt` references `defaultScale`, `globalMinScale`, `globalMaxScale` — addressed in Task 7. `SizeCraftCommand.kt` references `SizeCraftConfig.defaultScale` — addressed in Task 8.

- [ ] **Step 4: Commit**

  ```bash
  git add src/main/kotlin/dev/sizecraft/config/SizeCraftConfig.kt \
          src/main/resources/assets/sizecraft/lang/en_us.json
  git commit -m "feat: migrate SizeCraftConfig size bounds to step values"
  ```

---

### Task 7: ConfigScreen step migration

**Files:**

- Modify: `src/main/kotlin/dev/sizecraft/config/SizeCraftConfigScreen.kt`

- [ ] **Step 1: Update the three size fields in SizeCraftConfigScreen.kt**

  Replace the entire default steps entry block (lines ~25–36):

  ```kotlin
  sizeCategory.addEntry(
      entryBuilder.startDoubleField(
          Component.translatable("sizecraft.config.default_steps"),
          SizeCraftConfig.defaultSteps
      )
          .setDefaultValue { SizeCraftConfig._defaultSteps.default }
          .setMin(-10.0)
          .setMax(10.0)
          .setTooltip(Component.translatable("sizecraft.config.default_steps.tooltip"))
          .setSaveConsumer { SizeCraftConfig._defaultSteps.set(it) }
          .build()
  )
  ```

  Replace the global min entry block (lines ~38–49):

  ```kotlin
  sizeCategory.addEntry(
      entryBuilder.startDoubleField(
          Component.translatable("sizecraft.config.global_min_steps"),
          SizeCraftConfig.globalMinSteps
      )
          .setDefaultValue { SizeCraftConfig._globalMinSteps.default }
          .setMin(-10.0)
          .setMax(10.0)
          .setTooltip(Component.translatable("sizecraft.config.global_min_steps.tooltip"))
          .setSaveConsumer { SizeCraftConfig._globalMinSteps.set(it) }
          .build()
  )
  ```

  Replace the global max entry block (lines ~51–62):

  ```kotlin
  sizeCategory.addEntry(
      entryBuilder.startDoubleField(
          Component.translatable("sizecraft.config.global_max_steps"),
          SizeCraftConfig.globalMaxSteps
      )
          .setDefaultValue { SizeCraftConfig._globalMaxSteps.default }
          .setMin(-10.0)
          .setMax(10.0)
          .setTooltip(Component.translatable("sizecraft.config.global_max_steps.tooltip"))
          .setSaveConsumer { SizeCraftConfig._globalMaxSteps.set(it) }
          .build()
  )
  ```

- [ ] **Step 2: Compile check (ConfigScreen should now be clean)**

  ```bash
  ./gradlew compileKotlin 2>&1 | grep "error" | head -20
  ```

  Expected: only `SizeCraftCommand.kt` errors remain (references to `defaultScale`, `clampScale`).

- [ ] **Step 3: Commit**

  ```bash
  git add src/main/kotlin/dev/sizecraft/config/SizeCraftConfigScreen.kt
  git commit -m "feat: update Cloth Config screen for step-based size bounds"
  ```

---

### Task 8: Command step migration + lang keys

**Files:**

- Modify: `src/main/kotlin/dev/sizecraft/command/SizeCraftCommand.kt`
- Modify: `src/main/resources/assets/sizecraft/lang/en_us.json`

- [ ] **Step 1: Update the shorthand `/sizecraft <steps>` argument registration**

  In `onRegisterCommands`, replace the first argument block (the bare `/sizecraft <scale>` command):

  ```kotlin
  .then(
      Commands.argument("steps", DoubleArgumentType.doubleArg(-10.0, 10.0))
          .executes { ctx -> setSelfSteps(ctx) }
  )
  ```

- [ ] **Step 2: Update the `/sizecraft set <player> <steps>` argument**

  ```kotlin
  .then(
      Commands.literal("set")
          .requires { src -> hasPermission(src, SizeCraftPermissions.RESIZE_OTHERS) }
          .then(
              Commands.argument("player", EntityArgument.player())
                  .then(
                      Commands.argument("steps", DoubleArgumentType.doubleArg(-10.0, 10.0))
                          .executes { ctx -> setSteps(ctx) }
                  )
          )
  )
  ```

- [ ] **Step 3: Update the `/sizecraft min` and `/sizecraft max` argument names**

  Both subcommands change `"scale"` → `"steps"` and range `0.001..100.0` → `-10.0..10.0`:

  ```kotlin
  .then(
      Commands.literal("min")
          .requires { src -> hasPermission(src, SizeCraftPermissions.RESIZE_OTHERS) }
          .then(
              Commands.argument("player", EntityArgument.player())
                  .then(
                      Commands.argument("steps", DoubleArgumentType.doubleArg(-10.0, 10.0))
                          .executes { ctx -> setMin(ctx) }
                  )
          )
  )
  .then(
      Commands.literal("max")
          .requires { src -> hasPermission(src, SizeCraftPermissions.RESIZE_OTHERS) }
          .then(
              Commands.argument("player", EntityArgument.player())
                  .then(
                      Commands.argument("steps", DoubleArgumentType.doubleArg(-10.0, 10.0))
                          .executes { ctx -> setMax(ctx) }
                  )
          )
  )
  ```

- [ ] **Step 4: Replace `getSelf` and `getOther` to show steps + scale**

  ```kotlin
  private fun getSelf(ctx: CommandContext<CommandSourceStack>): Int {
      val player = ctx.source.playerOrException
      val data = player.getData(SizeData.SIZE_DATA)
      ctx.source.sendSuccess({
          Component.translatable(
              "sizecraft.command.get.self",
              String.format("%.2f", data.steps),
              String.format("%.3f", data.scale)
          )
      }, false)
      return 1
  }

  private fun getOther(ctx: CommandContext<CommandSourceStack>): Int {
      val target = EntityArgument.getPlayer(ctx, "player")
      val data = target.getData(SizeData.SIZE_DATA)
      ctx.source.sendSuccess({
          Component.translatable(
              "sizecraft.command.get.other",
              target.displayName,
              String.format("%.2f", data.steps),
              String.format("%.3f", data.scale)
          )
      }, false)
      return 1
  }
  ```

- [ ] **Step 5: Replace `setSelfScale` with `setSelfSteps`**

  ```kotlin
  private fun setSelfSteps(ctx: CommandContext<CommandSourceStack>): Int {
      val target = ctx.source.playerOrException
      if (!canResize(ctx.source, target)) return 0

      val data = target.getData(SizeData.SIZE_DATA)
      val requestedSteps = DoubleArgumentType.getDouble(ctx, "steps")
      val clampedSteps = SizeEvents.clampSteps(requestedSteps, data)

      data.steps = clampedSteps
      target.setData(SizeData.SIZE_DATA, data)
      SizeEvents.applyScale(target, data.scale)

      ctx.source.sendSuccess({
          Component.translatable(
              "sizecraft.command.set",
              target.displayName,
              String.format("%.2f", clampedSteps),
              String.format("%.3f", data.scale)
          )
      }, true)

      if (clampedSteps != requestedSteps) {
          ctx.source.sendSuccess({
              Component.translatable(
                  "sizecraft.command.set.clamped",
                  String.format("%.2f", requestedSteps),
                  String.format("%.2f", clampedSteps)
              )
          }, false)
      }
      return 1
  }
  ```

- [ ] **Step 6: Replace `setScale` with `setSteps`**

  ```kotlin
  private fun setSteps(ctx: CommandContext<CommandSourceStack>): Int {
      val target = EntityArgument.getPlayer(ctx, "player")
      if (!canResize(ctx.source, target)) return 0

      val data = target.getData(SizeData.SIZE_DATA)
      val requestedSteps = DoubleArgumentType.getDouble(ctx, "steps")
      val clampedSteps = SizeEvents.clampSteps(requestedSteps, data)

      data.steps = clampedSteps
      target.setData(SizeData.SIZE_DATA, data)
      SizeEvents.applyScale(target, data.scale)

      ctx.source.sendSuccess({
          Component.translatable(
              "sizecraft.command.set",
              target.displayName,
              String.format("%.2f", clampedSteps),
              String.format("%.3f", data.scale)
          )
      }, true)

      if (clampedSteps != requestedSteps) {
          ctx.source.sendSuccess({
              Component.translatable(
                  "sizecraft.command.set.clamped",
                  String.format("%.2f", requestedSteps),
                  String.format("%.2f", clampedSteps)
              )
          }, false)
      }
      return 1
  }
  ```

- [ ] **Step 7: Replace `doReset` to use `defaultSteps`**

  ```kotlin
  private fun doReset(source: CommandSourceStack, target: ServerPlayer): Int {
      val data = target.getData(SizeData.SIZE_DATA)
      val clampedSteps = SizeEvents.clampSteps(SizeCraftConfig.defaultSteps, data)
      data.steps = clampedSteps
      target.setData(SizeData.SIZE_DATA, data)
      SizeEvents.applyScale(target, data.scale)

      source.sendSuccess({
          Component.translatable("sizecraft.command.reset", target.displayName)
      }, true)
      return 1
  }
  ```

- [ ] **Step 8: Replace `setMin` and `setMax` to use steps**

  ```kotlin
  private fun setMin(ctx: CommandContext<CommandSourceStack>): Int {
      val target = EntityArgument.getPlayer(ctx, "player")
      val steps = DoubleArgumentType.getDouble(ctx, "steps")
      val data = target.getData(SizeData.SIZE_DATA)
      data.minSteps = steps
      target.setData(SizeData.SIZE_DATA, data)

      if (data.steps < steps) {
          data.steps = steps
          target.setData(SizeData.SIZE_DATA, data)
          SizeEvents.applyScale(target, data.scale)
      }

      ctx.source.sendSuccess({
          Component.translatable("sizecraft.command.min", target.displayName, String.format("%.2f", steps))
      }, true)
      return 1
  }

  private fun setMax(ctx: CommandContext<CommandSourceStack>): Int {
      val target = EntityArgument.getPlayer(ctx, "player")
      val steps = DoubleArgumentType.getDouble(ctx, "steps")
      val data = target.getData(SizeData.SIZE_DATA)
      data.maxSteps = steps
      target.setData(SizeData.SIZE_DATA, data)

      if (data.steps > steps) {
          data.steps = steps
          target.setData(SizeData.SIZE_DATA, data)
          SizeEvents.applyScale(target, data.scale)
      }

      ctx.source.sendSuccess({
          Component.translatable("sizecraft.command.max", target.displayName, String.format("%.2f", steps))
      }, true)
      return 1
  }
  ```

- [ ] **Step 9: Update command lang keys in en_us.json**

  Replace these keys:

  ```json
  "sizecraft.command.get.self": "Your current scale is %s.",
  "sizecraft.command.get.other": "%s's current scale is %s.",
  "sizecraft.command.set": "Set %s's scale to %s.",
  "sizecraft.command.set.clamped": "Requested %s was clamped to %s (within allowed bounds).",
  "sizecraft.command.reset": "Reset %s's scale to default.",
  "sizecraft.command.min": "Set %s's minimum scale to %s.",
  "sizecraft.command.max": "Set %s's maximum scale to %s.",
  ```

  With:

  ```json
  "sizecraft.command.get.self": "Your current steps: %s (scale: %sx).",
  "sizecraft.command.get.other": "%s's current steps: %s (scale: %sx).",
  "sizecraft.command.set": "Set %s's steps to %s (scale: %sx).",
  "sizecraft.command.set.clamped": "Requested %s was clamped to %s steps (within allowed bounds).",
  "sizecraft.command.reset": "Reset %s's steps to default.",
  "sizecraft.command.min": "Set %s's minimum steps to %s.",
  "sizecraft.command.max": "Set %s's maximum steps to %s.",
  ```

- [ ] **Step 10: Full compile check**

  ```bash
  ./gradlew compileKotlin 2>&1 | grep "error"
  ```

  Expected: no output (clean compile).

- [ ] **Step 11: Run full test suite**

  ```bash
  ./gradlew test 2>&1 | tail -10
  ```

  Expected: `BUILD SUCCESSFUL`, all 16 tests pass.

- [ ] **Step 12: Commit**

  ```bash
  git add src/main/kotlin/dev/sizecraft/command/SizeCraftCommand.kt \
          src/main/resources/assets/sizecraft/lang/en_us.json
  git commit -m "feat: migrate commands to step-based sizing (/sizecraft <steps>, min/max in steps)"
  ```

---

## Self-Review

**Spec coverage:**

- ✅ `SizeData.steps` stored field, `scale` computed property (`6^steps`)
- ✅ `gridTier` computed property (ceil of steps)
- ✅ Nullable `minSteps`/`maxSteps` (null = global config fallback)
- ✅ Codec updated (new field names, Optional unwrapping)
- ✅ Config: `defaultSteps`, `globalMinSteps`, `globalMaxSteps` with sensible defaults (0, -3, 3)
- ✅ `SizeEvents.clampSteps`, `getEffectiveMinSteps`, `getEffectiveMaxSteps`
- ✅ Command step argument range −10 to +10
- ✅ `get` command shows both steps and scale
- ✅ `set`/`min`/`max`/`reset` operate in step-space
- ✅ Cloth Config screen updated
- ✅ All lang keys updated
- ✅ Unit tests for step math and clamp logic

**Type consistency check:**

- `SizeData.steps: Double` — used consistently in SizeEvents, SizeCraftCommand, SizeCraftConfig
- `SizeEvents.clampSteps(steps: Double, data: SizeData): Double` — matches all call sites in commands
- `SizeCraftConfig.defaultSteps/globalMinSteps/globalMaxSteps: Double` — matches `SizeEvents.getEffective*Steps` fallback
- `data.scale` (computed) — used for `SizeEvents.applyScale(player, data.scale)` in all command handlers ✅
- `DoubleArgumentType.getDouble(ctx, "steps")` — arg name `"steps"` matches all `Commands.argument("steps", ...)` registrations ✅

**No placeholders detected.**
