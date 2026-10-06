package com.linkcore.emulator

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.DocumentsContract
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridView
import java.io.File

object LibrarySmoke {
    fun run(i:Instrumentation) {
        val ctx=i.targetContext
        val index=File(ctx.filesDir,"game-library.json")
        val originalIndex=if(index.exists()) index.readBytes() else null
        val prefs=ctx.getSharedPreferences("library",0);val old=prefs.all.toMap()
        val store=LibraryStore(ctx)
        val tree=DocumentsContract.buildTreeDocumentUri("com.linkcore.emulator.test.library","root")
        var activity:Activity?=null
        var importedId:String?=null
        val existing=File(ctx.filesDir,"roms").listFiles()?.map {it.nameWithoutExtension}?.toSet() ?: emptySet()
        fun main(block:()->Unit) {var failure:Throwable?=null;i.runOnMainSync {try {block()}catch(t:Throwable){failure=t}};failure?.let {throw it}}
        fun field(name:String)=MainActivity::class.java.getDeclaredField(name).apply {isAccessible=true}
        fun waitReady() {repeat(100){if(field("scanning").getBoolean(activity).not()) return;SystemClock.sleep(100)};error("Library scan timed out")}
        fun find(v:View,match:(View)->Boolean):View? {if(match(v)) return v;if(v is ViewGroup) for(n in 0 until v.childCount) find(v.getChildAt(n),match)?.let {return it};return null}
        fun shot(name:String) {
            SystemClock.sleep(700);val image=checkNotNull(i.uiAutomation.takeScreenshot());val dir=File(ctx.getExternalFilesDir(null),"library-check").apply {mkdirs()}
            File(dir,"$name.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
        }
        try {
            val rpg=store.scan(DocumentsContract.buildTreeDocumentUri("com.linkcore.emulator.test.library","rpg"))
            check(rpg.size==1 && rpg.single().system=="RPG Maker XP" && rpg.single().title=="RPG fixture") {"RPG folder detection failed"}
            val found=store.scan(tree)
            check(found.size==6 && found.count {it.cover.isNotEmpty()}==2) {"Nested folder or cover matching failed"}
            check(found.first {it.name=="Nebula.gba"}.cover.isNotEmpty()) {"Case-insensitive cover matching failed"}
            val isolated=object:android.content.ContextWrapper(ctx) {
                override fun getFilesDir()=File(ctx.cacheDir,"dedup-check").apply {mkdirs()}
            }
            val isolatedIndex=File(isolated.filesDir,"game-library.json")
            isolatedIndex.delete()
            try {
                val testStore=LibraryStore(isolated)
                val original=found.first()
                testStore.remember("", "Previously imported", original.cached)
                val oldKey=testStore.read().single().key
                testStore.setCover(oldKey,"file:///custom-cover.png")
                testStore.replaceFolder(found+original.copy(key="renamed-copy",name="Renamed.gba"))
                check(testStore.read().size==6) {"Identical ROM duplicated"}
                check(testStore.read().single {it.cached==original.cached}.cover=="file:///custom-cover.png") {"Cover lost during merge"}
                testStore.remember(original.uri,original.name,original.cached)
                testStore.mergeImported()
                check(testStore.read().size==6) {"Duplicate returned after import"}
                check(testStore.read().map {it.cached}.distinct().size==6) {"Different ROMs merged"}
            } finally {isolatedIndex.delete()}
            store.replaceFolder(found)
            check(LibraryStore(ctx).read().count {it.uri.startsWith("content://com.linkcore.emulator.test.library")}==6)
            // Simulate an added game: the saved index does not contain the last folder entry.
            store.replaceFolder(found.dropLast(1))
            prefs.edit().remove("treeUris").putString("treeUri",tree.toString()).commit()
            activity=i.startActivitySync(Intent(ctx,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            main {activity!!.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE};SystemClock.sleep(700);waitReady();check(store.read().count {it.uri.startsWith("content://com.linkcore.emulator.test.library")}==6) {"Startup did not discover new game"};shot("library-landscape")
            main {activity!!.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT};SystemClock.sleep(700);shot("library-portrait")
            main {
                val search=find(activity!!.window.decorView){it is EditText} as EditText
                search.setText("Nebula")
                val grid=find(activity!!.window.decorView){it is GridView} as GridView
                check(grid.height>100 && grid.getGlobalVisibleRect(android.graphics.Rect())) {"Game collection is outside the visible screen"}
                check(grid.adapter.count==1) {"Search failed"}
                search.setText("")
            }
            val game=found.first {it.name=="Aurora.gba"}
            main {MainActivity::class.java.getDeclaredMethod("launchLibraryGame",LibraryGame::class.java).apply {isAccessible=true}.invoke(activity,game)}
            repeat(100){if(field("running").getBoolean(activity)) return@repeat;SystemClock.sleep(50)}
            main {check(field("loaded").getBoolean(activity) && field("running").getBoolean(activity)) {"Library game did not start"}}
            SystemClock.sleep(600)
            main {MainActivity::class.java.getDeclaredMethod("goHome").apply {isAccessible=true}.invoke(activity)}
            SystemClock.sleep(700);waitReady()
            importedId=prefs.getString("lastId",null)
            check(!File(ctx.filesDir,"covers/game-$importedId.png").exists()) {"Automatic gameplay preview still generated"}
            main {
                field("coverTarget").set(activity,game.key)
                MainActivity::class.java.getDeclaredMethod("onActivityResult",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,Intent::class.java).apply {isAccessible=true}.invoke(activity,45,Activity.RESULT_OK,Intent().setData(android.net.Uri.parse(game.cover)))
            }
            SystemClock.sleep(1000);waitReady()
            check(store.read().first {it.key==game.key}.cover.startsWith("file:")) {"Custom cover not stored"}
            main {activity!!.finish()};i.waitForIdleSync();activity=null
        } finally {
            activity?.let {a->main {a.finish()};i.waitForIdleSync()}
            if(originalIndex==null) index.delete() else index.writeBytes(originalIndex)
            if(importedId!=null && importedId !in existing) {
                File(ctx.filesDir,"roms/$importedId.gba").delete();File(ctx.filesDir,"saves/$importedId.sav").delete();File(ctx.filesDir,"covers/game-$importedId.png").delete()
            }
            prefs.edit().clear().apply {for((key,value) in old) when(value){is String->putString(key,value);is Boolean->putBoolean(key,value);is Int->putInt(key,value);is Long->putLong(key,value);is Float->putFloat(key,value);is Set<*>->putStringSet(key,value.filterIsInstance<String>().toSet())}}.commit()
        }
    }
}


