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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

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
                val remoteSize = head.contentLength() ?: 0L
                val storedSize = getStoredSize(context, modelId, file.localFileName)
                val storedSha256 = getStoredSha256(context, modelId, file.localFileName)

                // Verify local file integrity: check size and SHA256
                val needsRedownload =
                    if (destFile.exists()) {
                        val localSize = destFile.length()
                        val sizeMatches = localSize == storedSize && localSize == remoteSize

                        if (!sizeMatches) {
                            Log.w(
                                TAG,
                                "File ${file.localFileName} size mismatch (local=$localSize, stored=$storedSize, remote=$remoteSize)",
                            )
                            true
                        } else if (storedSha256 != null) {
                            // Verify SHA256 if we have it stored
                            val localSha256 = calculateSha256(destFile)
                            if (localSha256 != storedSha256) {
                                Log.w(TAG, "File ${file.localFileName} SHA256 mismatch")
                                true
                            } else {
                                // Check if remote has newer version
                                remoteEtag == null || storedEtag != remoteEtag
                            }
                        } else {
                            // No SHA256 stored, just check ETag
                            remoteEtag == null || storedEtag != remoteEtag
                        }
                    } else {
                        true
                    }

                if (!needsRedownload) {
                    Log.d(TAG, "File ${file.localFileName} is up to date and verified.")
                    result[file.localFileName] = destFile
                } else {
                    Log.d(TAG, "File ${file.localFileName} needs download (local ETag=$storedEtag, remote=$remoteEtag).")
                    filesToDownload.add(file)
                    fileSizes[file.url] = remoteSize
                    totalBytesToDownload += remoteSize
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check update for ${file.url}", e)
                // If we have a local file, verify its integrity before using it
                if (destFile.exists()) {
                    val storedSize = getStoredSize(context, modelId, file.localFileName)
                    if (storedSize > 0 && destFile.length() == storedSize) {
                        Log.w(TAG, "Using existing file ${file.localFileName} (network check failed)")
                        result[file.localFileName] = destFile
                    } else {
                        throw IllegalStateException("Local file ${file.localFileName} is corrupted and cannot verify remote version")
                    }
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

                    // Verify downloaded file size
                    val expectedSize = fileSizes[file.url] ?: 0L
                    if (expectedSize > 0 && tempFile.length() != expectedSize) {
                        tempFile.delete()
                        throw IllegalStateException(
                            "Downloaded file size mismatch for ${file.localFileName}: " +
                                "expected=$expectedSize, actual=${tempFile.length()}",
                        )
                    }

                    // Calculate and save SHA256
                    val sha256 = calculateSha256(tempFile)
                    setStoredSha256(context, modelId, file.localFileName, sha256)

                    // Save ETag and size
                    val etag = httpResponse.etag()
                    if (etag != null) {
                        setStoredEtag(context, modelId, file.localFileName, etag)
                    }
                    setStoredSize(context, modelId, file.localFileName, tempFile.length())
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

    private fun getStoredEtag(
        context: Context,
        modelId: String,
        fileName: String,
    ): String? {
        val prefs = context.getSharedPreferences("model_manager", Context.MODE_PRIVATE)
        return prefs.getString("${modelId}_${fileName}_etag", null)
    }

    private fun setStoredEtag(
        context: Context,
        modelId: String,
        fileName: String,
        etag: String,
    ) {
        val prefs = context.getSharedPreferences("model_manager", Context.MODE_PRIVATE)
        prefs.edit().putString("${modelId}_${fileName}_etag", etag).apply()
    }

    private fun getStoredSize(
        context: Context,
        modelId: String,
        fileName: String,
    ): Long {
        val prefs = context.getSharedPreferences("model_manager", Context.MODE_PRIVATE)
        return prefs.getLong("${modelId}_${fileName}_size", -1L)
    }

    private fun setStoredSize(
        context: Context,
        modelId: String,
        fileName: String,
        size: Long,
    ) {
        val prefs = context.getSharedPreferences("model_manager", Context.MODE_PRIVATE)
        prefs.edit().putLong("${modelId}_${fileName}_size", size).apply()
    }

    private fun getStoredSha256(
        context: Context,
        modelId: String,
        fileName: String,
    ): String? {
        val prefs = context.getSharedPreferences("model_manager", Context.MODE_PRIVATE)
        return prefs.getString("${modelId}_${fileName}_sha256", null)
    }

    private fun setStoredSha256(
        context: Context,
        modelId: String,
        fileName: String,
        sha256: String,
    ) {
        val prefs = context.getSharedPreferences("model_manager", Context.MODE_PRIVATE)
        prefs.edit().putString("${modelId}_${fileName}_sha256", sha256).apply()
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
