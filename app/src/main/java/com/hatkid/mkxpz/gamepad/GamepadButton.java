// SPDX-License-Identifier: MIT
// Copyright (c) 2026 BRUMA contributors
// Original BRUMA implementation of the existing runtime contract.
package com.hatkid.mkxpz.gamepad;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.widget.ImageView;
/** Original renderer/input view. Appearance metrics are the existing BRUMA contract. */
public class GamepadButton extends ImageView {
    public interface OnKeyDownListener {void onKeyDown(int key);}
    public interface OnKeyUpListener {void onKeyUp(int key);}
    private OnKeyDownListener down=key->{};private OnKeyUpListener up=key->{};
    private int key,owner=-1,opacity=255,textPixels=36;
    private Drawable plate,symbol;private String label;
    private Paint lettering;
    public GamepadButton(Context context){super(context);}
    public GamepadButton(Context c,AttributeSet a){super(c,a);}
    public GamepadButton(Context c,AttributeSet a,int s){super(c,a,s);}
    public GamepadButton(Context c,AttributeSet a,int s,int r){super(c,a,s,r);}
    public void setOnKeyDownListener(OnKeyDownListener l){down=l;}
    public void setOnKeyUpListener(OnKeyUpListener l){up=l;}
    public void setKey(int code){key=code;invalidate();}
    @Override public void setBackground(Drawable d){plate=d;invalidate();}
    @Override public void setBackgroundResource(int id){setBackground(getContext().getDrawable(id));}
    @Override public void setForeground(Drawable d){symbol=d;label=null;invalidate();}
    public void setForegroundResource(int id){setForeground(getContext().getDrawable(id));}
    public void setForegroundText(String text){label=text;symbol=null;invalidate();}
    @Override public void setImageAlpha(int alpha){opacity=Math.max(0,Math.min(255,alpha));invalidate();}
    public void initBitmap(){invalidate();}
    @Override protected void onDraw(Canvas canvas){
        int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
        int save=canvas.saveLayerAlpha(0,0,w,h,opacity);
        if(plate!=null){plate.setBounds(0,0,w,h);plate.draw(canvas);}
        if(symbol!=null){int sw=Math.round(w*.9f),sh=Math.round(h*.9f);symbol.setBounds((w-sw)/2,(h-sh)/2,(w-sw)/2+sw,(h-sh)/2+sh);symbol.draw(canvas);}
        if(label!=null){
            if(lettering==null){lettering=new Paint(Paint.ANTI_ALIAS_FLAG);lettering.setColor(Color.WHITE);lettering.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));lettering.setTextAlign(Paint.Align.CENTER);}
            lettering.setTextSize(textPixels);
            while(textPixels>2&&(lettering.measureText(label)+.5f>w*.85f||lettering.getFontMetricsInt(null)>h*.85f)){textPixels-=2;lettering.setTextSize(textPixels);}
            canvas.drawText(label,w/2,h/2-lettering.ascent()/2f,lettering);
        }
        canvas.restoreToCount(save);
    }
    public void release(){if(owner>=0){owner=-1;setPressed(false);up.onKeyUp(key);}}
    @Override protected void onDetachedFromWindow(){release();super.onDetachedFromWindow();}
    @Override public boolean onTouchEvent(MotionEvent e){
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:if(owner<0){owner=e.getPointerId(e.getActionIndex());setPressed(true);down.onKeyDown(key);}break;
            case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:release();break;
            case MotionEvent.ACTION_POINTER_UP:if(e.getPointerId(e.getActionIndex())==owner)release();break;
        }
        return true;
    }
}
