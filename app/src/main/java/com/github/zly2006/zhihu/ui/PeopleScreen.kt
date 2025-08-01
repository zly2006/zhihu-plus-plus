package com.github.zly2006.zhihu.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.signFetchRequest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import kotlinx.serialization.json.JsonObject
import org.jetbrains.annotations.Async

class PersonViewModel(
    val person: Person,
) : ViewModel() {
    var avatar by mutableStateOf("")
    var name by mutableStateOf(person.name)
    var headline by mutableStateOf("")
    var followerCount by mutableIntStateOf(0)
    var answerCount by mutableIntStateOf(0)
    var articleCount by mutableIntStateOf(0)

    suspend fun load(context: Context) {
        context as MainActivity
        val jojo = context.httpClient.get("https://www.zhihu.com/api/v4/members/${person.id}") {
            signFetchRequest(context)
        }.raiseForStatus().body<JsonObject>()
        val person = AccountData.decodeJson<DataHolder.People>(jojo)
        this.avatar = person.avatarUrl
        this.name = person.name
        this.headline = person.headline
        this.followerCount = person.followerCount
        this.answerCount = person.answerCount
        this.articleCount = person.articlesCount
        context.postHistory(
            Person(
                id = person.id,
                name = person.name,
                urlToken = person.urlToken ?: "",
            )
        )
    }
}

suspend fun HttpResponse.raiseForStatus() = apply {
    if (status.value >= 400) {
        throw RuntimeException("HTTP error: ${status.value} ${status.description} on ${request.url}: \n ${bodyAsText()}")
    }
}

@Composable
fun PeopleScreen(
    person: Person,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = viewModel { PersonViewModel(person) }

    LaunchedEffect(viewModel) {
        try {
            viewModel.load(context)
        } catch (e: Exception) {
            Log.e("PeopleScreen", "Error loading person data", e)
            Toast.makeText(
                context,
                "加载用户信息失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        AsyncImage(
            model = viewModel.avatar,
            contentDescription = "用户头像",
            modifier = Modifier.padding(16.dp)
                .width(128.dp)
                .height(128.dp)
                .clip(CircleShape),
        )
        Text(
            "用户: ${person.name}",
        )
        Text(viewModel.headline)
        Text("关注者： ${viewModel.followerCount}")
        Text("回答数： ${viewModel.answerCount}")
        Text("文章数： ${viewModel.articleCount}")
    }
}
