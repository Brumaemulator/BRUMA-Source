package com.linkcore.emulator

import android.app.Instrumentation
import android.net.Uri
import android.provider.DocumentsContract
import com.hatkid.mkxpz.RpgImporter
import java.io.File
import java.security.MessageDigest

object RpgImportChecks {
    fun run(test:Instrumentation) {
        val ctx=test.targetContext
        val root=File(ctx.cacheDir,"rpg-import-check-${System.currentTimeMillis()}").apply{mkdirs()}
        val fixture=Uri.parse("content://${test.context.packageName}.library/tree/root")
        fun digest(file:File)=MessageDigest.getInstance("SHA-256").run {
            file.inputStream().use { input -> val b=ByteArray(65536);while(true){val n=input.read(b);if(n<0)break;update(b,0,n)} };digest().toList()
        }
        fun manifest(dir:File)=dir.walkTopDown().filter{it.isFile}.associate{it.relativeTo(dir).path to digest(it)}
        try {
            val one=File(root,"fixture-one");val four=File(root,"fixture-four")
            RpgImporter(ctx.contentResolver,null).copy(fixture,"root",one,1)
            var lastDone=0;var lastTotal=0
            RpgImporter(ctx.contentResolver,{done,total,_,_->lastDone=done;lastTotal=total}).copy(fixture,"root",four,4)
            check(manifest(one)==manifest(four));check(lastDone==8 && lastTotal==8)
            var failed=false
            try{RpgImporter(ctx.contentResolver,null).copy(fixture,"rpg",File(root,"failure"),4)}catch(e:java.io.IOException){failed=true}
            check(failed){"Missing source did not fail"}
            val tree=Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AJuegos%20gba")
            val id="primary:Juegos gba/Pokemon Revival/Audio/SE"
            val results=StringBuilder("PASS fixture parity, completion progress and missing-file failure\n")
            var baseline:Map<String,List<Byte>>?=null
            for(workers in listOf(1,4)) {
                val target=File(root,"real-$workers")
                val start=android.os.SystemClock.elapsedRealtime()
                RpgImporter(ctx.contentResolver,null).copy(tree,id,target,workers)
                val elapsed=android.os.SystemClock.elapsedRealtime()-start
                val files=manifest(target)
                if(baseline==null)baseline=files else check(baseline==files){"Real source content differs"}
                results.append("$workers workers: $elapsed ms, ${files.size} files\n")
            }
            File(ctx.filesDir,"rpg-import-check.txt").writeText(results.toString())
        } finally {root.deleteRecursively()}
    }
}
