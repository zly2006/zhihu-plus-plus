package com.github.zly2006.zhihu.ui

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.signFetchRequest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import kotlinx.serialization.json.JsonObject

class PersonViewModel(
    val person: Person,
) : ViewModel() {
    var avatar by mutableStateOf("")
    var name by mutableStateOf(person.name)
    var headline by mutableStateOf("")

    suspend fun load(context: Context) {
        context as MainActivity
        val jojo = context.httpClient.get("https://www.zhihu.com/api/v4/members/${person.id}") {
            signFetchRequest(context)
        }.raiseForStatus().body<JsonObject>()
        val person = AccountData.decodeJson<DataHolder.People>(jojo)
        this.avatar = person.avatar_url
        this.name = person.name
        this.headline = person.headline
        context.postHistory(
            Person(
                id = person.id,
                name = person.name,
                urlToken = person.url_token ?: "",
            )
        )
    }
}

fun HttpResponse.raiseForStatus() = apply {
    if (status.value >= 400) {
        throw RuntimeException("HTTP error: ${status.value} ${status.description} on ${request.url}",)
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
        viewModel.load(context)
    }

    Surface(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            "用户: ${person.name}",
        )
    }
}
