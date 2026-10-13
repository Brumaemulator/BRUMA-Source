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
object PokemonSmoke {
    fun run(i:Instrumentation) {
        val context=i.targetContext
        val prefs=context.getSharedPreferences("library",0)
        val old=prefs.all.toMap()
        val original=File(context.filesDir,"roms").listFiles()!!.first { file ->
            file.inputStream().use { it.skip(160);val title=ByteArray(12);it.read(title);String(title).startsWith("POKEMON") }
        }
        val originalSave=File(context.filesDir,"saves/${original.nameWithoutExtension}.sav")
        val before=if(originalSave.exists()) originalSave.readBytes() else null
        val copy=File(context.cacheDir,"pokemon-performance.gba")
        val save=File(context.filesDir,"saves/pokemon-performance.sav")
        original.copyTo(copy,true)
        if(before!=null) save.writeBytes(before) else save.delete()
        val output=File(context.getExternalFilesDir(null),"pokemon-check").apply {mkdirs()}
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
            repeat(10) {press(if(it%2==0) 8 else 1)}
            SystemClock.sleep(4000);press(1);SystemClock.sleep(2500);press(1);SystemClock.sleep(2500);press(2)
            repeat(4) {press(16)} 
            moveFor(12000);shot("landscape-original")
            main {call("pauseGame");(field("surface").get(activity) as GameSurface).fillScreen=true;call("enterGame")}
            moveFor(15000);shot("landscape-full")
            for(speed in listOf(2,4)) for(sound in listOf(false,true)) {
                main {
                    call("pauseGame");field("fastForward").setBoolean(activity,true)
                    field("fastMultiplier").setInt(activity,speed);field("fastSound").setBoolean(activity,sound);call("enterGame")
                }
                SystemClock.sleep(12000);shot("fast-${speed}-sound-${sound}")
            }
            main {call("pauseGame");field("fastForward").setBoolean(activity,false);activity!!.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;call("enterGame")}
            SystemClock.sleep(8000);shot("portrait")
            main {check(field("running").getBoolean(activity));activity!!.finish()};i.waitForIdleSync();activity=null
            check(if(before==null) !originalSave.exists() else originalSave.readBytes().contentEquals(before)) {"Original save changed"}
        } finally {
            activity?.let {a->main {a.finish()};i.waitForIdleSync()}
            NativeCore.close();copy.delete();save.delete()
            prefs.edit().clear().apply {
                for((key,value) in old) when(value) {is String->putString(key,value);is Boolean->putBoolean(key,value);is Int->putInt(key,value);is Long->putLong(key,value);is Float->putFloat(key,value)}
            }.commit()
        }
    }
}

