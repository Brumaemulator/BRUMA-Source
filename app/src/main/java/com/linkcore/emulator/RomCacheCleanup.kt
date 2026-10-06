// SPDX-License-Identifier: GPL-3.0-or-later
package com.linkcore.emulator

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract as Docs
import org.json.JSONArray
import java.io.File

/** Provenance and tombstones survive index deduplication and delayed cache deletion. */
object RomCacheCleanup {
    private fun prefs(c:Context)=c.getSharedPreferences("rom-source-cleanup",0)
    private fun external(c:Context,text:String):Boolean {
        val uri=Uri.parse(text.substringBefore('#'))
        if(uri.scheme=="content")return true
        if(uri.scheme!="file" || uri.path==null)return false
        return !File(uri.path!!).canonicalPath.startsWith(c.filesDir.canonicalPath+File.separator)
    }
    fun source(c:Context,g:LibraryGame,trees:List<Uri> = emptyList()):String {
        if(g.source.isNotBlank() && external(c,g.source))return g.source
        if(g.uri.isNotBlank() && external(c,g.uri))return g.uri
        // Old releases erased URI on refresh, but kept SHA-256(URI) as the entry key.
        // Recover only an exact match; never guess ownership by a game title/hash alone.
        c.contentResolver.persistedUriPermissions.forEach {permission->
            val uri=permission.uri
            if(LibraryStore.key(uri.toString())==g.key)return uri.toString()
        }
        for(tree in trees) {
            val candidate=runCatching {Docs.buildDocumentUriUsingTree(tree,Docs.getTreeDocumentId(tree)+"/"+g.name).toString()}.getOrNull() ?: continue
            if(LibraryStore.key(candidate)==g.key)return candidate
        }
        return origins(c,g.cached).firstOrNull{external(c,it)} ?: ""
    }
    fun origins(c:Context,id:String):List<String> = runCatching {
        val a=JSONArray(prefs(c).getString(id,"[]"));(0 until a.length()).map{a.getString(it)}
    }.getOrDefault(emptyList())
    fun belongsToScan(source:String,trees:List<Uri>):Boolean {
        if(source.isBlank())return false
        val uri=Uri.parse(source.substringBefore('#'))
        return trees.any {tree->runCatching {
            if(uri.authority!=tree.authority || !Docs.isTreeUri(uri))false else {
                val root=Docs.getTreeDocumentId(tree)
                Docs.getTreeDocumentId(uri)==root
            }
        }.getOrDefault(false)}
    }
    fun record(c:Context,g:LibraryGame) {
        if(g.system=="RPG Maker XP" || !g.cached.matches(Regex("[a-f0-9]{64}")))return
        val source=source(c,g)
        if(source.isBlank())return
        val p=prefs(c)
        val sources=runCatching {JSONArray(p.getString(g.cached,"[]"))}.getOrDefault(JSONArray())
        if((0 until sources.length()).none{sources.getString(it)==source})sources.put(source)
        p.edit().putString(g.cached,sources.toString()).remove("deleted:${g.cached}").apply()
    }
    fun deleted(c:Context,id:String)=prefs(c).getBoolean("deleted:$id",false)

    fun clean(c:Context,removed:List<LibraryGame>,retained:List<LibraryGame>,protected:Set<String>) {
        val p=prefs(c)
        val ids=removed.filter{it.system!="RPG Maker XP"}.map{it.cached}.filter{it.matches(Regex("[a-f0-9]{64}"))}.toMutableSet()
        // Retry files retained while an emulator was using them on a previous scan.
        ids+=p.all.keys.filter{it.startsWith("deleted:") && p.getBoolean(it,false)}.map{it.removePrefix("deleted:")}
        val root=File(c.filesDir,"roms").canonicalFile
        val processes=(c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses
        val ndsRunning=processes==null || processes.any{it.processName=="${c.packageName}:nds"}
        for(id in ids) {
            if(retained.any{it.cached==id})continue
            // Keep this marker until the source is rediscovered, so mergeImported
            // cannot resurrect the private copy even when deletion must be deferred.
            p.edit().putBoolean("deleted:$id",true).apply()
            if(id in protected)continue
            for(ext in listOf("gb","gbc","gba","nds")) {
                if(ext=="nds" && ndsRunning)continue
                val file=File(root,"$id.$ext")
                check(file.canonicalFile.parentFile==root && !java.nio.file.Files.isSymbolicLink(file.toPath()))
                if(file.exists() && !file.delete())android.util.Log.w("BrumaCleanup","ROM cache busy: $id.$ext")
            }
        }
    }
}
