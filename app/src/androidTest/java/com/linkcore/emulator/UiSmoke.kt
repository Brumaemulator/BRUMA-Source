package com.linkcore.emulator

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.io.File

/** Exercises the app's own views and document callbacks without global input injection. */
object UiSmoke {
    fun run(i: Instrumentation) {
        val index=File(i.targetContext.filesDir,"game-library.json")
        val previousIndex=if(index.exists()) index.readBytes() else null
        val prefs=i.targetContext.getSharedPreferences("library",0)
        val previousId=prefs.getString("lastId",null)
        val previousFill=prefs.getBoolean("fillScreen",false)
        val previousName=prefs.getString("lastName",null)
        var activity: Activity?=null
        var monitor: Instrumentation.ActivityMonitor?=null
        val output=File(i.targetContext.getExternalFilesDir(null),"ui-check").apply { mkdirs() }
        fun main(block: ()->Unit) {
            var error: Throwable?=null
            i.runOnMainSync { try { block() } catch(t:Throwable) { error=t } }
            error?.let { throw it }
        }
        fun find(v: View,prefix: String): View? {
            if(v is TextView && v.isShown && v.text.toString().startsWith(prefix)) return v
            if(v is ViewGroup) for(n in 0 until v.childCount) find(v.getChildAt(n),prefix)?.let { return it }
            return null
        }
        fun root(): View=activity!!.window.decorView
        fun field(name: String): Any? = MainActivity::class.java.getDeclaredField(name).apply { isAccessible=true }.get(activity)
        fun awaitReady() {
            repeat(100) {
                var ready=false
                main { ready=field("loaded")==true && field("running")==true }
                if(ready) return
                SystemClock.sleep(100)
            }
            error("Game did not start")
        }
        fun click(prefix:String, dialog:Boolean=false) {
            main {
                val view=if(dialog) (field("menu") as android.app.Dialog).window!!.decorView else root()
                checkNotNull(find(view,prefix)) { "Missing button: $prefix" }.performClick()
            }
            i.waitForIdleSync()
        }
        fun shot(name:String) {
            SystemClock.sleep(500) // Allow window animations and SurfaceView composition to complete.
            i.waitForIdleSync()
            val bitmap=checkNotNull(i.uiAutomation.takeScreenshot())
            File(output,name+".png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
        }
        try {
            activity=i.startActivitySync(Intent(i.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            main { activity!!.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            SystemClock.sleep(500)
            shot("home")
            val fixture=File(i.targetContext.cacheDir,"core-smoke/smoke.gba")
            val result=Instrumentation.ActivityResult(Activity.RESULT_OK,Intent().setData(Uri.fromFile(fixture)))
            monitor=i.addMonitor(IntentFilter(Intent.ACTION_OPEN_DOCUMENT).apply { addDataType("*/*"); addCategory(Intent.CATEGORY_OPENABLE) },result,true)
            click("＋  Abrir")
            awaitReady()
            i.removeMonitor(monitor); monitor=null
            main {
                fun padEvent(code:Int,down:Boolean)=android.view.KeyEvent(0,SystemClock.uptimeMillis(),if(down) android.view.KeyEvent.ACTION_DOWN else android.view.KeyEvent.ACTION_UP,code,0,0,101,0,0,android.view.InputDevice.SOURCE_GAMEPAD)
                val pad=(field("gamepad\$delegate") as Lazy<*>).value as GamepadInput
                activity!!.dispatchKeyEvent(padEvent(android.view.KeyEvent.KEYCODE_BUTTON_A,true));check(pad.keys==1)
                activity!!.dispatchKeyEvent(padEvent(android.view.KeyEvent.KEYCODE_BUTTON_A,false));check(pad.keys==0)
                activity!!.dispatchKeyEvent(padEvent(android.view.KeyEvent.KEYCODE_BUTTON_THUMBR,true))
                val pause=field("menu") as android.app.Dialog
                pause.dispatchKeyEvent(padEvent(android.view.KeyEvent.KEYCODE_BUTTON_THUMBR,false));check(field("running")==false) {"Pause immediately resumed on release"}
                pause.dispatchKeyEvent(padEvent(android.view.KeyEvent.KEYCODE_BUTTON_THUMBR,true))
                pause.dispatchKeyEvent(padEvent(android.view.KeyEvent.KEYCODE_BUTTON_THUMBR,false))
            }
            awaitReady()

            SystemClock.sleep(12000)
            shot("game")
            click("≫")
            main { check(field("fastForward")==true) }
            SystemClock.sleep(1500)
            click("≫")
            main { check(field("fastForward")==true && field("fastMultiplier")==4) }
            SystemClock.sleep(1500)
            click("≫")
            main { check(field("fastForward")==false) }

            val currentActivity=activity
            main { activity!!.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            SystemClock.sleep(700)
            awaitReady()
            main {
                check(activity===currentActivity) { "Rotation replaced the activity" }
                check(field("loaded")==true) { "Rotation unloaded the game" }
                check(activity!!.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT)
            }
            shot("portrait-game")
            click("Ⅱ  Pausa")
            main { check(field("running")==false && field("worker")==null) { "Pause left emulation running" } }
            shot("portrait-pause")
            main { activity!!.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            SystemClock.sleep(600)
            main { check(field("running")==false) { "Rotation resumed a paused game" } }
            val soundBefore=field("fastSound")
            click("Sonido al acelerar:",true)
            main {check(field("fastSound")!=soundBefore)}
            click("Sonido al acelerar:",true)
            main {check(field("fastSound")==soundBefore)}
            shot("pause")
            click("Mover botones",true)
            main { check((field("controls") as ControlsView).editing) }
            click("Listo")
            main { check(!(field("controls") as ControlsView).editing) }

            val fillBefore=(field("surface") as GameSurface).fillScreen
            click("Pantalla:",true)
            main { check((field("surface") as GameSurface).fillScreen!=fillBefore) { "Display mode did not change" } }
            click("▶  Seguir",true)
            awaitReady()
            SystemClock.sleep(12000)
            shot("original-ratio")
            main { i.callActivityOnPause(activity) }
            main { check(field("running")==false) { "Background audio/emulation did not stop" }; i.callActivityOnResume(activity) }
            main { check(field("running")==false) { "Resumed without explicit continue" } }
            click("Volver al inicio",true)
            main { checkNotNull(find(root(),"▶  Continuar partida")) { "Home lost paused game" } }
            main { activity!!.finish() }; i.waitForIdleSync()
            activity=i.startActivitySync(Intent(i.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            main { checkNotNull(find(root(),"▶  Volver a jugar")) { "Recreated activity lost last ROM" } }
            main { activity!!.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            SystemClock.sleep(500)
            shot("portrait-home")
            val importedId=prefs.getString("lastId",null)
            check(importedId!=null) { "Last game not persisted" }
            main { activity!!.finish() }; i.waitForIdleSync(); activity=null
            // Remove only the temporary ROM copied by this test; real games/saves are untouched.
            if(importedId!=previousId) {
                File(i.targetContext.filesDir,"roms/$importedId.gba").delete()
                File(i.targetContext.filesDir,"saves/$importedId.sav").delete()
            }
        } finally {
            monitor?.let { i.removeMonitor(it) }
            activity?.let { a->main { a.finish() }; i.waitForIdleSync() }
            if(previousIndex==null) index.delete() else index.writeBytes(previousIndex)
            prefs.edit().putBoolean("fillScreen",previousFill).apply {
                if(previousId==null) remove("lastId") else putString("lastId",previousId)
                if(previousName==null) remove("lastName") else putString("lastName",previousName)
            }.commit()
        }
    }
}