// SPDX-License-Identifier: GPL-3.0-or-later
package com.linkcore.emulator

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** Read-only source access. Keep stable hash filenames so all existing saves still match. */
object RomAccess {
 private data class Lease(val file:File,val descriptor:ParcelFileDescriptor?,var references:Int=1)
 private val leases=mutableMapOf<String,Lease>()
 fun sweep(c:Context) {
  val processes=(c.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).runningAppProcesses?:return
  val active=processes.filter{it.uid==android.os.Process.myUid()}.map{it.pid.toString()}.toSet()
  val base=File(c.cacheDir,"rom-sessions").canonicalFile
  base.listFiles()?.filter{it.isDirectory&&it.name.matches(Regex("[0-9]+"))&&it.name !in active}?.forEach{dir->
   if(dir.canonicalFile.parentFile==base)dir.listFiles()?.forEach{f->if(f.parentFile==dir)f.delete()}
   dir.delete()
  }
 }
 fun root(c:Context)=File(c.cacheDir,"rom-sessions/${android.os.Process.myPid()}").apply{mkdirs()}
 fun stream(c:Context,text:String):InputStream {
  val raw=text.substringBefore("#rom=");val input=c.contentResolver.openInputStream(Uri.parse(raw))?:throw IOException("Game unavailable")
  if(!text.contains("#rom="))return input
  val wanted=Uri.decode(text.substringAfter("#rom="));val zip=ZipInputStream(input.buffered(65536))
  try {while(true){val e=zip.nextEntry?:throw FileNotFoundException(wanted);if(!e.isDirectory&&e.name.replace('\\','/')==wanted)return zip;zip.closeEntry()}}
  catch(e:Exception){zip.close();throw e}
 }
 fun hash(c:Context,text:String):String=stream(c,text).use{digest(it)}
 private fun digest(input:InputStream):String {
  val sha=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(65536);var total=0L
  while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=512L*1024*1024){"ROM too large"};sha.update(buffer,0,n)}
  return sha.digest().joinToString(""){"%02x".format(it.toInt()and 255)}
 }
 @Synchronized fun release(file:File?) {
  if(file==null)return
  val lease=leases[file.absolutePath]?:return
  if(--lease.references>0)return
  leases.remove(file.absolutePath)
  lease.file.delete();lease.descriptor?.close()
 }
 fun isSession(c:Context,file:File)=file.absoluteFile.parentFile==root(c).absoluteFile
 fun open(c:Context,game:LibraryGame):File {
  val cached=RomFiles.cached(c,game.cached)
  if(cached!=null&&pins(c).contains(game.cached))return cached
  val source=game.source.ifBlank{game.uri}.ifBlank{game.uri}
  if(source.isBlank())return cached?:throw FileNotFoundException("Game unavailable")
  // Earlier imports sometimes stored a header title without the file extension.
  val name=if(RomFiles.system(game.name).isNotBlank())game.name else game.title+"."+game.system.lowercase(java.util.Locale.ROOT)
  return try{open(c,source,name)}catch(e:Exception){cached?:throw e}
 }
 fun open(c:Context,text:String,name:String):File {
  val ext=name.substringAfterLast('.').lowercase(java.util.Locale.ROOT);require(ext in listOf("gb","gbc","gba","nds"))
  if(!text.contains("#rom=")) {
   val uri=Uri.parse(text)
   // Already-managed originals/translated games must remain in place.
   if(uri.scheme=="file") {
    val f=File(uri.path!!)
    if(f.canonicalPath.startsWith(c.filesDir.canonicalPath+File.separator+"roms"+File.separator))return f
   }
   val fd=runCatching{c.contentResolver.openFileDescriptor(uri,"r")}.getOrNull()
   if(fd!=null) {
    try {
     Os.lseek(fd.fileDescriptor,0,OsConstants.SEEK_SET)
     val id=ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.dup(fd.fileDescriptor)).use{digest(it)}
     Os.lseek(fd.fileDescriptor,0,OsConstants.SEEK_SET)
     val link=File(root(c),"$id.$ext")
     synchronized(this){
      leases[link.absolutePath]?.let{it.references++;fd.close();return it.file}
      if(link.exists()||java.nio.file.Files.isSymbolicLink(link.toPath()))check(link.delete())
      Os.symlink("/proc/self/fd/${fd.fd}",link.path)
      // A provider may grant a descriptor while SELinux forbids reopening it by
      // pathname. Native runtimes reopen paths, so test that before choosing it.
      try{link.inputStream().use{check(it.read()>=0)}}catch(e:Exception){link.delete();throw e}
      leases[link.absolutePath]=Lease(link,fd)
     }
     return link
    } catch(e:Exception){fd.close()}
   }
  }
  // Compressed entries and non-seekable document providers need a temporary file,
  // never a permanent per-library ROM copy. Delete it when that session closes.
  val tmp=File.createTempFile("opening-",".tmp",root(c))
  try {
   val sha=MessageDigest.getInstance("SHA-256");var bytes=0L
   stream(c,text).use{input->tmp.outputStream().use{out->val b=ByteArray(65536);while(true){val n=input.read(b);if(n<0)break;bytes+=n;require(bytes<=(if(ext=="nds")512L else 32L)*1024*1024){"ROM too large"};sha.update(b,0,n);out.write(b,0,n)}}}
   val id=sha.digest().joinToString(""){"%02x".format(it.toInt()and 255)};val f=File(root(c),"$id.$ext")
   synchronized(this){leases[f.absolutePath]?.let{it.references++;return it.file};check(tmp.renameTo(f));leases[f.absolutePath]=Lease(f,null)}
   return f
  }finally{tmp.delete()}
 }
 fun pins(c:Context):Set<String> {
  val pins=mutableSetOf<String>()
  File(c.filesDir,"translations/links").listFiles()?.filter{it.extension=="json"}?.forEach{f->
   runCatching{val s=org.json.JSONObject(f.readText());if(s.optBoolean("active")){val m=s.getJSONObject("translation");pins+=m.getString("gameSha256");pins+=m.getString("targetSha256")}}
  }
  return pins
 }
 /** Delete only proven redundant legacy copies, never a sole imported original. */
 fun reclaim(c:Context,games:List<LibraryGame>,protected:Set<String>):Long {
  var freed=0L;val pinned=pins(c)+protected
  val root=File(c.filesDir,"roms").canonicalFile
  root.listFiles()?.filter{it.isFile&&it.name.matches(Regex("[a-f0-9]{64}\\.(gb|gbc|gba|nds)"))&&!java.nio.file.Files.isSymbolicLink(it.toPath())}?.forEach{f->
   val id=f.nameWithoutExtension
   if(id in pinned)return@forEach
   val sources=(games.filter{it.cached==id}.flatMap{listOf(it.source,it.uri)}+RomCacheCleanup.origins(c,id)).filter{it.isNotBlank()}.distinct()
   val identical=sources.any{s->runCatching{
    val u=Uri.parse(s.substringBefore("#rom="));val inside=u.scheme=="file" && File(u.path!!).canonicalPath.startsWith(c.filesDir.canonicalPath+File.separator)
    !inside && hash(c,s)==id
   }.getOrDefault(false)}
   if(identical&&runCatching{f.inputStream().use{digest(it)}==id}.getOrDefault(false)) {val size=f.length();if(f.delete())freed+=size}
  }
  return freed
 }
}
