package com.linkcore.emulator

import android.app.Activity
import android.app.Instrumentation
import android.content.ContextWrapper
import android.net.Uri
import android.os.Bundle
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject

/** Storage regression checks: isolated fixtures, never the user's library or saves. */
object StorageChecks {
 fun run(test:Instrumentation) {
  val report=Bundle();val lines=mutableListOf<String>()
  val token="storage-"+System.nanoTime()
  val root=File(test.targetContext.cacheDir,token).apply{mkdirs()}
  val c=object:ContextWrapper(test.targetContext){
   override fun getFilesDir()=File(root,"files").apply{mkdirs()}
   override fun getCacheDir()=File(root,"cache").apply{mkdirs()}
   override fun getSharedPreferences(n:String,m:Int)=super.getSharedPreferences("$token-$n",m)
  }
  fun payloadBytes(dir:File)=dir.walkTopDown().filter{it.isFile&&!java.nio.file.Files.isSymbolicLink(it.toPath())}.sumOf{it.length()}
  try{
   val store=LibraryStore(c);val tree=Uri.parse("content://"+test.context.packageName+".scale/tree/root")
   c.contentResolver.call(tree,"storage",null,null)
   val games=store.scan(tree);check(games.size==500&&games.all{it.system=="GBC"})
   store.add(games)
   check(payloadBytes(File(c.filesDir,"roms"))==0L)
   check(payloadBytes(File(c.filesDir,"archives"))==0L)
   lines+="PASS: 500 GBC originals (65,536,000 bytes), scan creates 0 ROM bytes."

   val zip=File(root,"500.zip")
   ZipOutputStream(zip.outputStream()).use{out->repeat(500){n->
    out.putNextEntry(ZipEntry("Games/Game-$n.gbc"))
    val b=ByteArray(131072);b[0]=n.toByte();b[1]=(n shr 8).toByte()
    out.write(b);out.closeEntry()
   }}
   val zipped=GameArchives.import(c,Uri.fromFile(zip),zip.length(),zip.lastModified())
   check(zipped.size==500)
   check(payloadBytes(File(c.filesDir,"roms"))==0L)
   check(payloadBytes(File(c.filesDir,"archives"))==0L)
   val opened=RomAccess.open(c,zipped[40])
   check(opened.length()==131072L&&opened.nameWithoutExtension==zipped[40].cached)
   check(payloadBytes(RomAccess.root(c))==131072L)
   val second=RomAccess.open(c,zipped[40]);RomAccess.release(opened)
   check(second.isFile);RomAccess.release(second)
   check(payloadBytes(RomAccess.root(c))==0L)
   val imported=RomFiles.import(c,Uri.parse(zipped[40].uri),zipped[40].name).first
   check(imported.nameWithoutExtension==zipped[40].cached&&imported.length()==131072L)
   check(imported.delete())
   lines+="PASS: ZIP with 500 GBC games: 0 bytes extracted by scan; selected game only 131,072 temporary bytes; 0 after close; patch input reads selected entry."

   val source=File(root,"original.gba")
   test.context.assets.open("smoke.gba").use{input->source.outputStream().use{input.copyTo(it)}}
   val direct=RomAccess.open(c,Uri.fromFile(source).toString(),source.name)
   check(java.nio.file.Files.isSymbolicLink(direct.toPath())){"Seekable source was copied"}
   val bitmap=android.graphics.Bitmap.createBitmap(240,160,android.graphics.Bitmap.Config.ARGB_8888)
   val audio=ShortArray(4096);val save=File(c.filesDir,"saves/"+direct.nameWithoutExtension+".sav").apply{parentFile!!.mkdirs()}
   check(NativeCore.load(direct.path,save.path)==null)
   repeat(8){check(NativeCore.frame(0,bitmap,audio)>0)}
   check(NativeCore.save());NativeCore.close()
   val saved=save.readBytes();check(saved[0]==0x42.toByte())
   val same=RomAccess.open(c,Uri.fromFile(source).toString(),source.name)
   RomAccess.release(direct);check(same.isFile)
   check(NativeCore.load(same.path,save.path)==null)
   repeat(8){check(NativeCore.frame(0,bitmap,audio)>0)}
   check(android.graphics.Color.red(bitmap.getPixel(120,80))>200&&android.graphics.Color.green(bitmap.getPixel(120,80))>200)
   NativeCore.close();RomAccess.release(same);bitmap.recycle()
   check(!same.exists()&&source.isFile&&save.readBytes().contentEquals(saved))
   check(payloadBytes(RomAccess.root(c))==0L)
   lines+="PASS: native GBA plays through read-only descriptor, 0 copied ROM bytes; save survives close/reopen."

   val id=RomAccess.hash(c,Uri.fromFile(source).toString())
   val local=File(c.filesDir,"roms/$id.gba").apply{parentFile!!.mkdirs();source.copyTo(this)}
   val game=LibraryGame(LibraryStore.key(source.path),source.name,Uri.fromFile(source).toString(),cached=id,source=Uri.fromFile(source).toString())
   val cover=File(c.filesDir,"covers/test.png").apply{parentFile!!.mkdirs();writeText("cover")}
   check(RomAccess.reclaim(c,listOf(game),setOf(id))==0L&&local.isFile)
   check(RomAccess.reclaim(c,listOf(game.copy(uri="file:///unavailable",source="file:///unavailable")),emptySet())==0L&&local.isFile)
   val state=File(c.filesDir,"translations/links/$id.json").apply{parentFile!!.mkdirs()}
   state.writeText(JSONObject().put("active",true).put("translation",JSONObject().put("gameSha256",id).put("targetSha256",id)).toString())
   check(RomAccess.reclaim(c,listOf(game),emptySet())==0L&&local.isFile)
   state.delete()
   check(RomAccess.reclaim(c,listOf(game),emptySet())==source.length()&&!local.exists())
   check(source.isFile&&save.readBytes().contentEquals(saved)&&cover.readText()=="cover")
   lines+="PASS: verified redundant copy removed; active game, translation pins and unavailable original protected; saves/covers unchanged."
   val mixed=File(root,"mixed-rpg-and-cartridge.zip")
   ZipOutputStream(mixed.outputStream()).use{out->
    for((name,bytes) in listOf("RPG/Game.ini" to "[Game]\nTitle=Storage fixture\n".toByteArray(),"RPG/Data/Scripts.rxdata" to byteArrayOf(4,8),"Cartridges/test.gba" to source.readBytes())){
     out.putNextEntry(ZipEntry(name));out.write(bytes);out.closeEntry()
    }
   }
   val mixedGames=GameArchives.import(c,Uri.fromFile(mixed),mixed.length(),mixed.lastModified())
   check(mixedGames.size==2&&mixedGames.any{it.system=="RPG Maker XP"})
   check(payloadBytes(File(c.filesDir,"roms"))==0L)
   check(mixedGames.first{it.system=="GBA"}.uri.contains("#rom="))
   check(GameArchives.import(c,Uri.fromFile(mixed),mixed.length(),mixed.lastModified())==mixedGames)
   lines+="PASS: mixed RPG Maker/cartridge ZIP copies RPG folder only; cartridge stays in source ZIP."
   for(ext in listOf("gb","gbc")){
    val original=File(root,"original.$ext")
    test.context.assets.open("storage.$ext").use{input->original.outputStream().use{input.copyTo(it)}}
    val file=RomAccess.open(c,Uri.fromFile(original).toString(),original.name)
    check(java.nio.file.Files.isSymbolicLink(file.toPath()))
    val frame=android.graphics.Bitmap.createBitmap(160,144,android.graphics.Bitmap.Config.ARGB_8888)
    check(NativeCore.load(file.path,File(root,"test-$ext.sav").path)==null)
    repeat(90){check(NativeCore.frame(0,frame,audio)>0)}
    val colors=mutableSetOf<Int>();for(y in 0 until 144 step 5)for(x in 0 until 160 step 7)colors+=frame.getPixel(x,y)
    check(colors.size>2){"$ext video colors="+colors.size};NativeCore.close();RomAccess.release(file);frame.recycle()
    check(payloadBytes(RomAccess.root(c))==0L&&original.isFile)
   }
   val nds=File(root,"original.nds")
   test.context.assets.open("storage.nds").use{input->nds.outputStream().use{input.copyTo(it)}}
   val ndsFile=RomAccess.open(c,Uri.fromFile(nds).toString(),nds.name)
   check(java.nio.file.Files.isSymbolicLink(ndsFile.toPath()))
   val video=java.nio.ByteBuffer.allocateDirect(256*384*4)
   try{
    NdsNative.open(ndsFile.path,File(root,"test-nds.sav").path,root.path,5)
    repeat(120){check(NdsNative.frame(0,-1,0,video,audio)>=0)}
    val frame=android.graphics.Bitmap.createBitmap(256,384,android.graphics.Bitmap.Config.ARGB_8888)
    video.rewind();frame.copyPixelsFromBuffer(video)
    val colors=mutableSetOf<Int>();for(y in 0 until 384 step 8)for(x in 0 until 256 step 8)colors+=frame.getPixel(x,y)
    check(colors.size>2){"NDS video missing"};frame.recycle()
   }finally{NdsNative.close();RomAccess.release(ndsFile)}
   check(payloadBytes(RomAccess.root(c))==0L&&nds.isFile)
   lines+="PASS: native GB, GBC and NDS play directly from original descriptors with video; 0 copied ROM bytes."
   val app=test.targetContext;val index=File(app.filesDir,"game-library.json")
   val oldIndex=index.takeIf{it.exists()}?.readBytes()
   var translations:Activity?=null
   val before=payloadBytes(File(app.filesDir,"roms"))
   try{
    LibraryStore(app).add(games)
    translations=test.startActivitySync(android.content.Intent(app,com.linkcore.emulator.translations.TranslationsActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    val klass=com.linkcore.emulator.translations.TranslationsActivity::class.java
    val busy=klass.getDeclaredField("busy").apply{isAccessible=true}
    val status=klass.getDeclaredField("status").apply{isAccessible=true}
    var done=false
    repeat(300){if(!done){test.runOnMainSync{done=!busy.getBoolean(translations)};if(!done)Thread.sleep(100)}}
    check(done){"Translation catalog timed out"}
    test.runOnMainSync{
     val message=(status.get(translations) as android.widget.TextView).text.toString()
     check(message==app.resources.getQuantityString(R.plurals.translation_count,0,0)){"Translation lookup: $message"}
    }
    check(payloadBytes(File(app.filesDir,"roms"))==before){"Translation lookup copied games"}
   }finally{
    test.runOnMainSync{translations?.finish()};test.waitForIdleSync()
    if(oldIndex==null)index.delete()else index.writeBytes(oldIndex)
   }
   lines+="PASS: Translations UI queried real catalog with 500 GBC library entries; no ROMs copied."
   val session=java.util.concurrent.atomic.AtomicReference<android.content.Intent?>()
   val receiver=object:android.content.BroadcastReceiver(){override fun onReceive(c:android.content.Context,i:android.content.Intent){
    if(i.getStringExtra("system")=="NDS")session.set(android.content.Intent(i))
   }}
   RuntimeSessionHost.register(app,receiver,android.content.IntentFilter(RuntimeSessionHost.STATE))
   var dsPath:File?=null
   try{
    val ndsId=RomAccess.hash(c,Uri.fromFile(nds).toString())
    app.startActivity(android.content.Intent(app,NdsActivity::class.java).putExtra("gameUri",Uri.fromFile(nds).toString()).putExtra("gameName","Fixture.nds").putExtra("gameId",ndsId).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    var tries=0
    while(session.get()?.getBooleanExtra("ready",false)!=true&&tries++<150){RuntimeSessionHost.query(app);Thread.sleep(100)}
    val live=checkNotNull(session.get())
    check(live.getBooleanExtra("ready",false)){"NDS adapter failed to launch original"}
    dsPath=File(live.getStringExtra("path")!!)
    check(dsPath.nameWithoutExtension==ndsId&&dsPath.path.contains("/cache/rom-sessions/"))
    check(!File(app.filesDir,"roms/$ndsId.nds").exists())
    RuntimeSessionHost.close(app,live.getStringExtra("token")!!)
    tries=0
    while(dsPath.parentFile!!.listFiles().orEmpty().any{it.name==dsPath.name}&&tries++<100)Thread.sleep(100)
    check(dsPath.parentFile!!.listFiles().orEmpty().none{it.name==dsPath.name}){"NDS adapter left source handle"}
   }finally{RuntimeSessionHost.closeOthers(app,MainActivity::class.java.name);app.unregisterReceiver(receiver)}
   RomAccess.sweep(app)
   lines+="PASS: isolated NDS Activity opens original URI and closes its session; no permanent copy."
   report.putString("stream",lines.joinToString("\n")+"\n")
   File(test.targetContext.filesDir,"storage-results.txt").writeText(lines.joinToString("\n"))
   test.finish(Activity.RESULT_OK,report)
  }catch(e:Throwable){
   NativeCore.close()
   report.putString("stream",lines.joinToString("\n")+"\nFAIL: "+android.util.Log.getStackTraceString(e))
   test.finish(Activity.RESULT_CANCELED,report)
  }finally{root.deleteRecursively()}
 }
}
