# Performance Improvements

This document outlines the performance optimizations that have been made to the codebase and suggestions for future improvements.

## Completed Optimizations

### 1. HTMLDecoder - HTML Entity Decoding (✅ Implemented)

**Location**: `app/src/main/java/com/github/zly2006/zhihu/data/HTMLDecoder.kt`

**Problem**: 
- Used 11 chained `.replace()` calls
- Each call created a new intermediate String object
- For large HTML content, this resulted in significant memory allocations

**Solution**:
- Implemented single-pass regex-based replacement
- Pre-compiled regex pattern for reuse
- Added early return for strings without HTML entities
- Uses immutable map for entity lookups

**Performance Impact**:
- Reduced memory allocations from O(11n) to O(n) where n is string length
- Improved processing speed, especially for large HTML content
- Reduced garbage collection pressure

### 2. LocalRecommendationEngine - Recommendation Generation (✅ Implemented)

**Location**: `app/src/main/java/com/github/zly2006/zhihu/viewmodel/local/LocalRecommendationEngine.kt`

**Problem**:
- Used eager evaluation with mutableList and forEach
- Created intermediate collections unnecessarily
- Multiple iterations over the same data

**Solution**:
- Replaced forEach with sequence operations (asSequence, flatMap)
- Lazy evaluation reduces memory pressure
- Fewer intermediate collections created

**Performance Impact**:
- Reduced memory allocations
- Better performance for large recommendation sets
- More functional and maintainable code

### 3. UserBehaviorAnalyzer - Preference Calculation (✅ Implemented)

**Location**: `app/src/main/java/com/github/zly2006/zhihu/viewmodel/local/UserBehaviorAnalyzer.kt`

**Problem**:
- Nested forEach loops resulting in O(n × m) complexity
- Redundant iterations over CrawlingReason entries

**Solution**:
- Used groupingBy() and fold() operations
- Reduced algorithmic complexity
- Single-pass processing with accumulated results

**Performance Impact**:
- Reduced time complexity from O(n × m) to O(n + m)
- More efficient for large behavior datasets

## Known Issues and Recommendations

### 1. GlobalScope Usage (⚠️ Code Smell)

**Locations**: Multiple files including:
- `Utils.kt` - telemetry function
- `MainActivity.kt` - various UI operations
- `WebviewComp.kt` - WebView callbacks
- `ArticleViewModel.kt` - async operations
- And 25+ other locations

**Problem**:
GlobalScope launches coroutines that:
- Are not tied to any lifecycle
- Can cause memory leaks
- Make it hard to manage concurrent operations
- Can execute even after the context is destroyed

**Recommendation**:
- For Activities: Use `lifecycleScope` from lifecycle-runtime-ktx
- For ViewModels: Use `viewModelScope` from lifecycle-viewmodel-ktx
- For Composables: Use `rememberCoroutineScope()`
- For fire-and-forget operations (like telemetry): Document why GlobalScope is intentional

**Exception**: 
The `telemetry()` function in `Utils.kt` may legitimately use GlobalScope as it's a fire-and-forget analytics operation that should complete regardless of caller lifecycle.

### 2. Database Query Optimization

**Location**: `app/src/main/java/com/github/zly2006/zhihu/viewmodel/local/LocalContentDao.kt`

**Current State**:
- Multiple sequential queries for checking task counts
- N+1 query pattern in some flows

**Recommendation**:
Add batch query methods to DAO:
```kotlin
@Query("""
    SELECT reason, COUNT(*) as count 
    FROM crawling_results 
    GROUP BY reason
""")
suspend fun getResultCountsByAllReasons(): Map<CrawlingReason, Int>
```

This would reduce multiple database round-trips to a single query.

### 3. Content Filtering Performance

**Location**: `app/src/main/java/com/github/zly2006/zhihu/viewmodel/filter/ContentFilterManager.kt`

**Potential Issue**:
- `filterContentList()` loads all filtered IDs into memory
- Could be slow for large datasets

**Recommendation**:
- Consider pagination or streaming for large filter lists
- Add indices on frequently queried columns
- Cache filtered content IDs in memory with TTL

### 4. Infinite Loop in Recommendation Stream

**Location**: `LocalRecommendationEngine.getRecommendationStream()`

**Issue**:
```kotlin
fun getRecommendationStream(): Flow<List<LocalFeed>> = flow {
    while (true) {  // Infinite loop
        try {
            val recommendations = generateRecommendations()
            emit(recommendations)
            kotlinx.coroutines.delay(30_000L)
        } catch (e: Exception) {
            kotlinx.coroutines.delay(60_000L)
        }
    }
}
```

**Recommendation**:
- This is actually fine for a Flow that's meant to be long-lived
- Ensure collectors properly cancel when done
- Consider adding a maximum iteration limit or cancellation check

## Best Practices for Future Development

### 1. String Operations
- ✅ Use StringBuilder for multiple concatenations
- ✅ Use regex for pattern matching instead of multiple replace() calls
- ✅ Pre-compile regex patterns for reuse
- ✅ Add early returns for common cases

### 2. Collections
- ✅ Use sequences for lazy evaluation when processing large datasets
- ✅ Prefer `asSequence()` for chained operations
- ✅ Use `groupingBy()` and `fold()` instead of manual accumulation
- ✅ Consider immutable collections when data doesn't need to change

### 3. Database Operations
- ✅ Batch operations when possible
- ✅ Use indices on frequently queried columns
- ✅ Consider pagination for large result sets
- ✅ Use transactions for multiple related operations

### 4. Coroutines
- ✅ Use structured concurrency (lifecycleScope, viewModelScope)
- ✅ Avoid GlobalScope except for truly global operations
- ✅ Use proper dispatchers (IO for network/disk, Main for UI)
- ✅ Handle cancellation properly

### 5. Memory Management
- ✅ Release resources in finally blocks
- ✅ Use weak references for callbacks when appropriate
- ✅ Avoid memory leaks from static references
- ✅ Profile memory usage for critical paths

## Measuring Performance

To validate these improvements, consider:

1. **Profiling**: Use Android Studio Profiler to measure:
   - CPU usage
   - Memory allocations
   - Network requests
   - Database queries

2. **Benchmarking**: Add benchmark tests for critical paths:
   - HTML entity decoding
   - Recommendation generation
   - User preference calculation

3. **Monitoring**: Track in production:
   - App startup time
   - Screen transition times
   - Memory usage patterns
   - Crash-free sessions

## Future Optimization Opportunities

1. **Image Loading**: Review Coil configuration for optimal caching
2. **Network Layer**: Consider request batching and caching strategies
3. **UI Rendering**: Profile and optimize Compose recompositions
4. **Background Tasks**: Optimize WorkManager task scheduling
5. **Local Database**: Consider Room query optimization and indices
