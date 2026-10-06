// SPDX-License-Identifier: MIT
// Copyright (c) 2026 BRUMA contributors
// Original BRUMA implementation of the existing runtime contract.
package com.hatkid.mkxpz.gamepad;
import android.content.Context;
import android.view.*;
import android.widget.RelativeLayout;
import com.linkcore.emulator.R;
/** Original composition/controller adapter; no dependency on the unlicensed port. */
public final class Gamepad {
    public interface OnKeyDownListener {void onKeyDown(int key);}
    public interface OnKeyUpListener {void onKeyUp(int key);}
    private OnKeyDownListener down=key->{};private OnKeyUpListener up=key->{};
    private GamepadConfig config=new GamepadConfig();private boolean invisible;
    private final RpgInput input=new RpgInput((key,pressed)->{if(pressed)down.onKeyDown(key);else up.onKeyUp(key);});
    private RelativeLayout mGamepadLayout;
    private GamepadDPad gpadDPad;
    private GamepadButton gpadBtnA,gpadBtnB,gpadBtnC,gpadBtnX,gpadBtnY,gpadBtnZ,gpadBtnL,gpadBtnR,gpadBtnCTRL,gpadBtnALT,gpadBtnSHIFT;
    public void setOnKeyDownListener(OnKeyDownListener l){down=l;}
    public void setOnKeyUpListener(OnKeyUpListener l){up=l;}
    public void init(GamepadConfig c,boolean hidden){config=c;invisible=hidden;}
    private int unit(Context c,int n){int id=c.getResources().getIdentifier("_"+n+"sdp","dimen",c.getPackageName());return c.getResources().getDimensionPixelSize(id);}
    private void bind(GamepadButton button,int key){
        button.setKey(key);button.setForegroundText(KeyEvent.keyCodeToString(key).replace("KEYCODE_","").replace("_LEFT","").replace("_RIGHT",""));
        button.setOnKeyDownListener(k->input.set("touch:"+button.getId(),k,true));button.setOnKeyUpListener(k->input.set("touch:"+button.getId(),k,false));
    }
    private GamepadButton button(Context c,int id,int key,boolean square){GamepadButton b=new GamepadButton(c);b.setId(id);b.setFocusable(false);b.setBackgroundResource(square?R.drawable.gamepad_button_square:R.drawable.gamepad_button_circle);b.setImageAlpha(Math.round(Math.min(config.opacity,100)*2.55f));bind(b,key);return b;}
    public void attachTo(Context c,ViewGroup parent){
        mGamepadLayout=new RelativeLayout(c);mGamepadLayout.setId(R.id.gamepad_layout);mGamepadLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        parent.addView(mGamepadLayout,new RelativeLayout.LayoutParams(-1,-1));
        gpadDPad=new GamepadDPad(c);gpadDPad.setId(R.id.dpad);gpadDPad.setBackgroundResource(R.drawable.gamepad_dpad_back);gpadDPad.setForegroundResource(R.drawable.gamepad_dpad_front);gpadDPad.setImageAlpha(Math.round(Math.min(config.opacity,100)*2.55f));gpadDPad.isDiagonal=config.diagonalMovement;
        gpadDPad.setOnKeyDownListener(k->input.set("touch:dpad",k,true));gpadDPad.setOnKeyUpListener(k->input.set("touch:dpad",k,false));
        gpadBtnA=button(c,R.id.button_A,config.keycodeA,false);gpadBtnB=button(c,R.id.button_B,config.keycodeB,false);gpadBtnC=button(c,R.id.button_C,config.keycodeC,false);
        gpadBtnX=button(c,R.id.button_X,config.keycodeX,false);gpadBtnY=button(c,R.id.button_Y,config.keycodeY,false);gpadBtnZ=button(c,R.id.button_Z,config.keycodeZ,false);
        gpadBtnL=button(c,R.id.button_L,config.keycodeL,false);gpadBtnR=button(c,R.id.button_R,config.keycodeR,false);
        gpadBtnCTRL=button(c,R.id.button_CTRL,config.keycodeCTRL,true);gpadBtnALT=button(c,R.id.button_ALT,config.keycodeALT,true);gpadBtnSHIFT=button(c,R.id.button_SHIFT,config.keycodeSHIFT,true);
        for(GamepadButton b:allButtons()){int w=unit(c,b==gpadDPad?110:(b==gpadBtnCTRL||b==gpadBtnALT||b==gpadBtnSHIFT?40:38));int h=unit(c,b==gpadDPad?110:(b==gpadBtnCTRL||b==gpadBtnALT||b==gpadBtnSHIFT?20:38));mGamepadLayout.addView(b,new RelativeLayout.LayoutParams(Math.round(w*config.scale/100f),Math.round(h*config.scale/100f)));}
        // Initial positions reproduce the existing XML's layout metrics. Saved positions still override them.
        mGamepadLayout.post(()->{
            int width=mGamepadLayout.getWidth(),height=mGamepadLayout.getHeight();
            gpadDPad.setX(unit(c,10));gpadDPad.setY(height-unit(c,10)-gpadDPad.getHeight());
            gpadBtnL.setX(unit(c,12));gpadBtnR.setX(width-unit(c,12)-gpadBtnR.getWidth());gpadBtnL.setY(unit(c,27));gpadBtnR.setY(unit(c,27));
            GamepadButton[] actions={gpadBtnX,gpadBtnY,gpadBtnZ,gpadBtnA,gpadBtnB,gpadBtnC};
            for(int n=0;n<actions.length;n++){actions[n].setX(width-unit(c,140)+unit(c,4)+unit(c,42)*(n%3));actions[n].setY(height-unit(c,98)+unit(c,n<3?2:48));}
            GamepadButton[] mods={gpadBtnCTRL,gpadBtnALT,gpadBtnSHIFT};
            for(int n=0;n<mods.length;n++){mods[n].setX((width-unit(c,145))/2f+unit(c,4)+unit(c,48)*n);mods[n].setY(height-unit(c,33)+unit(c,2));}
            setupEditor(c);
        });
        if(invisible)mGamepadLayout.setAlpha(0);
    }
    public void showView(){if(mGamepadLayout!=null){mGamepadLayout.clearAnimation();mGamepadLayout.setAlpha(1);mGamepadLayout.setVisibility(View.VISIBLE);}}
    public void hideView(){releaseAll();if(mGamepadLayout!=null){mGamepadLayout.clearAnimation();mGamepadLayout.setVisibility(View.INVISIBLE);}}
    public boolean processGamepadEvent(KeyEvent e){return input.key(e);}
    public boolean processDPadEvent(MotionEvent e){return input.motion(e);}
    public void releaseDevice(int id){input.release("device:"+id+":");}
    public void releaseAll(){for(GamepadButton b:allButtons())if(b!=null)b.release();input.release("");}
    private boolean editing=false;
    private android.content.SharedPreferences positions;
    private final java.util.Map<GamepadButton,float[]> defaults=new java.util.HashMap<>();
    private GamepadButton[] allButtons() {return new GamepadButton[]{gpadDPad,gpadBtnA,gpadBtnB,gpadBtnC,gpadBtnX,gpadBtnY,gpadBtnZ,gpadBtnL,gpadBtnR,gpadBtnCTRL,gpadBtnALT,gpadBtnSHIFT};}
    private void setupEditor(Context context) {
        positions=context.getSharedPreferences("rpg-controls",0);
        mGamepadLayout.post(()->{
            int[] root=new int[2];mGamepadLayout.getLocationOnScreen(root);
            for(GamepadButton button:allButtons()) {
                int[] xy=new int[2];button.getLocationOnScreen(xy);
                defaults.put(button,new float[]{(xy[0]-root[0])/(float)Math.max(1,mGamepadLayout.getWidth()-button.getWidth()),(xy[1]-root[1])/(float)Math.max(1,mGamepadLayout.getHeight()-button.getHeight())});
            }
            for(GamepadButton button:allButtons()) {
                int width=button.getWidth(),height=button.getHeight();
                ((ViewGroup)button.getParent()).removeView(button);
                mGamepadLayout.addView(button,new RelativeLayout.LayoutParams(width,height));
                final float[] drag=new float[4];
                button.setOnTouchListener((view,event)->{
                    if(!editing)return false;
                    switch(event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:drag[0]=event.getRawX();drag[1]=event.getRawY();drag[2]=view.getX();drag[3]=view.getY();break;
                        case MotionEvent.ACTION_MOVE:view.setX(Math.max(0,Math.min(mGamepadLayout.getWidth()-view.getWidth(),drag[2]+event.getRawX()-drag[0])));view.setY(Math.max(0,Math.min(mGamepadLayout.getHeight()-view.getHeight(),drag[3]+event.getRawY()-drag[1])));break;
                        case MotionEvent.ACTION_UP:positions.edit().putFloat(view.getId()+"x",view.getX()/Math.max(1,mGamepadLayout.getWidth()-view.getWidth())).putFloat(view.getId()+"y",view.getY()/Math.max(1,mGamepadLayout.getHeight()-view.getHeight())).apply();break;
                    }
                    return true;
                });
            }
            // The old empty layout groups no longer participate in hit testing.
            mGamepadLayout.post(this::restorePositions);
            mGamepadLayout.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol || b-t!=ob-ot)restorePositions();});
        });
    }
    private void restorePositions() {
        for(GamepadButton button:allButtons()) {
            float[] fallback=defaults.get(button);if(fallback==null)continue;
            button.setX(Math.max(0,mGamepadLayout.getWidth()-button.getWidth())*positions.getFloat(button.getId()+"x",fallback[0]));
            button.setY(Math.max(0,mGamepadLayout.getHeight()-button.getHeight())*positions.getFloat(button.getId()+"y",fallback[1]));
        }
    }
    public void edit(boolean active) {releaseAll();editing=active;if(active)showView();}
    public void resetPositions() {positions.edit().clear().apply();restorePositions();}
}
