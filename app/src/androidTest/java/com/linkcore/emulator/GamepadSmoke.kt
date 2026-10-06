package com.linkcore.emulator

import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

object GamepadSmoke {
    fun run(i:Instrumentation) {
        val context=object:ContextWrapper(i.targetContext) {
            override fun getSharedPreferences(name:String,mode:Int)=super.getSharedPreferences("test-gamepad",Context.MODE_PRIVATE)
        }
        val settings=context.getSharedPreferences("",0);settings.edit().clear().commit()
        var pauses=0;var speeds=0
        var input:GamepadInput?=null
        fun event(code:Int,down:Boolean,id:Int=101,repeat:Int=0)=KeyEvent(0,SystemClock.uptimeMillis(),if(down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP,code,repeat,0,id,0,0,InputDevice.SOURCE_GAMEPAD)
        fun motion(x:Float=0f,y:Float=0f,hatX:Float=0f,hatY:Float=0f,left:Float=0f,right:Float=0f):MotionEvent {
            val prop=MotionEvent.PointerProperties().apply {id=0}
            val coord=MotionEvent.PointerCoords().apply {setAxisValue(MotionEvent.AXIS_X,x);setAxisValue(MotionEvent.AXIS_Y,y);setAxisValue(MotionEvent.AXIS_HAT_X,hatX);setAxisValue(MotionEvent.AXIS_HAT_Y,hatY);setAxisValue(MotionEvent.AXIS_LTRIGGER,left);setAxisValue(MotionEvent.AXIS_RTRIGGER,right)}
            return MotionEvent.obtain(0,SystemClock.uptimeMillis(),MotionEvent.ACTION_MOVE,1,arrayOf(prop),arrayOf(coord),0,0,1f,1f,101,0,InputDevice.SOURCE_JOYSTICK,0)
        }
        var failure:Throwable?=null
        i.runOnMainSync {try {
            val pad=GamepadInput(context,{if(it==GamepadInput.PAUSE) pauses++ else speeds++},{})
            input=pad
            check(pad.key(event(KeyEvent.KEYCODE_BUTTON_A,true)));check(pad.keys==1)
            pad.key(event(KeyEvent.KEYCODE_BUTTON_Y,true));pad.key(event(KeyEvent.KEYCODE_BUTTON_A,false));check(pad.keys==1)
            pad.key(event(KeyEvent.KEYCODE_BUTTON_Y,false));check(pad.keys==0)
            pad.key(event(KeyEvent.KEYCODE_BUTTON_B,true));pad.key(event(KeyEvent.KEYCODE_DPAD_RIGHT,true));check(pad.keys==18)
            motion(-1f,-1f,left=1f,right=1f).let {pad.motion(it);it.recycle()};check(pad.keys and (32 or 64 or 512 or 256)==(32 or 64 or 512 or 256))
            pad.clear();motion(.1f,-.1f).let {pad.motion(it);it.recycle()};check(pad.keys==0)
            motion(hatX=1f,hatY=1f).let {pad.motion(it);it.recycle()};check(pad.keys==144)
            motion().let {pad.motion(it);it.recycle()};check(pad.keys==0)
            pad.key(event(KeyEvent.KEYCODE_BUTTON_A,true,101));pad.key(event(KeyEvent.KEYCODE_BUTTON_A,true,102));pad.key(event(KeyEvent.KEYCODE_BUTTON_A,false,101));check(pad.keys==1)
            pad.onInputDeviceRemoved(102);check(pad.keys==0)
            pad.key(event(KeyEvent.KEYCODE_BUTTON_THUMBR,true));pad.key(event(KeyEvent.KEYCODE_BUTTON_THUMBR,true,repeat=1));pad.key(event(KeyEvent.KEYCODE_BUTTON_THUMBR,false));check(pauses==1)
            pad.key(event(KeyEvent.KEYCODE_BUTTON_THUMBL,true));pad.key(event(KeyEvent.KEYCODE_BUTTON_THUMBL,false));check(speeds==1)
            pad.bind(event(KeyEvent.KEYCODE_BUTTON_1,true),8);pad.key(event(KeyEvent.KEYCODE_BUTTON_1,true));check(pad.keys==8)
            pad.clear();check(pad.keys==0)
            check(!pad.key(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_UP)))
            pad.close();input=null
            val restored=GamepadInput(context,{},{});input=restored
            restored.key(event(KeyEvent.KEYCODE_BUTTON_1,true));check(restored.keys==8)
            restored.reset();restored.key(event(KeyEvent.KEYCODE_BUTTON_1,false));check(!restored.key(event(KeyEvent.KEYCODE_BUTTON_1,true)))
        }catch(t:Throwable){failure=t}finally{input?.close();settings.edit().clear().commit()}}
        failure?.let {throw it}
    }
}
