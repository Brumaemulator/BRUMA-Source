// SPDX-License-Identifier: MIT
// Copyright (c) 2026 BRUMA contributors
// Original BRUMA implementation of the existing runtime contract.
// SPDX-License-Identifier: MIT
// Copyright (c) 2026 BRUMA contributors
// Original BRUMA implementation of the existing runtime contract.
package com.hatkid.mkxpz.gamepad;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

/** A held direction belongs to one finger until that finger is lifted. */
public class GamepadDPad extends GamepadButton {
    public Boolean isDiagonal = false;
    private int pointer = -1, held = 0;
    private OnKeyDownListener down = key -> {};
    private OnKeyUpListener up = key -> {};
    private static final int[] KEYS = {KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN};
    public GamepadDPad(Context c) {super(c);}
    public GamepadDPad(Context c, AttributeSet a) {super(c,a);}
    public GamepadDPad(Context c, AttributeSet a,int s) {super(c,a,s);}
    public GamepadDPad(Context c, AttributeSet a,int s,int r) {super(c,a,s,r);}
    @Override public void setOnKeyDownListener(OnKeyDownListener listener) {down=listener;}
    @Override public void setOnKeyUpListener(OnKeyUpListener listener) {up=listener;}
    private void update(int next) {
        for(int i=0;i<KEYS.length;i++) if((held & (1<<i))!=0 && (next & (1<<i))==0) up.onKeyUp(KEYS[i]);
        for(int i=0;i<KEYS.length;i++) if((held & (1<<i))==0 && (next & (1<<i))!=0) down.onKeyDown(KEYS[i]);
        held=next;
    }
    @Override public void release() {update(0);pointer=-1;}
    @Override public boolean onTouchEvent(MotionEvent event) {
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN) pointer=event.getPointerId(0);
        if(action==MotionEvent.ACTION_CANCEL || action==MotionEvent.ACTION_UP ||
            (action==MotionEvent.ACTION_POINTER_UP && event.getPointerId(event.getActionIndex())==pointer)) {
            release();return true;
        }
        int index=event.findPointerIndex(pointer);
        if(index<0) return true;
        float x=(event.getX(index)-getWidth()/2f)/(getWidth()/2f);
        float y=(event.getY(index)-getHeight()/2f)/(getHeight()/2f);
        int next=0;
        if(Math.max(Math.abs(x),Math.abs(y))>.20f) {
            if(Boolean.TRUE.equals(isDiagonal)) {
                if(Math.abs(x)>.4f) next|=x<0?1:2;
                if(Math.abs(y)>.4f) next|=y<0?4:8;
            }
            if(next==0) next=Math.abs(x)>Math.abs(y)?(x<0?1:2):(y<0?4:8);
        }
        update(next);
        return true;
    }
}
