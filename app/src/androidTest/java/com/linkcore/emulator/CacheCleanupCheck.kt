package com.linkcore.emulator
import android.app.Instrumentation
import android.net.Uri
import java.io.File

object CacheCleanupCheck {
 fun run(i:Instrumentation) {
  val c=i.targetContext
  val root=File(c.filesDir,"rpg-games")
  val dir=File(root,"cleanup-${System.nanoTime()}.pending").apply{mkdirs()}
  var retained:File?=null
  fun put(path:String,value:String) {val f=File(dir,path);f.parentFile!!.mkdirs();f.writeText(value)}
  try {
   put("Game.ini","[Game]\nTitle=Cleanup fixture\n")
   put("Data/Scripts.rxdata","isolated fixture scripts")
   put("Data/custom-progress.rxdata","unknown custom save")
   put("Graphics/Characters/test.png","image fixture")
   put("Audio/BGM/test.ogg","music fixture")
   put("UserData/File-A.rxdata","saved progress")
   put("bruma-origin.txt","content://unavailable.test/document/game")
   val identity=RpgIdentity.of(c,Uri.fromFile(dir));check(identity.isNotEmpty())
   retained=File(c.filesDir,"rpg-retained/$identity");check(!retained!!.exists())
   val snapshot=RpgCacheCleanup.preserve(dir,retained!!)
   check(!File(snapshot,"Graphics/Characters/test.png").exists())
   check(!File(snapshot,"Audio/BGM/test.ogg").exists())
   check(File(snapshot,"Data/custom-progress.rxdata").readText()=="unknown custom save")
   check(File(snapshot,"UserData/File-A.rxdata").readText()=="saved progress")
   check(runCatching {RpgCacheCleanup.removeCopy(root,root)}.isFailure)
   val linked=File(dir,"linked")
   android.system.Os.symlink(c.cacheDir.absolutePath,linked.absolutePath)
   check(runCatching {RpgCacheCleanup.preserve(dir,retained!!)}.isFailure)
   java.nio.file.Files.delete(linked.toPath())
   check(RpgCacheCleanup.removeCopy(root,dir)>0 && !dir.exists())
   put("Game.ini","[Game]\nTitle=Cleanup fixture\n")
   put("Data/Scripts.rxdata","isolated fixture scripts")
   put("UserData/File-A.rxdata","packaged save")
   RpgCacheCleanup.restore(c,dir)
   check(File(dir,"UserData/File-A.rxdata").readText()=="saved progress")
   check(File(dir,"Data/custom-progress.rxdata").readText()=="unknown custom save")
   put("UserData/File-A.rxdata","new progress")
   RpgCacheCleanup.restore(c,dir)
   check(File(dir,"UserData/File-A.rxdata").readText()=="new progress")
   check(!RpgIdentity.confirmedMissing(c,""))
   check(!RpgIdentity.confirmedMissing(c,"content://unavailable.test/document/game"))
   val tree="content://com.android.externalstorage.documents/tree/primary%3AJuegos%20gba/document/"
   check(!RpgIdentity.confirmedMissing(c,tree+Uri.encode("primary:Juegos gba/Pokemon Cuerpo de Cristal (v1.3).zip")))
   check(RpgIdentity.confirmedMissing(c,tree+Uri.encode("primary:Juegos gba/Pokémon Ente Beta 1.zip")))
   check(!RpgIdentity.confirmedMissing(c,tree+Uri.encode("primary:missing-parent-bruma/game.zip")))
  } finally {if(dir.exists())RpgCacheCleanup.removeCopy(root,dir);retained?.let {check(it.canonicalPath.startsWith(File(c.filesDir,"rpg-retained").canonicalPath+File.separator));it.deleteRecursively()}}
 }
}

