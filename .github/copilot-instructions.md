# Zhihu++ Copilot Instructions

This is a privacy-focused Zhihu (知乎) Android client with local recommendation algorithms, ad-blocking, and content filtering.

## Build, Test, and Lint

### Build Commands
```bash
# Build lite debug variant (use this to verify changes)
./gradlew assembleLiteDebug

# Build full debug variant (includes NLP features)
./gradlew assembleFullDebug

# Build release variants
./gradlew assembleLiteRelease
./gradlew assembleFullRelease

# Build all variants
./gradlew assemble
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run tests for specific variant
./gradlew testLiteDebugUnitTest
./gradlew testFullDebugUnitTest

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
./gradlew connectedLiteDebugAndroidTest
```

Test files are located in `app/src/test/java/com/github/zly2006/zhihu/`

### Code Formatting
```bash
# Format all Kotlin code with ktlint
./gradlew ktlintFormat

# Check code style without fixing
./gradlew ktlintCheck
```

**IMPORTANT**: Always run `./gradlew assembleLiteDebug` to verify changes, then `./gradlew ktlintFormat` before committing.

## Project Architecture

### Module Structure
- **app**: Main Android application with Jetpack Compose UI
  - `src/main`: Shared code for all variants
  - `src/full`: Full variant-specific code (includes NLP features)
  - `src/lite`: Lite variant-specific code (lightweight, no ML features)
- **sentence_embeddings**: Android AAR library with Rust-based HuggingFace tokenizer (full variant only)
- **rs-hf-tokenizer**: Rust library for tokenization (builds native libraries for Android)

### Build Variants
The app has two product flavors:
- **lite**: Lightweight version (~3MB) without ML/NLP features
- **full**: Complete version with HanLP NLP and sentence embedding support

### Key Components

#### Data Layer
- **AccountData** (`data/AccountData.kt`): Manages authentication, HTTP client, cookie storage, and all network requests
  - Contains `httpClient()` for creating configured Ktor clients
  - Handles snake_case to camelCase conversion for Zhihu API responses
  - Methods: `fetch()`, `fetchGet()`, `fetchPost()`, `decodeJson()`, `snake_case2camelCase()`
  
- **DataHolder** (`data/DataHolder.kt`): Provides data models and content fetching utilities
  - `getContentDetail()` fetches article/answer details from Zhihu API
  
- **HistoryStorage**: Manages local reading history using Room database

#### UI Layer
- Jetpack Compose-based UI with Material 3
- Screens in `ui/` directory (e.g., `HomeScreen.kt`, `ArticleScreen.kt`, `CommentScreen.kt`)
- Reusable components in `ui/components/` (e.g., `FeedCard.kt`, `PaginatedList.kt`)
- Settings screens in `ui/subscreens/`

#### Navigation
- Type-safe navigation using sealed interface `NavDestination` (`NavDestination.kt`)
- Top-level destinations: Home, Follow, HotList, History, OnlineHistory, Account
- Nested destinations for articles, questions, comments, etc.

#### ViewModels
- Located in `viewmodel/` package with subdirectories:
  - `feed/`: Feed-related ViewModels
  - `comment/`: Comment ViewModels (RootCommentViewModel, ChildCommentViewModel)
  - `filter/`: Content filtering logic
  - `local/`: Local recommendation algorithms
  - `za/`: Zhihu API interaction

#### Theme & Styling
- `theme/ThemeManager.kt`: Manages dynamic Material You theming
- `theme/Theme.kt`: Theme definitions

#### Utilities
- `util/`: Helper classes for HTML rendering, power save mode, telemetry, etc.
- `updater/`: App update checking and management

## Key Conventions

### Data Serialization
- **DataHolder** and data classes use `camelCase` for property names
- **Zhihu API** returns `snake_case` JSON
- **Conversion is automatic** when using `AccountData.fetch*()` and `AccountData.decodeJson()` methods
  - These methods internally call `snake_case2camelCase()` before deserialization
  - Never manually convert or expect snake_case in data classes

Example:
```kotlin
// Zhihu API returns: {"answer_count": 10, "user_name": "Alice"}
// DataHolder defines: data class Question(val answerCount: Int, val userName: String)
// Conversion happens automatically in AccountData.decodeJson()
```

### Kotlin Serialization
- Use `@Serializable` annotation for data classes
- Use `@SerialName` only when property name differs from both camelCase and snake_case
- Json configuration in `AccountData.json`: `ignoreUnknownKeys = true`, `isLenient = true`

### Ktor HTTP Client
- Always use `AccountData.httpClient(context)` to get properly configured clients
- Web API requests require `signFetchRequest(context)` for zse96 v2 signatures
- Android API requests use headers from `AccountData.ANDROID_HEADERS` and `AccountData.ANDROID_USER_AGENT`

### Jetpack Compose
- Use `@Composable` functions for UI components
- Prefer `LaunchedEffect` for side effects with proper keys
- Use `collectAsState()` for Flow/StateFlow observation
- Material 3 components throughout the app

### Code Reuse
When asked to check for duplicate code:
1. Identify modified code snippets
2. Use `grep` tool to search for similar patterns elsewhere
3. Extract common logic into functions, data classes, or interfaces
4. Prefer creating utility functions in `util/` package

### File Operations
- **Do NOT** use `rm` or delete commands when encountering "Path already exists" errors
- **Always use `edit` tool** to precisely replace text content in existing files
- Never retry file creation multiple times; switch to editing immediately

### Room Database
- Use KSP annotation processor (not kapt)
- Entities and DAOs follow standard Room conventions

### ProGuard/R8
- Release builds enable minification and resource shrinking
- ProGuard rules in `app/proguard-rules.pro`

### Build Features
- ViewBinding: Enabled
- BuildConfig: Enabled (includes `GIT_HASH`, `IS_LITE`)
- Compose: Enabled

## Development Dependencies

### Core Libraries
- Kotlin 2.2.21 with Compose Compiler
- Ktor 3.3.3 for networking
- Kotlinx Serialization for JSON
- Jetpack Compose with Material 3
- AndroidX Navigation Compose
- Room 2.8.4 with KSP
- Coil 3.3.0 for image loading

### Full Variant Only
- HanLP 1.8.4 (Chinese NLP)
- sentence_embeddings (custom module with Rust tokenizer)

### Tools
- ktlint 14.0.1 for code formatting
- Android Gradle Plugin 8.12.3
- Grgit 5.3.3 for Git integration

## Native Development (Full Variant)

The `sentence_embeddings` module requires Rust toolchain:
- Install Rust targets for Android: `aarch64-linux-android`, `armv7-linux-androideabi`, `i686-linux-android`, `x86_64-linux-android`
- Install `cargo-ndk`: `cargo install cargo-ndk`
- Android NDK 29.0.14206865 required
- Build script: `rs-hf-tokenizer/build-android.sh`

See `sentence_embeddings/README.md` for detailed setup instructions.

## License
Custom license (not free software) - see LICENSE.md. Allows use and modification with restrictions on commercial use and naming.
