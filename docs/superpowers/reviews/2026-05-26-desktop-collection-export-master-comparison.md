# Desktop Collection Export Master Comparison

## Scope

- Slice: implement the JVM `CollectionContentEnvironment.exportCollectionItemsToHtmlZip()` platform adapter.
- Current file changed: `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.jvm.kt`.
- Master files compared:
  - `master:app/src/main/java/com/github/zly2006/zhihu/viewmodel/CollectionContentViewModel.kt`
  - `master:app/src/main/java/com/github/zly2006/zhihu/util/CollectionHtmlExportUtils.kt`

## Master Shape Preserved

- The common `CollectionContentViewModel.exportAllToHtmlZip()` shape is unchanged in this slice: load all items, emit progress, call environment export, then write completed dialog state.
- The JVM export adapter follows the master export order from the Android implementation:
  - create staging directory;
  - emit initial progress;
  - iterate collection items;
  - resolve only answer/article destinations;
  - render per-item HTML with `buildArticleExportFileName()`;
  - count success/skipped/failed and emit progress in `finally`;
  - create ZIP only when at least one item succeeds;
  - return total/success/skipped/failed plus ZIP path.
- Key function names retained or mirrored:
  - `exportCollectionItemsToHtmlZip`
  - `resolveDesktopExportContent`
  - `buildArticleExportFileName`
  - `zipDirectoryContents`
  - `addFileToZip`

## Required Platform Differences

- Android master uses `Context`, `cacheDir`, external files dir, `ContentDetailCache`, and Android HTTP/login wrappers; those stay Android-only.
- JVM uses `DesktopArticleViewModelRuntime` for article detail fetch and HTML rendering, `DesktopAccountStore` for authenticated client creation, `~/Downloads/Zhihu++` for user-visible output, and JVM `ZipOutputStream` for ZIP writing.
- `CollectionHtmlExportUtils.kt` was not moved to common because the existing boundary review keeps file/ZIP helpers platform-side until a real KMP filesystem abstraction is defined.

## Similarity Result

- No UI structure was changed.
- The migrated/common ViewModel path remains aligned with master at the function-flow level.
- The new JVM adapter mirrors the master export algorithm and key function sequence; differences are limited to platform IO, detail fetch, and authenticated client providers.
