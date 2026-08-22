package com.lumenconnection.stream.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.IOException

/**
 * Grava arquivos finalizados na biblioteca do usuário:
 * - padrão: MediaStore (Movies/Music/Downloads → "Lumen Stream")
 * - opcional: pasta escolhida via SAF (tree uri)
 */
object MediaSaver {

    const val FOLDER = "Lumen Stream"

    fun mimeTypeFor(ext: String): String = when (ext.lowercase()) {
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "opus" -> "audio/opus"
        "ogg", "oga" -> "audio/ogg"
        "wav" -> "audio/wav"
        "srt", "vtt" -> "text/plain"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        else -> "application/octet-stream"
    }

    fun isVideoExt(ext: String) = ext.lowercase() in setOf("mp4", "m4v", "webm", "mkv")
    fun isAudioExt(ext: String) = ext.lowercase() in setOf("mp3", "m4a", "opus", "ogg", "oga", "wav")
    fun isSubtitleExt(ext: String) = ext.lowercase() in setOf("srt", "vtt")

    /** Copia [file] para o destino configurado e retorna a uri final. Apaga o arquivo temporário. */
    fun persist(context: Context, file: File, customTreeUri: String?): Uri {
        val uri = if (customTreeUri != null) {
            saveToTree(context, Uri.parse(customTreeUri), file)
        } else {
            saveToMediaStore(context, file)
        }
        file.delete()
        return uri
    }

    private fun saveToMediaStore(context: Context, file: File): Uri {
        val ext = file.extension
        val mime = mimeTypeFor(ext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val (collection, relativeDir) = when {
                isVideoExt(ext) -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to
                    "${Environment.DIRECTORY_MOVIES}/$FOLDER"
                isAudioExt(ext) -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to
                    "${Environment.DIRECTORY_MUSIC}/$FOLDER"
                else -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to
                    "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER"
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(collection, values)
                ?: throw IOException("MediaStore insert failed for ${file.name}")
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } ?: throw IOException("openOutputStream failed")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        }

        // API 26–28: diretório público + media scanner
        val publicDir = when {
            isVideoExt(ext) -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            isAudioExt(ext) -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
        val outDir = File(publicDir, FOLDER).apply { mkdirs() }
        val outFile = File(outDir, file.name)
        file.copyTo(outFile, overwrite = true)
        MediaScannerConnection.scanFile(context, arrayOf(outFile.absolutePath), arrayOf(mime), null)
        return Uri.fromFile(outFile)
    }

    private fun saveToTree(context: Context, treeUri: Uri, file: File): Uri {
        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("invalid tree uri")
        val mime = mimeTypeFor(file.extension)
        tree.findFile(file.name)?.delete()
        val doc = tree.createFile(mime, file.name)
            ?: throw IOException("createFile failed in tree")
        context.contentResolver.openOutputStream(doc.uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        } ?: throw IOException("openOutputStream failed")
        return doc.uri
    }
}
