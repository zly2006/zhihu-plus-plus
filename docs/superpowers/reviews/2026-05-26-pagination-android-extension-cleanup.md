# Pagination Android Extension Cleanup

## Scope

- Deleted file: `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/PaginationAndroidExtensions.android.kt`.
- Current retained adapter entry: `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/viewmodel/AndroidPaginationEnvironment.android.kt`.

## Evidence

- `rg` found no imports or call sites for the old `com.github.zly2006.zhihu.ui` package extension functions.
- The same function names and behavior are retained beside the current Android pagination environment:
  - `PaginationViewModel<*>.refresh(context)`
  - `PaginationViewModel<*>.loadMore(context)`
  - `PaginationViewModel<*>.httpClient(context)`

## Master Shape

- This is not a UI migration and does not alter `PaginationViewModel` flow.
- The retained extension functions still construct the current Android `paginationEnvironment(context)` and call the same shared `refresh`, `loadMore`, and `httpClient` entry points.
- The deleted file was a stale duplicate shell from an earlier package location, so removing it does not change function order, route structure, UI layout, or user-visible behavior.
