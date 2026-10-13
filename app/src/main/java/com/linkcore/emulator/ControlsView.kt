package com.linkcore.emulator

import android.content.Context
import android.graphics.*
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

class ControlsView(context: Context) : View(context) {
    @Volatile var keys=0
        private set
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private val density=resources.displayMetrics.density
    private data class Pad(val label:String,val mask:Int,val rect:RectF,val round:Boolean=false)
    private val pads=mutableListOf<Pad>()
    private var cross=RectF()
    private val crossPath=Path()
    private var portrait=false
    var editing=false
        private set
    private val positions=context.getSharedPreferences("controls",0)
    private var selected:RectF?=null
    private var lastX=0f
    private var lastY=0f
    private fun panelTop(screen:RectF,h:Int):Float = minOf(screen.bottom+152*density, h-296*density).coerceAtLeast(screen.bottom+32*density)
    fun edit(enabled:Boolean) { editing=enabled; clearKeys(); invalidate() }
    fun resetPositions() { positions.edit().clear().apply(); onSizeChanged(width,height,width,height); invalidate() }
    private fun storePositions() {
        val edit=positions.edit()
        (listOf("DPAD" to cross)+pads.map { it.label to it.rect }).forEach { (id,r) ->
            edit.putFloat(id+"x",r.centerX()/width).putFloat(id+"y",r.centerY()/height)
        }
        edit.apply()
    }

    init { contentDescription=context.getString(R.string.ui_62) }

    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) {
        fun rect(x:Float,y:Float,width:Float,height:Float)=RectF(x,y,x+width*density,y+height*density)
        pads.clear()
        portrait=h>w
        if(portrait) {
            val screen=GameLayout.viewport(w,h,density)
            val panelTop=panelTop(screen,h)
            val area=minOf(h-panelTop-16*density,280*density)
            val cy=panelTop+area*.48f
            val padSize=minOf(112*density,w*.30f)
            cross=RectF(24*density,cy-padSize/2,24*density+padSize,cy+padSize/2)
            pads+=Pad("A",1,rect(w-83*density,cy-47*density,56f,56f),true)
            pads+=Pad("B",2,rect(w-147*density,cy+6*density,56f,56f),true)
            pads+=Pad("L",512,rect(28*density,panelTop+18*density,76f,34f))
            pads+=Pad("R",256,rect(w-104*density,panelTop+18*density,76f,34f))
            val bottomY=minOf(cy+86*density,h-60*density)
            pads+=Pad("SELECT",4,rect(w/2f-82*density,bottomY,70f,30f))
            pads+=Pad("START",8,rect(w/2f+12*density,bottomY,70f,30f))
        } else {
            cross=rect(20*density,h-184*density,144f,144f)
            pads+=Pad("A",1,rect(w-100*density,h-200*density,72f,72f),true)
            pads+=Pad("B",2,rect(w-184*density,h-128*density,72f,72f),true)
            pads+=Pad("L",512,rect(24*density,18*density,90f,42f))
            pads+=Pad("R",256,rect(w-114*density,18*density,90f,42f))
            pads+=Pad("SELECT",4,rect(w/2f-88*density,h-44*density,76f,30f))
            pads+=Pad("START",8,rect(w/2f+12*density,h-44*density,76f,30f))
        }
        if(!portrait) {
            (listOf("DPAD" to cross)+pads.map { it.label to it.rect }).forEach { (id,r) ->
                if(positions.contains(id+"x")) {
                    val x=(positions.getFloat(id+"x",.5f)*w-r.width()/2).coerceIn(0f,maxOf(0f,w-r.width()))
                    val y=(positions.getFloat(id+"y",.5f)*h-r.height()/2).coerceIn(0f,maxOf(0f,h-r.height()))
                    r.offsetTo(x,y)
                }
            }
        }
        clearKeys()
        rebuildCross()
    }
    private fun rebuildCross() {
        val l=cross.left; val t=cross.top; val a=cross.width()/3
        crossPath.reset()
        crossPath.moveTo(l+a,t); crossPath.lineTo(l+2*a,t); crossPath.lineTo(l+2*a,t+a)
        crossPath.lineTo(l+3*a,t+a); crossPath.lineTo(l+3*a,t+2*a); crossPath.lineTo(l+2*a,t+2*a)
        crossPath.lineTo(l+2*a,t+3*a); crossPath.lineTo(l+a,t+3*a); crossPath.lineTo(l+a,t+2*a)
        crossPath.lineTo(l,t+2*a); crossPath.lineTo(l,t+a); crossPath.lineTo(l+a,t+a); crossPath.close()
    }

    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        if(portrait) {
            val screen=GameLayout.viewport(width,height,density)
            paint.style=Paint.Style.FILL;paint.color=Look.bg
            canvas.drawRect(0f,0f,width.toFloat(),screen.top-8*density,paint)
            canvas.drawRect(0f,screen.bottom+8*density,width.toFloat(),height.toFloat(),paint)
            val panelTop=panelTop(screen,height)
            val panelBottom=minOf(height-16*density,panelTop+280*density)
            paint.shader=LinearGradient(0f,screen.bottom,0f,panelBottom,0xff222839.toInt(),0xff141827.toInt(),Shader.TileMode.CLAMP)
            canvas.drawRoundRect(8*density,panelTop,width-8*density,panelBottom,28*density,28*density,paint)
            paint.shader=null
            paint.style=Paint.Style.STROKE;paint.color=0xff384157.toInt();paint.strokeWidth=3*density
            canvas.drawRoundRect(screen.left-1*density,screen.top-3*density,screen.right+1*density,screen.bottom+3*density,8*density,8*density,paint)
            paint.style=Paint.Style.FILL;paint.color=Look.mint
            canvas.drawCircle(32*density,screen.top-22*density,3*density,paint)
            paint.color=Look.muted;paint.typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);paint.textSize=9*density;paint.textAlign=Paint.Align.LEFT
            canvas.drawText("POWER",43*density,screen.top-19*density,paint)
            paint.color=0xff333b51.toInt();paint.strokeWidth=4*density;paint.strokeCap=Paint.Cap.ROUND
            for(n in 0..4) canvas.drawLine(width-(94-n*12)*density,panelBottom-25*density,width-(82-n*12)*density,panelBottom-13*density,paint)
        }
        paint.style=Paint.Style.FILL
        paint.pathEffect=CornerPathEffect(8*density)
        paint.color=if(portrait) 0xff101523.toInt() else 0x80101523.toInt()
        canvas.drawPath(crossPath,paint)
        canvas.save(); canvas.clipPath(crossPath)
        val a=cross.width()/3
        val cells=listOf(Triple(64,1,0),Triple(128,1,2),Triple(32,0,1),Triple(16,2,1))
        for((mask,x,y) in cells) if(keys and mask!=0) {
            paint.color=0xbb71eaca.toInt()
            canvas.drawRect(cross.left+x*a,cross.top+y*a,cross.left+(x+1)*a,cross.top+(y+1)*a,paint)
        }
        canvas.restore()
        paint.style=Paint.Style.STROKE; paint.strokeWidth=1.3f*density; paint.color=0x80ffffff.toInt()
        canvas.drawPath(crossPath,paint); paint.pathEffect=null
        paint.strokeCap=Paint.Cap.ROUND; paint.strokeJoin=Paint.Join.ROUND; paint.strokeWidth=2.5f*density
        for((mask,x,y) in cells) {
            val cx=cross.left+(x+.5f)*a; val cy=cross.top+(y+.5f)*a
            val r=7*density
            val arrow=Path()
            when(mask) {
                64->{ arrow.moveTo(cx-r,cy+r/2);arrow.lineTo(cx,cy-r/2);arrow.lineTo(cx+r,cy+r/2) }
                128->{ arrow.moveTo(cx-r,cy-r/2);arrow.lineTo(cx,cy+r/2);arrow.lineTo(cx+r,cy-r/2) }
                32->{ arrow.moveTo(cx+r/2,cy-r);arrow.lineTo(cx-r/2,cy);arrow.lineTo(cx+r/2,cy+r) }
                16->{ arrow.moveTo(cx-r/2,cy-r);arrow.lineTo(cx+r/2,cy);arrow.lineTo(cx-r/2,cy+r) }
            }
            paint.color=if(keys and mask!=0) Look.bg else 0xddffffff.toInt()
            canvas.drawPath(arrow,paint)
        }
        paint.style=Paint.Style.FILL; paint.color=0x55ffffff
        canvas.drawCircle(cross.centerX(),cross.centerY(),4*density,paint)
        for(pad in pads) {
            val active=keys and pad.mask!=0
            val radius=if(pad.round) pad.rect.width()/2 else 14*density
            paint.style=Paint.Style.FILL
            paint.color=if(active) 0xcc71eaca.toInt() else if(pad.mask==1) { if(portrait) Look.purple else 0x889c86ff.toInt() } else { if(portrait) 0xff111625.toInt() else 0x80101523.toInt() }
            canvas.drawRoundRect(pad.rect,radius,radius,paint)
            paint.style=Paint.Style.STROKE;paint.strokeWidth=1.3f*density
            paint.color=if(active) Look.mint else if(pad.mask==1) 0xccbfafff.toInt() else 0x80ffffff.toInt()
            canvas.drawRoundRect(pad.rect,radius,radius,paint)
            paint.style=Paint.Style.FILL;paint.color=if(active) Look.bg else 0xefffffff.toInt()
            paint.textSize=(if(pad.label.length>2) 10 else if(pad.round) 27 else 18)*density
            paint.typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);paint.textAlign=Paint.Align.CENTER
            canvas.drawText(pad.label,pad.rect.centerX(),pad.rect.centerY()-(paint.ascent()+paint.descent())/2,paint)
        }
    }

    override fun onTouchEvent(event:MotionEvent):Boolean {
        if(editing && !portrait) {
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    selected=pads.asReversed().firstOrNull { it.rect.contains(event.x,event.y) }?.rect ?: cross.takeIf { it.contains(event.x,event.y) }
                    lastX=event.x;lastY=event.y
                }
                MotionEvent.ACTION_MOVE -> selected?.let { r ->
                    r.offsetTo((r.left+event.x-lastX).coerceIn(0f,maxOf(0f,width-r.width())),(r.top+event.y-lastY).coerceIn(0f,maxOf(0f,height-r.height())))
                    lastX=event.x;lastY=event.y;rebuildCross();invalidate()
                }
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> { storePositions();selected=null }
            }
            return true
        }
        if(event.actionMasked==MotionEvent.ACTION_CANCEL || event.actionMasked==MotionEvent.ACTION_UP) {
            clearKeys();if(event.actionMasked==MotionEvent.ACTION_UP) performClick();return true
        }
        var next=0
        for(i in 0 until event.pointerCount) {
            if(event.actionMasked==MotionEvent.ACTION_POINTER_UP && i==event.actionIndex) continue
            val x=event.getX(i);val y=event.getY(i)
            if(cross.contains(x,y)) {
                val dx=(x-cross.centerX())/(cross.width()/2);val dy=(y-cross.centerY())/(cross.height()/2)
                if(dx<-.28f) next=next or 32
                if(dx>.28f) next=next or 16
                if(dy<-.28f) next=next or 64
                if(dy>.28f) next=next or 128
            }
            for(pad in pads) {
                val hit=RectF(pad.rect)
                // Small visual pills retain a minimum 48dp touch target.
                if(hit.height()<48*density) hit.inset(0f,-(48*density-hit.height())/2)
                if(hit.contains(x,y)) next=next or pad.mask
            }
        }
        if(next and keys.inv()!=0) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        keys=next;invalidate();return true
    }
    fun clearKeys() {keys=0;invalidate()}
    override fun performClick():Boolean {super.performClick();return true}
}