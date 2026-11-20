// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from BRUMA 1.0.3: official repository fallback for GB/GBC/GBA/NDS (2026-11-20).
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
    private val unavailableLegacy=java.util.Collections.synchronizedSet(mutableSetOf<String>())
    @Volatile private var connection:HttpURLConnection?=null
    fun begin() {cancelled.set(false)}
    fun cancel() {cancelled.set(true);connection?.disconnect()}
    private fun checkCancelled() {if(cancelled.get() || Thread.currentThread().isInterrupted) throw CoverFailure(R.string.art_cancelled)}
    companion object {
        const val BASE="https://thumbnails.libretro.com/Nintendo%20-%20Game%20Boy%20Advance/Named_Boxarts/"
        fun base(system:String)=when(system){"GB"->"https://thumbnails.libretro.com/Nintendo%20-%20Game%20Boy/Named_Boxarts/";"GBC"->"https://thumbnails.libretro.com/Nintendo%20-%20Game%20Boy%20Color/Named_Boxarts/";"NDS"->"https://thumbnails.libretro.com/Nintendo%20-%20Nintendo%20DS/Named_Boxarts/";else->BASE}
        fun repository(system:String):String=when(system){"GB"->"Nintendo_-_Game_Boy";"GBC"->"Nintendo_-_Game_Boy_Color";"GBA"->"Nintendo_-_Game_Boy_Advance";"NDS"->"Nintendo_-_Nintendo_DS";else->throw IllegalArgumentException("Unsupported system")}
        private fun api(system:String)="https://api.github.com/repos/libretro-thumbnails/"+repository(system)+"/git/trees/"
        private fun mirror(system:String)="https://raw.githubusercontent.com/libretro-thumbnails/"+repository(system)+"/master/Named_Boxarts/"
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
        if(uri.scheme!="https" || listOf("GB","GBC","GBA","NDS").none {url.startsWith(base(it))||url.startsWith(api(it))||url.startsWith(mirror(it))}) throw CoverFailure(R.string.art_invalid)
        try {
            val conn=java.net.URL(url).openConnection() as HttpURLConnection
            connection=conn;conn.instanceFollowRedirects=false;conn.connectTimeout=15000;conn.readTimeout=20000
            conn.setRequestProperty("User-Agent","Bruma/${BuildConfig.VERSION_NAME}")
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
        val parsed=try {parseIndex(String(download(base(system),4*1024*1024),Charsets.UTF_8)).also{if(it.isEmpty())throw CoverFailure(R.string.art_invalid)}}catch(e:CoverFailure){
            checkCancelled();unavailableLegacy.add(system)
            // Use two non-recursive Git trees: no GitHub 1,000-item directory limit and no key.
            val root=org.json.JSONObject(String(download(api(system)+"master",1024*1024),Charsets.UTF_8))
            val entries=root.getJSONArray("tree");var sha:String?=null
            for(i in 0 until entries.length()){val entry=entries.getJSONObject(i);if(entry.getString("path")=="Named_Boxarts"&&entry.getString("type")=="tree")sha=entry.getString("sha")}
            require(sha?.matches(Regex("[a-f0-9]{40}"))==true)
            val dir=org.json.JSONObject(String(download(api(system)+sha,8*1024*1024),Charsets.UTF_8));require(!dir.optBoolean("truncated",false))
            val files=dir.getJSONArray("tree");(0 until files.length()).map{files.getJSONObject(it)}.filter{it.getString("type")=="blob"}.map{it.getString("path")}.filter{it.endsWith(".png",true)&&!it.contains('/')&&!it.contains('\\')&&it.length<250}.distinct().sorted()
        }
        if(parsed.isEmpty())throw CoverFailure(R.string.art_invalid)
        runCatching {cache.writeText(parsed.joinToString("\n"){"<a href=\""+Uri.encode(it)+"\">"})}
        return parsed
    }
    fun fetchCover(filename:String,system:String="GBA"):ByteArray {
        require(filename.endsWith(".png") && !filename.contains('/') && !filename.contains('\\'))
        return if(unavailableLegacy.contains(system))download(mirror(system)+Uri.encode(filename),8*1024*1024)else try{download(base(system)+Uri.encode(filename),8*1024*1024)}catch(e:CoverFailure){checkCancelled();unavailableLegacy.add(system);download(mirror(system)+Uri.encode(filename),8*1024*1024)}
    }
}

