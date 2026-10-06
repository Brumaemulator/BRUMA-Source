package com.linkcore.emulator

import android.content.Context
import java.io.File
import java.util.zip.InflaterInputStream

/** Conservative classification; uncertain games retain the existing modern engine. */
object RpgEngine {
 @JvmStatic fun available(c:Context)=File(c.applicationInfo.nativeLibraryDir,"libbruma_classic.so").isFile
 @JvmStatic fun classic(c:Context,dir:File):Boolean {
  if(!available(c))return false
  val prefs=c.getSharedPreferences("rpg",0)
  when(prefs.getString("engine:"+dir.name,"auto")){"modern"->return false;"classic"->return true}
  val pack=File(dir,"Data/Scripts.rxdata")
  return try {
   val bytes=readPack(pack)
   // Content-based cache: replacement archives can retain size and modification time.
   val hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt() and 255)}
   val key="engine-detect-v3:$hash"
   if(prefs.contains(key))prefs.getBoolean(key,false) else {
    val result=detectBytes(bytes)
    prefs.edit().putBoolean(key,result).apply();result
   }
  }catch(e:Exception){
   // Do not permanently cache a failed or incomplete inspection.
   android.util.Log.w("BrumaEngine","Could not inspect scripts; keeping default engine",e)
   false
  }
 }
 private fun readPack(file:File):ByteArray {
  require(file.length() in 2..(16L*1024*1024))
  return file.inputStream().use {input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=16*1024*1024);out.write(buffer,0,n)};out.toByteArray()}
 }
 internal fun detect(file:File):Boolean = detectBytes(readPack(file))
 private fun detectBytes(bytes:ByteArray):Boolean {
  val rows=MarshalScripts(bytes).read() as List<*>
  val scripts=ArrayList<String>();var total=0
  for(row in rows){
   val compressed=(row as? List<*>)?.getOrNull(2) as? ByteArray ?: continue
   val inflated=InflaterInputStream(compressed.inputStream()).use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=8*1024*1024);out.write(buffer,0,n)};out.toByteArray()}
   total+=inflated.size;require(total<=64*1024*1024)
   scripts.add(String(inflated,Charsets.ISO_8859_1))
  }
  require(scripts.isNotEmpty())
  return classify(scripts)
 }
 // A reference to System.uptime or Essentials::VERSION is not a Ruby version
 // requirement. Shared plugins contain these references on both generations.
 internal fun classify(scripts:List<String>):Boolean {
  var oldSyntax=false;var legacyWindow=false;var legacyDefinition=false
  for(script in scripts){
   val code=codeOnly(script)
   if(Regex("(?m)^\\s*when\\s+-?\\d+(?:\\s*,\\s*-?\\d+)*\\s*:(?!:)").containsMatchIn(code) ||
      Regex("\\bretry\\s+if\\s+deleting\\s*==\\s*false\\b").containsMatchIn(code))oldSyntax=true
   if(Regex("\\bdef\\s+Win32API\\s*\\.\\s*pbFindRgssWindow\\b").containsMatchIn(code) &&
      Regex("@@GetCurrentThreadId\\s*=").containsMatchIn(code))legacyWindow=true
   if(Regex("(?m)^\\s*(?:ESSENTIALS_VERSION|ESSENTIALSVERSION)\\s*=(?!=)").containsMatchIn(code))legacyDefinition=true
  }
  return oldSyntax || (legacyWindow && legacyDefinition)
 }
 // Mask comments and quoted/heredoc text while preserving line boundaries.
 // This is a bounded lexical filter, not an evaluator: game code is never run.
 private fun codeOnly(source:String):String {
  val result=StringBuilder(source.length);var quote='\u0000';var escaped=false;var block=false
  var heredoc:String?=null;var percentOpen='\u0000';var percentClose='\u0000';var percentDepth=0
  for(line in source.lineSequence()){
   if(heredoc!=null){if(line.trim()==heredoc)heredoc=null;result.append('\n');continue}
   if(quote=='\u0000' && percentDepth==0 && line.startsWith("=begin")){block=true}
   if(block){if(line.startsWith("=end"))block=false;result.append('\n');continue}
   var i=0
   while(i<line.length){
    val c=line[i]
    if(percentDepth>0){
     result.append(' ')
     if(escaped)escaped=false else if(c=='\\')escaped=true else if(c==percentClose)percentDepth-- else if(c==percentOpen && percentOpen!=percentClose)percentDepth++
     i++;continue
    }
    if(quote!='\u0000'){
     result.append(' ')
     if(escaped)escaped=false else if(c=='\\')escaped=true else if(c==quote)quote='\u0000'
     i++;continue
    }
    if(c=='#')break
    if(c=='%'){
     val match=Regex("^%(?:[qQwWrixs])?([^A-Za-z0-9\\s])").find(line.substring(i))
     if(match!=null){percentOpen=match.groupValues[1][0];percentClose=when(percentOpen){'('->')';'['->']';'{'->'}';'<'->'>';else->percentOpen};percentDepth=1;result.append(' ');i+=match.value.length;continue}
    }
    if(c=='\'' || c=='"' || c=='`'){quote=c;result.append(' ');i++;continue}
    if(c=='<' && i+1<line.length && line[i+1]=='<'){
     val match=Regex("^<<[-~]?['\"]?([A-Z_][A-Z_0-9]*)['\"]?").find(line.substring(i))
     if(match!=null){heredoc=match.groupValues[1];result.append(' ');i+=match.value.length;continue}
    }
    result.append(c);i++
   }
   result.append('\n')
  }
  return result.toString()
 }
 private class MarshalScripts(private val bytes:ByteArray){
  var p=0;val refs=ArrayList<Any?>();val symbols=ArrayList<ByteArray>()
  fun byte():Int{require(p<bytes.size);return bytes[p++].toInt() and 255}
  fun int():Int {val n=byte().toByte().toInt();if(n==0)return 0;if(n>4)return n-5;if(n< -4)return n+5;var v=if(n<0)-1 else 0;repeat(kotlin.math.abs(n)){i->v=(v and (255 shl (8*i)).inv()) or (byte() shl (8*i))};return v}
  fun raw():ByteArray {val n=int();require(n>=0 && n<=bytes.size-p);val out=bytes.copyOfRange(p,p+n);p+=n;return out}
  fun read():Any? {require(byte()==4 && byte()==8);return value(0)}
  fun value(depth:Int):Any? {
   require(depth<32 && refs.size<100000)
   return when(byte().toChar()){
    '0'->null;'T'->true;'F'->false;'i'->int()
    '"'->raw().also{refs.add(it)}
    '['->{val n=int();require(n in 0..100000);val a=ArrayList<Any?>();refs.add(a);repeat(n){a.add(value(depth+1))};a}
    ':'->raw().also{symbols.add(it)}
    ';'->symbols[int()]
    '@'->refs[int()]
    'I'->{val v=value(depth+1);val n=int();require(n in 0..100);repeat(n){value(depth+1);value(depth+1)};v}
    else->error("Unsupported script metadata")
   }
  }
 }
}
