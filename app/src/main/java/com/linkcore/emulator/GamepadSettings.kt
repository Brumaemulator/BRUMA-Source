package com.linkcore.emulator

import android.app.Activity
import android.app.AlertDialog
import android.view.KeyEvent
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class GamepadSettings(private val activity:Activity,private val input:GamepadInput) {
    private var dialog:AlertDialog?=null
    fun show() {
        dialog?.dismiss()
        val box=LinearLayout(activity).apply {orientation=LinearLayout.VERTICAL;setPadding(28,16,28,16)}
        val devices=input.devices()
        box.addView(TextView(activity).apply {text=if(devices.isEmpty()) activity.getString(R.string.pad_none) else devices.joinToString("\n") {it.name}})
        box.addView(TextView(activity).apply {setText(R.string.pad_help);setPadding(0,20,0,20)})
        box.addView(Look.button(activity,activity.getString(R.string.pad_assign)) {choose()})
        box.addView(Look.button(activity,activity.getString(R.string.pad_reset)) {input.reset();show()})
        dialog=AlertDialog.Builder(activity).setTitle(R.string.pad_title).setView(ScrollView(activity).apply {addView(box)}).setPositiveButton(android.R.string.ok,null).show()
    }
    private fun choose() {
        dialog?.dismiss()
        val actions=intArrayOf(1,2,512,256,8,4,64,128,32,16,GamepadInput.PAUSE,GamepadInput.SPEED)
        val labels=arrayOf("A","B","L","R","START","SELECT",activity.getString(R.string.pad_up),activity.getString(R.string.pad_down),activity.getString(R.string.pad_left),activity.getString(R.string.pad_right),activity.getString(R.string.pad_pause),activity.getString(R.string.pad_speed))
        dialog=AlertDialog.Builder(activity).setTitle(R.string.pad_assign).setItems(labels) {_,n->capture(actions[n],labels[n])}.setNegativeButton(android.R.string.cancel) {_,_->show()}.show()
    }
    private fun capture(action:Int,label:String) {
        val capture=AlertDialog.Builder(activity).setTitle(label).setMessage(R.string.pad_press).setNegativeButton(android.R.string.cancel) {_,_->show()}.create()
        var pressed:KeyEvent?=null
        capture.setOnKeyListener {_,_,event->
            if(!GamepadInput.controller(event)) false else {
                if(event.action==KeyEvent.ACTION_DOWN && event.repeatCount==0) pressed=KeyEvent(event)
                if(event.action==KeyEvent.ACTION_UP && pressed?.keyCode==event.keyCode) {
                    input.bind(pressed!!,action);capture.dismiss();show()
                }
                true
            }
        }
        dialog=capture;capture.show()
    }
    fun close() {dialog?.dismiss()}
}
