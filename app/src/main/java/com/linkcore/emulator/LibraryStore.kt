package com.linkcore.emulator

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract as Docs
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

 data class LibraryGame(val key:String,val name:String,val uri:String="",val cover:String="",val cached:String="",val size:Long=0,val modified:Long=0,val system:String="GBA",val identity:String="",val source:String="") {
    val hasCover:Boolean get()=cover.isNotBlank() && !(cached.isNotBlank() && Uri.parse(cover).scheme=="file" && Uri.parse(cover).lastPathSegment=="game-$cached.png")
    val title:String get()=name.replace(Regex("(?i)\\.(gba|gbc|gb|nds)$"),"").replace('_',' ')
}

/** Stores only metadata. Source folders and their files are never modified. */
class LibraryStore(private val context:Context) {
    private val index=AtomicFile(File(context.filesDir,"game-library.json"))
    companion object {
        fun key(text:String)=MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") {"%02x".format(it.toInt() and 255)}
        fun coverName(name:String)=name.substringBeforeLast('.').lowercase(java.util.Locale.ROOT)
    }
    @Synchronized fun read():List<LibraryGame> = runCatching {
        val array=JSONArray(index.openRead().bufferedReader().use {it.readText()})
        (0 until array.length()).map {n->val j=array.getJSONObject(n);LibraryGame(j.getString("key"),j.getString("name"),j.optString("uri"),j.optString("cover"),j.optString("cached"),j.optLong("size"),j.optLong("modified"),j.optString("system","GBA"),j.optString("identity"),j.optString("source"))}
    }.getOrDefault(emptyList())
    @Synchronized private fun write(games:List<LibraryGame>) {
        val array=JSONArray()
        games.forEach {g->array.put(JSONObject().put("key",g.key).put("name",g.name).put("uri",g.uri).put("cover",g.cover).put("cached",g.cached).put("size",g.size).put("modified",g.modified).put("system",g.system).put("identity",g.identity).put("source",g.source))}
        val stream=index.startWrite()
        try {stream.write(array.toString().toByteArray());index.finishWrite(stream)} catch(e:Exception){index.failWrite(stream);throw e}
    }
    // Only local metadata is needed for the first paint; validation happens in scan.
    fun cachedGames():List<LibraryGame> = read().map {g->
        if(g.system=="RPG Maker XP" && g.cached.isNotBlank())
            g.copy(uri=Uri.fromFile(File(context.filesDir,"rpg-games/${g.cached}")).toString()) else g
    }.sortedWith(compareBy<LibraryGame>{it.system}.thenBy{it.title.lowercase()})
    private fun scanFingerprint(uri:String,size:Long,modified:Long):String {
        val cache=context.getSharedPreferences("rom-scan-signatures",0)
        val key=key(uri);val signature="$size:$modified:"
        val old=cache.getString(key,null)
        // Providers with unknown metadata must be re-read, never assumed unchanged.
        if(size>0 && modified>0 && old!=null && old.startsWith(signature))return old.removePrefix(signature)
        val hash=fingerprint(uri)
        if(hash.isNotBlank() && size>0 && modified>0)cache.edit().putString(key,signature+hash).apply()
        return hash
    }
    private fun fingerprint(uri:String):String = runCatching {
        val digest=MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(Uri.parse(uri))!!.use {input->
            val buffer=ByteArray(65536);var total=0L
            while(true) {val n=input.read(buffer);if(n<0) break;total+=n;require(total<=512L*1024*1024);digest.update(buffer,0,n)}
            require(total>=192)
        }
        digest.digest().joinToString("") {"%02x".format(it.toInt() and 255)}
    }.getOrDefault("")
    private fun hydrate(g:LibraryGame):LibraryGame {
        if(g.system!="RPG Maker XP")return g.copy(source=RomCacheCleanup.source(context,g))
        val dir=File(context.filesDir,"rpg-games/${g.cached}").takeIf{g.cached.isNotEmpty() && File(it,"Game.ini").isFile()}
        val origin=dir?.let{File(it,"bruma-origin.txt").takeIf{it.isFile}?.readText()?.trim()}
        val source=g.source.ifBlank{origin?.substringBefore("#rpg=") ?: g.uri.takeIf{it.startsWith("content:")} ?: ""}
        val identity=g.identity.ifBlank{val uri=dir?.let{Uri.fromFile(it)} ?: Uri.parse(g.uri);RpgIdentity.of(context,uri)}
        return g.copy(source=source,identity=identity)
    }
    private fun live(g:LibraryGame)=g.system!="RPG Maker XP" || RpgIdentity.sourceExists(context,g.source)
    @Synchronized fun linkRpg(source:String,dir:File) {
        val identity=RpgIdentity.of(context,Uri.fromFile(dir))
        val games=read().map{hydrate(it)}
        val linked=games.map{if(it.system=="RPG Maker XP" && (it.source==source || it.uri==source || (identity.isNotBlank() && it.identity==identity)))it.copy(uri=Uri.fromFile(dir).toString(),cached=dir.name,source=source,identity=identity) else it}
        write(unique(linked))
    }
    private fun unique(games:List<LibraryGame>):List<LibraryGame> {
        val result=linkedMapOf<String,LibraryGame>()
        for(raw in games) {
            val game=hydrate(raw)
            if(!live(game))continue
            val identity=if(game.system=="RPG Maker XP" && game.identity.isNotBlank()) "rpg:"+game.identity else if(game.cached.isNotBlank()) game.system+":"+game.cached else "entry:"+game.key
            val old=result[identity]
            if(old==null) result[identity]=game else {
                val primary=if((old.source.contains(".zip",true) && !game.source.contains(".zip",true)) || (old.uri.isBlank() && game.uri.isNotBlank())) game else old
                val cover=listOf(old,game).firstOrNull {it.hasCover && it.cover.startsWith("file:")}?.cover
                    ?: listOf(primary,old,game).firstOrNull {it.hasCover}?.cover ?: ""
                val local=listOf(primary,old,game).firstOrNull {it.cached.isNotEmpty() && File(context.filesDir,"rpg-games/${it.cached}/Game.ini").isFile()}
                result[identity]=if(primary.system=="RPG Maker XP" && local!=null)primary.copy(cover=cover,cached=local.cached,uri=Uri.fromFile(File(context.filesDir,"rpg-games/${local.cached}")).toString()) else primary.copy(cover=cover)
            }
        }
        return result.values.toList()
    }
    @Synchronized fun remember(uri:String,name:String,id:String,system:String=RomFiles.system(name).ifEmpty {"GBA"}) {
        RomCacheCleanup.record(context,LibraryGame(key(uri),name,uri,cached=id,system=system,source=uri))
        val old=read().toMutableList();val n=old.indexOfFirst {it.uri==uri && uri.isNotEmpty()}
        if(n>=0) old[n]=old[n].copy(cached=id,system=system,source=old[n].source.ifBlank{uri}) else if(old.none {it.cached==id}) old+=LibraryGame(key(if(uri.isEmpty()) id else uri),name,uri,cached=id,system=system,source=uri)
        old.forEach{RomCacheCleanup.record(context,hydrate(it))}
        write(unique(old))
    }
    @Synchronized fun add(games:List<LibraryGame>) {
        games.forEach{RomCacheCleanup.record(context,hydrate(it))}
        write(unique(read()+games))
    }
    @Synchronized fun setCover(key:String,path:String) {write(allGames().map {if(it.key==key) it.copy(cover=path) else it})}
    @Synchronized fun mergeImported() {
        val list=read().map {if(it.system in setOf("GBA","GB","GBC") && it.cached.isBlank() && it.uri.isNotBlank()) it.copy(cached=fingerprint(it.uri)) else it}.toMutableList()
        File(context.filesDir,"roms").listFiles()?.filter {it.name.matches(Regex("[a-f0-9]{64}\\.(gba|gbc|gb|nds)"))}?.forEach {f->
            if(!RomCacheCleanup.deleted(context,f.nameWithoutExtension) && list.none {it.cached==f.nameWithoutExtension}) {
                val title=runCatching {f.inputStream().use {input->input.skip(when(f.extension){"gba"->160;"nds"->0;else->308});val b=ByteArray(12);input.read(b);String(b,Charsets.US_ASCII).trim {it<=' '}}}.getOrDefault("GBA")
                list+=LibraryGame(key(f.name),title.ifBlank {"GBA"},cached=f.nameWithoutExtension,system=RomFiles.system(f.name))
            }
        }
        write(unique(list.filter {it.system=="RPG Maker XP" || it.uri.isNotEmpty() || RomFiles.cached(context,it.cached)!=null}))
    }
    fun allGames():List<LibraryGame> {
        val items=read().map{hydrate(it)}.filter{live(it)}.associateBy {it.key}.toMutableMap()
        File(context.filesDir,"rpg-games").listFiles()?.filter {it.isDirectory && !it.name.endsWith(".pending") && File(it,"Game.ini").isFile() && File(it,"Data/Scripts.rxdata").isFile()}?.forEach {dir->
            val origin=File(dir,"bruma-origin.txt").takeIf {it.isFile}?.readText()?.trim()
            if(origin!=null && !RpgIdentity.sourceExists(context,origin))return@forEach
            val id=key(origin ?: dir.absolutePath)
            val title=File(dir,"Game.ini").readLines().firstOrNull {it.trim().startsWith("Title=")}?.substringAfter('=')?.trim() ?: dir.name
            val previous=items[id]
            if(previous!=null && previous.cached.isNotEmpty() && previous.cached!=dir.name && File(context.filesDir,"rpg-games/${previous.cached}/Game.ini").isFile())return@forEach
            items[id]=LibraryGame(id,title,Uri.fromFile(dir).toString(),previous?.cover ?: "",dir.name,system="RPG Maker XP")
        }
        return unique(items.values.toList()).sortedWith(compareBy<LibraryGame> {it.system}.thenBy {it.title.lowercase()})
    }
    fun preview(existing:List<LibraryGame>,found:List<LibraryGame>):List<LibraryGame> {
        val merged=existing.toMutableList()
        for(game in found) {
            val n=merged.indexOfFirst {it.key==game.key || (game.identity.isNotBlank() && it.identity==game.identity) || (game.cached.isNotBlank() && it.cached==game.cached)}
            if(n<0)merged+=game else {
                val old=merged[n]
                merged[n]=game.copy(cover=old.cover.takeIf {old.hasCover} ?: game.cover,
                    cached=game.cached.ifBlank {old.cached},
                    uri=if(game.system=="RPG Maker XP" && old.cached.isNotBlank())old.uri else game.uri)
            }
        }
        return merged.sortedWith(compareBy<LibraryGame>{it.system}.thenBy{it.title.lowercase()})
    }
    fun scan(tree:Uri,onGame:((LibraryGame)->Unit)?=null):List<LibraryGame> {
        val found=ArrayList<LibraryGame>();val visited=HashSet<String>()
        val archives=ArrayList<Array<String>>()
        fun emit(game:LibraryGame) {found+=game;onGame?.invoke(game)}
        fun visit(id:String,depth:Int) {
            require(depth<=8) {context.getString(R.string.library_limit)}
            if(!visited.add(id)) return
            require(visited.size<=512 && found.size<=2000) {context.getString(R.string.library_limit)}
            val children=Docs.buildChildDocumentsUriUsingTree(tree,id)
            val rows=ArrayList<Array<String>>()
            context.contentResolver.query(children,arrayOf(Docs.Document.COLUMN_DOCUMENT_ID,Docs.Document.COLUMN_DISPLAY_NAME,Docs.Document.COLUMN_MIME_TYPE,Docs.Document.COLUMN_SIZE,Docs.Document.COLUMN_LAST_MODIFIED),null,null,null)?.use {c->
                while(c.moveToNext()) {require(rows.size<10000){context.getString(R.string.library_limit)};rows+=arrayOf(c.getString(0),c.getString(1) ?: "",c.getString(2) ?: "",c.getLong(3).toString(),c.getLong(4).toString())}
            } ?: error(context.getString(R.string.library_unavailable))
            if(rows.any {it[1].equals("Game.ini",true)} && rows.any {it[1].equals("Data",true) && it[2]==Docs.Document.MIME_TYPE_DIR}) {
                val data=rows.first {it[1].equals("Data",true) && it[2]==Docs.Document.MIME_TYPE_DIR}
                val hasScripts=context.contentResolver.query(Docs.buildChildDocumentsUriUsingTree(tree,data[0]),arrayOf(Docs.Document.COLUMN_DISPLAY_NAME),null,null,null)?.use {cursor->
                    var foundScript=false;while(cursor.moveToNext())if(cursor.getString(0).equals("Scripts.rxdata",true))foundScript=true;foundScript
                } ?: false
                if(!hasScripts)return
                val uri=Docs.buildDocumentUriUsingTree(tree,id).toString()
                val ini=rows.first {it[1].equals("Game.ini",true)}
                val title=context.contentResolver.openInputStream(Docs.buildDocumentUriUsingTree(tree,ini[0]))?.bufferedReader()?.use {reader->
                    reader.lineSequence().take(100).firstOrNull {it.trim().startsWith("Title=")}?.substringAfter('=')?.trim()
                } ?: id.substringAfterLast('/')
                emit(LibraryGame(key(uri),title,uri,system="RPG Maker XP",identity=RpgIdentity.of(context,Uri.parse(uri)),source=uri))
                return
            }
            val images=rows.filter {it[1].substringAfterLast('.').lowercase() in setOf("png","jpg","jpeg","webp")}.associate {coverName(it[1]) to Docs.buildDocumentUriUsingTree(tree,it[0]).toString()}
            for(r in rows.sortedBy {if(RomFiles.system(it[1]).isNotEmpty())0 else 1}) {
                if(r[2]==Docs.Document.MIME_TYPE_DIR) visit(r[0],depth+1)
                else if(r[1].endsWith(".zip",true)) {
                    archives+=r
                } else if(RomFiles.system(r[1]).isNotEmpty()) {
                    require(found.size<2000){context.getString(R.string.library_limit)}
                    val uri=Docs.buildDocumentUriUsingTree(tree,r[0]).toString()
                    emit(LibraryGame(key(uri),r[1],uri,images[coverName(r[1])] ?: "",cached=scanFingerprint(uri,r[3].toLong(),r[4].toLong()),size=r[3].toLong(),modified=r[4].toLong(),system=RomFiles.system(r[1])))
                }
            }
        }
        visit(Docs.getTreeDocumentId(tree),0)
        // Discover loose games throughout the tree before doing any costly extraction.
        for(r in archives) {
            val imported=runCatching {GameArchives.import(context,Docs.buildDocumentUriUsingTree(tree,r[0]),r[3].toLong(),r[4].toLong())}.onFailure {android.util.Log.w("BrumaLibrary","ZIP skipped: ${r[1]}",it)}.getOrDefault(emptyList())
            imported.forEach {emit(hydrate(it))}
        }
        return found
    }
    @Synchronized fun replaceFolder(found:List<LibraryGame>,trees:List<Uri> = emptyList(),protected:Set<String> = emptySet()) {
        val old=read().flatMap {raw->
            val g=hydrate(raw)
            if(g.system=="RPG Maker XP")listOf(g) else listOf(g)+RomCacheCleanup.origins(context,g.cached).filter{it!=g.source}.map{origin->g.copy(key=key(origin),source=origin,uri=origin.substringBefore("#rom="))}
        }
        val present=found.map{it.key}.toSet()
        fun missing(g:LibraryGame):Boolean {
            if(g.key in present)return false
            val source=g.source.ifBlank{RomCacheCleanup.source(context,g,trees)}
            return (RomCacheCleanup.belongsToScan(source,trees) && !RpgIdentity.sourceExists(context,source)) || RpgIdentity.confirmedMissing(context,source)
        }
        (old+found).forEach{RomCacheCleanup.record(context,it.copy(source=it.source.ifBlank{RomCacheCleanup.source(context,it,trees)}))}
        val removed=old.filter{missing(it)}
        val joined=found.map {g->
            val previous=old.firstOrNull{g.identity.isNotEmpty() && g.identity==it.identity} ?: old.firstOrNull {g.cached.isNotEmpty() && it.cached==g.cached} ?: old.firstOrNull {it.key==g.key}
            if(g.system=="RPG Maker XP" && previous!=null && previous.cached.isNotEmpty() && File(context.filesDir,"rpg-games/${previous.cached}/Game.ini").isFile()) g.copy(cover=previous.cover,uri=Uri.fromFile(File(context.filesDir,"rpg-games/${previous.cached}")).toString(),cached=previous.cached) else g.copy(cover=previous?.cover?.takeIf {it.startsWith("file:")} ?: g.cover,cached=g.cached)
        }.toMutableList()
        // Only a completed scan or confirmed missing source removes an entry.
        // Keep its provenance when it belongs to an inaccessible or unselected folder.
        old.filter {it !in removed && joined.none {g->g.cached==it.cached || g.key==it.key}}.forEach {joined+=it}
        val retained=unique(joined).sortedBy {it.title.lowercase()}
        write(retained)
        RomCacheCleanup.clean(context,removed,retained,protected)
    }
}





