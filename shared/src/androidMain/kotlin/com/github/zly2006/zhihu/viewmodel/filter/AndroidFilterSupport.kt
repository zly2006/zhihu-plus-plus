/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu.viewmodel.filter
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.github.zly2006.zhihu.shared.nlp.KeywordWeightExtractor
import com.github.zly2006.zhihu.shared.platform.androidSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun BlocklistManager.exportAllBlocklistToJson(context: Context): File = withContext(Dispatchers.IO) {
    val dir = context.getExternalFilesDir(null) ?: context.filesDir
    val file = File(dir, "zhihupp_blocklist.json")
    file.writeText(exportAllBlocklistToJsonText())
    file
}

suspend fun BlocklistManager.importAllBlocklistFromJson(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val text = context.contentResolver
        .openInputStream(uri)
        ?.bufferedReader()
        ?.readText()
        ?: return@withContext "读取文件失败"
    importAllBlocklistFromJsonText(text)
}

private const val CONTENT_FILTER_DATABASE_NAME = "content_filter_database"

@Volatile
private var contentFilterDatabase: ContentFilterDatabase? = null

fun getContentFilterDatabase(context: Context): ContentFilterDatabase =
    contentFilterDatabase ?: synchronized(ContentFilterDatabase::class) {
        contentFilterDatabase ?: buildContentFilterDatabase(
            Room.databaseBuilder<ContentFilterDatabase>(
                context.applicationContext,
                CONTENT_FILTER_DATABASE_NAME,
            ),
        ).also {
            contentFilterDatabase = it
        }
    }

object AndroidContentFilterRuntime {
    var semanticMatcher: KeywordSemanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() }
    var keywordWeightExtractor: KeywordWeightExtractor = KeywordWeightExtractor { _, _ -> emptyList() }
}

@Composable
actual fun getContentFilterDatabase(): ContentFilterDatabase {
    val context = LocalContext.current
    return remember(context) { getContentFilterDatabase(context) }
}

fun Context.contentFilterSettings(): FeedFilterSettings =
    androidSettingsStore(this).toFeedFilterSettings()
