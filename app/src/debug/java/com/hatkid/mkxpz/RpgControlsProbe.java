package com.hatkid.mkxpz;
import android.content.*;
import android.view.*;
import android.widget.*;
import org.libsdl.app.SDLActivity;
/** Debug-only driver restricted to the Android shell's DUMP permission. */
public class RpgControlsProbe extends BroadcastReceiver {
    public void onReceive(Context context,Intent intent) {
        if(intent.getBooleanExtra("launch",false)) {
            String path=intent.hasExtra("path")?intent.getStringExtra("path"):context.getSharedPreferences("rpg",0).getString("path","");
            context.startActivity(new Intent(context,RpgLibraryActivity.class).putExtra("launchUri",intent.hasExtra("uri")?intent.getStringExtra("uri"):android.net.Uri.fromFile(new java.io.File(path)).toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        if(!(SDLActivity.getContext() instanceof MainActivity)) return;
        MainActivity activity=(MainActivity)SDLActivity.getContext();
        if(intent.hasExtra("option")) {
            try {java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod("applyRpgOption",int.class);method.setAccessible(true);method.invoke(activity,intent.getIntExtra("option",0));}catch(Exception e){throw new RuntimeException(e);}
            return;
        }
        if(intent.hasExtra("drag")) {
            View pad=intent.hasExtra("dragLabel")?find(activity.getWindow().getDecorView(),intent.getStringExtra("dragLabel")):activity.findViewById(com.linkcore.emulator.R.id.dpad);
            int[] loc=new int[2];pad.getLocationOnScreen(loc);
            float x=loc[0]+pad.getWidth()/2f,y=loc[1]+pad.getHeight()/2f;long now=android.os.SystemClock.uptimeMillis();
            for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP}) {
                MotionEvent event=MotionEvent.obtain(now,now+20,action,x+(action==0?0:120),y-(action==0?0:70),0);activity.dispatchTouchEvent(event);event.recycle();
            }
            return;
        }
        if(intent.hasExtra("stick")) {
            float x=intent.getStringExtra("stick").equals("right")?1f:0f;
            float y=intent.getStringExtra("stick").equals("up")?-1f:0f;
            stick(activity,x,y);
            activity.getWindow().getDecorView().postDelayed(()->stick(activity,0,0),intent.getIntExtra("duration",500));
            return;
        }
        if(intent.hasExtra("padButton")) {
            int key=intent.getIntExtra("padButton",KeyEvent.KEYCODE_BUTTON_A);
            activity.dispatchKeyEvent(new KeyEvent(0,0,KeyEvent.ACTION_DOWN,key,0,0,999,0,0,InputDevice.SOURCE_GAMEPAD));
            activity.getWindow().getDecorView().postDelayed(()->activity.dispatchKeyEvent(new KeyEvent(0,0,KeyEvent.ACTION_UP,key,0,0,999,0,0,InputDevice.SOURCE_GAMEPAD)),100);
            return;
        }
        if(intent.hasExtra("direction")) {
            View pad=intent.hasExtra("dragLabel")?find(activity.getWindow().getDecorView(),intent.getStringExtra("dragLabel")):activity.findViewById(com.linkcore.emulator.R.id.dpad);
            int[] location=new int[2];pad.getLocationOnScreen(location);
            String direction=intent.getStringExtra("direction");
            float x=location[0]+pad.getWidth()*(direction.equals("left")?.15f:direction.equals("right")?.85f:.5f);
            float y=location[1]+pad.getHeight()*(direction.equals("up")?.15f:direction.equals("down")?.85f:.5f);
            long time=android.os.SystemClock.uptimeMillis();
            MotionEvent down=MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,x,y,0);
            activity.dispatchTouchEvent(down);down.recycle();
            pad.postDelayed(()->{MotionEvent up=MotionEvent.obtain(time,android.os.SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x,y,0);activity.dispatchTouchEvent(up);up.recycle();},intent.getIntExtra("duration",900));
            return;
        }
        String label=intent.getStringExtra("button");
        click(activity.getWindow().getDecorView(),label);
    }
    private void stick(MainActivity activity,float x,float y) {
        MotionEvent.PointerProperties props=new MotionEvent.PointerProperties();props.id=0;
        MotionEvent.PointerCoords coords=new MotionEvent.PointerCoords();coords.setAxisValue(MotionEvent.AXIS_X,x);coords.setAxisValue(MotionEvent.AXIS_Y,y);
        MotionEvent event=MotionEvent.obtain(0,android.os.SystemClock.uptimeMillis(),MotionEvent.ACTION_MOVE,1,new MotionEvent.PointerProperties[]{props},new MotionEvent.PointerCoords[]{coords},0,0,1,1,999,0,InputDevice.SOURCE_JOYSTICK,0);
        activity.dispatchGenericMotionEvent(event);event.recycle();
    }
    private View find(View view,String label) {
        if(view instanceof Button && ((Button)view).getText().toString().equals(label))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View result=find(((ViewGroup)view).getChildAt(i),label);if(result!=null)return result;}
        return null;
    }
    private boolean click(View view,String label) {
        if(view instanceof Button && ((Button)view).getText().toString().equals(label)) return view.performClick();
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++)
            if(click(((ViewGroup)view).getChildAt(i),label))return true;
        return false;
    }
}





