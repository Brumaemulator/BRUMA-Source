package com.linkcore.emulator.translations
internal object XdeltaNative {
    init{System.loadLibrary("translationpatch")}
    external fun decode(source:String,patch:ByteArray,target:String,targetBytes:Long):Long
}
