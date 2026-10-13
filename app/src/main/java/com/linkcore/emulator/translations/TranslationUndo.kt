// SPDX-License-Identifier: GPL-3.0-or-later
package com.linkcore.emulator.translations
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Stores only original bytes that differ, including any truncated tail. */
object TranslationUndo {
    fun create(original:File,translated:File,destination:File){
        val tmp=File.createTempFile("undo-",".pending",destination.parentFile)
        try{
            DataOutputStream(GZIPOutputStream(tmp.outputStream())).use{out->
                out.writeUTF("BRUMAUNDO1");out.writeLong(original.length());out.writeUTF(PatchEngine.sha(original));out.writeUTF(PatchEngine.sha(translated))
                RandomAccessFile(original,"r").use{a->RandomAccessFile(translated,"r").use{b->
                    val left=ByteArray(65536);val right=ByteArray(65536);var offset=0L
                    while(offset<a.length()){
                        val n=minOf(left.size.toLong(),a.length()-offset).toInt();a.readFully(left,0,n)
                        val available=minOf(n.toLong(),maxOf(0L,b.length()-offset)).toInt();if(available>0)b.readFully(right,0,available)
                        var i=0;while(i<n){if(i<available && left[i]==right[i]){i++;continue};val start=i;while(i<n && (i>=available || left[i]!=right[i]))i++;out.writeLong(offset+start);out.writeInt(i-start);out.write(left,start,i-start)}
                        offset+=n
                    }
                }};out.writeLong(-1)
            };check(tmp.renameTo(destination))
        }finally{tmp.delete()}
    }
    fun restore(translated:File,undo:File,output:File){
        require(!output.exists());try{
            DataInputStream(GZIPInputStream(undo.inputStream())).use{input->
                require(input.readUTF()=="BRUMAUNDO1");val size=input.readLong();require(size in 1..PatchEngine.LIMIT)
                val sourceHash=input.readUTF();val translatedHash=input.readUTF();require(PatchEngine.sha(translated)==translatedHash)
                translated.copyTo(output)
                RandomAccessFile(output,"rw").use{out->out.setLength(size);var end=0L;val bytes=ByteArray(65536)
                    while(true){val offset=input.readLong();if(offset==-1L)break;val n=input.readInt();require(offset>=end && n in 1..bytes.size && offset<=size-n);input.readFully(bytes,0,n);out.seek(offset);out.write(bytes,0,n);end=offset+n}
                    require(input.read()==-1)
                };require(PatchEngine.sha(output)==sourceHash){"Restored SHA-256 mismatch"}
            }
        }catch(e:Exception){output.delete();throw e}
    }
}
