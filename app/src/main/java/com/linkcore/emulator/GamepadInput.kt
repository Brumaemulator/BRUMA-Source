package com.linkcore.emulator

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs

/** Android supplies the same input events for supported USB and Bluetooth controllers. */
class GamepadInput(private val context:Context,private val shortcut:(Int)->Unit,private val disconnected:()->Unit,private val devicesChanged:(Boolean)->Unit = {},private val controllerActivity:()->Unit = {}):InputManager.InputDeviceListener {
    companion object {
        const val PAUSE=1024
        const val SPEED=2048
        fun defaults(code:Int):Int=when(code) {
            KeyEvent.KEYCODE_BUTTON_A,KeyEvent.KEYCODE_BUTTON_Y->1
            KeyEvent.KEYCODE_BUTTON_B,KeyEvent.KEYCODE_BUTTON_X->2
            KeyEvent.KEYCODE_BUTTON_SELECT->4
            KeyEvent.KEYCODE_BUTTON_START->8
            KeyEvent.KEYCODE_DPAD_RIGHT->16
            KeyEvent.KEYCODE_DPAD_LEFT->32
            KeyEvent.KEYCODE_DPAD_UP->64
            KeyEvent.KEYCODE_DPAD_DOWN->128
            KeyEvent.KEYCODE_BUTTON_R1,KeyEvent.KEYCODE_BUTTON_R2->256
            KeyEvent.KEYCODE_BUTTON_L1,KeyEvent.KEYCODE_BUTTON_L2->512
            KeyEvent.KEYCODE_BUTTON_THUMBR,KeyEvent.KEYCODE_BACK->PAUSE
            KeyEvent.KEYCODE_BUTTON_THUMBL->SPEED
            else->0
        }
        fun controller(event:android.view.InputEvent):Boolean = event.isFromSource(InputDevice.SOURCE_GAMEPAD) || event.isFromSource(InputDevice.SOURCE_JOYSTICK) || event.isFromSource(InputDevice.SOURCE_DPAD) || event.device?.let {it.supportsSource(InputDevice.SOURCE_GAMEPAD) || it.supportsSource(InputDevice.SOURCE_JOYSTICK)}==true
        fun directions(x:Float,y:Float,dead:Float):Int = (if(x>dead) 16 else if(x< -dead) 32 else 0) or (if(y>dead) 128 else if(y< -dead) 64 else 0)
    }
    private val prefs=context.getSharedPreferences("gamepad-mapping",0)
    private val manager=context.getSystemService(InputManager::class.java)
    private val held=mutableMapOf<Pair<Int,Int>,Int>()
    private val axes=mutableMapOf<Int,Int>()
    private val connected=mutableSetOf<Int>()
    @Volatile var keys=0
        private set
    init {manager.registerInputDeviceListener(this,Handler(Looper.getMainLooper()));devices().forEach {connected+=it.id};devicesChanged(connected.isNotEmpty())}
    fun devices():List<InputDevice> = InputDevice.getDeviceIds().toList().mapNotNull {InputDevice.getDevice(it)}.filter {it.supportsSource(InputDevice.SOURCE_GAMEPAD) || it.supportsSource(InputDevice.SOURCE_JOYSTICK) || it.supportsSource(InputDevice.SOURCE_DPAD)}
    private fun profile(device:InputDevice?):String=LibraryStore.key(device?.descriptor ?: "generic")
    fun bind(event:KeyEvent,action:Int) {prefs.edit().putInt(profile(event.device)+":"+event.keyCode,action).apply();clear()}
    fun reset() {prefs.edit().clear().apply();clear()}
    fun clear() {held.clear();axes.clear();keys=0}
    private fun publish() {keys=(held.values+axes.values).fold(0) {a,b->a or b} and 1023}
    fun key(event:KeyEvent):Boolean {
        if(!controller(event)) return false
        if(event.action==KeyEvent.ACTION_DOWN) controllerActivity()
        val token=event.deviceId to event.keyCode
        if(event.action==KeyEvent.ACTION_UP) {val old=held.remove(token);publish();return old!=null || prefs.getInt(profile(event.device)+":"+event.keyCode,defaults(event.keyCode))!=0}
        if(event.action!=KeyEvent.ACTION_DOWN) return false
        val action=prefs.getInt(profile(event.device)+":"+event.keyCode,defaults(event.keyCode))
        if(action==0) return false
        if(event.repeatCount==0 && !held.containsKey(token)) {held[token]=action;if(action==PAUSE || action==SPEED) shortcut(action)}
        publish();return true
    }
    fun motion(event:MotionEvent):Boolean {
        if(!event.isFromSource(InputDevice.SOURCE_JOYSTICK)) return false
        if(event.actionMasked==MotionEvent.ACTION_CANCEL) {axes.remove(event.deviceId);publish();return true}
        if(event.actionMasked!=MotionEvent.ACTION_MOVE) return false
        fun axis(code:Int)=event.getAxisValue(code)
        fun flat(code:Int)=maxOf(.22f,event.device?.getMotionRange(code,event.source)?.flat ?: 0f)
        val stick=directions(axis(MotionEvent.AXIS_X),axis(MotionEvent.AXIS_Y),maxOf(flat(MotionEvent.AXIS_X),flat(MotionEvent.AXIS_Y)))
        val hat=directions(axis(MotionEvent.AXIS_HAT_X),axis(MotionEvent.AXIS_HAT_Y),.5f)
        val left=maxOf(axis(MotionEvent.AXIS_LTRIGGER),axis(MotionEvent.AXIS_BRAKE))>.5f
        val right=maxOf(axis(MotionEvent.AXIS_RTRIGGER),axis(MotionEvent.AXIS_GAS))>.5f
        val current=stick or hat or (if(left) 512 else 0) or (if(right) 256 else 0)
        axes[event.deviceId]=current
        if(current!=0) controllerActivity()
        publish();return true
    }
    override fun onInputDeviceAdded(id:Int) {if(devices().any {it.id==id}) connected+=id;devicesChanged(connected.isNotEmpty())}
    override fun onInputDeviceChanged(id:Int) {held.keys.removeAll {it.first==id};axes.remove(id);connected.remove(id);publish();onInputDeviceAdded(id)}
    override fun onInputDeviceRemoved(id:Int) {held.keys.removeAll {it.first==id};axes.remove(id);publish();if(connected.remove(id)) disconnected();devicesChanged(connected.isNotEmpty())}
    fun close() {clear();manager.unregisterInputDeviceListener(this)}
}
