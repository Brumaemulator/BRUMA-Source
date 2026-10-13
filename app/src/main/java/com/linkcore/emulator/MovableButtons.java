package com.linkcore.emulator;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Positions utility buttons without letting edit gestures trigger game actions. */
public final class MovableButtons {
    private final ViewGroup host;
    private final SharedPreferences prefs;
    private final BooleanSupplier editing;
    private final List<Runnable> restorers=new ArrayList<>();
    public MovableButtons(Context context,ViewGroup host,String namespace,BooleanSupplier editing) {
        this.host=host;this.editing=editing;prefs=context.getSharedPreferences(namespace,0);
        host.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol || b-t!=ob-ot)restore();});
    }
    private String key(String name){return name+":"+(host.getWidth()>host.getHeight()?"landscape":"portrait");}
    private float clamp(float n,float max){return Math.max(0,Math.min(Math.max(0,max),n));}
    public void bind(View button,String name,int slot,boolean allowEditClick) {
        Runnable restore=()->{
            if(host.getWidth()==0 || button.getWidth()==0)return;
            float w=Math.max(0,host.getWidth()-button.getWidth()),h=Math.max(0,host.getHeight()-button.getHeight());
            float defaultX=clamp(w/2+slot*(button.getWidth()+8*host.getResources().getDisplayMetrics().density)/2,w);
            button.setX(clamp(prefs.getFloat(key(name)+"x",w>0?defaultX/w:0)*w,w));
            button.setY(clamp(prefs.getFloat(key(name)+"y",h>0?10*host.getResources().getDisplayMetrics().density/h:0)*h,h));
        };
        restorers.add(restore);host.post(restore);
        final float[] start=new float[4];final boolean[] moved={false};final int[] pointer={-1};
        int slop=ViewConfiguration.get(host.getContext()).getScaledTouchSlop();
        button.setOnTouchListener((view,event)->{
            if(!editing.getAsBoolean())return false;
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){pointer[0]=event.getPointerId(0);start[0]=event.getRawX();start[1]=event.getRawY();start[2]=view.getX();start[3]=view.getY();moved[0]=false;return true;}
            if(action==MotionEvent.ACTION_CANCEL){pointer[0]=-1;restore.run();return true;}
            if(pointer[0]<0)return true;
            if(action==MotionEvent.ACTION_MOVE && event.findPointerIndex(pointer[0])==0){
                float dx=event.getRawX()-start[0],dy=event.getRawY()-start[1];
                moved[0]|=Math.hypot(dx,dy)>slop;
                if(moved[0]){view.setX(clamp(start[2]+dx,host.getWidth()-view.getWidth()));view.setY(clamp(start[3]+dy,host.getHeight()-view.getHeight()));}
            }
            if(action==MotionEvent.ACTION_UP || (action==MotionEvent.ACTION_POINTER_UP && event.getPointerId(event.getActionIndex())==pointer[0])) {
                if(moved[0])prefs.edit().putFloat(key(name)+"x",view.getX()/Math.max(1,host.getWidth()-view.getWidth())).putFloat(key(name)+"y",view.getY()/Math.max(1,host.getHeight()-view.getHeight())).apply();
                else if(allowEditClick)view.performClick();
                pointer[0]=-1;
            }
            return true;
        });
    }
    public void restore(){for(Runnable r:restorers)r.run();}
    public void reset(){prefs.edit().clear().apply();restore();}
}
