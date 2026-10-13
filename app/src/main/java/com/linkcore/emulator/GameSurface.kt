package com.linkcore.emulator

import android.content.Context
import android.graphics.*
import android.view.SurfaceView
import android.view.Choreographer
import android.view.Surface

object GameLayout {
    fun viewport(w:Int,h:Int,d:Float):RectF {
        if(h<=w) return RectF(0f,0f,w.toFloat(),h.toFloat())
        val margin=4*d
        val screenHeight=(w-2*margin)*2/3
        val top=maxOf(64*d,(h-screenHeight-300*d)/2)
        return RectF(margin,top,w-margin,top+(w-2*margin)*2/3)
    }
}

class GameSurface(context: Context) : SurfaceView(context) {
    private val paint=Paint().apply {isFilterBitmap=false}
    @Volatile var fillScreen=false
    @Volatile private var gameWidth=240
    @Volatile private var gameHeight=160
    private val frameLock=Any()
    private class FrameSlot(width:Int,height:Int) {
        val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        var generatedAt=0L
    }
    private var pending=Array(3) {FrameSlot(240,160)}
    private var displayed=FrameSlot(240,160)
    private var head=0
    private var queued=0
    private var primed=false
    private var attached=false
    private var previousPresentation=0L
    private var previousVsync=0L
    private var vsyncPeriod=8_333_333L
    private val cadence=FrameCadence()
    private val ages=ArrayList<Double>(300)
    private val intervals=ArrayList<Double>(300)
    private val callback=object:Choreographer.FrameCallback {
        override fun doFrame(time:Long) {
            if(!attached) return
            if(previousVsync!=0L) {
                val delta=time-previousVsync
                if(delta in 6_000_000L..20_000_000L) vsyncPeriod=(vsyncPeriod*7+delta)/8
            }
            previousVsync=time
            if(isShown && holder.surface.isValid) {
                val frame=synchronized(frameLock) {
                    // Prime one spare frame so producer/vsync jitter does not alternate short and long gaps.
                    if(!primed && queued>=2) primed=true
                    if(primed && queued>0 && cadence.ready(time,vsyncPeriod)) {
                        // Transfer ownership instead of copying the frame on the UI thread.
                        val recycled=displayed;displayed=pending[head];pending[head]=recycled
                        head=(head+1)%3;queued--;displayed.bitmap
                    } else null
                }
                if(frame!=null) {
                    if(displayed.generatedAt>0)ages.add((System.nanoTime()-displayed.generatedAt)/1e6)
                    render(frame)
                    cadence.presented(time)
                    if(previousPresentation!=0L) {
                        val ms=(time-previousPresentation)/1e6
                        if(ms<100) intervals.add(ms) else {intervals.clear();ages.clear()}
                        if(intervals.size==300) {
                            val sorted=intervals.sorted()
                            val sortedAges=ages.sorted()
                            if(sortedAges.isNotEmpty())android.util.Log.i("BrumaLatency","frame age medianMs=${sortedAges[sortedAges.size/2]} p95Ms=${sortedAges[(sortedAges.size*0.95).toInt().coerceAtMost(sortedAges.lastIndex)]}")
                            ages.clear()
                            android.util.Log.i("BrumaPacing","submit medianMs=${sorted[150]} p95Ms=${sorted[284]} maxMs=${sorted.last()} over25ms=${sorted.count {it>25}}/300")
                            intervals.clear()
                        }
                    }
                    previousPresentation=time
                }
            } else {previousPresentation=0L;intervals.clear();ages.clear();synchronized(frameLock) {head=0;queued=0;primed=false;cadence.reset()}}
            Choreographer.getInstance().postFrameCallback(this)
        }
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow();attached=true;Choreographer.getInstance().postFrameCallback(callback)
    }
    override fun onDetachedFromWindow() {
        attached=false;Choreographer.getInstance().removeFrameCallback(callback);super.onDetachedFromWindow()
    }
    fun snapshot():Bitmap? = synchronized(frameLock) {if(previousPresentation!=0L) displayed.bitmap.copy(Bitmap.Config.ARGB_8888,false) else null}
    fun clearPending() {
        synchronized(frameLock) {head=0;queued=0;primed=false;cadence.reset()}
        previousPresentation=0L;intervals.clear();ages.clear()
    }
    fun setGameFrameRate() {
        if(android.os.Build.VERSION.SDK_INT>=30 && holder.surface.isValid)
            holder.surface.setFrameRate(display?.refreshRate ?: 60f,Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
    }
    fun drawFrame(bitmap:Bitmap,generatedAt:Long=System.nanoTime()) {
        synchronized(frameLock) {
            if(gameWidth!=bitmap.width || gameHeight!=bitmap.height) {gameWidth=bitmap.width;gameHeight=bitmap.height;pending=Array(3){FrameSlot(gameWidth,gameHeight)};displayed=FrameSlot(gameWidth,gameHeight);head=0;queued=0;primed=false;cadence.reset()}
            // Keep one spare GB/GBC frame for scheduling jitter, without retaining a third old frame.
            val capacity=if(gameWidth==160 && gameHeight==144) 2 else 3
            if(queued>=capacity) {head=(head+1)%3;queued--}
            pending[(head+queued)%3].generatedAt=generatedAt
            pending[(head+queued)%3].canvas.drawBitmap(bitmap,0f,0f,null);queued++
        }
    }
    private fun render(bitmap:Bitmap) {
        if(!holder.surface.isValid) return
        val canvas=holder.lockHardwareCanvas() ?: return
        try {
            canvas.drawColor(Look.bg)
            val region=GameLayout.viewport(canvas.width,canvas.height,resources.displayMetrics.density)
            val destination=if(fillScreen) region else {
                val scale=minOf(region.width()/bitmap.width,region.height()/bitmap.height)
                val left=region.centerX()-bitmap.width*scale/2;val top=region.centerY()-bitmap.height*scale/2
                RectF(left,top,left+bitmap.width*scale,top+bitmap.height*scale)
            }
            canvas.drawBitmap(bitmap,Rect(0,0,bitmap.width,bitmap.height),destination,paint)
        } finally {holder.unlockCanvasAndPost(canvas)}
    }
}
