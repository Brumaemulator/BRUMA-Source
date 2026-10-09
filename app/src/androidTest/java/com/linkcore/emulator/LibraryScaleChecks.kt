package com.linkcore.emulator
import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import java.io.File
object LibraryScaleChecks {
 fun run(test:Instrumentation){
  val token="library-scale-${System.nanoTime()}"
  val root=File(test.targetContext.cacheDir,token).apply{mkdirs()}
  val ctx=object:ContextWrapper(test.targetContext){
   override fun getFilesDir()=root
   override fun getSharedPreferences(name:String,mode:Int)=super.getSharedPreferences("$token-$name",mode)
  }
  val store=LibraryStore(ctx);val uri=Uri.parse("content://${test.context.packageName}.scale/tree/root")
  fun call(method:String)=ctx.contentResolver.call(uri,method,null,null)!!
  try{
   call("reset");val discovered=ArrayList<LibraryGame>();var firstOpens=-1
   val initial=store.scan(uri) {game->if(discovered.isEmpty())firstOpens=call("stats").getInt("opens");discovered+=game}
   check(discovered==initial && firstOpens==1) {"Results withheld until complete scan"}
   check(store.read().isEmpty()) {"Partial scan persisted"}
   val covered=initial.first().copy(cover="file:///custom-cover.png")
   val preview=store.preview(listOf(covered),initial)
   check(preview.size==300 && preview.first {it.key==covered.key}.cover==covered.cover)
   check(initial.size==300);check(call("stats").getInt("opens")==300)
   val archive=File(root,"many.zip")
   val payload=ByteArray(8192) {(it*31).toByte()}
   java.util.zip.ZipOutputStream(archive.outputStream()).use {z->
    fun entry(name:String,bytes:ByteArray) {z.putNextEntry(java.util.zip.ZipEntry(name));z.write(bytes);z.closeEntry()}
    entry("Game/Game.ini","[Game]\nTitle=Progress fixture\n".toByteArray())
    entry("Game/Data/Scripts.rxdata",byteArrayOf(4,8))
    repeat(500) {entry("Game/Graphics/asset-$it.png",payload)}
   }
   val unpacked=GameArchives.import(ctx,Uri.fromFile(archive)).single()
   val unpackedDir=File(Uri.parse(unpacked.uri).path!!)
   repeat(500) {check(File(unpackedDir,"Graphics/asset-$it.png").readBytes().contentEquals(payload))}
   check(GameArchives.import(ctx,Uri.fromFile(archive))==listOf(unpacked))
   store.add(initial)
   call("reset");val start=android.os.SystemClock.elapsedRealtime();val cached=store.cachedGames();val elapsed=android.os.SystemClock.elapsedRealtime()-start
   check(cached.size==300);check(call("stats").getInt("opens")==0);check(call("stats").getInt("queries")==0)
   val again=store.scan(uri);check(again==initial);check(call("stats").getInt("opens")==0)
   call("reset");call("change");val changed=store.scan(uri);check(call("stats").getInt("opens")==1);check(changed[0].cached!=initial[0].cached)
   call("reset");call("remove");check(store.scan(uri).size==299);check(call("stats").getInt("opens")==0)
   File(test.targetContext.filesDir,"library-scale-result.txt").writeText("PASS: 300 games; cached list $elapsed ms without source access; unchanged scan 0 ROM reads; changed ROM 1 read; removed source disappears from scan.\n")
  }finally{root.deleteRecursively();ctx.getSharedPreferences("rom-scan-signatures",0).edit().clear().commit()}
 }
}
