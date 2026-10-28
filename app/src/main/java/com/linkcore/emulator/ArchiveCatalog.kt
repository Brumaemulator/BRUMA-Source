// SPDX-License-Identifier: GPL-3.0-or-later
package com.linkcore.emulator
import android.content.Context
import android.net.Uri
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import org.json.JSONArray
import org.json.JSONObject

/** A catalog scan never writes decompressed cartridge payloads. */
object ArchiveCatalog {
 data class Result(val games:List<LibraryGame>,val hasRpg:Boolean)
 fun read(c:Context,uri:Uri,size:Long=0,modified:Long=0):Result {
  val key=LibraryStore.key(uri.toString());val prefs=c.getSharedPreferences("archive-catalog-v2",0);val signature="$size:$modified:"
  if(size>0&&modified>0)runCatching{prefs.getString(key,null)?.takeIf{it.startsWith(signature)}?.let{
   val j=JSONObject(it.removePrefix(signature));val a=j.getJSONArray("games")
   return Result((0 until a.length()).map{n->val g=a.getJSONObject(n);LibraryGame(g.getString("key"),g.getString("name"),g.getString("uri"),cached=g.getString("hash"),system=g.getString("system"),source=g.getString("uri"))},j.getBoolean("rpg"))
  }}
  val games=mutableListOf<LibraryGame>();val paths=hashSetOf<String>();var total=0L;var entries=0;val buffer=ByteArray(65536)
  c.contentResolver.openInputStream(uri)!!.use{raw->ZipInputStream(raw.buffered(65536)).use{zip->
   while(true){val entry=zip.nextEntry?:break;require(++entries<=100000){"ZIP entry limit"}
    val path=entry.name.replace('\\','/');val parts=path.split('/').filter{it.isNotEmpty()}
    require(!path.startsWith('/')&&parts.none{it==".."||it=="."||it.contains(':')}&&parts.size<=32){"Unsafe ZIP path"}
    require(paths.add(path.trimEnd('/').lowercase(java.util.Locale.ROOT))){"Duplicate ZIP path"}
    val system=RomFiles.system(path);val digest=if(system.isNotEmpty())MessageDigest.getInstance("SHA-256")else null;var length=0L
    if(!entry.isDirectory)while(true){val n=zip.read(buffer);if(n<0)break;length+=n;total+=n;require(total<=8L*1024*1024*1024){"ZIP > 8 GB"};if(digest!=null){require(length<=(if(system=="NDS")512L else 32L)*1024*1024){"ROM too large"};digest.update(buffer,0,n)}}
    if(digest!=null){require(length>=if(system=="NDS")512 else if(system=="GBA")192 else 336);val hash=digest.digest().joinToString(""){"%02x".format(it.toInt()and 255)};val source=uri.toString()+"#rom="+Uri.encode(path);games+=LibraryGame(LibraryStore.key(source),path.substringAfterLast('/'),source,cached=hash,system=system,source=source)}
    zip.closeEntry()
   }
  }}
  val rpg=paths.any{p->p.endsWith("game.ini")&&paths.contains(p.removeSuffix("game.ini")+"data/scripts.rxdata")}
  if(size>0&&modified>0){val a=JSONArray();games.forEach{a.put(JSONObject().put("key",it.key).put("name",it.name).put("uri",it.uri).put("hash",it.cached).put("system",it.system))};prefs.edit().putString(key,signature+JSONObject().put("games",a).put("rpg",rpg)).apply()}
  return Result(games,rpg)
 }
}
