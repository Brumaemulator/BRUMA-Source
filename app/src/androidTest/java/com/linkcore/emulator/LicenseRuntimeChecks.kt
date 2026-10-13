// SPDX-License-Identifier: MIT
// Copyright (c) 2026 BRUMA contributors
package com.linkcore.emulator
import android.app.*
import android.content.*
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/** Test-only runner; uses original fixtures and never opens or changes a user save. */
object LicenseRuntimeChecks {
 fun run(test:Instrumentation){with(test){
  val result=Bundle();val state=AtomicReference<Intent?>()
  val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context,e:Intent){if(e.getStringExtra("system")=="RPG Maker XP")state.set(Intent(e))}}
  RuntimeSessionHost.register(targetContext,receiver,IntentFilter(RuntimeSessionHost.STATE))
  val classicConfig=File(targetContext.getExternalFilesDir(null),"mkxp.json")
  val originalClassicConfig=classicConfig.takeIf{it.exists()}?.readBytes()
  try{
   RpgInputChecks.run(this)
   targetContext.startActivity(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
   for(classic in listOf(false,true)){
    val dir=File(targetContext.filesDir,"rpg-games/license-fixture-${if(classic)"classic" else "modern"}").apply{mkdirs()}
    val resultFile=File(dir,"result.txt");resultFile.delete()
    val progress=File(dir,"progress.txt");progress.delete()
    val script=File(dir,"fixture.rb")
    val wav=File(dir,"pulse.wav")
    val samples=32000;val bytes=ByteArray(44+samples*2)
    fun str(offset:Int,text:String){text.toByteArray().copyInto(bytes,offset)}
    fun le(offset:Int,n:Int,size:Int){repeat(size){bytes[offset+it]=(n shr (it*8)).toByte()}}
    str(0,"RIFF");le(4,bytes.size-8,4);str(8,"WAVEfmt ");le(16,16,4);le(20,1,2);le(22,1,2);le(24,32000,4);le(28,64000,4);le(32,2,2);le(34,16,2);str(36,"data");le(40,samples*2,4)
    repeat(samples){val sample=(kotlin.math.sin(it*2*Math.PI*440/32000)*1500).toInt();le(44+it*2,sample,2)};wav.writeBytes(bytes)
    val ffi=if(classic)"" else """
      raise 'Alias' unless Win32API == MiniFFI
      length = MiniFFI.new('libSDL2.so', 'SDL_strlen', 'P', 'N')
      raise 'String pointer' unless length.Call('bruma') == 5
      raise 'Array imports' unless MiniFFI.new('libSDL2.so', 'SDL_strlen', ['p'], 'n').call('test') == 4
      value = "abcd\0"
      changed = MiniFFI.new('libSDL2.so', 'SDL_memset', 'PIN', 'P').call(value, 120, 4)
      raise 'Mutable pointer' unless changed == 'xxxx' && value[0,4] == 'xxxx'
      raise 'Zero args' unless MiniFFI.new('libSDL2.so', 'SDL_GetTicks', nil, 'N').call >= 0
      raise 'Bool type' unless MiniFFI.new('libSDL2.so', 'SDL_setenv', 'PPB', 'I').call('BRUMA_FFI_TEST', 'ok', true) == 0
      begin
        length.call
        raise 'Wrong arity accepted'
      rescue RuntimeError => e
        raise unless e.message.include?('wrong number')
      end
    """.trimIndent()
    script.writeText("""
     fixture_passed=false
     begin
       $ffi
       Graphics.frame_rate = 40
       Audio.bgm_play('pulse.wav', 25, 100)
       sprite=Sprite.new
       sprite.bitmap=Bitmap.new(128,128)
       sprite.bitmap.fill_rect(0,0,128,128,Color.new(80,190,170))
       started=Time.now
       confirms=0
       120.times do |n|
         Graphics.update
         Input.update
         confirms+=1 if Input.trigger?(Input::C)
         File.open('${progress.path}','w'){|f|f.write(n.to_s)}
       end
       elapsed=Time.now-started
       raise 'Frame rate' unless elapsed > 2.3 && elapsed < 8
       raise 'Enter not received' unless confirms > 0
       File.open('${resultFile.path}','w'){|f|f.write('PASS frames=120 rate=40 elapsed='+elapsed.to_s+' enter='+confirms.to_s)}
       fixture_passed=true
       loop { Graphics.update }
     rescue Exception => e
       raise if fixture_passed
       File.open('${resultFile.path}','w'){|f|f.write('FAIL '+e.to_s+' '+e.backtrace.join(' / '))}
       loop { Graphics.update }
     end
    """.trimIndent())
    File(dir,"Game.ini").writeText("[Game]\nTitle=BRUMA license regression\nLibrary=RGSS102E.dll\n")
    File(dir,"mkxp.json").writeText(org.json.JSONObject().put("gameFolder",dir.path).put("rgssVersion",1).put("customScript",script.path).put("fixedFramerate",0).put("syncToRefreshrate",false).put("vsync",false).put("defScreenW",512).put("defScreenH",384).toString())
    if(classic)classicConfig.writeText(File(dir,"mkxp.json").readText())
    state.set(null)
    val klass=if(classic)com.hatkid.mkxpz.ClassicActivity::class.java else com.hatkid.mkxpz.MainActivity::class.java
    targetContext.startActivity(Intent(targetContext,klass).putExtra("gamePath",dir.path).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    repeat(150){if(!progress.exists()&&!resultFile.exists())SystemClock.sleep(100)}
    check(progress.exists()||resultFile.exists()){ "Engine did not reach fixture: classic=$classic" }
    if(!resultFile.exists()){
     var clicked=false
     repeat(80){if(!clicked){
      val enter=uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Enter")?.firstOrNull{it.isVisibleToUser}
      clicked=enter?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)==true
      if(!clicked)SystemClock.sleep(25)
     }}
     if(!clicked){uiAutomation.takeScreenshot()?.let{bitmap->File(targetContext.cacheDir,"license-test-ui.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}}
     check(clicked){"Enter control absent: "+uiAutomation.rootInActiveWindow?.toString()}
    }
    repeat(100){if(!resultFile.exists())SystemClock.sleep(100)}
    check(resultFile.exists()){ "Fixture did not complete" };check(resultFile.readText().startsWith("PASS")){resultFile.readText()}
    uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
    repeat(30){RuntimeSessionHost.query(targetContext);if(state.get()?.getBooleanExtra("paused",false)!=true)SystemClock.sleep(100)}
    val paused=state.get();check(paused?.getBooleanExtra("paused",false)==true){"Back did not pause"}
    val token=paused!!.getStringExtra("token")!!
    targetContext.startActivity(Intent(targetContext,klass).putExtra("brumaResume",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
    repeat(30){RuntimeSessionHost.query(targetContext);if(state.get()?.getBooleanExtra("paused",true)!=false)SystemClock.sleep(100)}
    check(state.get()?.getStringExtra("token")==token&&!state.get()!!.getBooleanExtra("paused",true)){"Resume changed session"}
    File(targetContext.cacheDir,"license-${if(classic)"classic" else "modern"}-result.txt").writeText(resultFile.readText())
    RuntimeSessionHost.close(targetContext,token);SystemClock.sleep(800)
   }
   result.putString("stream","PASS: original ARM64 MiniFFI API fixture; modern/classic RGSS1 40 FPS; Enter; touch/controller source merging; back/pause/same-session resume. Audio WAV play command completed. Physical controller/audio listening not claimed.\n")
   finish(Activity.RESULT_OK,result)
  }catch(e:Throwable){result.putString("stream","FAIL: "+e.stackTraceToString());finish(Activity.RESULT_CANCELED,result)}
  finally{listOf("modern","classic").forEach{File(targetContext.filesDir,"rpg-games/license-fixture-$it").deleteRecursively()};targetContext.unregisterReceiver(receiver);if(originalClassicConfig!=null)classicConfig.writeBytes(originalClassicConfig)else classicConfig.delete()}
 }}
}
