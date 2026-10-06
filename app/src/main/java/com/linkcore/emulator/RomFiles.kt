package com.linkcore.emulator

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.security.MessageDigest

object RomFiles {
 fun system(name:String)=when(name.substringAfterLast('.').lowercase(java.util.Locale.ROOT)){"gb"->"GB";"gbc"->"GBC";"gba"->"GBA";"nds"->"NDS";else->""}
 fun cached(context:Context,id:String):File?=listOf("gba","gbc","gb","nds").map {File(context.filesDir,"roms/$id.$it")}.firstOrNull {it.isFile}
 fun name(context:Context,uri:Uri):String=if(uri.scheme=="file") File(uri.path!!).name else context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use {if(it.moveToFirst())it.getString(0) else null} ?: "game"
 fun import(context:Context,uri:Uri,displayName:String=name(context,uri)):Pair<File,String> {
  require(system(displayName).isNotEmpty()) {"GB / GBC / GBA / NDS"}
  val folder=File(context.filesDir,"roms").apply {mkdirs()};val temp=File.createTempFile("import-",".tmp",folder)
  try {
   val digest=MessageDigest.getInstance("SHA-256");var length=0L
   context.contentResolver.openInputStream(uri)!!.use {input->temp.outputStream().use {out->val buffer=ByteArray(65536);while(true){val n=input.read(buffer);if(n<0)break;length+=n;require(length<=(if(system(displayName)=="NDS")512L else 32L)*1024*1024){"ROM too large"};digest.update(buffer,0,n);out.write(buffer,0,n)}}}
   require(length>=when(system(displayName)){"GBA"->192;"NDS"->512;else->336}){"Invalid ROM"}
   val hash=digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)}
   val file=cached(context,hash) ?: File(folder,"$hash.${displayName.substringAfterLast('.').lowercase(java.util.Locale.ROOT)}")
   if(!file.exists())check(temp.renameTo(file))
   return file to displayName
  } finally {temp.delete()}
 }
}
