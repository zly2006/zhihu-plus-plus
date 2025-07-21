package com.github.zly2006.zhihu.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Person

@Composable
fun PeopleScreen(
    person: Person,
    onNavigate: (NavDestination) -> Unit,
) {
    Text(
        "用户: ${person.name}",
    )
}
