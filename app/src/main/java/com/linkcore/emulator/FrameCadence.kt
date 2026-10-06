package com.linkcore.emulator

/** Quantizes original GBA cadence to the nearest available display refresh. */
class FrameCadence {
    private var next=0L
    private val period=16_742_706L
    fun ready(time:Long,vsync:Long)=next==0L || time+vsync/2>=next
    fun presented(time:Long) {
        if(next==0L || time-next>period*2) next=time
        next+=period
    }
    fun reset() {next=0L}
}
