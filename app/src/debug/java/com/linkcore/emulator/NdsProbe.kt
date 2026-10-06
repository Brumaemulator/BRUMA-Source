package com.linkcore.emulator
import android.content.*
import android.os.*
import android.view.*
import java.io.File
/** Debug only; requires shell DUMP permission. */
class NdsProbe:BroadcastReceiver(){
 override fun onReceive(c:Context,i:Intent){when(i.getStringExtra("op")){
 "launch"->{val rom=File(c.filesDir,"roms").listFiles()?.firstOrNull{it.extension=="nds"};if(rom!=null)c.startActivity(Intent(c,NdsActivity::class.java).putExtra("romPath",rom.path).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));else android.util.Log.e("NdsProbe","No NDS ROM imported")}
 "key"->{val a=NdsActivity.active?.get()?:return;val key=i.getIntExtra("key",96);val ms=i.getLongExtra("ms",100);a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,key));Handler(Looper.getMainLooper()).postDelayed({a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP,key))},ms)}
 "touch"->{val a=NdsActivity.active?.get()?:return;val x=i.getFloatExtra("x",0f);val y=i.getFloatExtra("y",0f);val now=SystemClock.uptimeMillis();val down=MotionEvent.obtain(now,now,0,x,y,0);a.dispatchTouchEvent(down);down.recycle();Handler(Looper.getMainLooper()).postDelayed({val up=MotionEvent.obtain(now,SystemClock.uptimeMillis(),1,x,y,0);a.dispatchTouchEvent(up);up.recycle()},100)}
 "orientation"->{NdsActivity.active?.get()?.requestedOrientation=i.getIntExtra("value",-1)}
 "exit"->NdsActivity.active?.get()?.finish()
 }}
}
