package com.linkcore.emulator
import android.app.Instrumentation
import android.net.Uri
import android.provider.DocumentsContract as Docs
import java.io.File

object DeletedSourceCheck {
 fun run(i:Instrumentation) {
  val c=i.targetContext
  val store=LibraryStore(c)
  val game=store.read().first {it.source.contains("Ente",true)}
  val dir=File(c.filesDir,"rpg-games/${game.cached}")
  check(File(dir,"Game.ini").isFile)
  check(!RpgIdentity.sourceExists(c,game.source)) {"Deleted source still reports present"}
  val tree=Uri.parse(game.source)
  val existing=Docs.buildDocumentUriUsingTree(tree,"primary:Juegos gba/Pokemon Cuerpo de Cristal (v1.3).zip")
  check(RpgIdentity.sourceExists(c,existing.toString())) {"Existing ZIP lost"}
  check(RpgIdentity.sourceExists(c,"")) {"Legacy standalone import lost"}
  check(RpgIdentity.sourceExists(c,"content://unavailable.test/tree/test/document/test")) {"Unavailable provider treated as deletion"}
  check(store.allGames().none {it.cached==game.cached}) {"Deleted source re-added from private copy"}
  check(File(dir,"Game.ini").isFile) {"Private game was deleted"}
 }
}
