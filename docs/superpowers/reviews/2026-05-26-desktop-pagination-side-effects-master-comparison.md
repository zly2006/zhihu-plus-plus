# Desktop Pagination Side Effects Master Comparison

## Scope

- Slice: implement missing JVM `PaginationEnvironment` side-effect adapters used by shared feed/question ViewModels.
- Current file changed: `shared/src/jvmMain/kotlin/com/github/zly2006/zhihu/viewmodel/PaginationEnvironment.jvm.kt`.
- Master/current Android file compared: `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/AndroidPaginationEnvironment.android.kt`, which preserves the original Android `PaginationEnvironment` behavior after migration.

## Master Shape Preserved

- `followQuestion(questionId, follow)` keeps the Android URL and method selection:
  - `https://www.zhihu.com/api/v4/questions/{id}/followers`
  - `POST` for follow, `DELETE` for unfollow.
- `sendFeedReadStatus(feed)` keeps the Android target mapping and payload shape:
  - answer/article/pin only;
  - `listOf(type, id, "read")`;
  - POST to `https://www.zhihu.com/lastread/touch`.
- `markItemsAsTouched(items)` keeps the Android flow:
  - early return for empty input;
  - map each `(type, id)` to `listOf(type, id, "touch")`;
  - return the original item set only when the HTTP response succeeds.
- `clearAllHistory()` keeps the Android online clear payload:
  - `pairs = []`;
  - `clear = true`;
  - POST to `https://api.zhihu.com/read_history/batch_del`.

## Required Platform Differences

- Android uses `Context`, `AccountData`, Android cookie storage and Android history storage.
- JVM uses `DesktopAccountStore`, desktop cookie storage and `DesktopHistoryStorage`.
- Request signing is still the same shared `signZhihuFetchRequest()` algorithm, but JVM passes `dc0` explicitly from desktop cookies.
- No UI, navigation, ViewModel flow or visible text was changed.

## Similarity Result

- Function names and request/payload order match the Android migrated source.
- Similarity is structural rather than a direct `git mv`, because this is a JVM platform adapter filling previously default no-op methods.
- Differences are limited to platform storage/client providers and are required by KMP source-set boundaries.
