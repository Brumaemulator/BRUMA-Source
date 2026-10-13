// SPDX-License-Identifier: GPL-3.0-or-later
package com.linkcore.emulator
import java.nio.ByteBuffer
object NdsNative {
 init {System.loadLibrary("bruma_nds")}
 external fun open(rom:String,save:String,directory:String,language:Int)
 external fun frame(keys:Int,x:Int,y:Int,video:ByteBuffer,audio:ShortArray):Int
 external fun flush()
 external fun close()
}
