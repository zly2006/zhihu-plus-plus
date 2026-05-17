package com.github.zly2006.zhihu.latex

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.hrm.latex.renderer.font.MathFont
import com.hrm.latex.renderer.model.LatexFontFamilies
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val FONT_VERSION = "1"
private const val KATEX_BASE = "https://registry.npmmirror.com/katex/0.16.11/files/dist/fonts"
private val LM_MATH_URLS = listOf(
    "https://mirrors.ustc.edu.cn/CTAN/fonts/lm-math/opentype/latinmodern-math.otf",
    "https://mirrors.tuna.tsinghua.edu.cn/CTAN/fonts/lm-math/opentype/latinmodern-math.otf",
)

private val KATEX_FONTS = mapOf(
    "mainRegular" to "KaTeX_Main-Regular.ttf",
    "mainBold" to "KaTeX_Main-Bold.ttf",
    "mainItalic" to "KaTeX_Main-Italic.ttf",
    "mainBoldItalic" to "KaTeX_Main-BoldItalic.ttf",
    "mathItalic" to "KaTeX_Math-Italic.ttf",
    "mathBoldItalic" to "KaTeX_Math-BoldItalic.ttf",
    "amsRegular" to "KaTeX_AMS-Regular.ttf",
    "sansSerifRegular" to "KaTeX_SansSerif-Regular.ttf",
    "sansSerifBold" to "KaTeX_SansSerif-Bold.ttf",
    "sansSerifItalic" to "KaTeX_SansSerif-Italic.ttf",
    "typewriterRegular" to "KaTeX_Typewriter-Regular.ttf",
    "caligraphicRegular" to "KaTeX_Caligraphic-Regular.ttf",
    "caligraphicBold" to "KaTeX_Caligraphic-Bold.ttf",
    "frakturRegular" to "KaTeX_Fraktur-Regular.ttf",
    "frakturBold" to "KaTeX_Fraktur-Bold.ttf",
    "scriptRegular" to "KaTeX_Script-Regular.ttf",
    "size1Regular" to "KaTeX_Size1-Regular.ttf",
    "size2Regular" to "KaTeX_Size2-Regular.ttf",
    "size3Regular" to "KaTeX_Size3-Regular.ttf",
    "size4Regular" to "KaTeX_Size4-Regular.ttf",
)

class DownloadedFonts(
    val katexFamilies: LatexFontFamilies,
    val mathFont: MathFont,
)

private fun fontDir(context: Context): File =
    File(context.cacheDir, "latex-fonts/v$FONT_VERSION")

private fun isDownloaded(context: Context): Boolean =
    fontDir(context).resolve(".done").exists()

suspend fun downloadLatexFonts(context: Context, client: HttpClient): DownloadedFonts = withContext(Dispatchers.IO) {
    val dir = fontDir(context)
    dir.mkdirs()

    // Download KaTeX fonts
    for ((_, filename) in KATEX_FONTS) {
        val file = File(dir, filename)
        if (!file.exists()) {
            val bytes = client.get("$KATEX_BASE/$filename").bodyAsBytes()
            file.writeBytes(bytes)
        }
    }

    // Download Latin Modern Math (try multiple mirrors, validate content)
    val lmFile = File(dir, "latinmodern-math.otf")
    if (!lmFile.exists()) {
        var lastError: Exception? = null
        for (url in LM_MATH_URLS) {
            try {
                val bytes = client.get(url).bodyAsBytes()
                // Validate: OTF files start with "OTTO" magic bytes (0x4F54544F)
                if (bytes.size > 4 &&
                    bytes[0] == 0x4F.toByte() &&
                    bytes[1] == 0x54.toByte() &&
                    bytes[2] == 0x54.toByte() &&
                    bytes[3] == 0x4F.toByte()
                ) {
                    lmFile.writeBytes(bytes)
                    break
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        if (!lmFile.exists()) throw lastError ?: RuntimeException("Failed to download Latin Modern Math from all mirrors")
    }

    // Build FontFamilies from downloaded bytes
    val mainRegular = fontFromFile(File(dir, "KaTeX_Main-Regular.ttf"), FontWeight.Normal, FontStyle.Normal)
    val mainBold = fontFromFile(File(dir, "KaTeX_Main-Bold.ttf"), FontWeight.Bold, FontStyle.Normal)
    val mainItalic = fontFromFile(File(dir, "KaTeX_Main-Italic.ttf"), FontWeight.Normal, FontStyle.Italic)
    val mainBoldItalic = fontFromFile(File(dir, "KaTeX_Main-BoldItalic.ttf"), FontWeight.Bold, FontStyle.Italic)
    val mathItalic = fontFromFile(File(dir, "KaTeX_Math-Italic.ttf"), FontWeight.Normal, FontStyle.Italic)
    val mathBoldItalic = fontFromFile(File(dir, "KaTeX_Math-BoldItalic.ttf"), FontWeight.Bold, FontStyle.Italic)
    val amsRegular = fontFromFile(File(dir, "KaTeX_AMS-Regular.ttf"))
    val sansSerifRegular = fontFromFile(File(dir, "KaTeX_SansSerif-Regular.ttf"), FontWeight.Normal, FontStyle.Normal)
    val sansSerifBold = fontFromFile(File(dir, "KaTeX_SansSerif-Bold.ttf"), FontWeight.Bold, FontStyle.Normal)
    val sansSerifItalic = fontFromFile(File(dir, "KaTeX_SansSerif-Italic.ttf"), FontWeight.Normal, FontStyle.Italic)
    val typewriterRegular = fontFromFile(File(dir, "KaTeX_Typewriter-Regular.ttf"))
    val caligraphicRegular = fontFromFile(File(dir, "KaTeX_Caligraphic-Regular.ttf"), FontWeight.Normal, FontStyle.Normal)
    val caligraphicBold = fontFromFile(File(dir, "KaTeX_Caligraphic-Bold.ttf"), FontWeight.Bold, FontStyle.Normal)
    val frakturRegular = fontFromFile(File(dir, "KaTeX_Fraktur-Regular.ttf"), FontWeight.Normal, FontStyle.Normal)
    val frakturBold = fontFromFile(File(dir, "KaTeX_Fraktur-Bold.ttf"), FontWeight.Bold, FontStyle.Normal)
    val scriptRegular = fontFromFile(File(dir, "KaTeX_Script-Regular.ttf"))
    val size1Regular = fontFromFile(File(dir, "KaTeX_Size1-Regular.ttf"))
    val size2Regular = fontFromFile(File(dir, "KaTeX_Size2-Regular.ttf"))
    val size3Regular = fontFromFile(File(dir, "KaTeX_Size3-Regular.ttf"))
    val size4Regular = fontFromFile(File(dir, "KaTeX_Size4-Regular.ttf"))

    val families = LatexFontFamilies(
        main = FontFamily(mainRegular, mainBold, mainItalic, mainBoldItalic),
        math = FontFamily(mathItalic, mathBoldItalic),
        ams = FontFamily(amsRegular),
        sansSerif = FontFamily(sansSerifRegular, sansSerifBold, sansSerifItalic),
        monospace = FontFamily(typewriterRegular),
        caligraphic = FontFamily(caligraphicRegular, caligraphicBold),
        fraktur = FontFamily(frakturRegular, frakturBold),
        script = FontFamily(scriptRegular),
        size1 = FontFamily(size1Regular),
        size2 = FontFamily(size2Regular),
        size3 = FontFamily(size3Regular),
        size4 = FontFamily(size4Regular),
    )

    // Build OTF MathFont from Latin Modern Math bytes
    val otfBytes = lmFile.readBytes()
    val otfFamily = FontFamily(Font(lmFile))

    dir.resolve(".done").createNewFile()

    DownloadedFonts(
        katexFamilies = families,
        mathFont = MathFont.OTF(otfBytes, otfFamily),
    )
}

private fun fontFromFile(file: File, weight: FontWeight = FontWeight.Normal, style: FontStyle = FontStyle.Normal): Font =
    Font(file, weight, style)

enum class FontLoadState {
    IDLE,
    DOWNLOADING,
    READY,
    ERROR,
}

class FontLoadResult(
    val state: FontLoadState,
    val downloaded: DownloadedFonts? = null,
)

@Composable
fun rememberLatexFonts(context: Context, client: HttpClient): FontLoadResult {
    var result by remember { mutableStateOf(FontLoadResult(FontLoadState.IDLE)) }

    LaunchedEffect(Unit) {
        if (!isDownloaded(context)) {
            result = FontLoadResult(FontLoadState.DOWNLOADING)
        }
        try {
            val fonts = downloadLatexFonts(context, client)
            result = FontLoadResult(FontLoadState.READY, fonts)
        } catch (_: Exception) {
            result = FontLoadResult(FontLoadState.ERROR)
        }
    }

    return result
}
