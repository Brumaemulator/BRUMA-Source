package com.linkcore.emulator

import android.graphics.Bitmap

object NativeCore {
    init { System.loadLibrary("linkcore") }
    external fun load(rom: String, saveFile: String): String?
    external fun frame(keys: Int, bitmap: Bitmap, audio: ShortArray): Int
    external fun videoSize(): Int
    external fun save(): Boolean
    external fun close()
}

