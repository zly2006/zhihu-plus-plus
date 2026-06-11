package com.github.zly2006.zhihu.navprobe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavController

/**
 * Compile + behaviour probe for the vendored miuix-nav runtime. Two screens driven by a
 * [rememberNavController], exercising push/pop and the predictive-back / swipe-dismiss transition.
 * Not wired into the app navigation graph; serves to validate the vendor on every target and gives
 * a runnable surface for verifying gesture feel on a device.
 */
@Serializable
private sealed interface ProbeRoute : NavKey {
    @Serializable
    data object Home : ProbeRoute

    @Serializable
    data class Detail(val n: Int) : ProbeRoute
}

@Composable
fun MiuixNavProbe() {
    val nav = rememberNavController<ProbeRoute>(ProbeRoute.Home)
    NavDisplay(navController = nav) {
        entry<ProbeRoute.Home> {
            Scaffold(topBar = { TopAppBar(title = "miuix-nav Probe - Home") }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Button(onClick = { nav.push(ProbeRoute.Detail(1)) }) {
                        Text("Open detail")
                    }
                }
            }
        }
        entry<ProbeRoute.Detail> { route ->
            Scaffold(topBar = { TopAppBar(title = "miuix-nav Probe - Detail ${route.n}") }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Text("Swipe from the edge for the predictive-back transition.")
                    Button(onClick = { nav.push(ProbeRoute.Detail(route.n + 1)) }) {
                        Text("Push another detail (${route.n + 1})")
                    }
                    Button(onClick = { nav.pop() }) {
                        Text("Back")
                    }
                }
            }
        }
    }
}
