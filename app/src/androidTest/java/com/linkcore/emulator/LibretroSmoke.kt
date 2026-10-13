package com.linkcore.emulator

import android.app.Instrumentation
import android.graphics.BitmapFactory

object LibretroSmoke {
    fun run(i:Instrumentation) {
        check(!LibraryGame("test","Test",cached="abc").hasCover)
        check(!LibraryGame("test","Test",cover="file:///data/user/0/com.linkcore.emulator/files/covers/game-abc.png",cached="abc").hasCover)
        check(LibraryGame("test","Test",cover="file:///covers/libretro-123.png",cached="abc").hasCover)
        check(LibraryGame("test","Test",cover="content://images/custom.png",cached="abc").hasCover)
        val names=listOf("Pokemon - FireRed Version (USA).png","Pokemon - FireRed Version (Europe).png","Pokemon - Edicion Rojo Fuego (Spain).png","Metroid Fusion (USA).png")
        check(LibretroClient.match("Metroid_Fusion.gba",names,false)=="Metroid Fusion (USA).png")
        check(LibretroClient.match("Pokémon - FireRed Version.gba",names,false)==names[0])
        check(LibretroClient.match("Pokemon.gba",names,false)==null)
        check(LibretroClient.match("Unknown hack.gba",names,true)==null)
        val ndsNames=listOf("Pokemon - Edicion Diamante (Spain) (Rev 5).png","Pokemon - Diamond Version (USA).png","Nintendogs - Labrador & Friends (Europe) (En,Fr,De,Es,It).png")
        check(LibretroClient.match("1248 - Pokemon - Edicion Diamante (Spain) (Rev 5).nds",ndsNames,true)==ndsNames[0])
        check(LibretroClient.match("0102 - Nintendogs - Labrador & Friends (Europe) (En,Fr,De,Es,It).nds",ndsNames,true)==ndsNames[2])
        check(LibretroClient.parseIndex("""<a href="Metroid%20Fusion%20(USA).png">x</a><a href="../bad.png">x</a><a href="https://evil.example/image.png">x</a>""")==listOf("Metroid Fusion (USA).png"))
        val client=LibretroClient(i.targetContext)
        val catalog=client.catalog()
        check(catalog.size>1000) {"Catalog missing"}
        val cover=checkNotNull(LibretroClient.match("Metroid Fusion (USA).gba",catalog,false))
        val bytes=client.fetchCover(cover)
        val bitmap=checkNotNull(BitmapFactory.decodeByteArray(bytes,0,bytes.size)) {"Live cover invalid"}
        check(bitmap.width>0 && bitmap.height>0);bitmap.recycle()
        client.cancel()
        try {client.catalog();error("Cancellation ignored")} catch(e:CoverFailure) {check(e.messageId==R.string.art_cancelled)}
    }
}
