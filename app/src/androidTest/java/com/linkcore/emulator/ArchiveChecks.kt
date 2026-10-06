package com.linkcore.emulator
import android.app.Instrumentation
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract as Docs
import java.io.File
import java.util.zip.*
object ArchiveChecks {
 fun run(i:Instrumentation) {
  val c=i.targetContext;val tree=Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AJuegos%20gba")
  val crystal=Docs.buildDocumentUriUsingTree(tree,"primary:Juegos gba/Pokemon - Edicion Cristal (Spain).gbc")
  val (rom,_)=RomFiles.import(c,crystal)
  val dir=File(c.cacheDir,"archive-checks").apply{mkdirs()}
  check(NativeCore.load(rom.path,File(dir,"crystal-test.sav").path)==null)
  check(NativeCore.videoSize()==(160 shl 16 or 144))
  val frame=Bitmap.createBitmap(160,144,Bitmap.Config.ARGB_8888);val samples=ShortArray(4096)
  var audio=false;val colors=HashSet<Int>()
  repeat(360){check(NativeCore.frame(0,frame,samples)>0);if(samples.any{it.toInt()!=0})audio=true;for(y in 0 until 144 step 12)for(x in 0 until 160 step 12)colors+=frame.getPixel(x,y)}
  check(colors.size>4){"GBC video missing"};check(audio){"GBC audio missing"}
  File(dir,"crystal.png").outputStream().use{frame.compress(Bitmap.CompressFormat.PNG,100,it)}
  NativeCore.close();frame.recycle()
  val logo=rom.readBytes().copyOfRange(0x104,0x134)
  fun fixture(color:Boolean):ByteArray {val b=ByteArray(32768);b[0x100]=0xc3.toByte();b[0x101]=0x50;b[0x102]=1;logo.copyInto(b,0x104);b[0x143]=if(color)0x80.toByte() else 0;val code=byteArrayOf(0xaf.toByte(),0xe0.toByte(),0x40,0x3e,0xe4.toByte(),0xe0.toByte(),0x47,0x3e,0x91.toByte(),0xe0.toByte(),0x40,0x18,0xfe.toByte());code.copyInto(b,0x150);var sum=0;for(n in 0x134..0x14c)sum=(sum-b[n].toUByte().toInt()-1)and 255;b[0x14d]=sum.toByte();return b}
  fun zip(name:String,entries:List<Pair<String,ByteArray>>):File {val f=File(dir,name);ZipOutputStream(f.outputStream()).use{z->entries.forEach{(path,data)->z.putNextEntry(ZipEntry(path));z.write(data);z.closeEntry()}};return f}
  val mixed=zip("mixed.zip",listOf("folder/test.gb" to fixture(false),"folder/test.gbc" to fixture(true)))
  val games=GameArchives.import(c,Uri.fromFile(mixed));check(games.map{it.system}.toSet()==setOf("GB","GBC"));check(GameArchives.import(c,Uri.fromFile(mixed))==games)
  games.forEach {g->check(NativeCore.load(RomFiles.cached(c,g.cached)!!.path,File(dir,g.system+".sav").path)==null);val b=Bitmap.createBitmap(160,144,Bitmap.Config.ARGB_8888);repeat(3){check(NativeCore.frame(0,b,samples)>0)};NativeCore.close();b.recycle()}
  val unsafe=zip("unsafe.zip",listOf("../escape.txt" to byteArrayOf(1)));check(runCatching{GameArchives.import(c,Uri.fromFile(unsafe))}.isFailure);check(!File(c.filesDir,"archive-staging/escape.txt").exists())
  val nested=zip("rpg.zip",listOf("Wrapper/Game/Game.ini" to "[Game]\nTitle=Archive fixture\n".toByteArray(),"Wrapper/Game/Data/Scripts.rxdata" to byteArrayOf(4,8)))
  val rpg=GameArchives.import(c,Uri.fromFile(nested));check(rpg.single().system=="RPG Maker XP");check(File(Uri.parse(rpg.single().uri).path!!,"Data/Scripts.rxdata").isFile())
  // Only generated fixture files are removed; never the user's games or saves.
  games.forEach{RomFiles.cached(c,it.cached)?.delete()};File(Uri.parse(rpg.single().uri).path!!).deleteRecursively()
  listOf(mixed,nested,unsafe).forEach{File(c.filesDir,"archive-index/${LibraryStore.key(Uri.fromFile(it).toString())}.json").delete()}
  val gba=File(dir,"regression.gba");i.context.assets.open("smoke.gba").use{input->gba.outputStream().use{input.copyTo(it)}}
  check(NativeCore.load(gba.path,File(dir,"gba-test.sav").path)==null)
  check(NativeCore.videoSize()==(240 shl 16 or 160));val gbaFrame=Bitmap.createBitmap(240,160,Bitmap.Config.ARGB_8888)
  repeat(8){check(NativeCore.frame(1,gbaFrame,samples)>0)};check(android.graphics.Color.green(gbaFrame.getPixel(120,80))>200)
  NativeCore.close();gbaFrame.recycle()
  val z=Docs.buildDocumentUriUsingTree(tree,"primary:Juegos gba/POKEMON Z V2.18.zip")
  val actual=GameArchives.import(c,z);check(actual.any{it.system=="RPG Maker XP"}){"Pokemon Z not detected"};LibraryStore(c).add(actual)
 }
}

