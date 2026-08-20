@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hrm.markdown.renderer.selection.androidx

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key

internal actual fun isMarkdownSelectionCopyKeyEvent(keyEvent: KeyEvent): Boolean =
    keyEvent.key == Key.C && keyEvent.isMetaPressed || keyEvent.key == Key.Copy

internal actual fun Modifier.markdownSelectionMagnifier(manager: MarkdownSelectionManager): Modifier = this

internal actual fun Modifier.addMarkdownSelectionContainerTextContextMenuComponents(
    selectionManager: MarkdownSelectionManager,
): Modifier = this
