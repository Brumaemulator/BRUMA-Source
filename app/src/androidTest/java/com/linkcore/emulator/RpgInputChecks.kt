package com.linkcore.emulator
import android.app.Instrumentation
import android.view.*
import com.hatkid.mkxpz.gamepad.*
object RpgInputChecks {
 fun run(test:Instrumentation) {
  val output=mutableListOf<Pair<Int,Boolean>>()
  val input=RpgInput {key,down->output.add(key to down)}
  val right=KeyEvent.KEYCODE_DPAD_RIGHT
  input.set("touch",right,true);input.set("device:1:key",right,true);input.set("touch",right,false)
  check(output==listOf(right to true));input.release("device:1:");check(output.last()==right to false)
  output.clear()
  fun key(action:Int,code:Int)=KeyEvent(0,0,action,code,0,0,12,0,0,InputDevice.SOURCE_GAMEPAD)
  check(input.key(key(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BUTTON_A)))
  check(input.key(key(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BUTTON_A)))
  check(output==listOf(KeyEvent.KEYCODE_C to true,KeyEvent.KEYCODE_C to false))
  check(!input.key(key(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_UP)))
  output.clear()
  fun axis(x:Float,y:Float) {
   val coords=MotionEvent.PointerCoords().apply {setAxisValue(MotionEvent.AXIS_X,x);setAxisValue(MotionEvent.AXIS_Y,y)}
   val props=MotionEvent.PointerProperties().apply {id=0}
   val event=MotionEvent.obtain(0,0,MotionEvent.ACTION_MOVE,1,arrayOf(props),arrayOf(coords),0,0,1f,1f,12,0,InputDevice.SOURCE_JOYSTICK,0)
   check(input.motion(event));event.recycle()
  }
  axis(1f,0f);axis(0.1f,0f);check(output==listOf(right to true,right to false))
  output.clear();axis(-1f,-1f);input.release("");check(output.size==4)
  test.runOnMainSync {
   val pad=GamepadDPad(test.targetContext);pad.layout(0,0,200,200)
   output.clear();pad.setOnKeyDownListener {output.add(it to true)};pad.setOnKeyUpListener {output.add(it to false)}
   fun touch(action:Int,x:Float,y:Float) {val event=MotionEvent.obtain(0,0,action,x,y,0);check(pad.onTouchEvent(event));event.recycle()}
   touch(MotionEvent.ACTION_DOWN,180f,100f);touch(MotionEvent.ACTION_MOVE,180f,100f)
   check(output==listOf(right to true));touch(MotionEvent.ACTION_MOVE,100f,100f)
   check(output.last()==right to false);touch(MotionEvent.ACTION_MOVE,100f,20f);touch(MotionEvent.ACTION_CANCEL,100f,20f)
   check(output.takeLast(2)==listOf(KeyEvent.KEYCODE_DPAD_UP to true,KeyEvent.KEYCODE_DPAD_UP to false))
  }
 }
}
