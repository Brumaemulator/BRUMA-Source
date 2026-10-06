package com.linkcore.emulator

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import java.io.File
import java.security.MessageDigest

/** Uses the user's local ROM and a separate copy of its battery save. */
object CrystalPacingCheck {
    fun run(i:Instrumentation) {
        val context=i.targetContext
        val prefs=context.getSharedPreferences("library",0)
        val old=prefs.all.toMap()
        val game=LibraryStore(context).allGames().first{it.system=="GBC" && it.name.contains("Cristal",true)}
        val original=checkNotNull(RomFiles.cached(context,game.cached))
        val originalSave=File(context.filesDir,"saves/${original.nameWithoutExtension}.sav")
        val before=if(originalSave.exists()) originalSave.readBytes() else null
        val copy=File(context.cacheDir,"crystal-performance.gbc")
        val save=File(context.filesDir,"saves/crystal-performance.sav")
        original.copyTo(copy,true)
        if(before!=null) save.writeBytes(before) else save.delete()
        val output=File(context.getExternalFilesDir(null),"crystal-pacing").apply {mkdirs()}
        var activity:Activity?=null
        fun main(block:()->Unit) {var error:Throwable?=null;i.runOnMainSync {try {block()} catch(t:Throwable){error=t}};error?.let {throw it}}
        fun field(name:String)=MainActivity::class.java.getDeclaredField(name).apply {isAccessible=true}
        fun call(name:String) {MainActivity::class.java.getDeclaredMethod(name).apply {isAccessible=true}.invoke(activity)}
        fun press(mask:Int) {
            val controls=field("controls").get(activity) as ControlsView
            val keys=ControlsView::class.java.getDeclaredField("keys").apply {isAccessible=true}
            main {keys.setInt(controls,mask)};SystemClock.sleep(100);main {controls.clearKeys()};SystemClock.sleep(250)
        }
        fun moveFor(milliseconds:Int) {
            val controls=field("controls").get(activity) as ControlsView
            val keys=ControlsView::class.java.getDeclaredField("keys").apply {isAccessible=true}
            try {
                repeat(milliseconds/800) {
                    main {keys.setInt(controls,16)};SystemClock.sleep(350)
                    main {controls.clearKeys()};SystemClock.sleep(50)
                    main {keys.setInt(controls,32)};SystemClock.sleep(350)
                    main {controls.clearKeys()};SystemClock.sleep(50)
                }
            } finally {main {controls.clearKeys()}}
        }
        fun shot(name:String) {
            val bitmap=checkNotNull(i.uiAutomation.takeScreenshot())
            File(output,"$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }
        try {
            activity=i.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            main {
                activity!!.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                field("rom").set(activity,copy);field("romName").set(activity,"Pokémon · prueba local")
                (field("surface").get(activity) as GameSurface).fillScreen=false
                call("enterGame")
            }
            SystemClock.sleep(4000)
            press(8);SystemClock.sleep(2500);shot("menu");press(1);SystemClock.sleep(1000);press(1);SystemClock.sleep(1000);press(1);SystemClock.sleep(3000);shot("loaded")
            moveFor(16000);shot("landscape-original")
            main {(field("surface").get(activity) as GameSurface).fillScreen=true}
            moveFor(16000);shot("landscape-full")
            main {activity!!.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
            moveFor(6000);shot("portrait")
            for(multiplier in listOf(2,4)) {
                main {field("fastMultiplier").setInt(activity,multiplier);field("fastForward").setBoolean(activity,true)}
                moveFor(6000)
            }
            main {field("fastForward").setBoolean(activity,false)}
            moveFor(6000);shot("normal-after-fast")
            main {check(field("running").getBoolean(activity));activity!!.finish()};i.waitForIdleSync();activity=null
            check(if(before==null) !originalSave.exists() else originalSave.readBytes().contentEquals(before)) {"Original save changed"}
        } finally {
            activity?.let {a->main {a.finish()};i.waitForIdleSync()}
            NativeCore.close();copy.delete();save.delete()
            prefs.edit().clear().apply {
                for((key,value) in old) when(value) {is String->putString(key,value);is Boolean->putBoolean(key,value);is Int->putInt(key,value);is Long->putLong(key,value);is Float->putFloat(key,value);is Set<*>->putStringSet(key,value.filterIsInstance<String>().toSet())}
            }.commit()
        }
    }
}

