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
    kotlinx.cinterop.BetaInteropApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
)

package com.github.zly2006.zhihu.macos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.platform.LocalMacosContentAreaInset
import com.github.zly2006.zhihu.platform.MacosUserMessageHost
import com.github.zly2006.zhihu.ui.MacosWindowChrome
import com.github.zly2006.zhihu.ui.MacosWindowNavigationItem
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCSignatureOverride
import platform.AppKit.NSColor
import platform.AppKit.NSImage
import platform.AppKit.NSImageView
import platform.AppKit.NSOutlineView
import platform.AppKit.NSOutlineViewDataSourceProtocol
import platform.AppKit.NSOutlineViewDelegateProtocol
import platform.AppKit.NSScrollView
import platform.AppKit.NSTableCellView
import platform.AppKit.NSTableColumn
import platform.AppKit.NSTableViewRowSizeStyleDefault
import platform.AppKit.NSTableViewStyle.NSTableViewStyleSourceList
import platform.AppKit.NSTextField
import platform.AppKit.NSView
import platform.AppKit.NSViewHeightSizable
import platform.AppKit.NSViewWidthSizable
import platform.AppKit.NSVisualEffectMaterialSidebar
import platform.AppKit.NSVisualEffectView
import platform.AppKit.NSWindow
import platform.AppKit.labelWithString
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSIndexSet
import platform.Foundation.NSSelectorFromString
import platform.darwin.NSObject

private const val SIDEBAR_WIDTH = 240.0
private const val SIDEBAR_COLUMN_IDENTIFIER = "com.github.zly2006.zhihu.macos.sidebar.column"

@Composable
internal fun MacosNativeWindowChrome(
    window: NSWindow,
    chrome: MacosWindowChrome,
    content: @Composable (Modifier) -> Unit,
) {
    var sidebarVisible by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize()) {
        val contentAreaModifier = if (sidebarVisible) {
            Modifier.padding(start = SIDEBAR_WIDTH.dp)
        } else {
            Modifier
        }
        CompositionLocalProvider(
            LocalMacosContentAreaInset provides if (sidebarVisible) SIDEBAR_WIDTH.dp else 0.dp,
        ) {
            MacosUserMessageHost(modifier = contentAreaModifier) {
                content(Modifier.fillMaxSize())
            }
        }
    }

    MacosNativeSidebar(
        window = window,
        items = chrome.navigationItems,
        visible = sidebarVisible,
    )
    MacosNativeToolbar(
        window = window,
        leadingActions = listOf(
            MacosNativeToolbarAction(
                identifier = "sidebar",
                label = "侧栏",
                systemSymbolName = "sidebar.left",
                action = { sidebarVisible = !sidebarVisible },
            ),
        ),
        trailingActions = chrome.trailingToolbarItems.map { item ->
            MacosNativeToolbarAction(
                identifier = item.identifier,
                label = item.label,
                systemSymbolName = item.systemSymbolName,
                action = item.action,
            )
        },
    )
}

@Composable
private fun MacosNativeSidebar(
    window: NSWindow,
    items: List<MacosWindowNavigationItem>,
    visible: Boolean,
) {
    val controller = remember(window) { MacosNativeSidebarController(window) }

    SideEffect {
        controller.update(items, visible)
    }

    DisposableEffect(window, controller) {
        controller.update(items, visible)
        controller.attach()
        onDispose(controller::detach)
    }
}

internal class MacosNativeSidebarController(
    private val window: NSWindow,
) : NSObject(),
    NSOutlineViewDataSourceProtocol,
    NSOutlineViewDelegateProtocol {
    private class SectionNode(
        val title: String,
        val items: List<ItemNode>,
    ) : NSObject()

    private class ItemNode(
        val model: MacosWindowNavigationItem,
    ) : NSObject()

    private data class VisualItem(
        val identifier: String,
        val title: String,
        val systemSymbolName: String,
        val sectionTitle: String,
        val selected: Boolean,
    )

    private var sections: List<SectionNode> = emptyList()
    private var items: List<MacosWindowNavigationItem> = emptyList()
    private var visualItems: List<VisualItem> = emptyList()
    private var attached = false

    private val tableColumn = NSTableColumn(SIDEBAR_COLUMN_IDENTIFIER)
    internal val outlineView = NSOutlineView().apply {
        addTableColumn(tableColumn)
        outlineTableColumn = tableColumn
        headerView = null
        style = NSTableViewStyleSourceList
        rowSizeStyle = NSTableViewRowSizeStyleDefault
        floatsGroupRows = false
        allowsMultipleSelection = false
        allowsEmptySelection = false
        backgroundColor = NSColor.clearColor
        target = this@MacosNativeSidebarController
        action = NSSelectorFromString("selectNavigation:")
        setDataSource(this@MacosNativeSidebarController)
        setDelegate(this@MacosNativeSidebarController)
    }
    private val scrollView = NSScrollView().apply {
        drawsBackground = false
        hasVerticalScroller = true
        autohidesScrollers = true
        documentView = outlineView
        setAutoresizingMask(NSViewWidthSizable or NSViewHeightSizable)
    }
    private val sidebarView = NSVisualEffectView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
        material = NSVisualEffectMaterialSidebar
        setAutoresizingMask(NSViewHeightSizable)
        addSubview(scrollView)
    }

    fun update(
        items: List<MacosWindowNavigationItem>,
        visible: Boolean,
    ) {
        this.items = items
        sidebarView.setHidden(!visible)
        val newVisualItems = items.map { item ->
            VisualItem(
                identifier = item.identifier,
                title = item.title,
                systemSymbolName = item.systemSymbolName,
                sectionTitle = item.sectionTitle,
                selected = item.selected,
            )
        }
        if (newVisualItems == visualItems) return
        visualItems = newVisualItems
        sections = items
            .groupBy(MacosWindowNavigationItem::sectionTitle)
            .map { (title, sectionItems) ->
                SectionNode(title, sectionItems.map(::ItemNode))
            }
        if (attached) reload()
    }

    fun attach() {
        if (attached || sections.isEmpty()) return
        val contentView = window.contentView ?: return
        val contentBounds = contentView.bounds
        sidebarView.frame = CGRectMake(0.0, 0.0, SIDEBAR_WIDTH, CGRectGetHeight(contentBounds))
        scrollView.frame = sidebarView.bounds
        contentView.addSubview(sidebarView)
        attached = true
        reload()
    }

    fun detach() {
        if (!attached) return
        attached = false
        outlineView.setDelegate(null)
        outlineView.setDataSource(null)
        sidebarView.removeFromSuperview()
    }

    @ObjCSignatureOverride
    override fun outlineView(
        outlineView: NSOutlineView,
        numberOfChildrenOfItem: Any?,
    ): Long = when (numberOfChildrenOfItem) {
        null -> sections.size.toLong()
        is SectionNode -> numberOfChildrenOfItem.items.size.toLong()
        else -> 0L
    }

    @ObjCSignatureOverride
    override fun outlineView(
        outlineView: NSOutlineView,
        child: Long,
        ofItem: Any?,
    ): Any = when (ofItem) {
        null -> sections[child.toInt()]
        is SectionNode -> ofItem.items[child.toInt()]
        else -> error("Unsupported sidebar parent")
    }

    @ObjCSignatureOverride
    override fun outlineView(
        outlineView: NSOutlineView,
        isItemExpandable: Any,
    ): Boolean = isItemExpandable is SectionNode

    @ObjCSignatureOverride
    override fun outlineView(
        outlineView: NSOutlineView,
        isGroupItem: Any,
    ): Boolean = isGroupItem is SectionNode

    @ObjCSignatureOverride
    override fun outlineView(
        outlineView: NSOutlineView,
        shouldSelectItem: Any,
    ): Boolean = shouldSelectItem is ItemNode

    override fun outlineView(
        outlineView: NSOutlineView,
        viewForTableColumn: NSTableColumn?,
        item: Any,
    ): NSView = when (item) {
        is SectionNode -> NSTextField.labelWithString(item.title)
        is ItemNode -> NSTableCellView().apply {
            val imageView = NSImageView().apply {
                image = NSImage.imageWithSystemSymbolName(item.model.systemSymbolName, item.model.title)
            }
            val label = NSTextField.labelWithString(item.model.title)
            this.imageView = imageView
            textField = label
            addSubview(imageView)
            addSubview(label)
        }
        else -> error("Unsupported sidebar item")
    }

    @ObjCAction
    fun selectNavigation(sender: NSObject) {
        val selectedItem = outlineView.itemAtRow(outlineView.selectedRow) as? ItemNode ?: return
        items.firstOrNull { it.identifier == selectedItem.model.identifier }?.action?.invoke()
    }

    private fun reload() {
        outlineView.reloadData()
        sections.forEach(outlineView::expandItem)
        val selectedNode = sections
            .flatMap(SectionNode::items)
            .firstOrNull { it.model.selected }
            ?: return
        val selectedRow = outlineView.rowForItem(selectedNode)
        if (selectedRow >= 0) {
            outlineView.selectRowIndexes(NSIndexSet.indexSetWithIndex(selectedRow.toULong()), false)
            outlineView.scrollRowToVisible(selectedRow)
        }
    }
}
