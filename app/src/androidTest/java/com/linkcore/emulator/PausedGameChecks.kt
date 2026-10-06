package com.linkcore.emulator

import android.app.Instrumentation
import android.content.*
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.*
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import java.io.File
import java.util.concurrent.atomic.AtomicReference

object PausedGameChecks {
    fun run(i:Instrumentation){
        val report=AtomicReference<Intent?>()
        val rpgReport=AtomicReference<Intent?>()
        val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context,e:Intent){if(e.getStringExtra("system")=="NDS")report.set(Intent(e)) else if(e.getStringExtra("system")=="RPG Maker XP")rpgReport.set(Intent(e))}}
        RuntimeSessionHost.register(i.targetContext,receiver,IntentFilter(RuntimeSessionHost.STATE))
        val monitor=i.addMonitor(MainActivity::class.java.name,null,false)
        var home=i.startActivitySync(Intent(i.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) as MainActivity
        val output=File(i.targetContext.cacheDir,"paused-game-checks").apply{mkdirs()}
        val prefs=i.targetContext.getSharedPreferences("library",0)
        val previousId=prefs.getString("lastId",null);val previousName=prefs.getString("lastName",null)
        val rpgPrefs=i.targetContext.getSharedPreferences("rpg",0);val previousRpgPath=rpgPrefs.getString("path",null)
        fun ui(block:()->Unit){var failure:Throwable?=null;i.runOnMainSync{try{block()}catch(t:Throwable){failure=t}};failure?.let{throw it}}
        fun field(name:String)=MainActivity::class.java.getDeclaredField(name).apply{isAccessible=true}.get(home)
        fun method(name:String,vararg params:Any){ui{MainActivity::class.java.declaredMethods.first{it.name==name}.apply{isAccessible=true}.invoke(home,*params)}}
        fun await(message:String,check:()->Boolean){repeat(150){if(check())return;SystemClock.sleep(100)};error(message)}
        fun currentHome(){(monitor.lastActivity as? MainActivity)?.let{if(!it.isDestroyed)home=it}}
        fun visibleCard():Boolean{var result=false;ui{currentHome();result=(field("continueCard") as View).isShown};return result}
        fun cardSession():RuntimeSession?{var value:RuntimeSession?=null;ui{currentHome();value=field("continueSession") as? RuntimeSession};return value}
        fun shot(name:String){SystemClock.sleep(500);val b=checkNotNull(i.uiAutomation.takeScreenshot());File(output,"$name.png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()}
        fun clickAccessible(vararg texts:String){
            await("Missing control ${texts.joinToString()}"){
                val root=i.uiAutomation.rootInActiveWindow?:return@await false
                val node=texts.asSequence().flatMap{root.findAccessibilityNodeInfosByText(it).asSequence()}.firstOrNull{it.isVisibleToUser}
                node?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true
            }
        }
        fun state(paused:Boolean,token:String?=null){await("NDS did not reach paused=$paused"){
            RuntimeSessionHost.query(i.targetContext)
            val e=report.get();e?.getBooleanExtra("ready",false)==true&&e.getBooleanExtra("paused",false)==paused&&(token==null||e.getStringExtra("token")==token)
        }}
        try{
            ui{home.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
            await("Library unavailable"){var ready=false;ui{ready=(field("libraryGames") as List<*>).isNotEmpty()};ready}
            check(!visibleCard()){ "Resume card shown with no live game" }
            val games=LibraryStore(i.targetContext).cachedGames()
            val nds=games.first{it.system=="NDS"&&it.name.contains("nintendogs",true)}
            method("launchLibraryGame",nds)
            state(false)
            val token=checkNotNull(report.get()?.getStringExtra("token"))
            SystemClock.sleep(1000)
            check(i.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)){"Back action unavailable"}
            clickAccessible("Pausar y volver a BRUMA","Pause and return to BRUMA")
            state(true,token)
            await("No NDS resume card"){visibleCard()&&cardSession()?.token==token}
            shot("nds-paused-portrait")
            method("resumeCurrentGame")
            state(false,token)
            SystemClock.sleep(700)
            check(i.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)){"Back action unavailable"}
            clickAccessible("Pausar y volver a BRUMA","Pause and return to BRUMA")
            state(true,token)
            await("NDS session changed after resume"){visibleCard()&&cardSession()?.token==token}
            ui{home.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}
            shot("nds-paused-landscape")
            method("showContinueOptions")
            clickAccessible("Cerrar juego","Close game")
            await("Resume card remained after NDS close"){!visibleCard()}
            await("NDS session remained open"){RuntimeSessionHost.query(i.targetContext);report.get()?.getBooleanExtra("ready",true)==false}
            ui{home.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
            shot("no-paused-game")
            // Existing original homebrew fixture: no user save or library metadata is changed.
            val fixture=File(output,"session.gba")
            i.context.assets.open("smoke.gba").use{input->fixture.outputStream().use{input.copyTo(it)}}
            ui{MainActivity::class.java.getDeclaredField("rom").apply{isAccessible=true}.set(home,fixture);MainActivity::class.java.getDeclaredField("romName").apply{isAccessible=true}.set(home,"BRUMA session check")}
            method("enterGame")
            await("GBA did not start"){var loaded=false;ui{loaded=field("loaded")==true};loaded}
            method("goHome")
            check(visibleCard()&&cardSession()?.system=="GBA"){ "No GBA paused card" }
            method("resumeCurrentGame")
            await("GBA did not resume"){var running=false;ui{running=field("running")==true};running}
            method("goHome");method("showContinueOptions");clickAccessible("Cerrar juego","Close game")
            check(!visibleCard()){ "GBA card remained after close" }
            val rpgGames=games.filter{it.system=="RPG Maker XP"&&File(android.net.Uri.parse(it.uri).path.orEmpty(),"Game.ini").isFile()}
            for(classic in listOf(false,true)){
                val game=(if(classic)rpgGames.reversed() else rpgGames).firstOrNull{RpgEngine.classic(i.targetContext,File(android.net.Uri.parse(it.uri).path!!))==classic}?:error("Missing RPG fixture for classic=$classic")
                rpgReport.set(null);method("launchLibraryGame",game)
                await("RPG did not start, classic=$classic"){RuntimeSessionHost.query(i.targetContext);rpgReport.get()?.getBooleanExtra("ready",false)==true}
                val rpgToken=checkNotNull(rpgReport.get()?.getStringExtra("token"))
                // Let scripts and rendering finish starting before testing a pause.
                SystemClock.sleep(20000)
                check(i.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
                try{await("RPG resume card missing"){visibleCard()&&cardSession()?.token==rpgToken}}catch(t:Throwable){shot("rpg-failure");error("${t.message}; state=${rpgReport.get()?.extras}; card=${cardSession()}; shown=${visibleCard()}; inGame=${field("inGame")}; busy=${field("busy")}")}
                shot(if(classic)"rpg-classic-paused" else "rpg-modern-paused")
                method("resumeCurrentGame")
                try{await("RPG session did not resume"){RuntimeSessionHost.query(i.targetContext);val e=rpgReport.get();e?.getStringExtra("token")==rpgToken&&e.getBooleanExtra("ready",false)&&!e.getBooleanExtra("paused",true)}}catch(t:Throwable){shot("rpg-resume-failure");error("${t.message}; state=${rpgReport.get()?.extras}; card=${cardSession()}")}
                SystemClock.sleep(500)
                check(i.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
                await("RPG lost its session after resume"){visibleCard()&&cardSession()?.token==rpgToken}
                method("showContinueOptions");clickAccessible("Cerrar juego","Close game")
                await("RPG card remained after close"){!visibleCard()}
                SystemClock.sleep(1000)
            }
        }finally{
            i.targetContext.unregisterReceiver(receiver);i.removeMonitor(monitor)
            prefs.edit().putString("lastId",previousId).putString("lastName",previousName).commit()
            rpgPrefs.edit().putString("path",previousRpgPath).commit()
            ui{currentHome();if(!home.isDestroyed){home.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_USER;home.finish()}}
        }
    }
}
