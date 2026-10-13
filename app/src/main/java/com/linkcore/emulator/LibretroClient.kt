package com.linkcore.emulator

import android.content.Context
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class CoverFailure(val messageId:Int):Exception()
class LibretroClient(private val context:Context) {
    private val cancelled=AtomicBoolean(false)
    @Volatile private var connection:HttpURLConnection?=null
    fun begin() {cancelled.set(false)}
    fun cancel() {cancelled.set(true);connection?.disconnect()}
    private fun checkCancelled() {if(cancelled.get() || Thread.currentThread().isInterrupted) throw CoverFailure(R.string.art_cancelled)}
    companion object {
        const val BASE="https://thumbnails.libretro.com/Nintendo%20-%20Game%20Boy%20Advance/Named_Boxarts/"
        fun base(system:String)=when(system){"GB"->"https://thumbnails.libretro.com/Nintendo%20-%20Game%20Boy/Named_Boxarts/";"GBC"->"https://thumbnails.libretro.com/Nintendo%20-%20Game%20Boy%20Color/Named_Boxarts/";"NDS"->"https://thumbnails.libretro.com/Nintendo%20-%20Nintendo%20DS/Named_Boxarts/";else->BASE}
        fun normalized(name:String):String=Normalizer.normalize(name.replace(Regex("(?i)\\.(gba|gbc|gb|nds|png)$"),"").replace(Regex("^\\s*\\d{3,5}\\s*[-_. ]+"),"").replace(Regex("\\([^)]*\\)|\\[[^]]*]"),""),Normalizer.Form.NFD).replace(Regex("\\p{M}"),"").lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"),"")
        fun parseIndex(html:String):List<String> = Regex("href=\"([^\"]+)\"",RegexOption.IGNORE_CASE).findAll(html).map {Uri.decode(it.groupValues[1])}.filter {it.endsWith(".png",true) && !it.contains('/') && !it.contains('\\') && it.length<250}.distinct().sorted().toList()
        fun match(name:String,catalog:List<String>,spanish:Boolean):String? {
            val exact=name.replace(Regex("(?i)\\.(gba|gbc|gb|nds)$"),"")+".png"
            catalog.firstOrNull {it.equals(exact,true)}?.let {return it}
            val key=normalized(name)
            if(key.isBlank()) return null
            return catalog.filter {normalized(it)==key}.minByOrNull {if(spanish) when {it.contains("(Spain)")->0;it.contains("(Europe)")->1;it.contains("(USA)")->2;else->3} else when {it.contains("(USA)")->0;it.contains("(Europe)")->1;else->2}}
        }
    }
    private fun download(url:String,limit:Int):ByteArray {
        checkCancelled()
        val uri=Uri.parse(url)
        if(uri.scheme!="https" || uri.host!="thumbnails.libretro.com" || listOf("GB","GBC","GBA","NDS").none {url.startsWith(base(it))}) throw CoverFailure(R.string.art_invalid)
        try {
            val conn=java.net.URL(url).openConnection() as HttpURLConnection
            connection=conn;conn.instanceFollowRedirects=false;conn.connectTimeout=15000;conn.readTimeout=20000
            conn.setRequestProperty("User-Agent","BrumaGBA/${BuildConfig.VERSION_NAME}")
            try {
                if(conn.responseCode==404) throw CoverFailure(R.string.art_not_found)
                if(conn.responseCode!=200 || conn.contentLengthLong>limit) throw CoverFailure(R.string.art_network)
                return conn.inputStream.use {input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                    while(true) {checkCancelled();val n=input.read(buffer);if(n<0) break;if(out.size()+n>limit) throw CoverFailure(R.string.art_invalid);out.write(buffer,0,n)}
                    out.toByteArray()}
            } finally {conn.disconnect();connection=null}
        } catch(e:CoverFailure){throw e} catch(e:Exception){checkCancelled();throw CoverFailure(R.string.art_network)}
    }
    fun catalog(system:String="GBA"):List<String> {
        checkCancelled()
        val cache=File(context.cacheDir,"libretro-${system.lowercase(Locale.ROOT)}-index.html")
        if(cache.exists() && System.currentTimeMillis()-cache.lastModified()<7*86400000L) {
            val parsed=parseIndex(cache.readText());if(parsed.isNotEmpty()) return parsed
        }
        val html=String(download(base(system),4*1024*1024),Charsets.UTF_8)
        val parsed=parseIndex(html);if(parsed.isEmpty()) throw CoverFailure(R.string.art_invalid)
        runCatching {cache.writeText(html)}
        return parsed
    }
    fun fetchCover(filename:String,system:String="GBA"):ByteArray {
        require(filename.endsWith(".png") && !filename.contains('/') && !filename.contains('\\'))
        return download(base(system)+Uri.encode(filename),8*1024*1024)
    }
}

