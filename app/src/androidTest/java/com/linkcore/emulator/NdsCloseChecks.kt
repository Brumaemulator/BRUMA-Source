package com.linkcore.emulator

import android.app.Activity
import android.app.Instrumentation
import android.content.*
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.util.AtomicFile
import org.json.JSONArray
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/** Exercises the DS menu through accessibility; writes only a private fixture save. */
object NdsCloseChecks {
 fun run(i:Instrumentation) {
  val c=i.targetContext
  val result=Bundle()
  val id=LibraryStore.key("nds-close-test-${System.nanoTime()}")
  val fixture=File(c.filesDir,"roms/$id.nds")
  val save=File(c.filesDir,"saves/$id.nds.sav")
  val state=AtomicReference<Intent?>()
  val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context,e:Intent){if(e.getStringExtra("system")=="NDS" && File(e.getStringExtra("path").orEmpty()).name==fixture.name)state.set(Intent(e))}}
  val monitor=i.addMonitor(MainActivity::class.java.name,null,false)
  var home:MainActivity?=null
  fun ui(block:()->Unit){var failure:Throwable?=null;i.runOnMainSync{try{block()}catch(t:Throwable){failure=t}};failure?.let{throw it}}
  fun await(message:String,check:()->Boolean){repeat(180){if(check())return;SystemClock.sleep(100)};error(message)}
  fun query(){RuntimeSessionHost.query(c)}
  fun libraryVisible():Boolean {var visible=false;ui{
   val activity=(monitor.lastActivity as? MainActivity)?.takeIf{!it.isDestroyed}?:home
   if(activity!=null && !activity.isDestroyed){home=activity;val field=MainActivity::class.java.getDeclaredField("home").apply{isAccessible=true};visible=(field.get(activity) as View).isShown && activity.hasWindowFocus()}
  };return visible}
  fun click(vararg text:String){await("Missing menu action: ${text.joinToString()}"){
   val root=i.uiAutomation.rootInActiveWindow?:return@await false
   text.asSequence().flatMap{root.findAccessibilityNodeInfosByText(it).asSequence()}.firstOrNull{it.isVisibleToUser}?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true
  }}
  fun menu(){check(i.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK));SystemClock.sleep(300)}
  var registered=false
  try {
   val original=LibraryStore(c).read().first{it.system=="NDS" && RomFiles.cached(c,it.cached)!=null}
   checkNotNull(RomFiles.cached(c,original.cached)).copyTo(fixture)
   RuntimeSessionHost.register(c,receiver,IntentFilter(RuntimeSessionHost.STATE));registered=true
   home=i.startActivitySync(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
   await("Library not visible before DS launch"){libraryVisible()}
   for(pauseFirst in listOf(false,true)) {
    state.set(null)
    c.startActivity(Intent(c,NdsActivity::class.java).putExtra("romPath",fixture.path).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    await("DS failed to start"){query();state.get()?.getBooleanExtra("ready",false)==true}
    val token=state.get()!!.getStringExtra("token")
    SystemClock.sleep(1000)
    if(pauseFirst) {
     menu();click("Pausar y volver a BRUMA","Pause and return to BRUMA")
     await("Pause did not return to library"){libraryVisible()}
     c.startActivity(Intent(c,NdsActivity::class.java).putExtra("romPath",fixture.path).putExtra("brumaResume",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
     await("Same paused DS session did not resume"){query();val e=state.get();e!=null && e.getStringExtra("token")==token && e.getBooleanExtra("ready",false) && !e.getBooleanExtra("paused",true)}
     SystemClock.sleep(600)
    }
    menu();click("Cerrar juego","Close game")
    await("Closing DS exited BRUMA instead of showing the library"){libraryVisible()}
    await("Closed DS session still ready"){query();state.get()?.getBooleanExtra("ready",true)==false}
    var card=false
    ui{val activity=checkNotNull(home);val field=MainActivity::class.java.getDeclaredField("continueCard").apply{isAccessible=true};card=(field.get(activity) as View).isShown}
    check(!card){"Resume card still visible after closing DS"}
    SystemClock.sleep(700)
   }
   result.putString("stream","PASS: actual NDS Close game menu returns to focused BRUMA library, session closed/no continue card; fresh launch and pause/resume/close; isolated ROM/save copy, original saves untouched.\n")
  } catch(t:Throwable) {result.putString("stream",t.stackTraceToString())}
  finally {
   if(registered)c.unregisterReceiver(receiver)
   i.removeMonitor(monitor)
   // Only these generated fixture files are disposable; preserve all real entries.
   check(fixture.canonicalFile.parentFile==File(c.filesDir,"roms").canonicalFile)
   fixture.delete();save.delete()
   val index=AtomicFile(File(c.filesDir,"game-library.json"))
   runCatching {
    val old=JSONArray(index.openRead().bufferedReader().use{it.readText()});val kept=JSONArray()
    for(n in 0 until old.length()){val game=old.getJSONObject(n);if(game.optString("cached")!=id)kept.put(game)}
    val out=index.startWrite();try{out.write(kept.toString().toByteArray());index.finishWrite(out)}catch(t:Throwable){index.failWrite(out);throw t}
   }
  }
  i.finish(if(result.getString("stream")!!.startsWith("PASS:"))Activity.RESULT_OK else Activity.RESULT_CANCELED,result)
 }
}
