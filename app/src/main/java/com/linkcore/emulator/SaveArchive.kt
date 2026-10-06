package com.linkcore.emulator

import android.util.AtomicFile
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.util.zip.*

/** Portable battery saves, bound to the exact ROM. Never contains game data. */
object SaveArchive {
    class Failure(val resourceId:Int):IllegalArgumentException()
    private val sizes = setOf(512, 8192, 32768, 65536, 131072)
    private fun digest(data: ByteArray) = MessageDigest.getInstance("SHA-256").digest(data)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun bounded(input: InputStream, limit: Int): ByteArray {
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer)
            if (n == -1) break
            require(result.size() + n <= limit) { throw Failure(R.string.save_error_0) }
            result.write(buffer, 0, n)
        }
        return result.toByteArray()
    }

    fun encode(romHash: String, name: String, data: ByteArray): ByteArray {
        require(data.size in sizes) { throw Failure(R.string.save_error_1) }
        val metadata = JSONObject().put("format", "linkcore-save").put("version", 1)
            .put("romSha256", romHash).put("game", name).put("saveSha256", digest(data))
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(metadata.toString().toByteArray(Charsets.UTF_8)); zip.closeEntry()
            zip.putNextEntry(ZipEntry("game.sav"))
            zip.write(data); zip.closeEntry()
        }
        return bytes.toByteArray()
    }

    fun decode(input: InputStream, romHash: String): ByteArray {
        // Bound both the compressed document and each decompressed entry.
        val archive = bounded(input, 512 * 1024)
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(entry.name in setOf("manifest.json", "game.sav") && !entries.containsKey(entry.name)) {
                    throw Failure(R.string.save_error_2)
                }
                entries[entry.name] = bounded(zip, if (entry.name == "manifest.json") 4096 else 131072)
                zip.closeEntry()
            }
        }
        val metadata = JSONObject(String(entries["manifest.json"] ?: throw Failure(R.string.save_error_3), Charsets.UTF_8))
        require(metadata.optString("format") == "linkcore-save" && metadata.optInt("version") == 1) { throw Failure(R.string.save_error_4) }
        require(metadata.optString("romSha256") == romHash) { throw Failure(R.string.save_error_5) }
        val data = entries["game.sav"] ?: throw Failure(R.string.save_error_6)
        require(data.size in sizes && digest(data) == metadata.optString("saveSha256")) { throw Failure(R.string.save_error_7) }
        return data
    }

    /** Raw battery saves have no ROM identity; the user selects the matching game. */
    fun decodeImport(input: InputStream, romHash: String, documentName: String): ByteArray {
        if (!documentName.endsWith(".sav", ignoreCase = true)) return decode(input, romHash)
        val data = bounded(input, sizes.max())
        require(data.size in sizes) { throw Failure(R.string.save_error_1) }
        return data
    }

    fun replace(file: File, data: ByteArray) {
        require(data.size in sizes) { throw Failure(R.string.save_error_8) }
        file.parentFile?.mkdirs()
        if (file.exists()) {
            file.copyTo(File(file.parentFile, file.name + ".before-import-" + System.currentTimeMillis()), false)
        }
        val atomic = AtomicFile(file)
        val output = atomic.startWrite()
        try { output.write(data); atomic.finishWrite(output) }
        catch (error: Throwable) { atomic.failWrite(output); throw error }
    }
}
