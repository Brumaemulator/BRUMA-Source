package com.linkcore.emulator
import android.app.Instrumentation
import java.io.File
import java.nio.ByteBuffer

object NdsLanguageChecks {
    fun run(test:Instrumentation, expected:Int) {
            check(BrumaLocale.ndsLanguage(test.targetContext)==expected) {"App locale did not reach DS firmware"}
            val roms=File(test.targetContext.filesDir,"roms").listFiles()!!.filter {it.extension=="nds"}
            check(roms.isNotEmpty()) {"No installed DS test ROM"}
            val root=File(test.targetContext.cacheDir,"nds-language-check").apply {mkdirs()}
            val video=ByteBuffer.allocateDirect(256*384*4)
            val samples=ShortArray(4096)
            for(rom in roms) {
                for(language in 0..6) {
                    val save=File(root,"${rom.nameWithoutExtension}-$language.sav")
                    try {
                        // Native open verifies the language at 0x027FFCE4, where games read it.
                        NdsNative.open(rom.path,save.path,root.path,language)
                        repeat(30) {check(NdsNative.frame(0,-1,0,video,samples) in 1..4096)}
                    } finally {NdsNative.close()}
                }
            }
            val nintendogs=roms.first {it.inputStream().use {input -> val title=ByteArray(12);input.read(title);String(title).startsWith("NINTENDOGS")}}
            for(language in listOf(5,1)) {
                try {
                    NdsNative.open(nintendogs.path,File(root,"nintendogs-visual-$language.sav").path,root.path,language)
                    repeat(600) {check(NdsNative.frame(0,-1,0,video,samples)>=0)}
                    repeat(3) {NdsNative.frame(0,128,100,video,samples)}
                    repeat(300) {check(NdsNative.frame(0,-1,0,video,samples)>=0)}
                    val bitmap=android.graphics.Bitmap.createBitmap(256,384,android.graphics.Bitmap.Config.ARGB_8888)
                    video.rewind();bitmap.copyPixelsFromBuffer(video)
                    File(root,"nintendogs-$language.png").outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
                    bitmap.recycle()
                } finally {NdsNative.close()}
            }
    }
}
