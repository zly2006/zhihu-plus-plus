## 内容过滤功能使用指南

### 功能概述
内容过滤功能可以自动过滤首页推荐中展示超过2次但用户未点击的内容，减少重复推荐，提升用户体验。

### 主要特性
1. **智能过滤**：自动记录内容展示次数和用户交互
2. **数据清理**：定期清理过期数据，防止数据库膨胀
3. **可配置**：用户可在设置中开启/关闭此功能
4. **统计信息**：提供详细的过滤统计数据
5. **独立数据库**：使用单独的数据库，不影响其他功能

### 在推荐页面中的集成示例

#### 1. 在RecyclerView中的使用

```kotlin
// 在Adapter的onBindViewHolder中记录展示
override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val item = items[position]
    
    // 记录内容展示
    lifecycleScope.launch {
        recordDisplay(context, ContentType.ANSWER, item.answerId)
    }
    
    // 绑定其他视图数据...
    holder.bind(item)
    
    // 在点击事件中记录交互
    holder.itemView.setOnClickListener {
        lifecycleScope.launch {
            recordInteraction(context, ContentType.ANSWER, item.answerId)
        }
        // 处理点击事件...
    }
}
```

#### 2. 在Compose中的使用

```kotlin
@Composable
fun RecommendationItem(
    answer: Answer,
    onItemClick: () -> Unit
) {
    val context = LocalContext.current
    
    // 记录内容展示
    LaunchedEffect(answer.id) {
        recordDisplay(context, ContentType.ANSWER, answer.id)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // 记录用户交互
                lifecycleScope.launch {
                    recordInteraction(context, ContentType.ANSWER, answer.id)
                }
                onItemClick()
            }
    ) {
        // UI内容...
    }
}
```

#### 3. 在获取推荐数据时过滤

```kotlin
class RecommendationRepository {
    
    suspend fun getFilteredRecommendations(context: Context): List<Answer> {
        // 获取原始推荐数据
        val rawRecommendations = fetchRecommendationsFromAPI()
        
        // 应用内容过滤
        return rawRecommendations.filterByViewHistory(context) { answer ->
            Pair(ContentType.ANSWER, answer.id)
        }
    }
}
```

#### 4. 在ViewModel中的使用

```kotlin
class HomeViewModel : ViewModel() {
    
    private val _recommendations = MutableLiveData<List<Answer>>()
    val recommendations: LiveData<List<Answer>> = _recommendations
    
    fun loadRecommendations(context: Context) {
        viewModelScope.launch {
            try {
                val filteredRecommendations = repository.getFilteredRecommendations(context)
                _recommendations.value = filteredRecommendations
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
    
    fun onItemInteraction(context: Context, answerId: String) {
        viewModelScope.launch {
            recordInteraction(context, ContentType.ANSWER, answerId)
        }
    }
}
```

### 数据清理和维护

建议在应用启动时执行一次数据清理：

```kotlin
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 应用启动时清理过期数据
        lifecycleScope.launch {
            ContentFilterExtensions.performMaintenanceCleanup(this@MainActivity)
        }
    }
}
```

### 用户设置

用户可以在账号设置页面中：
- 开启/关闭内容过滤功能
- 查看过滤统计信息（总记录数、已过滤内容数、过滤率）
- 手动清理过期数据
- 重置所有过滤数据

### 技术实现细节

1. **数据库设计**：
   - 使用独立的Room数据库
   - 自动迁移和数据清理
   - 支持批量操作优化性能

2. **性能优化**：
   - 异步处理，不阻塞UI
   - 批量过滤减少数据库查询
   - 定期清理防止数据膨胀

3. **错误处理**：
   - 所有数据库操作都有异常处理
   - 出错时不影响正常功能
   - 提供降级方案

### 注意事项

1. 所有数据库操作都是异步的，需要在协程中执行
2. 记录展示时机要准确，避免重复记录
3. 用户交互要及时记录，确保过滤准确性
4. 定期检查数据库大小，必要时进行清理
