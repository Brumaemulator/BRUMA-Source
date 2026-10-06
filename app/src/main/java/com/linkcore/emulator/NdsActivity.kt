// SPDX-License-Identifier: GPL-3.0-or-later
package com.linkcore.emulator

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.input.InputManager
import android.media.*
import android.view.*
import android.widget.FrameLayout
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.locks.LockSupport
import kotlin.math.*

/** Each console lives in :nds; only the emulation thread accesses its native state. */
class NdsActivity:Activity(),InputManager.InputDeviceListener {
 override fun attachBaseContext(newBase:Context){super.attachBaseContext(BrumaLocale.attach(newBase))}
 companion object {var active:java.lang.ref.WeakReference<NdsActivity>?=null;private val engineLock=Any()}
 private lateinit var screen:NdsScreen
 private lateinit var controls:NdsControls
 @Volatile private var alive=true
 @Volatile private var resumed=false
 @Volatile private var paused=false
 @Volatile private var pauseOnReturn=false
 @Volatile private var focused=false
 @Volatile private var touchKeys=0
 @Volatile private var padKeys=0
 @Volatile private var axisKeys=0
 @Volatile private var stylus=-1
 @Volatile private var speed=1
 @Volatile private var fastSound=false
 private var hidden=false
 @Volatile private var controllerInUse=false
 @Volatile private var activeRomPath:String?=null
 @Volatile private var sessionReady=false
 private var sessionHost:RuntimeSessionHost?=null
 private var returningToLibrary=false
 private var backCallback:android.window.OnBackInvokedCallback?=null
 private var worker:Thread?=null
 private val es get()=resources.configuration.locales[0].language=="es"
 internal fun tr(a:String,b:String)=if(es)a else b
 override fun onCreate(state:Bundle?){
  super.onCreate(state);active=java.lang.ref.WeakReference(this)
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  screen=NdsScreen(this);controls=NdsControls(this)
  screen.setOnTouchListener{_,event->if(event.actionMasked==MotionEvent.ACTION_DOWN){controllerInUse=false;updateControlsVisibility()};false}
  val root=FrameLayout(this);screen.focusLayout=getSharedPreferences("nds_ui",MODE_PRIVATE).getBoolean("focus_layout",false);screen.singleScreen=getSharedPreferences("nds_ui",MODE_PRIVATE).getInt("single_screen",0);root.addView(screen);root.addView(controls);setContentView(root)
  fastSound=getSharedPreferences("nds_ui",MODE_PRIVATE).getBoolean("fast_sound",false)
  (getSystemService(Context.INPUT_SERVICE) as InputManager).registerInputDeviceListener(this,android.os.Handler(mainLooper))
  immersive()
  val path=intent.getStringExtra("romPath")
  val rom=path?.let{File(it).canonicalFile}
  val base=File(filesDir,"roms").canonicalFile
  if(rom==null||rom.parentFile!=base||rom.extension!="nds"||!rom.isFile){errorDialog(tr("No se encuentra el juego de DS.","DS game not found."));return}
  activeRomPath=rom.path
  sessionHost=RuntimeSessionHost(this,"NDS",java.util.function.Supplier{activeRomPath.orEmpty()},java.util.function.BooleanSupplier{sessionReady&&alive},Runnable{alive=false;release();finish()})
  if(android.os.Build.VERSION.SDK_INT>=33){backCallback=android.window.OnBackInvokedCallback{options()};onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,backCallback!!)}
  worker=Thread({runConsole(rom)},"Bruma-NDS").also{it.start()}
 }
 override fun onNewIntent(nextIntent:android.content.Intent){
  super.onNewIntent(nextIntent);setIntent(nextIntent)
  val next=nextIntent.getStringExtra("romPath")?.let{runCatching{File(it).canonicalFile}.getOrNull()}?:return
  val base=File(filesDir,"roms").canonicalFile
  if(next.parentFile!=base||next.extension!="nds"||!next.isFile){errorDialog(tr("No se encuentra el juego de DS.","DS game not found."));return}
  if(nextIntent.getBooleanExtra("brumaResume",false)){pauseOnReturn=false;returningToLibrary=false;paused=false}
  if(next.path==activeRomPath)return
  switchRom(next)
 }
 private fun switchRom(next:File){
  val previous=worker
  alive=false;paused=true;release()
  Thread({
   try{previous?.join()}catch(_:InterruptedException){Thread.currentThread().interrupt();return@Thread}
   runOnUiThread{
    if(!isDestroyed&&!isFinishing){activeRomPath=next.path;alive=true;paused=false;worker=Thread({runConsole(next)},"Bruma-NDS").also{it.start()}}
   }
  },"Bruma-NDS-switch").start()
 }
 private fun immersive(){
  if(android.os.Build.VERSION.SDK_INT>=30){window.setDecorFitsSystemWindows(false);window.insetsController?.let{it.hide(WindowInsets.Type.systemBars());it.systemBarsBehavior=WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE}}
  else {window.decorView.systemUiVisibility=5894}
 }
 private fun release(){touchKeys=0;padKeys=0;axisKeys=0;stylus=-1;if(::controls.isInitialized)controls.clear()}
 override fun onResume(){super.onResume();resumed=true;returningToLibrary=false;immersive();if(pauseOnReturn){pauseOnReturn=false;paused=false;window.decorView.post{if(resumed&&alive&&!isFinishing&&!paused)options()}};sessionHost?.pause(false)}
 override fun onPause(){if(resumed&&alive&&!isFinishing)pauseOnReturn=true;resumed=false;release();super.onPause();sessionHost?.pause(true)}
 override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);focused=hasFocus;if(hasFocus)immersive() else release()}
 override fun onDestroy(){alive=false;sessionHost?.dispose();sessionHost=null;if(android.os.Build.VERSION.SDK_INT>=33)backCallback?.let{onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)};release();(getSystemService(Context.INPUT_SERVICE) as InputManager).unregisterInputDeviceListener(this);if(active?.get()===this)active=null;super.onDestroy()}
 private fun updateControlsVisibility(){if(::controls.isInitialized)controls.visibility=if(controllerInUse)View.GONE else View.VISIBLE}
 override fun onInputDeviceRemoved(id:Int){release();controllerInUse=false;updateControlsVisibility()}
 override fun onInputDeviceAdded(id:Int){}
 override fun onInputDeviceChanged(id:Int){release();controllerInUse=false;updateControlsVisibility()}
 @Deprecated("Activity compatibility") override fun onBackPressed(){options()}
 fun options(){
  if(paused)return;paused=true;release()
  val screenLabel=when(screen.singleScreen){1->tr("Solo pantalla superior","Top screen only");2->tr("Solo pantalla táctil","Touch screen only");else->tr("Ambas pantallas","Both screens")}
  val labels=arrayOf(tr("Continuar","Resume"),tr("Velocidad: ","Speed: ")+"${speed}x",tr("Sonido en avance rápido: ","Fast-forward sound: ")+(if(fastSound)tr("Activado","On") else tr("Desactivado","Off")),tr("Intercambiar pantallas","Swap screens"),tr("Diseño horizontal: ","Landscape layout: ")+(if(screen.focusLayout)tr("Principal + mini","Main + mini") else tr("Iguales","Equal")),tr("Pantalla visible: ","Visible screens: ")+screenLabel,if(controls.editing)tr("Guardar posiciones","Save positions") else tr("Mover botones","Move buttons"),tr("Restablecer botones","Reset button positions"),if(hidden)tr("Mostrar controles","Show controls") else tr("Ocultar controles","Hide controls"),tr("Pausar y volver a BRUMA","Pause and return to BRUMA"),tr("Cerrar juego","Close game"))
  AlertDialog.Builder(this).setTitle("Nintendo DS").setItems(labels){_,i->when(i){1->{speed=when(speed){1->2;2->4;else->1}};2->{fastSound=!fastSound;getSharedPreferences("nds_ui",MODE_PRIVATE).edit().putBoolean("fast_sound",fastSound).apply()};3->{screen.swapped=!screen.swapped};4->{screen.focusLayout=!screen.focusLayout;getSharedPreferences("nds_ui",MODE_PRIVATE).edit().putBoolean("focus_layout",screen.focusLayout).apply();screen.relayout()};5->{screen.singleScreen=(screen.singleScreen+1)%3;getSharedPreferences("nds_ui",MODE_PRIVATE).edit().putInt("single_screen",screen.singleScreen).apply();screen.relayout()};6->{controls.editing=!controls.editing;controllerInUse=false;updateControlsVisibility();controls.invalidate();if(controls.editing)android.widget.Toast.makeText(this,tr("Arrastra los botones y toca ☰ para terminar","Drag buttons and tap ☰ to finish"),android.widget.Toast.LENGTH_LONG).show()};7->{controls.resetPositions();android.widget.Toast.makeText(this,tr("Posiciones restablecidas","Button positions reset"),android.widget.Toast.LENGTH_SHORT).show()};8->{hidden=!hidden;controls.invalidate()};9->returnToBrumaLibrary();10->closeAndReturnToLibrary()}}.setOnDismissListener{if(!returningToLibrary)paused=false;immersive()}.show()
 }
 private fun closeAndReturnToLibrary(){
  returningToLibrary=true;pauseOnReturn=false;paused=true;alive=false;sessionReady=false
  release();sessionHost?.publish()
  // singleTask can leave DS as the task root: finish() alone returns to Android.
  // Explicitly bring back the existing library (or create it if it was reclaimed).
  startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
  finish()
 }
 private fun returnToBrumaLibrary(){returningToLibrary=true;paused=true;release();sessionHost?.pause(true);startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))}
 private fun errorDialog(message:String){runOnUiThread{if(!isFinishing&&!isDestroyed){paused=true;AlertDialog.Builder(this).setTitle("Nintendo DS").setMessage(message).setPositiveButton("OK"){_,_->finish()}.setCancelable(false).show()}}}
 private fun runConsole(rom:File)=synchronized(engineLock){
  var audio:AudioTrack?=null;var opened=false
  try{
   val saves=File(filesDir,"saves").apply{mkdirs()};val local=File(filesDir,"nds").apply{mkdirs()}
   NdsNative.open(rom.path,File(saves,rom.nameWithoutExtension+".nds.sav").path,local.path,BrumaLocale.ndsLanguage(this));opened=true;sessionReady=true;sessionHost?.publish()
   val format=AudioFormat.Builder().setSampleRate(48000).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
   audio=AudioTrack.Builder().setAudioFormat(format).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setBufferSizeInBytes(maxOf(8192,AudioTrack.getMinBufferSize(48000,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT))).setTransferMode(AudioTrack.MODE_STREAM).build()
   check(audio.state==AudioTrack.STATE_INITIALIZED){"Audio initialization failed"}
   android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY)
   val video=ByteBuffer.allocateDirect(256*384*4);val samples=ShortArray(4096);val bitmap=Bitmap.createBitmap(256,384,Bitmap.Config.ARGB_8888)
   var deadline=System.nanoTime();var lastFlush=deadline;var report=deadline;var frames=0;var sleeping=true;var oldSpeed=1;var oldAudible=false
   while(alive){
    if(!resumed||paused||!focused||!screen.ready){
     if(!sleeping){audio.pause();audio.flush();NdsNative.flush();sleeping=true}
     Thread.sleep(20);deadline=System.nanoTime();report=deadline;frames=0;continue
    }
    val rate=speed
    val audible=rate==1||fastSound
    if(sleeping||rate!=oldSpeed||audible!=oldAudible){audio.pause();audio.flush();if(audible)audio.play();sleeping=false;oldSpeed=rate;oldAudible=audible;deadline=System.nanoTime()}
    repeat(rate){val pen=stylus;val count=NdsNative.frame(touchKeys or padKeys or axisKeys,if(pen<0)-1 else pen and 255,if(pen<0)0 else pen ushr 8,video,samples);check(count>=0){"NDS stopped"};if(audible&&count>0){var offset=0;while(offset<count&&alive){val n=audio.write(samples,offset,count-offset,AudioTrack.WRITE_BLOCKING);check(n>0){"Audio write failed: $n"};offset+=n}};frames++}
    video.rewind();bitmap.copyPixelsFromBuffer(video);screen.render(bitmap)
    val now=System.nanoTime();if(now-lastFlush>1_000_000_000L){NdsNative.flush();lastFlush=now}
    if(now-report>=5_000_000_000L){android.util.Log.i("BrumaNDS","fps=%.2f speed=%d underruns=%d".format(java.util.Locale.ROOT,frames*1e9/(now-report),rate,audio.underrunCount));report=now;frames=0}
    deadline+=16_715_114L;val remaining=deadline-System.nanoTime();if(remaining>0)LockSupport.parkNanos(remaining) else if(remaining < -100_000_000L)deadline=System.nanoTime()
   }
   bitmap.recycle()
  }catch(e:Throwable){android.util.Log.e("BrumaNDS","Session failed",e);errorDialog(e.message?:"NDS error")}
  finally{sessionReady=false;sessionHost?.publish();try{if(opened)NdsNative.close()}catch(e:Throwable){android.util.Log.e("BrumaNDS","Save failed",e);errorDialog(e.message?:"Save failed")};audio?.release()}
 }
 private fun bit(code:Int)=when(code){KeyEvent.KEYCODE_BUTTON_A,KeyEvent.KEYCODE_DPAD_CENTER->0;KeyEvent.KEYCODE_BUTTON_B->1;KeyEvent.KEYCODE_BUTTON_SELECT->2;KeyEvent.KEYCODE_BUTTON_START->3;KeyEvent.KEYCODE_DPAD_RIGHT->4;KeyEvent.KEYCODE_DPAD_LEFT->5;KeyEvent.KEYCODE_DPAD_UP->6;KeyEvent.KEYCODE_DPAD_DOWN->7;KeyEvent.KEYCODE_BUTTON_R1->8;KeyEvent.KEYCODE_BUTTON_L1->9;KeyEvent.KEYCODE_BUTTON_X->10;KeyEvent.KEYCODE_BUTTON_Y->11;else->-1}
 override fun dispatchKeyEvent(e:KeyEvent):Boolean{
  if(e.keyCode==KeyEvent.KEYCODE_BUTTON_MODE){if(e.action==KeyEvent.ACTION_UP)options();return true}
  val b=bit(e.keyCode);if(b>=0){if(controls.editing)return true;if(e.action==KeyEvent.ACTION_DOWN){controllerInUse=true;updateControlsVisibility()};if(!paused&&focused){padKeys=if(e.action==KeyEvent.ACTION_DOWN)padKeys or (1 shl b) else padKeys and (1 shl b).inv()};return true};return super.dispatchKeyEvent(e)
 }
 override fun onGenericMotionEvent(e:MotionEvent):Boolean{
  if(e.isFromSource(InputDevice.SOURCE_JOYSTICK)&&e.action==MotionEvent.ACTION_MOVE){
   if(controls.editing){axisKeys=0;return true}
   fun axis(a:Int):Float{val v=e.getAxisValue(a);val dead=max(0.2f,e.device?.getMotionRange(a,e.source)?.flat?:0.2f);return if(abs(v)>dead)v else 0f}
   val x=axis(MotionEvent.AXIS_X)+axis(MotionEvent.AXIS_HAT_X);val y=axis(MotionEvent.AXIS_Y)+axis(MotionEvent.AXIS_HAT_Y)
   axisKeys=if(paused||!focused)0 else (if(x>0.3f)16 else if(x< -0.3f)32 else 0) or (if(y>0.3f)128 else if(y< -0.3f)64 else 0);if(axisKeys!=0){controllerInUse=true;updateControlsVisibility()};return true
  };return super.onGenericMotionEvent(e)
 }
 private inner class NdsScreen(context:Context):SurfaceView(context),SurfaceHolder.Callback{
  @Volatile var ready=false
  @Volatile var swapped=false
  @Volatile var focusLayout=false
  @Volatile var singleScreen=0
  @Volatile var top=RectF()
  @Volatile var bottom=RectF()
  @Volatile var single=RectF()
  private var viewportW=0
  private var viewportH=0
  private val paint=Paint().apply{isFilterBitmap=false}
  private val topSource=Rect(0,0,256,192)
  private val bottomSource=Rect(0,192,256,384)
  init{holder.addCallback(this)}
  override fun surfaceCreated(h:SurfaceHolder){ready=true}
  override fun surfaceDestroyed(h:SurfaceHolder){ready=false}
  override fun surfaceChanged(h:SurfaceHolder,f:Int,w:Int,he:Int){layoutScreens(w,he)}
  fun relayout(){if(viewportW>0&&viewportH>0)layoutScreens(viewportW,viewportH)}
  private fun layoutScreens(w:Int,h:Int){
   viewportW=w;viewportH=h
   val d=resources.displayMetrics.density;val gap=8*d
   if(singleScreen!=0){val aw=(w-32*d).coerceAtLeast(100*d);val ah=(h-32*d).coerceAtLeast(100*d);val sw=min(aw,ah*4f/3f);val sh=sw*.75f;single=RectF((w-sw)/2f,(h-sh)/2f,(w+sw)/2f,(h+sh)/2f);top=if(singleScreen==1)single else RectF();bottom=if(singleScreen==2)single else RectF();return}
   single=RectF()
   if(h>w){val sw=min(w-16*d,(h-260*d-48*d-gap)*2f/3f).coerceAtLeast(100*d);val sh=sw*0.75f;val x=(w-sw)/2;val y=48*d;top=RectF(x,y,x+sw,y+sh);bottom=RectF(x,y+sh+gap,x+sw,y+sh*2+gap)}
   else if(focusLayout){
    val miniScale=0.42f;val availableH=(h-40*d).coerceAtLeast(100*d);val availableW=(w-128*d).coerceAtLeast(180*d)
    val maxMainH=min(availableH,(availableW-gap)/(4f/3f*(1f+miniScale)))
    val mainW=maxMainH*4f/3f;val mainH=maxMainH;val miniW=mainW*miniScale;val miniH=mainH*miniScale
    val startX=(w-(mainW+gap+miniW))/2f;val y=(h-mainH)/2f
    top=RectF(startX,y,startX+mainW,y+mainH);bottom=RectF(startX+mainW+gap,y+(mainH-miniH)/2f,startX+mainW+gap+miniW,y+(mainH+miniH)/2f)
   }
   else {val sw=min((w-160*d-gap)/2,(h-44*d)*4f/3f).coerceAtLeast(80*d);val sh=sw*0.75f;val x=(w-sw*2-gap)/2;val y=(h-sh)/2;top=RectF(x,y,x+sw,y+sh);bottom=RectF(x+sw+gap,y,x+sw*2+gap,y+sh)}
  }
  fun render(bitmap:Bitmap){if(!ready)return;var c:Canvas?=null;try{c=holder.lockHardwareCanvas();c.drawColor(Color.rgb(9,12,20));if(singleScreen==1){topSource.set(0,0,256,192);c.drawBitmap(bitmap,topSource,single,paint)}else if(singleScreen==2){bottomSource.set(0,192,256,384);c.drawBitmap(bitmap,bottomSource,single,paint)}else{if(swapped){topSource.set(0,192,256,384);bottomSource.set(0,0,256,192)}else{topSource.set(0,0,256,192);bottomSource.set(0,192,256,384)};c.drawBitmap(bitmap,topSource,top,paint);c.drawBitmap(bitmap,bottomSource,bottom,paint)}}catch(_:IllegalStateException){}finally{if(c!=null)holder.unlockCanvasAndPost(c)}}
  fun touchRect()=when(singleScreen){1->RectF();2->single;else->if(swapped)top else bottom}
 }
 private inner class NdsControls(context:Context):View(context){
  private val p=Paint(Paint.ANTI_ALIAS_FLAG)
  private val buttons=mutableListOf<Pair<String,RectF>>()
  private val pointers=mutableMapOf<Int,String>()
  private var penId=-1
  private val positions=context.getSharedPreferences("nds_controls",Context.MODE_PRIVATE)
  private fun positionKey(label:String,axis:String)=((if(height>width)"portrait" else "landscape")+"."+label+"."+axis)
  fun resetPositions(){positions.edit().clear().apply();if(width>0&&height>0)onSizeChanged(width,height,width,height);invalidate()}
  var editing=false
  private var dragLabel:String?=null
  private var lastX=0f
  private var lastY=0f
  private val bits=mapOf("A" to 0,"B" to 1,"SELECT" to 2,"START" to 3,"→" to 4,"←" to 5,"↑" to 6,"↓" to 7,"R" to 8,"L" to 9,"X" to 10,"Y" to 11)
  fun clear(){pointers.clear();penId=-1;invalidate()}
  override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){
   val d=resources.displayMetrics.density;buttons.clear()
   fun add(s:String,x:Float,y:Float,bw:Float=48*d,bh:Float=bw){buttons.add(s to RectF(x-bw/2,y-bh/2,x+bw/2,y+bh/2))}
   add("☰",w/2f,23*d,64*d,36*d)
   val portrait=h>w;val cy=if(portrait)h-130*d else h*0.65f;val left=if(portrait)88*d else 66*d;val right=w-left;val step=38*d
   add("↑",left,cy-step);add("↓",left,cy+step);add("←",left-step,cy);add("→",left+step,cy)
   add("X",right,cy-step);add("B",right,cy+step);add("Y",right-step,cy);add("A",right+step,cy)
   val shoulderY=if(portrait)h-220*d else 45*d
   add("L",left,shoulderY,80*d,34*d);add("R",right,shoulderY,80*d,34*d)
   add("SELECT",w/2f-48*d,h-35*d,85*d,30*d);add("START",w/2f+48*d,h-35*d,85*d,30*d)
   buttons.filter{it.first!="☰"}.forEach{(label,r)->val xKey=positionKey(label,"x");val yKey=positionKey(label,"y");if(positions.contains(xKey)){val x=(positions.getFloat(xKey,.5f)*w-r.width()/2).coerceIn(0f,(w-r.width()).coerceAtLeast(0f));val y=(positions.getFloat(yKey,.5f)*h-r.height()/2).coerceIn(0f,(h-r.height()).coerceAtLeast(0f));r.offsetTo(x,y)}}
  }
  override fun onDraw(c:Canvas){buttons.forEach{(s,r)->if(editing||!hidden||s=="☰"){
   p.style=Paint.Style.FILL;p.color=if(editing&&s!="☰")0x995c4c9b.toInt() else if(pointers.containsValue(s))0xff9381f5.toInt() else 0xc0252a3b.toInt();c.drawRoundRect(r,16f,16f,p)
   p.style=Paint.Style.STROKE;p.strokeWidth=resources.displayMetrics.density;p.color=0xff777d93.toInt();c.drawRoundRect(r,16f,16f,p)
   p.style=Paint.Style.FILL;p.color=Color.WHITE;p.textAlign=Paint.Align.CENTER;p.textSize=(if(s.length>2)12 else 21)*resources.displayMetrics.density;c.drawText(s,r.centerX(),r.centerY()-(p.ascent()+p.descent())/2,p)
  }}}
  override fun onTouchEvent(e:MotionEvent):Boolean{
   val index=e.actionIndex;val id=e.getPointerId(index)
   if(editing){when(e.actionMasked){MotionEvent.ACTION_DOWN->{val hit=buttons.asReversed().firstOrNull{it.second.contains(e.getX(index),e.getY(index))};if(hit?.first=="☰"){editing=false;controllerInUse=false;updateControlsVisibility();android.widget.Toast.makeText(this@NdsActivity,tr("Posiciones guardadas","Positions saved"),android.widget.Toast.LENGTH_SHORT).show();invalidate();return true};dragLabel=hit?.first;lastX=e.getX(index);lastY=e.getY(index)};MotionEvent.ACTION_MOVE->{val r=buttons.firstOrNull{it.first==dragLabel}?.second;if(r!=null){r.offsetTo((r.left+e.getX(0)-lastX).coerceIn(0f,(width-r.width()).coerceAtLeast(0f)),(r.top+e.getY(0)-lastY).coerceIn(0f,(height-r.height()).coerceAtLeast(0f)));lastX=e.getX(0);lastY=e.getY(0);invalidate()}};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{val edit=positions.edit();buttons.filter{it.first!="☰"}.forEach{(label,r)->edit.putFloat(positionKey(label,"x"),r.centerX()/width.coerceAtLeast(1)).putFloat(positionKey(label,"y"),r.centerY()/height.coerceAtLeast(1))};edit.apply();dragLabel=null;invalidate()}};return true}
   if(e.actionMasked==MotionEvent.ACTION_CANCEL){release();return true}
   if(e.actionMasked==MotionEvent.ACTION_DOWN||e.actionMasked==MotionEvent.ACTION_POINTER_DOWN){
    val x=e.getX(index);val y=e.getY(index);val hit=buttons.firstOrNull{(!hidden||it.first=="☰")&&it.second.contains(x,y)}?.first
    if(hit=="☰"){options();return true}
    if(hit!=null)pointers[id]=hit else if(penId<0&&screen.touchRect().contains(x,y))penId=id
   }
   if(e.actionMasked==MotionEvent.ACTION_UP||e.actionMasked==MotionEvent.ACTION_POINTER_UP){pointers.remove(id);if(id==penId){penId=-1;stylus=-1}}
   if(e.actionMasked==MotionEvent.ACTION_MOVE){for(i in 0 until e.pointerCount){val pid=e.getPointerId(i);if(pointers.containsKey(pid))pointers[pid]=buttons.firstOrNull{it.first!="☰"&&it.second.contains(e.getX(i),e.getY(i))}?.first?:""}}
   val pen=e.findPointerIndex(penId);if(pen>=0){val r=screen.touchRect();val x=((e.getX(pen)-r.left)*256/r.width()).toInt().coerceIn(0,255);val y=((e.getY(pen)-r.top)*192/r.height()).toInt().coerceIn(0,191);stylus=x or (y shl 8)}
   touchKeys=pointers.values.fold(0){mask,s->mask or (bits[s]?.let{1 shl it}?:0)};invalidate();return true
  }
 }
}
