package com.linkcore.emulator

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

/** ZIPs are unpacked only into private staging storage, never into source folders. */
object GameArchives {
 private fun message(c:Context)=if(c.resources.configuration.locales[0].language=="es") "El ZIP no contiene ROMs GB/GBC/GBA ni un juego RPG Maker XP compatible." else "The ZIP contains no GB/GBC/GBA ROMs or compatible RPG Maker XP game."
 @Synchronized fun import(c:Context,uri:Uri,size:Long=0,modified:Long=0):List<LibraryGame> {
  val key=LibraryStore.key(uri.toString());val indexes=File(c.filesDir,"archive-index").apply{mkdirs()};val marker=AtomicFile(File(indexes,"$key.json"))
  var actualSize=size;var actualModified=modified
  if(size==0L)runCatching {c.contentResolver.query(uri,null,null,null,null)?.use {q->if(q.moveToFirst()){val s=q.getColumnIndex("_size");val m=q.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED);if(s>=0)actualSize=q.getLong(s);if(m>=0)actualModified=q.getLong(m)}}}
  val previous=runCatching {JSONObject(marker.openRead().bufferedReader().use {it.readText()})}.getOrNull()
  if(previous!=null && previous.optLong("size")==actualSize && previous.optLong("modified")==actualModified){
   val a=previous.getJSONArray("games");val games=(0 until a.length()).map{n->val j=a.getJSONObject(n);LibraryGame(j.getString("key"),j.getString("name"),j.getString("uri"),cached=j.getString("cached"),system=j.getString("system"),source=j.optString("source",uri.toString()))}
   if(games.isNotEmpty() && games.all{if(it.system=="RPG Maker XP")File(Uri.parse(it.uri).path!!,"Game.ini").isFile() else RomFiles.cached(c,it.cached)!=null})return games
  }
  val staging=File(c.filesDir,"archive-staging").apply{mkdirs()};val pending=File(staging,"$key-${System.nanoTime()}").apply{mkdirs()}
  val games=ArrayList<LibraryGame>();val created=ArrayList<File>()
  try {
   var bytes=0L;var entries=0;val seen=HashSet<String>();val buffer=ByteArray(65536)
   c.contentResolver.openInputStream(uri)!!.buffered(256*1024).use {raw->ZipInputStream(raw).use {zip->
    while(true){val entry=zip.nextEntry ?: break;require(++entries<=100000){"ZIP entry limit"}
     val path=entry.name.replace('\\','/');val parts=path.split('/').filter{it.isNotEmpty()}
     require(!path.startsWith('/') && parts.none{it==".." || it=="." || it.contains(':')} && parts.size<=32){"Unsafe ZIP path"}
     val target=File(pending,path).canonicalFile;require(target.path.startsWith(pending.canonicalPath+File.separator)){"Unsafe ZIP path"}
     require(seen.add(path.trimEnd('/').lowercase(java.util.Locale.ROOT))){"Duplicate ZIP path"}
     if(entry.isDirectory){check(target.mkdirs() || target.isDirectory)}else{
      check(target.parentFile!!.mkdirs() || target.parentFile!!.isDirectory)
      target.outputStream().use{out->while(true){val n=zip.read(buffer);if(n<0)break;bytes+=n;require(bytes<=8L*1024*1024*1024){"ZIP > 8 GB"};out.write(buffer,0,n)}}
     };zip.closeEntry()
    }
   }}
   fun visit(dir:File){
    val files=dir.listFiles()?.toList() ?: emptyList()
    val ini=files.firstOrNull{it.isFile && it.name.equals("Game.ini",true)}
    val data=files.firstOrNull{it.isDirectory && it.name.equals("Data",true)}
    val scripts=data?.listFiles()?.firstOrNull{it.isFile && it.name.equals("Scripts.rxdata",true)}
    if(ini!=null && data!=null && scripts!=null){
     require(ini.length()<=1024*1024){"Game.ini > 1 MB"}
     val relative=dir.relativeTo(pending).invariantSeparatorsPath
     val origin=uri.toString()+"#rpg="+Uri.encode(relative)
     val root=File(c.filesDir,"rpg-games").apply{mkdirs()}
     val dest=File(root,"zip-"+LibraryStore.key(origin+":"+actualSize+":"+actualModified).take(40))
     if(!dest.exists()){
      if(scripts.name!="Scripts.rxdata")check(scripts.renameTo(File(data,"Scripts.rxdata")))
      if(data.name!="Data")check(data.renameTo(File(dir,"Data")))
      if(ini.name!="Game.ini")check(ini.renameTo(File(dir,"Game.ini")))
      check(dir.renameTo(dest));created.add(dest);File(dest,"bruma-origin.txt").writeText(origin)
     }
     val title=File(dest,"Game.ini").useLines{lines->lines.take(100).firstOrNull{it.trim().startsWith("Title=")}?.substringAfter('=')?.trim()} ?: dest.name
     games+=LibraryGame(LibraryStore.key(origin),title,Uri.fromFile(dest).toString(),cached=dest.name,system="RPG Maker XP",source=origin)
     return
    }
    for(file in files){if(file.isDirectory)visit(file) else if(RomFiles.system(file.name).isNotEmpty()){
     val (rom,name)=RomFiles.import(c,Uri.fromFile(file),file.name)
     val origin=uri.toString()+"#rom="+Uri.encode(file.relativeTo(pending).invariantSeparatorsPath)
     games+=LibraryGame(LibraryStore.key(origin),name,Uri.fromFile(rom).toString(),cached=rom.nameWithoutExtension,system=RomFiles.system(rom.name),source=origin)
    }}
   }
   visit(pending);if(games.isEmpty())throw IOException(message(c))
   val array=JSONArray();games.forEach{g->array.put(JSONObject().put("key",g.key).put("name",g.name).put("uri",g.uri).put("cached",g.cached).put("system",g.system).put("source",g.source))}
   val out=marker.startWrite();try{out.write(JSONObject().put("size",actualSize).put("modified",actualModified).put("games",array).toString().toByteArray());marker.finishWrite(out)}catch(e:Exception){marker.failWrite(out);throw e}
   return games
  } catch(e:Exception){created.forEach{it.deleteRecursively()};throw e}finally{
   // pending was generated directly under our private staging directory.
   check(pending.canonicalPath.startsWith(staging.canonicalPath+File.separator));pending.deleteRecursively()
  }
 }
}

