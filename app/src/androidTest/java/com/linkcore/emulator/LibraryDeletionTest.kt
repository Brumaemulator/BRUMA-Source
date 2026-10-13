package com.linkcore.emulator

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract as Docs
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Only synthetic inputs and an isolated filesDir/prefs namespace; no user library writes. */
object LibraryDeletionTest {

    fun run(i:Instrumentation) {
        val targetContext=i.targetContext
        val report=Bundle()
        val base=File(targetContext.cacheDir,"deletion-test-${System.nanoTime()}").apply{mkdirs()}
        val namespace=base.name
        val c=object:ContextWrapper(targetContext) {
            override fun getFilesDir()=File(base,"private").apply{mkdirs()}
            override fun getSharedPreferences(name:String,mode:Int)=super.getSharedPreferences("$namespace-$name",mode)
        }
        try {
            val store=LibraryStore(c)
            fun fixture(name:String,tag:Int)=File(base,name).apply{writeBytes(ByteArray(1024){(it+tag).toByte()})}
            fun imported(file:File):LibraryGame {
                val uri=Uri.fromFile(file)
                val (rom,name)=RomFiles.import(c,uri)
                store.remember(uri.toString(),name,rom.nameWithoutExtension,RomFiles.system(name))
                return store.read().first{it.cached==rom.nameWithoutExtension}
            }
            for((n,ext) in listOf("gb","gbc","gba","nds").withIndex()) {
                val file=fixture("fixture.$ext",n)
                val g=imported(file)
                val save=File(c.filesDir,"saves/${g.cached}.sav").apply{parentFile!!.mkdirs();writeText("save-$ext")}
                store.replaceFolder(emptyList())
                check(store.read().any{it.cached==g.cached}){"Existing $ext import lost"}
                check(file.delete())
                store.replaceFolder(emptyList())
                check(RomFiles.cached(c,g.cached)==null){"$ext private ROM not deleted"}
                store.mergeImported()
                check(store.allGames().none{it.cached==g.cached}){"$ext resurrected"}
                check(save.readText()=="save-$ext"){"Save data removed"}
            }
            val zip=File(base,"roms.zip")
            ZipOutputStream(zip.outputStream()).use{out->
                for((n,ext) in listOf("gb","gbc","gba","nds").withIndex()){
                    out.putNextEntry(ZipEntry("nested/game.$ext"));out.write(ByteArray(1024){(it+40+n).toByte()});out.closeEntry()
                }
            }
            val games=GameArchives.import(c,Uri.fromFile(zip),zip.length(),zip.lastModified())
            check(games.size==4 && games.all{it.source.contains("#rom=")})
            store.add(games)
            val reused=GameArchives.import(c,Uri.fromFile(zip),zip.length(),zip.lastModified())
            check(reused.map{it.source}==games.map{it.source}){"Archive index lost origin"}
            check(zip.delete())
            store.replaceFolder(emptyList());store.mergeImported()
            check(games.all{g->RomFiles.cached(c,g.cached)==null && store.allGames().none{it.cached==g.cached}}){"ZIP copies remained"}

            val first=fixture("duplicate.gba",60)
            val second=File(base,"duplicate-again.gba").apply{writeBytes(first.readBytes())}
            val g=imported(first);imported(second)
            check(first.delete());store.replaceFolder(emptyList())
            check(RomFiles.cached(c,g.cached)!=null && store.read().any{it.cached==g.cached}){"Valid duplicate was deleted"}
            check(second.delete());store.replaceFolder(emptyList());store.mergeImported()
            check(RomFiles.cached(c,g.cached)==null && store.read().none{it.cached==g.cached}){"Last duplicate not removed"}

            val active=fixture("paused.gba",70);val paused=imported(active);check(active.delete())
            store.replaceFolder(emptyList(),protected=setOf(paused.cached));store.mergeImported()
            check(store.read().none{it.cached==paused.cached} && RomFiles.cached(c,paused.cached)!=null){"In-use ROM protection failed"}
            store.replaceFolder(emptyList());check(RomFiles.cached(c,paused.cached)==null){"Deferred deletion failed"}

            val unknown=LibraryGame(LibraryStore.key("unavailable"),"offline.gba","content://unavailable.test/tree/root/document/root%2Foffline.gba",cached="a".repeat(64))
            File(c.filesDir,"roms/${unknown.cached}.gba").writeBytes(ByteArray(1024))
            store.add(listOf(unknown));store.replaceFolder(emptyList())
            check(store.read().any{it.cached==unknown.cached}){"Unavailable folder treated as deletion"}

            val tree=Docs.buildTreeDocumentUri("com.android.externalstorage.documents","primary:fixture")
            val uri=Docs.buildDocumentUriUsingTree(tree,"primary:fixture/legacy.gba").toString()
            val legacy=LibraryGame(LibraryStore.key(uri),"legacy.gba",cached="b".repeat(64))
            check(RomCacheCleanup.source(c,legacy,listOf(tree))==uri){"Old erased URI recovery failed"}
            check(RomCacheCleanup.belongsToScan(uri,listOf(tree)))
            check(!RomCacheCleanup.belongsToScan(uri,listOf(Docs.buildTreeDocumentUri("com.android.externalstorage.documents","primary:other"))))
            val rpg=File(c.filesDir,"rpg-games/fixture").apply{mkdirs()}
            File(rpg,"Game.ini").writeText("[Game]\nTitle=Fixture\n")
            File(rpg,"Data").mkdirs();File(rpg,"Data/Scripts.rxdata").writeBytes(ByteArray(32))
            val source=fixture("rpg.zip",80)
            File(rpg,"bruma-origin.txt").writeText(Uri.fromFile(source).toString())
            check(store.allGames().any{it.cached==rpg.name})
            check(source.delete());check(store.allGames().none{it.cached==rpg.name}){"Classic/modern RPG removal failed"}
            check(RpgIdentity.confirmedMissing(c,Uri.fromFile(source).toString()+"#rpg=game"))
            check(RpgCacheCleanup.clean(c)>0 && !rpg.exists()){"RPG extracted game copy remained"}
            // A real native GBA session remains open while its external source
            // is deleted. The monitor must close it before deleting the cache.
            val runningSource=File(base,"running.gba")
            i.context.assets.open("smoke.gba").use{input->runningSource.outputStream().use{input.copyTo(it)}}
            val runningGame=imported(runningSource)
            val runningRom=checkNotNull(RomFiles.cached(c,runningGame.cached))
            val nativeSave=File(c.filesDir,"saves/${runningGame.cached}.sav")
            check(NativeCore.load(runningRom.path,nativeSave.path)==null)
            val bitmap=android.graphics.Bitmap.createBitmap(240,160,android.graphics.Bitmap.Config.ARGB_8888)
            NativeCore.frame(0,bitmap,ShortArray(4096));bitmap.recycle()
            val open=java.util.concurrent.atomic.AtomicBoolean(true)
            val closed=java.util.concurrent.CountDownLatch(1)
            var callbackFailure:Throwable?=null
            val monitor=ActiveGameSourceMonitor(c,java.util.function.Supplier{runningRom.path},java.util.function.BooleanSupplier{open.get()},Runnable {
                try {
                    NativeCore.save();NativeCore.close();open.set(false)
                    store.replaceFolder(emptyList());store.mergeImported()
                } catch(t:Throwable){callbackFailure=t} finally{closed.countDown()}
            })
            try {
                check(!ActiveGameSourceMonitor.confirmedMissing(c,runningRom.path))
                i.runOnMainSync{monitor.start()}
                Thread.sleep(200);check(closed.count==1L){"Existing running game closed"}
                check(runningSource.delete())
                check(closed.await(8,java.util.concurrent.TimeUnit.SECONDS)){"Deleted running game not closed"}
                callbackFailure?.let{throw it}
                check(!runningRom.exists() && store.read().none{it.cached==runningGame.cached}){"Running game resurrected"}
                check(nativeSave.isFile){"Running game save lost"}
            } finally {
                i.runOnMainSync{monitor.dispose()};NativeCore.close()
            }
            // Exercise the actual Android storage provider with a uniquely named
            // synthetic folder. It contains only bytes created by this test.
            val granted=targetContext.getSharedPreferences("library",0).getString("treeUri",null)
            if(granted!=null) {
                val tree=Uri.parse(granted)
                val root=Docs.buildDocumentUriUsingTree(tree,Docs.getTreeDocumentId(tree))
                val parent=checkNotNull(Docs.createDocument(c.contentResolver,root,Docs.Document.MIME_TYPE_DIR,"BrumaDeletionTest-${System.nanoTime()}"))
                try {
                    val child=checkNotNull(Docs.createDocument(c.contentResolver,parent,Docs.Document.MIME_TYPE_DIR,"nested"))
                    val leaf=checkNotNull(Docs.createDocument(c.contentResolver,child,"application/octet-stream","fixture.gba"))
                    c.contentResolver.openOutputStream(leaf)!!.use{it.write(ByteArray(1024){(it+90).toByte()})}
                    val (rom,name)=RomFiles.import(c,leaf)
                    store.remember(leaf.toString(),name,rom.nameWithoutExtension)
                    val direct=Docs.buildDocumentUri(leaf.authority!!,Docs.getDocumentId(leaf))
                    // Reproduce Donkey Kong: direct document origin plus tree origin.
                    store.remember(direct.toString(),name,rom.nameWithoutExtension)
                    check(!RpgIdentity.confirmedMissing(c,leaf.toString()))
                    check(!RpgIdentity.confirmedMissing(c,direct.toString()+"#rom=fixture.gba"))
                    check(Docs.deleteDocument(c.contentResolver,parent))
                    check(RpgIdentity.confirmedMissing(c,direct.toString()+"#rom=fixture.gba")){"Deleted direct document/ZIP origin not detected"}
                    check(ActiveGameSourceMonitor.confirmedMissing(c,rom.path)){"Mixed direct/tree origins kept deleted game alive"}
                    check(RpgIdentity.confirmedMissing(c,leaf.toString())){"Deleted ancestor folder not detected"}
                    store.replaceFolder(emptyList(),listOf(tree));store.mergeImported()
                    check(!rom.exists() && store.read().none{it.cached==rom.nameWithoutExtension}){"SAF folder deletion retained ROM"}
                } finally {
                    runCatching{Docs.deleteDocument(c.contentResolver,parent)}
                }
            }
            report.putString("stream","PASS: GB/GBC/GBA/NDS deleted source + private ROM cleanup, ZIP origin roundtrip/removal, duplicate source retention, unavailable provider protection, paused ROM deferral, legacy URI recovery, RPG shared source removal, real Android SAF ancestor-folder deletion, active native GBA source deletion/closes/cache removed, direct-document + tree origins after ancestor deletion; saves preserved.\n")
            i.finish(Activity.RESULT_OK,report)
        } catch(t:Throwable) {
            report.putString("stream",t.stackTraceToString());i.finish(Activity.RESULT_CANCELED,report)
        } finally {
            check(base.canonicalFile.parentFile==targetContext.cacheDir.canonicalFile)
            base.deleteRecursively()
            for(name in listOf("rom-source-cleanup","rpg-content-identity","rom-scan-signatures"))targetContext.deleteSharedPreferences("$namespace-$name")
        }
    }
}
