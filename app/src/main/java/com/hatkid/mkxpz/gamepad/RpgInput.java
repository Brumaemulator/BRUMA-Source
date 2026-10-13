// SPDX-License-Identifier: MIT
// Copyright (c) 2026 BRUMA contributors
// Original BRUMA implementation of the existing runtime contract.
// SPDX-License-Identifier: MIT
// Copyright (c) 2026 BRUMA contributors
// Original BRUMA implementation of the existing runtime contract.
package com.hatkid.mkxpz.gamepad;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.util.*;

/** Merge independent input sources so releasing one never cancels another. */
public final class RpgInput {
    public interface Sink {void send(int key,boolean pressed);}
    private final Sink sink;
    private final Map<String,Set<Integer>> sources=new HashMap<>();
    public RpgInput(Sink sink) {this.sink=sink;}
    public void set(String source,int key,boolean pressed) {
        boolean before=isHeld(key);
        Set<Integer> keys=sources.computeIfAbsent(source,k->new HashSet<>());
        if(pressed) keys.add(key); else keys.remove(key);
        if(keys.isEmpty()) sources.remove(source);
        boolean after=isHeld(key);
        if(before!=after) sink.send(key,after);
    }
    private boolean isHeld(int key) {for(Set<Integer> keys:sources.values()) if(keys.contains(key)) return true;return false;}
    public void release(String prefix) {
        for(String source:new ArrayList<>(sources.keySet())) if(source.startsWith(prefix))
            for(int key:new HashSet<>(sources.get(source))) set(source,key,false);
    }
    public static int mapping(int key) {
        switch(key) {
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_DPAD_RIGHT: return key;
            case KeyEvent.KEYCODE_BUTTON_A: case KeyEvent.KEYCODE_DPAD_CENTER: return KeyEvent.KEYCODE_C;
            case KeyEvent.KEYCODE_BUTTON_B: return KeyEvent.KEYCODE_X;
            case KeyEvent.KEYCODE_BUTTON_X: return KeyEvent.KEYCODE_Z;
            case KeyEvent.KEYCODE_BUTTON_Y: return KeyEvent.KEYCODE_S;
            case KeyEvent.KEYCODE_BUTTON_L1: return KeyEvent.KEYCODE_Q;
            case KeyEvent.KEYCODE_BUTTON_R1: return KeyEvent.KEYCODE_W;
            case KeyEvent.KEYCODE_BUTTON_START: return KeyEvent.KEYCODE_ENTER;
            case KeyEvent.KEYCODE_BUTTON_SELECT: return KeyEvent.KEYCODE_ESCAPE;
            case KeyEvent.KEYCODE_BUTTON_L2: return KeyEvent.KEYCODE_SHIFT_LEFT;
            case KeyEvent.KEYCODE_BUTTON_R2: return KeyEvent.KEYCODE_CTRL_LEFT;
            default:return 0;
        }
    }
    public boolean key(KeyEvent e) {
        if(!e.isFromSource(InputDevice.SOURCE_GAMEPAD) && !e.isFromSource(InputDevice.SOURCE_DPAD) &&
            !e.isFromSource(InputDevice.SOURCE_JOYSTICK)) return false;
        int mapped=mapping(e.getKeyCode());if(mapped==0) return false;
        if(e.getAction()==KeyEvent.ACTION_DOWN || e.getAction()==KeyEvent.ACTION_UP)
            set("device:"+e.getDeviceId()+":key:"+e.getKeyCode(),mapped,e.getAction()==KeyEvent.ACTION_DOWN && !e.isCanceled());
        return true;
    }
    private float axis(MotionEvent e,int axis) {
        float value=e.getAxisValue(axis),dead=.25f;
        InputDevice device=e.getDevice();
        InputDevice.MotionRange range=device==null?null:device.getMotionRange(axis,e.getSource());
        if(range!=null) dead=Math.max(dead,range.getFlat());
        return Math.abs(value)>dead?value:0;
    }
    public boolean motion(MotionEvent e) {
        if(!e.isFromSource(InputDevice.SOURCE_JOYSTICK) && !e.isFromSource(InputDevice.SOURCE_DPAD)) return false;
        String id="device:"+e.getDeviceId()+":axis:";
        if(e.getActionMasked()==MotionEvent.ACTION_CANCEL) {release(id);return true;}
        if(e.getActionMasked()!=MotionEvent.ACTION_MOVE) return false;
        // Keep hat and stick independent, including their return to centre.
        direction(id+"hat",axis(e,MotionEvent.AXIS_HAT_X),axis(e,MotionEvent.AXIS_HAT_Y));
        direction(id+"stick",axis(e,MotionEvent.AXIS_X),axis(e,MotionEvent.AXIS_Y));
        set(id+"lt",KeyEvent.KEYCODE_SHIFT_LEFT,axis(e,MotionEvent.AXIS_LTRIGGER)>.5f);
        set(id+"rt",KeyEvent.KEYCODE_CTRL_LEFT,axis(e,MotionEvent.AXIS_RTRIGGER)>.5f);
        return true;
    }
    private void direction(String id,float x,float y) {
        set(id,KeyEvent.KEYCODE_DPAD_LEFT,x<-.35f);set(id,KeyEvent.KEYCODE_DPAD_RIGHT,x>.35f);
        set(id,KeyEvent.KEYCODE_DPAD_UP,y<-.35f);set(id,KeyEvent.KEYCODE_DPAD_DOWN,y>.35f);
    }
}

