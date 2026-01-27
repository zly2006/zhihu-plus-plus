package com.github.zly2006.zhihu.nlp

import android.content.Context
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.head
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.etag
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ModelManager {
    private const val TAG = "ModelManager"
    private val client = HttpClient(Android)

    data class RemoteFile(
        val url: String,
        val localFileName: String,
    )

    suspend fun downloadModel(
        context: Context,
        modelId: String,
        files: List<RemoteFile>,
        onProgress: (Float) -> Unit,
    ): Map<String, File> = withContext(Dispatchers.IO) {
        val modelDir = File(context.filesDir, "models/$modelId")
        if (!modelDir.exists()) modelDir.mkdirs()

        val result = mutableMapOf<String, File>()
        val fileSizes = mutableMapOf<String, Long>()
        var totalBytesToDownload = 0L

        // Check for updates and calculate total size
        val filesToDownload = mutableListOf<RemoteFile>()

        for (file in files) {
            val destFile = File(modelDir, file.localFileName)

            try {
                val head = client.head(file.url)
                val remoteEtag = head.etag()
                val storedEtag = getStoredEtag(context, modelId, file.localFileName)
                val length = head.contentLength() ?: 0L

                // If file exists and ETag matches, skip download
                if (destFile.exists() && remoteEtag != null && storedEtag == remoteEtag) {
                    Log.d(TAG, "File ${file.localFileName} is up to date.")
                    result[file.localFileName] = destFile
                } else {
                    Log.d(TAG, "File ${file.localFileName} needs update (local=$storedEtag, remote=$remoteEtag).")
                    filesToDownload.add(file)
                    fileSizes[file.url] = length
                    totalBytesToDownload += length
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check update for ${file.url}", e)
                // If we have a local file, assume it's good enough if network fails
                if (destFile.exists()) {
                    result[file.localFileName] = destFile
                } else {
                    throw e
                }
            }
        }

        if (filesToDownload.isEmpty()) {
            onProgress(1.0f)
            return@withContext result
        }

        var downloadedBytesGlobal = 0L

        for (file in filesToDownload) {
            val destFile = File(modelDir, file.localFileName)
            val tempFile = File(modelDir, file.localFileName + ".tmp")

            Log.d(TAG, "Downloading ${file.url}...")

            client
                .prepareGet(file.url) {
                    onDownload { bytesSentTotal, contentLength ->
                        // handled manually below for aggregated progress
                    }
                }.execute { httpResponse ->
                    val channel = httpResponse.bodyAsChannel()
                    val output = FileOutputStream(tempFile)
                    try {
                        val input = channel.toInputStream()
                        val buffer = ByteArray(8192)
                        var bytesRead = input.read(buffer)
                        while (bytesRead >= 0) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytesGlobal += bytesRead
                            if (totalBytesToDownload > 0) {
                                onProgress(downloadedBytesGlobal.toFloat() / totalBytesToDownload)
                            }
                            bytesRead = input.read(buffer)
                        }
                    } finally {
                        output.close()
                    }

                    // Save ETag
                    val etag = httpResponse.etag()
                    if (etag != null) {
                        setStoredEtag(context, modelId, file.localFileName, etag)
                    }
                }

            if (destFile.exists()) destFile.delete()
            tempFile.renameTo(destFile)
            result[file.localFileName] = destFile
        }

        // Ensure all requested files are in result
        for (file in files) {
            if (!result.containsKey(file.localFileName)) {
                result[file.localFileName] = File(modelDir, file.localFileName)
            }
        }

        return@withContext result
    }

    private fun getStoredEtag(context: Context, modelId: String, fileName: String): String? {
        val prefs = context.getSharedPreferences("model_manager", Context.MODE_PRIVATE)
        return prefs.getString("${modelId}_${fileName}_etag", null)
    }

    private fun setStoredEtag(context: Context, modelId: String, fileName: String, etag: String) {
        val prefs = context.getSharedPreferences("model_manager", Context.MODE_PRIVATE)
        prefs.edit().putString("${modelId}_${fileName}_etag", etag).apply()
    }
}
