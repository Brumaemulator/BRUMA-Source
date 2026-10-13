// SPDX-License-Identifier: GPL-3.0-or-later
package com.linkcore.emulator.translations
import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import com.linkcore.emulator.*
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject

class TranslationInstaller(private val context:Context){
    companion object {
        private fun stateFile(c:Context,hash:String)=File(c.filesDir,"translations/links/$hash.json")
        private fun state(c:Context,hash:String):JSONObject?=if(!hash.matches(Regex("[a-f0-9]{64}")))null else runCatching{JSONObject(stateFile(c,hash).readText())}.getOrNull()
        fun active(c:Context,m:JSONObject)=state(c,m.getString("gameSha256"))?.optBoolean("active")==true
        fun mapGame(c:Context,g:LibraryGame):LibraryGame {
            val s=state(c,g.cached)?:return g;if(!s.optBoolean("active"))return g
            val m=s.getJSONObject("translation");val file=RomFiles.cached(c,m.getString("targetSha256"))?:return g
            val internal=Uri.parse(g.uri).scheme=="file" && Uri.parse(g.uri).path?.startsWith(c.filesDir.path+"/")==true
            return g.copy(cached=file.nameWithoutExtension,name=g.title+" ["+m.getString("language")+"]",uri=if(internal)Uri.fromFile(file).toString()else g.uri,source=if(internal)Uri.fromFile(file).toString()else g.source)
        }
        private fun gameJson(g:LibraryGame)=JSONObject().put("key",g.key).put("name",g.name).put("uri",g.uri).put("cover",g.cover).put("cached",g.cached).put("size",g.size).put("modified",g.modified).put("system",g.system).put("identity",g.identity).put("source",g.source)
        private fun game(j:JSONObject)=LibraryGame(j.getString("key"),j.getString("name"),j.optString("uri"),j.optString("cover"),j.optString("cached"),j.optLong("size"),j.optLong("modified"),j.optString("system","GBA"),j.optString("identity"),j.optString("source"))
    }
    private fun writeState(hash:String,s:JSONObject){val f=stateFile(context,hash);f.parentFile!!.mkdirs();val atomic=AtomicFile(f);val out=atomic.startWrite();try{out.write(s.toString().toByteArray());atomic.finishWrite(out)}catch(e:Exception){atomic.failWrite(out);throw e}}
    private fun removeManaged(f:File){val root=File(context.filesDir,"roms").canonicalFile;if(f.canonicalFile.parentFile==root)check(!f.exists()||f.delete())}
    private fun activate(original:LibraryGame,m:JSONObject,source:File,translated:File,undo:File){
        val hash=m.getString("gameSha256")
        val s=JSONObject().put("active",true).put("translation",m).put("originalGame",gameJson(original.copy(cached=hash))).put("undo",undo.name).put("extension",source.extension)
        writeState(hash,s)
        val internal=Uri.parse(original.uri).scheme=="file" && Uri.parse(original.uri).path?.startsWith(context.filesDir.path+"/")==true
        val translatedGame=original.copy(name=original.title+" ["+m.getString("language")+"]",cached=translated.nameWithoutExtension,uri=if(internal)Uri.fromFile(translated).toString()else original.uri,source=if(internal)Uri.fromFile(translated).toString()else original.source)
        LibraryStore(context).replaceTranslation(hash,translated.nameWithoutExtension,translatedGame)
        removeManaged(source)
    }
    fun install(game:LibraryGame,m:JSONObject){
        require(m.getString("id").matches(Regex("[a-z0-9][a-z0-9-]{0,63}")));require(m.getBoolean("approved"));require(!active(context,m)){"Translation already installed"}
        val original=RomFiles.cached(context,game.cached)?:if(game.uri.isNotBlank())RomFiles.import(context,Uri.parse(game.uri),game.name).first else error(context.getString(R.string.library_unavailable))
        val hash=PatchEngine.sha(original);require(hash==m.getString("gameSha256")){"Game hash mismatch"}
        val data=TranslationClient(context).patch(m.getString("id"),hash);val actual=MessageDigest.getInstance("SHA-256").digest(data).joinToString(""){"%02x".format(it.toInt()and 255)};require(actual==m.getString("patchSha256")){"Patch SHA-256 mismatch"}
        val folder=File(context.filesDir,"translations").apply{mkdirs()};val size=m.optLong("targetBytes").takeIf{it>0}?:original.length();require(folder.usableSpace>=original.length()+size*2+data.size){"Insufficient free space"}
        val undo=File(folder,"$hash.undo.gz");val temp=File(folder,"output-${java.util.UUID.randomUUID()}.${original.extension}");val verify=File(folder,"verify-${java.util.UUID.randomUUID()}.${original.extension}")
        try{
            PatchEngine.apply(m.getString("patchType"),original,data,temp,m.optLong("targetBytes"));require(PatchEngine.sha(temp)==m.getString("targetSha256"));require(PatchEngine.sha(original)==hash)
            TranslationUndo.create(original,temp,undo);TranslationUndo.restore(temp,undo,verify);require(PatchEngine.sha(verify)==hash);verify.delete()
            val imported=RomFiles.import(context,Uri.fromFile(temp),game.title+".${original.extension}").first
            activate(game,m,original,imported,undo)
        }finally{temp.delete();verify.delete()}
    }
    fun restore(m:JSONObject){
        val hash=m.getString("gameSha256");val s=state(context,hash)?:error("No restoration data");require(s.getBoolean("active"))
        val translated=RomFiles.cached(context,m.getString("targetSha256"))?:error("Translated game missing")
        val undo=File(context.filesDir,"translations/${s.getString("undo")}");require(undo.canonicalFile.parentFile==File(context.filesDir,"translations").canonicalFile)
        val tmp=File(context.filesDir,"translations/restore-${java.util.UUID.randomUUID()}.${s.getString("extension")}")
        try{
            TranslationUndo.restore(translated,undo,tmp);require(PatchEngine.sha(tmp)==hash)
            val original=game(s.getJSONObject("originalGame"));val restored=RomFiles.import(context,Uri.fromFile(tmp),original.title+".${s.getString("extension")}").first
            s.put("active",false);writeState(hash,s)
            LibraryStore(context).replaceTranslation(hash,translated.nameWithoutExtension,original.copy(cached=hash))
            removeManaged(translated);undo.delete()
        }finally{tmp.delete()}
    }
    /** One-time migration of full backups created by the earlier pilot. */
    fun migrateLegacy(){
        val folder=File(context.filesDir,"translations");folder.listFiles()?.filter{it.extension=="json"}?.forEach{history->
            val old=runCatching{JSONObject(history.readText())}.getOrNull()?:return@forEach
            val m=old.optJSONObject("translation")?:return@forEach;val hash=m.getString("gameSha256")
            if(state(context,hash)!=null)return@forEach
            val backup=File(folder,"originals/$hash.${RomFiles.cached(context,m.getString("targetSha256"))?.extension?:return@forEach}")
            val translated=RomFiles.cached(context,m.getString("targetSha256"))?:return@forEach
            if(!backup.isFile)return@forEach
            require(PatchEngine.sha(backup)==hash && PatchEngine.sha(translated)==m.getString("targetSha256"))
            val source=RomFiles.cached(context,hash)?:backup
            val original=LibraryStore(context).read().firstOrNull{it.cached==hash}?:LibraryGame(LibraryStore.key(hash),m.getString("game"),Uri.fromFile(File(context.filesDir,"roms/$hash.${backup.extension}")).toString(),cached=hash,system=m.getString("system"))
            val undo=File(folder,"$hash.undo.gz");TranslationUndo.create(backup,translated,undo)
            val verify=File(folder,"migration-${java.util.UUID.randomUUID()}.${backup.extension}")
            try{TranslationUndo.restore(translated,undo,verify);require(PatchEngine.sha(verify)==hash);activate(original,m,source,translated,undo);check(backup.delete());history.delete()}finally{verify.delete()}
        }
    }
}
