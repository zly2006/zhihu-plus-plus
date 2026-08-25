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

@file:OptIn(
    androidx.compose.ui.InternalComposeUiApi::class,
    androidx.compose.ui.test.ExperimentalTestApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
)

package com.github.zly2006.zhihu.macos.debug

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.backhandler.LocalCompatNavigationEventDispatcherOwner
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import com.github.zly2006.zhihu.account.LoginScreen
import com.github.zly2006.zhihu.data.BACKGROUND_UI_DEBUG_DATA_HOME_ENV
import com.github.zly2006.zhihu.data.macosBackgroundUiDebugDataDirectoryPath
import com.github.zly2006.zhihu.platform.MacosUserMessageHost
import com.github.zly2006.zhihu.platform.UserMessageDuration
import com.github.zly2006.zhihu.platform.showMacosUserMessage
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.MacosZhihuMain
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.impl.use
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.dataWithBytes
import platform.posix.STDERR_FILENO
import platform.posix.STDOUT_FILENO
import platform.posix.dup
import platform.posix.dup2
import platform.posix.fdopen
import platform.posix.fflush
import platform.posix.fputs
import platform.posix.getpid
import platform.posix.setenv
import platform.posix.unsetenv
import kotlin.time.TimeSource

private const val PROTOCOL = "ZHPP_BACKGROUND_UI_DEBUG_V1"
private const val DEFAULT_TIMEOUT_MS = 5_000L

fun main(args: Array<String>) {
    require(args.all { it == "--root=main" || it == "--root=login" }) {
        "Only --root=main and --root=login are supported"
    }
    val rootName = if ("--root=login" in args) "login" else "main"
    val isolatedDataHome =
        "${NSTemporaryDirectory().trimEnd('/')}/zhihupp-ui-debug-${NSUUID().UUIDString}"
    check(
        NSFileManager.defaultManager.createDirectoryAtPath(
            isolatedDataHome,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        ),
    ) {
        "Cannot create isolated background UI debug data directory"
    }
    try {
        check(setenv(BACKGROUND_UI_DEBUG_DATA_HOME_ENV, isolatedDataHome, 1) == 0) {
            "Cannot configure isolated background UI debug data directory"
        }
        check(macosBackgroundUiDebugDataDirectoryPath() == isolatedDataHome) {
            "Background UI debugger did not activate isolated data storage"
        }
        fflush(null)
        val protocolDescriptor = dup(STDOUT_FILENO)
        check(protocolDescriptor >= 0) { "Cannot duplicate stdout for the UI debug protocol" }
        check(dup2(STDERR_FILENO, STDOUT_FILENO) >= 0) { "Cannot redirect application logs to stderr" }
        val protocolOutput = checkNotNull(fdopen(protocolDescriptor, "w")) {
            "Cannot open the UI debug protocol stream"
        }

        val uiTest = SkikoComposeUiTest(width = 1280, height = 900)
        val backController = BackgroundBackController()
        try {
            uiTest.runTest {
                setContent {
                    val navigationEventDispatcherOwner =
                        checkNotNull(LocalCompatNavigationEventDispatcherOwner.current) {
                            "Compose navigation event dispatcher is unavailable"
                        }
                    SideEffect {
                        backController.connect(navigationEventDispatcherOwner)
                    }
                    if (rootName == "login") {
                        ZhihuTheme {
                            MacosUserMessageHost {
                                LoginScreen(
                                    onLoginComplete = {},
                                    onOpenTelemetrySettings = {},
                                )
                            }
                        }
                    } else {
                        ZhihuTheme {
                            MacosUserMessageHost {
                                MacosZhihuMain()
                            }
                        }
                    }
                }
                protocolOutput.emit(
                    buildJsonObject {
                        put("event", "ready")
                        put("protocol", PROTOCOL)
                        put("root", rootName)
                        put("windowHost", false)
                        put("dataMode", "isolated")
                        put("dataHome", isolatedDataHome)
                    },
                )

                while (true) {
                    val line = readlnOrNull() ?: break
                    val startedAt = TimeSource.Monotonic.markNow()
                    var requestId = ""
                    var operation = ""
                    val response = try {
                        val command = Json.parseToJsonElement(line).jsonObject
                        requestId = command.requiredString("id")
                        operation = command.requiredString("op")
                        val data = execute(command, rootName, isolatedDataHome, backController)
                        buildJsonObject {
                            put("id", requestId)
                            put("ok", true)
                            put("elapsedMs", startedAt.elapsedNow().inWholeMilliseconds)
                            put("data", data)
                        }
                    } catch (error: Throwable) {
                        println(
                            "[ui-debug][$requestId][$operation] " +
                                "${error::class.simpleName ?: "Error"}: ${error.message.orEmpty()}",
                        )
                        buildJsonObject {
                            put("id", requestId)
                            put("ok", false)
                            put("elapsedMs", startedAt.elapsedNow().inWholeMilliseconds)
                            put("error", error.message ?: error::class.simpleName ?: "Unknown error")
                        }
                    }
                    protocolOutput.emit(response)
                    if (operation == "quit") break
                }
            }
        } finally {
            backController.close()
        }
    } finally {
        unsetenv(BACKGROUND_UI_DEBUG_DATA_HOME_ENV)
        NSFileManager.defaultManager.removeItemAtPath(isolatedDataHome, error = null)
    }
}

private fun SkikoComposeUiTest.execute(
    command: JsonObject,
    rootName: String,
    isolatedDataHome: String,
    backController: BackgroundBackController,
): JsonElement =
    when (command.requiredString("op")) {
        "state" ->
            buildJsonObject {
                put("protocol", PROTOCOL)
                put("root", rootName)
                put("pid", getpid())
                put("bundle", NSBundle.mainBundle.bundlePath)
                put("windowHost", false)
                put("dataMode", "isolated")
                put("dataHome", isolatedDataHome)
                put("surfaceWidth", 1280)
                put("surfaceHeight", 900)
            }
        "dump" ->
            JsonPrimitive(
                if ("selector" in command) {
                    interaction(command).printToString(command.integer("maxDepth", 40))
                } else {
                    onRoot(command.boolean("useUnmergedTree", true))
                        .printToString(command.integer("maxDepth", 40))
                },
            )
        "list_clickables" ->
            JsonPrimitive(
                onAllNodes(
                    hasClickAction(),
                    useUnmergedTree = command.boolean("useUnmergedTree", true),
                ).printToString(command.integer("maxDepth", 2)),
            )
        "click" -> {
            interaction(command).performSemanticsAction(SemanticsActions.OnClick)
            JsonNull
        }
        "dismiss" -> {
            interaction(command).performSemanticsAction(SemanticsActions.Dismiss)
            JsonNull
        }
        "input" -> {
            val target = interaction(command)
            if (command.boolean("clear", true)) target.performTextClearance()
            target.performTextInput(command.requiredString("text"))
            JsonNull
        }
        "scroll" -> {
            val direction = command.requiredString("direction")
            interaction(command).performTouchInput {
                when (direction) {
                    "up" -> swipeUp()
                    "down" -> swipeDown()
                    "left" -> swipeLeft()
                    "right" -> swipeRight()
                    else -> error("Unsupported scroll direction: $direction")
                }
            }
            JsonNull
        }
        "key" -> {
            val key = when (command.requiredString("key")) {
                "escape" -> Key.Escape
                "enter" -> Key.Enter
                "tab" -> Key.Tab
                "space" -> Key.Spacebar
                "backspace" -> Key.Backspace
                "delete" -> Key.Delete
                "up" -> Key.DirectionUp
                "down" -> Key.DirectionDown
                "left" -> Key.DirectionLeft
                "right" -> Key.DirectionRight
                else -> error("Unsupported key")
            }
            val target = if ("selector" in command) {
                interaction(command)
            } else {
                onAllNodes(
                    isRoot(),
                    useUnmergedTree = command.boolean("useUnmergedTree", true),
                )[command.integer("rootIndex", 0)]
            }
            target.performKeyInput {
                pressKey(key)
            }
            JsonNull
        }
        "back" -> {
            backController.back()
            waitForIdle()
            JsonNull
        }
        "show_message" -> {
            val duration = when (command["duration"]?.jsonPrimitive?.contentOrNull ?: "short") {
                "short" -> UserMessageDuration.Short
                "long" -> UserMessageDuration.Long
                else -> error("Unsupported user message duration")
            }
            showMacosUserMessage(command.requiredString("message"), duration)
            waitForIdle()
            JsonNull
        }
        "wait" -> {
            val selector = command.selector()
            val exists = command.boolean("exists", true)
            waitUntil(
                conditionDescription = "selector ${selector.matcher.description} exists=$exists",
                timeoutMillis = command.long("timeoutMs", DEFAULT_TIMEOUT_MS),
            ) {
                val matches =
                    onAllNodes(selector.matcher, selector.useUnmergedTree)
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                (matches.size > selector.index) == exists
            }
            JsonNull
        }
        "wait_clickables" -> {
            val minimumCount = command.integer("minimumCount", 1)
            val useUnmergedTree = command.boolean("useUnmergedTree", true)
            require(minimumCount >= 0) { "minimumCount must not be negative" }
            waitUntil(
                conditionDescription = "at least $minimumCount clickable semantics nodes",
                timeoutMillis = command.long("timeoutMs", DEFAULT_TIMEOUT_MS),
            ) {
                onAllNodes(hasClickAction(), useUnmergedTree)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .size >= minimumCount
            }
            JsonPrimitive(
                onAllNodes(hasClickAction(), useUnmergedTree)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .size,
            )
        }
        "advance" -> {
            val milliseconds = command.long("milliseconds", 100L)
            require(milliseconds in 0L..60_000L) { "milliseconds must be between 0 and 60000" }
            mainClock.advanceTimeBy(milliseconds)
            waitForIdle()
            JsonNull
        }
        "screenshot" -> captureScreenshot(command.requiredString("file"))
        "quit" -> JsonNull
        else -> error("Unsupported operation: ${command.requiredString("op")}")
    }

private class BackgroundBackController {
    private val input = DirectNavigationEventInput()
    private var dispatcher: NavigationEventDispatcher? = null

    fun connect(owner: NavigationEventDispatcherOwner) {
        val newDispatcher = owner.navigationEventDispatcher
        if (dispatcher === newDispatcher) return
        dispatcher?.removeInput(input)
        newDispatcher.addInput(input)
        dispatcher = newDispatcher
    }

    fun back() {
        checkNotNull(dispatcher) { "Compose navigation event dispatcher is not connected" }
        input.backCompleted()
    }

    fun close() {
        dispatcher?.removeInput(input)
        dispatcher = null
    }
}

private fun SkikoComposeUiTest.interaction(command: JsonObject) =
    command.selector().let { selector ->
        onAllNodes(selector.matcher, selector.useUnmergedTree)[selector.index]
    }

private data class Selector(
    val matcher: SemanticsMatcher,
    val index: Int,
    val useUnmergedTree: Boolean,
)

private fun JsonObject.selector(): Selector {
    val selector = this["selector"]?.jsonObject ?: error("selector is required")
    val candidates =
        listOfNotNull(
            selector["tag"]?.jsonPrimitive?.contentOrNull?.let(::hasTestTag),
            selector["text"]?.jsonPrimitive?.contentOrNull?.let { hasText(it) },
            selector["textContains"]?.jsonPrimitive?.contentOrNull?.let { hasText(it, substring = true) },
            selector["contentDescription"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.let { hasContentDescription(it) },
        )
    require(candidates.size == 1) {
        "selector must contain exactly one of tag, text, textContains, or contentDescription"
    }
    val index = selector.integer("index", 0)
    require(index >= 0) { "selector.index must not be negative" }
    return Selector(
        matcher = candidates.single(),
        index = index,
        useUnmergedTree = selector.boolean("useUnmergedTree", true),
    )
}

private fun SkikoComposeUiTest.captureScreenshot(filePath: String): JsonObject {
    require(filePath.startsWith("/tmp/")) {
        "Offscreen screenshots must be written under /tmp"
    }
    require(filePath.split('/').none { it == "." || it == ".." }) {
        "Offscreen screenshot paths must not escape /tmp"
    }
    val bitmap = captureToImage()
    val pngBytes =
        Image.makeFromBitmap(bitmap.asSkiaBitmap()).use { image ->
            checkNotNull(image.encodeToData(EncodedImageFormat.PNG, 100)) {
                "Skia failed to encode the offscreen surface"
            }.use { encoded -> encoded.bytes }
        }
    val parentDirectory = filePath.substringBeforeLast('/')
    val fileManager = NSFileManager.defaultManager
    check(
        fileManager.createDirectoryAtPath(
            parentDirectory,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        ),
    ) {
        "Cannot create screenshot directory: $parentDirectory"
    }
    val pngData = pngBytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), pngBytes.size.toULong())
    }
    check(fileManager.createFileAtPath(filePath, contents = pngData, attributes = null)) {
        "Cannot write offscreen screenshot: $filePath"
    }

    val pixels = bitmap.toPixelMap()
    val sampledColors = mutableSetOf<Int>()
    val xStep = (bitmap.width / 32).coerceAtLeast(1)
    val yStep = (bitmap.height / 32).coerceAtLeast(1)
    for (y in 0 until bitmap.height step yStep) {
        for (x in 0 until bitmap.width step xStep) {
            sampledColors += pixels[x, y].toArgb()
        }
    }
    return buildJsonObject {
        put("file", filePath)
        put("bytes", pngBytes.size)
        put("sampledColors", sampledColors.size)
        put("nonUniform", sampledColors.size > 1)
    }
}

private fun JsonObject.requiredString(name: String): String =
    this[name]?.jsonPrimitive?.contentOrNull ?: error("$name is required")

private fun JsonObject.boolean(
    name: String,
    default: Boolean,
): Boolean = this[name]?.jsonPrimitive?.booleanOrNull ?: default

private fun JsonObject.integer(
    name: String,
    default: Int,
): Int = this[name]?.jsonPrimitive?.intOrNull ?: default

private fun JsonObject.long(
    name: String,
    default: Long,
): Long = this[name]?.jsonPrimitive?.longOrNull ?: default

private fun kotlinx.cinterop.CPointer<platform.posix.FILE>.emit(json: JsonObject) {
    check(fputs(json.toString(), this) >= 0) { "Cannot write UI debug protocol response" }
    check(fputs("\n", this) >= 0) { "Cannot terminate UI debug protocol response" }
    fflush(this)
}
