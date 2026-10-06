package com.linkcore.emulator

/** Streaming stereo box filter: accelerated, higher-pitched audio at a fixed 48kHz output. */
class SpeedAudio {
    private var left=0
    private var right=0
    private var pending=0
    fun reset() {left=0;right=0;pending=0}
    fun convert(input:ShortArray,count:Int,speed:Int,output:ShortArray):Int {
        require(speed==2 || speed==4)
        require(count%2==0 && count<=input.size)
        var out=0
        for(n in 0 until count step 2) {
            left+=input[n].toInt();right+=input[n+1].toInt();pending++
            if(pending==speed) {
                output[out++]=(left/speed).toShort();output[out++]=(right/speed).toShort()
                left=0;right=0;pending=0
            }
        }
        return out
    }
}
