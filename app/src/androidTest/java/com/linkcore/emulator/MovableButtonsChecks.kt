package com.linkcore.emulator
import android.app.Instrumentation
import android.view.*
import android.widget.*
object MovableButtonsChecks {
 fun run(test:Instrumentation) {test.runOnMainSync {
  val ctx=test.targetContext
  ctx.getSharedPreferences("utility-test",0).edit().clear().commit()
  val host=FrameLayout(ctx);host.layout(0,0,1000,600)
  val button=Button(ctx);host.addView(button);button.layout(0,0,100,50)
  var edit=true;var clicks=0
  button.setOnClickListener {clicks++}
  val positions=MovableButtons(ctx,host,"utility-test",{edit})
  positions.bind(button,"pause",-1,false);positions.restore()
  val beforeX=button.x;val beforeY=button.y
  fun touch(action:Int,x:Float,y:Float) {val e=MotionEvent.obtain(0,20,action,x,y,0);button.dispatchTouchEvent(e);e.recycle()}
  touch(0,20f,20f);touch(2,140f,90f);touch(1,140f,90f)
  check(button.x==beforeX+120f && button.y==beforeY+70f);check(clicks==0)
  val savedX=button.x;val savedY=button.y
  button.x=0f;button.y=0f;positions.restore();check(kotlin.math.abs(button.x-savedX)<0.01f && kotlin.math.abs(button.y-savedY)<0.01f)
  touch(0,20f,20f);touch(2,-10000f,-10000f);touch(1,-10000f,-10000f);check(button.x==0f && button.y==0f)
  touch(0,20f,20f);touch(2,100f,100f);touch(3,100f,100f);check(button.x==0f && button.y==0f)
  positions.reset();check(button.x==beforeX && button.y==beforeY)

  val options=Button(ctx);host.addView(options);options.layout(0,0,100,50);options.setOnClickListener {clicks++}
  edit=true;positions.bind(options,"options",1,true);positions.restore()
  val down=MotionEvent.obtain(0,0,0,20f,20f,0);val up=MotionEvent.obtain(0,20,1,20f,20f,0)
  options.dispatchTouchEvent(down);options.dispatchTouchEvent(up);down.recycle();up.recycle();check(clicks==1)
  ctx.getSharedPreferences("utility-test",0).edit().clear().commit()
 }}
}

