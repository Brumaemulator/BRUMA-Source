package com.linkcore.emulator
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import java.io.File
object GbcUiCheck {
 fun run(i:Instrumentation){
  val game=LibraryStore(i.targetContext).allGames().first{it.system=="GBC"}
  val save=File(i.targetContext.filesDir,"saves/${game.cached}.sav");val previous=save.takeIf{it.exists()}?.readBytes()
  val activity=i.startActivitySync(Intent(i.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
  try {
   i.runOnMainSync {MainActivity::class.java.getDeclaredMethod("launchLibraryGame",LibraryGame::class.java).apply{isAccessible=true}.invoke(activity,game)}
   SystemClock.sleep(15000)
   i.runOnMainSync {
    val controls=MainActivity::class.java.getDeclaredField("controls").apply{isAccessible=true}.get(activity) as ControlsView
    ControlsView::class.java.getDeclaredField("keys").apply{isAccessible=true}.setInt(controls,8)
   }
   SystemClock.sleep(100)
   i.runOnMainSync {
    val controls=MainActivity::class.java.getDeclaredField("controls").apply{isAccessible=true}.get(activity) as ControlsView
    ControlsView::class.java.getDeclaredField("keys").apply{isAccessible=true}.setInt(controls,0)
    val surface=MainActivity::class.java.getDeclaredField("surface").apply{isAccessible=true}.get(activity) as GameSurface
    surface.fillScreen=true
   }
   SystemClock.sleep(15000)
   var snapshot:Bitmap?=null
   i.runOnMainSync {val surface=MainActivity::class.java.getDeclaredField("surface").apply{isAccessible=true}.get(activity) as GameSurface;snapshot=surface.snapshot()}
   check(snapshot?.width==160 && snapshot?.height==144){"GBC viewport size"};snapshot?.recycle()
   val screenshot=i.uiAutomation.takeScreenshot()!!
   File(i.targetContext.cacheDir,"gbc-ui.png").outputStream().use{screenshot.compress(Bitmap.CompressFormat.PNG,100,it)};screenshot.recycle()
  } finally {
   i.runOnMainSync {i.callActivityOnPause(activity);activity.finish()};i.waitForIdleSync();NativeCore.close()
   if(previous!=null)save.writeBytes(previous) else save.delete()
  }
 }
}
