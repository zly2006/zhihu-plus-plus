/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.LocalUiStyle
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeMode
import com.github.zly2006.zhihu.theme.ThemeSnapshot
import com.github.zly2006.zhihu.theme.UiStyle
import com.github.zly2006.zhihu.theme.ZhihuTheme
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class UiStyleComponentsJvmTest {
    @AfterTest
    fun tearDown() {
        ThemeManager.load(ThemeSnapshot())
    }

    @Test
    fun hotSwitchingMaterial3AndMiuix_preservesStateAndRendersCoreControls() = runComposeUiTest {
        ThemeManager.load(ThemeSnapshot(uiStyle = UiStyle.Material3))

        setContent {
            ZhihuTheme {
                var counter by remember { mutableIntStateOf(0) }
                var checked by remember { mutableStateOf(false) }
                var sliderValue by remember { mutableFloatStateOf(0.5f) }
                var query by remember { mutableStateOf("") }
                var selectedTab by remember { mutableIntStateOf(0) }

                AppScaffold(
                    topBar = {
                        AppTopAppBar(
                            title = { Text("风格测试", modifier = Modifier.testTag("top_title")) },
                            actions = {
                                AppIconButton(onClick = { counter++ }, modifier = Modifier.testTag("top_action")) {
                                    Icon(Icons.Filled.Home, contentDescription = "主页")
                                }
                            },
                        )
                    },
                    bottomBar = {
                        AppMainNavigationBar(
                            items = listOf(
                                AppNavigationItem(
                                    label = "主页",
                                    icon = Icons.Filled.Home,
                                    selected = selectedTab == 0,
                                    tag = "nav_tab_home",
                                    onClick = { selectedTab = 0 },
                                ),
                                AppNavigationItem(
                                    label = "账号",
                                    icon = Icons.Filled.ManageAccounts,
                                    selected = selectedTab == 1,
                                    tag = "nav_tab_account",
                                    onClick = { selectedTab = 1 },
                                ),
                            ),
                            bottomPadding = 0.dp,
                            isDarkTheme = false,
                        )
                    },
                ) { padding ->
                    Column(Modifier.padding(padding)) {
                        Text("count=$counter", modifier = Modifier.testTag("counter_text"))
                        AppButton(onClick = { counter++ }, modifier = Modifier.testTag("inc_button")) {
                            Text("加一")
                        }
                        AppSwitch(
                            checked = checked,
                            onCheckedChange = { checked = it },
                            modifier = Modifier.testTag("test_switch"),
                        )
                        AppCheckbox(
                            checked = checked,
                            onCheckedChange = { checked = it },
                            modifier = Modifier.testTag("test_checkbox"),
                        )
                        AppSlider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            modifier = Modifier.testTag("test_slider"),
                        )
                        AppTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = "输入",
                            modifier = Modifier.testTag("test_input"),
                        )
                        AppPrimaryTabRow(selectedTabIndex = selectedTab) {
                            AppTab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("一") })
                            AppTab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("二") })
                        }
                        SettingItemGroup(title = "测试分组") {
                            SettingItemWithSwitch(
                                title = { Text("开关项") },
                                checked = checked,
                                onCheckedChange = { checked = it },
                                modifier = Modifier.testTag("test_setting_switch_row"),
                            )
                            SettingItem(
                                title = { Text("导航项") },
                                value = "默认",
                                rowKind = SettingRowKind.Navigation,
                                onClick = { counter++ },
                                modifier = Modifier.testTag("test_setting_nav_row"),
                            )
                        }
                    }
                }
            }
        }

        onNodeWithTag("inc_button").performClick()
        onNodeWithTag("inc_button").performClick()
        onNodeWithTag("counter_text").assertTextEquals("count=2")

        // 热切换到 Miuix：状态必须完整保留，所有控件与设置行正常渲染与交互
        runOnIdle {
            ThemeManager.setUiStyle(UiStyle.Miuix)
        }
        waitForIdle()
        onNodeWithTag("counter_text").assertTextEquals("count=2")
        onNodeWithTag("inc_button").performClick()
        onNodeWithTag("test_setting_nav_row").performClick()
        onNodeWithTag("test_setting_switch_row").performClick()
        onNodeWithTag("counter_text").assertTextEquals("count=4")
        onNodeWithTag("nav_tab_account").performClick()

        // 热切回 Material 3：状态仍完整保留
        runOnIdle {
            ThemeManager.setUiStyle(UiStyle.Material3)
        }
        waitForIdle()
        onNodeWithTag("counter_text").assertTextEquals("count=4")
    }

    @Test
    fun allThreeStyles_renderCardsTabsProgressAndSheetCustomDragHandle() = runComposeUiTest {
        var style by mutableStateOf(UiStyle.Material3)
        var cardClicks by mutableIntStateOf(0)
        var toggleChecked by mutableStateOf(false)

        setContent {
            ZhihuTheme {
                CompositionLocalProvider(LocalUiStyle provides style) {
                    Column {
                        AppCard(
                            onClick = { cardClicks++ },
                            modifier = Modifier.testTag("clickable_card"),
                        ) {
                            Text("clicks=$cardClicks", modifier = Modifier.testTag("card_clicks_text"))
                        }
                        AppLinearProgressIndicator(
                            progress = { 0.5f },
                            modifier = Modifier.testTag("linear_progress"),
                        )
                        AppPrimaryScrollableTabRow(selectedTabIndex = 0) {
                            AppTab(selected = true, onClick = {}, text = { Text("动态") })
                            AppTab(selected = false, onClick = {}, text = { Text("回答") })
                        }
                        AppIconToggleButton(
                            checked = toggleChecked,
                            onCheckedChange = { toggleChecked = it },
                            modifier = Modifier.testTag("icon_toggle"),
                        ) {
                            Icon(Icons.Filled.Home, contentDescription = "切换")
                        }
                        AppFilterChip(
                            selected = toggleChecked,
                            onClick = { toggleChecked = !toggleChecked },
                            label = { Text("筛选") },
                            enabled = false,
                            modifier = Modifier.testTag("disabled_chip"),
                        )
                        if (style != UiStyle.LiquidGlass) {
                            AppExtendedFloatingActionButton(
                                onClick = { cardClicks++ },
                                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                                text = { Text("预览") },
                                modifier = Modifier.testTag("extended_fab"),
                            )
                        }
                        MyModalBottomSheet(
                            onDismissRequest = {},
                            dragHandle = {
                                Text("自定义标题", modifier = Modifier.testTag("custom_drag_handle"))
                            },
                        ) {
                            Text("弹层正文", modifier = Modifier.testTag("sheet_body"))
                        }
                    }
                }
            }
        }

        for (targetStyle in listOf(UiStyle.Material3, UiStyle.Miuix, UiStyle.LiquidGlass)) {
            runOnIdle { style = targetStyle }
            waitForIdle()
            onNodeWithTag("custom_drag_handle", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag("sheet_body").assertIsDisplayed()
        }
    }

    @Test
    fun miuixCustomColor_clampedTabRow_andTallDialogConfirmButton_workCorrectly() = runComposeUiTest {
        val customGreenArgb = 0xFF1B873F.toInt()
        val customPurple = Color(0xFF7B2CBF)
        var capturedPrimary = Color.Unspecified
        var confirmed = false

        ThemeManager.load(
            ThemeSnapshot(
                themeMode = ThemeMode.LIGHT,
                useDynamicColor = false,
                customColor = customGreenArgb,
                uiStyle = UiStyle.Miuix,
            ),
        )

        setContent {
            ZhihuTheme {
                capturedPrimary = MaterialTheme.colorScheme.primary
                Column {
                    // 故意传入越界 selectedTabIndex，验证不抛 IndexOutOfBoundsException。
                    AppPrimaryTabRow(selectedTabIndex = 5) {
                        AppTab(selected = true, onClick = {}, text = { Text("一") })
                        AppTab(selected = false, onClick = {}, text = { Text("二") })
                    }
                    AppAlertDialog(
                        onDismissRequest = {},
                        title = { Text("长内容对话框") },
                        text = {
                            Column(
                                modifier = Modifier
                                    .height(2000.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Text("正文顶部")
                            }
                        },
                        confirmButton = {
                            AppButton(
                                onClick = { confirmed = true },
                                modifier = Modifier.testTag("tall_dialog_confirm"),
                            ) {
                                Text("确认")
                            }
                        },
                    )
                }
            }
        }

        waitForIdle()
        val firstPrimary = capturedPrimary
        onNodeWithTag("tall_dialog_confirm").assertIsDisplayed().performClick()
        waitForIdle()
        assertNotEquals(Color.Unspecified, firstPrimary)

        runOnIdle {
            ThemeManager.setCustomColor(customPurple)
        }
        waitForIdle()
        assertNotEquals(firstPrimary, capturedPrimary, "Miuix 关闭动态取色时，修改自定义主题色应立即改变主色调")
        kotlin.test.assertTrue(confirmed, "Miuix 长内容弹窗底部确认按钮必须保持可见且可点击")
    }
}
