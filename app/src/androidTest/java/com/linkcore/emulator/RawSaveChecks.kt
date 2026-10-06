package com.linkcore.emulator

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import java.io.File

/** Test-only checks. User file staged separately; no game/save data in test sources. */
object RawSaveChecks {
 fun run(test:Instrumentation) {
  val report=Bundle();val dir=File(test.targetContext.cacheDir,"raw-save-check").apply{mkdirs()}
  try {
   val hash="a".repeat(64)
   for(size in listOf(512,8192,32768,65536,131072)) {
    val data=ByteArray(size){(it%251).toByte()}
    check(SaveArchive.decodeImport(data.inputStream(),hash,"test.SAV").contentEquals(data))
    check(SaveArchive.decodeImport(SaveArchive.encode(hash,"test",data).inputStream(),hash,"test.lcsave").contentEquals(data))
   }
   for(size in listOf(0,511,131071,131073))check(runCatching{SaveArchive.decodeImport(ByteArray(size).inputStream(),hash,"bad.sav")}.isFailure)
   val archive=SaveArchive.encode(hash,"test",ByteArray(131072))
   check(runCatching{SaveArchive.decodeImport(archive.inputStream(),"b".repeat(64),"test.lcsave")}.isFailure)
   check(runCatching{SaveArchive.decodeImport(archive.copyOf(100).inputStream(),hash,"test.lcsave")}.isFailure)
   val external=File(test.targetContext.cacheDir,"external-fire-red.sav")
   val imported=external.inputStream().use{SaveArchive.decodeImport(it,hash,"external.sav")}
   check(imported.size==131072)
   val target=File(dir,"load-check.sav");val old=ByteArray(131072){17};target.writeBytes(old)
   SaveArchive.replace(target,imported)
   check(target.readBytes().contentEquals(imported))
   check(dir.listFiles()!!.any{it.name.startsWith("load-check.sav.before-import-")&&it.readBytes().contentEquals(old)})
   val library=LibraryStore(test.targetContext).read()
   val game=library.first{it.system=="GBA"&&it.name.contains("Rojo Fuego",true)}
   val rom=RomFiles.cached(test.targetContext,game.cached)!!
   check(NativeCore.load(rom.path,target.path)==null)
   try {
    val bitmap=android.graphics.Bitmap.createBitmap(240,160,android.graphics.Bitmap.Config.ARGB_8888)
    val audio=ShortArray(4096)
    try {repeat(240){check(NativeCore.frame(0,bitmap,audio)>=0)}}finally{bitmap.recycle()}
    check(NativeCore.save())
   } finally {NativeCore.close()}
   check(target.readBytes().contentEquals(imported)){"Core did not retain imported battery save"}
   report.putString("stream","PASS: raw .sav sizes and limits, original .lcsave ROM binding, atomic replacement and backup; actual external FireRed save loaded by mGBA for 240 frames and retained unchanged. User active save untouched.\n")
   test.finish(Activity.RESULT_OK,report)
  }catch(e:Throwable){report.putString("stream","FAIL: "+e.stackTraceToString());test.finish(Activity.RESULT_CANCELED,report)}
  finally{dir.deleteRecursively()}
 }
}
