// SPDX-License-Identifier: GPL-3.0-or-later
package com.linkcore.emulator

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.Executors
import java.util.function.BooleanSupplier
import java.util.function.Supplier

/** Checks only the open game's origins, never scans/hashes the library during play. */
class ActiveGameSourceMonitor(private val context:Context,private val path:Supplier<String>,
    private val ready:BooleanSupplier,private val removed:Runnable) {
    private val main=Handler(Looper.getMainLooper())
    private val worker=Executors.newSingleThreadExecutor {task->Thread(task,"Bruma-source-check").apply{isDaemon=true;priority=Thread.MIN_PRIORITY}}
    private var watching=false
    private var checking=false
    private var disposed=false
    private val tick=object:Runnable {override fun run(){
        if(!watching || disposed)return
        if(!checking && ready.asBoolean) {
            val current=path.get()
            if(current.isNotBlank()) {
                checking=true
                worker.execute {
                    val missing=runCatching{confirmedMissing(context,current)}.getOrDefault(false)
                    main.post {
                        checking=false
                        if(watching && !disposed && missing && ready.asBoolean && path.get()==current) {
                            stop();removed.run()
                        }
                    }
                }
            }
        }
        if(watching)main.postDelayed(this,3000)
    }}
    fun start(){if(disposed)return;watching=true;main.removeCallbacks(tick);main.post(tick)}
    fun stop(){watching=false;main.removeCallbacks(tick)}
    fun dispose(){stop();disposed=true;worker.shutdown()}

    companion object {
        fun confirmedMissing(c:Context,path:String):Boolean {
            val file=File(path).canonicalFile
            val romRoot=File(c.filesDir,"roms").canonicalFile
            val rpgRoot=File(c.filesDir,"rpg-games").canonicalFile
            val rpg=file.parentFile==rpgRoot
            if(file.parentFile!=romRoot && !rpg)return false
            val entries=LibraryStore(c).read().filter{if(rpg)it.cached==file.name || Uri.parse(it.uri).path==file.path else it.cached==file.nameWithoutExtension}
            val sources=linkedSetOf<String>()
            if(rpg) {
                entries.mapTo(sources){it.source}
                File(file,"bruma-origin.txt").takeIf{it.isFile}?.readText()?.trim()?.let{sources.add(it)}
            } else {
                val prefs=c.getSharedPreferences("library",0)
                val trees=prefs.getStringSet("treeUris",emptySet()).orEmpty().toMutableSet()
                prefs.getString("treeUri",null)?.let{trees.add(it)}
                entries.mapTo(sources){RomCacheCleanup.source(c,it,trees.map{tree->Uri.parse(tree)})}
                sources+=RomCacheCleanup.origins(c,file.nameWithoutExtension)
            }
            sources.remove("")
            // Unknown provenance or inaccessible providers are not proof of deletion.
            return sources.isNotEmpty() && sources.all{RpgIdentity.confirmedMissing(c,it)}
        }
    }
}
