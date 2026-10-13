package com.linkcore.emulator
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract as Docs
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest

object RpgIdentity {
 @JvmStatic fun of(context:Context,uri:Uri):String = runCatching {
  fun child(parent:Uri,name:String):Uri? {
   if(parent.scheme=="file")return File(parent.path!!).listFiles()?.firstOrNull{it.name.equals(name,true)}?.let{Uri.fromFile(it)}
   return context.contentResolver.query(Docs.buildChildDocumentsUriUsingTree(parent,Docs.getDocumentId(parent)),arrayOf(Docs.Document.COLUMN_DOCUMENT_ID,Docs.Document.COLUMN_DISPLAY_NAME),null,null,null)?.use{q->var result:Uri?=null;while(q.moveToNext())if(q.getString(1).equals(name,true))result=Docs.buildDocumentUriUsingTree(parent,q.getString(0));result}
  }
  val ini=child(uri,"Game.ini") ?: return ""
  val data=child(uri,"Data") ?: return ""
  val scripts=child(data,"Scripts.rxdata") ?: return ""
  val inputs=listOfNotNull(ini,scripts,child(data,"MapInfos.rxdata"),child(data,"System.rxdata"))
  val signature=inputs.joinToString("|"){u->if(u.scheme=="file"){val f=File(u.path!!);"$u:${f.length()}:${f.lastModified()}"}else context.contentResolver.query(u,arrayOf(Docs.Document.COLUMN_SIZE,Docs.Document.COLUMN_LAST_MODIFIED),null,null,null)?.use{q->if(q.moveToFirst())"$u:${q.getLong(0)}:${q.getLong(1)}" else u.toString()} ?: u.toString()}
  val cache=context.getSharedPreferences("rpg-content-identity",0);val cacheKey=LibraryStore.key(signature)
  cache.getString(cacheKey,null)?.let{return it}
  val digest=MessageDigest.getInstance("SHA-256")
  inputs.forEachIndexed{index,u->digest.update(index.toByte());context.contentResolver.openInputStream(u)!!.use{input->val b=ByteArray(65536);var total=0L;while(true){val n=input.read(b);if(n<0)break;total+=n;require(total<=64L*1024*1024);digest.update(b,0,n)}}}
  val hash=digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)};cache.edit().putString(cacheKey,hash).apply();hash
 }.getOrDefault("")
 @JvmStatic fun sourceExists(context:Context,source:String):Boolean {
  if(source.isBlank())return true
  val uri=Uri.parse(source.substringBefore("#rpg=").substringBefore("#rom="))
  if(uri.scheme=="file")return File(uri.path!!).exists()
  return try {
   context.contentResolver.query(uri,arrayOf(Docs.Document.COLUMN_DOCUMENT_ID),null,null,null)?.use{it.moveToFirst()} ?: sourceInParent(context,uri)
  } catch(e:FileNotFoundException) {false} catch(e:SecurityException) {true} catch(e:Exception) {sourceInParent(context,uri)}
 }
 // ExternalStorageProvider may wrap a missing document in IllegalArgumentException.
 // Only treat that as deletion after successfully listing its accessible parent.
 @JvmStatic fun confirmedMissing(context:Context,source:String):Boolean {
  if(source.isBlank())return false
  val uri=Uri.parse(source.substringBefore("#rpg=").substringBefore("#rom="))
  if(uri.scheme=="file")return uri.path?.let{!File(it).exists()} ?: false
  return !sourceInParent(context,Uri.parse(source.substringBefore("#rpg=").substringBefore("#rom=")))
 }
 private fun sourceInParent(context:Context,input:Uri):Boolean {
  if(input.authority!="com.android.externalstorage.documents")return true
  return try {
   // ACTION_OPEN_DOCUMENT returns /document/... rather than /tree/.../document/...
   // even for the same file. Use an existing granted folder to verify its parent.
   // No grant or a failed listing still means unknown, never proven deletion.
   val uri=if(Docs.isTreeUri(input))input else {
    val id=Docs.getDocumentId(input)
    val tree=context.contentResolver.persistedUriPermissions.asSequence()
     .filter{it.isReadPermission && it.uri.authority==input.authority && Docs.isTreeUri(it.uri)}
     .map{it.uri}.filter{val root=Docs.getTreeDocumentId(it);id==root || id.startsWith(root+"/")}
     .maxByOrNull{Docs.getTreeDocumentId(it).length} ?: return true
    Docs.buildDocumentUriUsingTree(tree,id)
   }
   val root=Docs.getTreeDocumentId(uri)
   var child=Docs.getDocumentId(uri)
   // The immediate parent may have been deleted along with the game. Climb only
   // within the granted tree and require a successful ancestor listing as proof.
   while(child.contains('/') && child!=root) {
    val parent=child.substringBeforeLast('/')
    if(parent!=root && !parent.startsWith(root+"/"))return true
    val exists=runCatching {
     context.contentResolver.query(Docs.buildChildDocumentsUriUsingTree(uri,parent),arrayOf(Docs.Document.COLUMN_DOCUMENT_ID),null,null,null)?.use {rows->
      var found=false;while(rows.moveToNext())if(rows.getString(0)==child)found=true
      found
     }
    }.getOrNull()
    if(exists!=null)return exists
    child=parent
   }
   true
  } catch(e:Exception) {true}
 }
}
