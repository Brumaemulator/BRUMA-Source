package com.linkcore.emulator

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/** Reclaims extracted RPG assets only after a successful source-parent listing proves deletion. */
object RpgCacheCleanup {
    private fun engineRunning(c:Context):Boolean {
        val manager=c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.runningAppProcesses?.any {it.processName=="${c.packageName}:rpg" || it.processName=="${c.packageName}:rpgclassic"} ?: true
    }
    private fun checkedFiles(root:File):List<File> {
        val base=root.canonicalFile
        check(base.isDirectory && !java.nio.file.Files.isSymbolicLink(root.toPath()))
        return root.walkTopDown().onFail {_,e->throw e}.toList().also {files->
            files.forEach {check(!java.nio.file.Files.isSymbolicLink(it.toPath()) && it.canonicalFile==File(base,it.relativeTo(root).path)) {"Unsafe cache path"}}
        }
    }
    private fun hash(f:File):ByteArray {
        val digest=MessageDigest.getInstance("SHA-256")
        f.inputStream().use {input->val b=ByteArray(65536);while(true){val n=input.read(b);if(n<0)break;digest.update(b,0,n)}}
        return digest.digest()
    }
    // Unknown/custom data is retained. Only standard game resources are disposable.
    internal fun asset(path:String):Boolean {
        val p=path.lowercase(Locale.ROOT);val name=p.substringAfterLast('/');val ext=name.substringAfterLast('.',"")
        if(p.startsWith("audio/") && ext in setOf("ogg","mp3","wav","mid","midi","flac","wma","m4a"))return true
        if(p.startsWith("graphics/") && ext in setOf("png","jpg","jpeg","bmp","gif","webp"))return true
        if(p.startsWith("fonts/") && ext in setOf("ttf","otf"))return true
        if(p.startsWith("movies/") && ext in setOf("mp4","avi","webm","ogv"))return true
        if(p.matches(Regex("data/(map[0-9]+|mapinfos|scripts|system|actors|classes|skills|items|weapons|armors|enemies|troops|states|animations|tilesets|commonevents)\\.rxdata")))return true
        return !p.contains('/') && ext in setOf("exe","dll","lnk")
    }
    internal fun preserve(dir:File,retained:File):File {
        val files=checkedFiles(dir)
        val snapshot=File(retained,"${System.currentTimeMillis()}-${System.nanoTime()}")
        check(snapshot.mkdirs())
        for(file in files.filter {it.isFile}) {
            val relative=file.relativeTo(dir).invariantSeparatorsPath
            if(asset(relative))continue
            val target=File(snapshot,relative);check(target.parentFile!!.mkdirs() || target.parentFile!!.isDirectory)
            file.copyTo(target)
            check(hash(file).contentEquals(hash(target))) {"Retained data verification failed"}
        }
        File(snapshot,".bruma-complete").writeText("1")
        return snapshot
    }
    internal fun removeCopy(root:File,dir:File):Long {
        check(dir.canonicalFile.parentFile==root.canonicalFile && dir.name!="." && dir.name!="..")
        val files=checkedFiles(dir);var bytes=0L
        // Keep provenance until every other file has been removed, allowing safe retries.
        val origin=File(dir,"bruma-origin.txt")
        for(file in files.filter {it.isFile && it!=origin}) {bytes+=file.length();check(file.delete())}
        for(folder in files.filter {it.isDirectory && it!=dir}.sortedByDescending {it.path.length})check(folder.delete())
        if(origin.exists()) {bytes+=origin.length();check(origin.delete())}
        check(dir.delete());return bytes
    }
    @JvmStatic fun clean(c:Context):Long = synchronized(GameArchives) {
        if(engineRunning(c))return@synchronized 0L
        val root=File(c.filesDir,"rpg-games").canonicalFile
        val library=LibraryStore(c).read();var released=0L
        for(dir in root.listFiles().orEmpty()) {
            if(!dir.isDirectory || dir.name.endsWith(".pending"))continue
            val origin=File(dir,"bruma-origin.txt").takeIf {it.isFile} ?: continue
            try {
                val source=origin.readText().trim()
                if(!RpgIdentity.confirmedMissing(c,source))continue
                // A deduplicated game may also be linked to another still-valid source.
                if(library.any {it.cached==dir.name && it.source.isNotBlank() && !RpgIdentity.confirmedMissing(c,it.source)})continue
                val identity=RpgIdentity.of(c,Uri.fromFile(dir)).ifBlank {LibraryStore.key(source)}
                preserve(dir,File(c.filesDir,"rpg-retained/$identity"))
                if(engineRunning(c) || !RpgIdentity.confirmedMissing(c,source))continue
                released+=removeCopy(root,dir)
            } catch(e:Exception) {android.util.Log.w("BrumaCleanup","Cache retained: ${dir.name}",e)}
        }
        if(released>0)android.util.Log.i("BrumaCleanup","Reclaimed $released bytes; save data retained")
        released
    }
    @JvmStatic fun restore(c:Context,dir:File) = synchronized(GameArchives) {
        val identity=RpgIdentity.of(c,Uri.fromFile(dir));if(identity.isBlank())return@synchronized
        val root=File(c.filesDir,"rpg-retained/$identity")
        val snapshots=root.listFiles().orEmpty().filter {File(it,".bruma-complete").isFile}.sortedBy {it.name}
        if(snapshots.isEmpty())return@synchronized
        val marker=File(dir,".bruma-restored")
        if(marker.exists())return@synchronized
        for(snapshot in snapshots)for(file in checkedFiles(snapshot).filter {it.isFile}) {
            val path=file.relativeTo(snapshot).invariantSeparatorsPath
            if(path.startsWith(".bruma-") || path=="bruma-origin.txt" || path.startsWith("mkxp.json") || path.startsWith("bruma-"))continue
            val target=File(dir,path)
            check(target.canonicalPath.startsWith(dir.canonicalPath+File.separator))
            val save=path.lowercase(Locale.ROOT).let {it.contains("save") || it.startsWith("userdata/") || it.substringAfterLast('/').startsWith("file-")}
            if(!target.exists() || save) {check(target.parentFile!!.mkdirs() || target.parentFile!!.isDirectory);file.copyTo(target,true);check(hash(file).contentEquals(hash(target)))}
        }
        marker.writeText("1")
    }
}
