package com.linkcore.emulator.translations

import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.zip.CRC32

/** Own implementation. No game data, remote code execution or in-place writes. */
object PatchEngine {
    const val LIMIT=512L*1024*1024
    fun sha(file:File):String {val d=MessageDigest.getInstance("SHA-256");file.inputStream().use {s->val b=ByteArray(65536);while(true){val n=s.read(b);if(n<0)break;d.update(b,0,n)}};return d.digest().joinToString(""){"%02x".format(it.toInt() and 255)}}
    private fun crc(file:File):Long {val c=CRC32();file.inputStream().use{s->val b=ByteArray(65536);while(true){val n=s.read(b);if(n<0)break;c.update(b,0,n)}};return c.value}
    private class Cursor(val data:ByteArray,var end:Int=data.size) {
        var p=0
        fun byte():Int {require(p<end){"Truncated patch"};return data[p++].toInt() and 255}
        fun big(n:Int):Long {var x=0L;repeat(n){x=(x shl 8) or byte().toLong()};return x}
        fun varint():Long {var value=0L;var shift=1L;repeat(9){val b=byte();require(shift<=Long.MAX_VALUE/128);value=Math.addExact(value,Math.multiplyExact((b and 127).toLong(),shift));if(b and 128!=0)return value;shift=shift shl 7;value=Math.addExact(value,shift)};error("Invalid varint")}
        fun skip(n:Long){require(n>=0 && n<=end-p);p+=n.toInt()}
    }
    private fun little(b:ByteArray,p:Int):Long {var x=0L;repeat(4){x=x or ((b[p+it].toLong() and 255) shl (8*it))};return x}
    private fun checksums(source:File,patch:ByteArray){require(patch.size>=16);val c=CRC32();c.update(patch,0,patch.size-4);require(c.value==little(patch,patch.size-4)){"Patch CRC mismatch"};require(crc(source)==little(patch,patch.size-12)){"Source CRC mismatch"}}
    fun apply(type:String,source:File,patch:ByteArray,target:File,targetBytes:Long=0) {
        require(source.length() in 1..LIMIT && patch.size in 1..2*1024*1024)
        require(source.canonicalFile!=target.canonicalFile && !target.exists()){"Original must be preserved"}
        try {
            when(type) {"IPS"->ips(source,patch,target);"BPS"->bps(source,patch,target);"UPS"->ups(source,patch,target);"XDELTA"->{require(targetBytes in 1..LIMIT);require(XdeltaNative.decode(source.path,patch,target.path,targetBytes)==targetBytes){"xdelta decoding failed"}};else->error("Patch format not enabled in this pilot")}
            if(type in setOf("BPS","UPS"))require(crc(target)==little(patch,patch.size-8)){"Target CRC mismatch"}
        }catch(e:Exception){target.delete();throw e}
    }
    private fun ips(source:File,b:ByteArray,target:File) {
        require(b.size>=8 && String(b,0,5,Charsets.US_ASCII)=="PATCH")
        source.copyTo(target);val c=Cursor(b);c.p=5
        RandomAccessFile(target,"rw").use {out->
            while(true){val offset=c.big(3);if(offset==0x454f46L)break;val n=c.big(2).toInt();out.seek(offset)
                if(n==0){val count=c.big(2);require(count>0 && offset+count<=LIMIT);val value=c.byte();repeat(count.toInt()){out.write(value)}}
                else {require(offset+n<=LIMIT && n<=c.end-c.p);out.write(b,c.p,n);c.p+=n}
            }
            if(c.p!=b.size){require(b.size-c.p==3);val size=c.big(3);require(size>0);out.setLength(size)}
        }
    }
    private fun bps(source:File,b:ByteArray,target:File) {
        require(b.size>=19 && String(b,0,4,Charsets.US_ASCII)=="BPS1");checksums(source,b)
        val c=Cursor(b,b.size-12);c.p=4;require(c.varint()==source.length());val size=c.varint();require(size in 1..LIMIT);c.skip(c.varint())
        RandomAccessFile(source,"r").use {input->RandomAccessFile(target,"rw").use {out->
            var sourceRelative=0L;var targetRelative=0L;var position=0L;val buffer=ByteArray(65536)
            while(position<size){val instruction=c.varint();val count=(instruction ushr 2)+1;require(count<=size-position)
                when((instruction and 3).toInt()) {
                    0->{require(position+count<=source.length());input.seek(position);var remain=count;while(remain>0){val n=minOf(remain,buffer.size.toLong()).toInt();input.readFully(buffer,0,n);out.seek(position);out.write(buffer,0,n);position+=n;remain-=n}}
                    1->{require(count<=c.end-c.p);out.seek(position);out.write(b,c.p,count.toInt());c.p+=count.toInt();position+=count}
                    2->{val delta=c.varint();sourceRelative=Math.addExact(sourceRelative,if(delta and 1L==0L)delta ushr 1 else -(delta ushr 1));require(sourceRelative>=0 && count<=source.length()-sourceRelative);input.seek(sourceRelative);var remain=count;while(remain>0){val n=minOf(remain,buffer.size.toLong()).toInt();input.readFully(buffer,0,n);out.seek(position);out.write(buffer,0,n);position+=n;sourceRelative+=n;remain-=n}}
                    3->{val delta=c.varint();targetRelative=Math.addExact(targetRelative,if(delta and 1L==0L)delta ushr 1 else -(delta ushr 1));require(targetRelative>=0 && targetRelative<position);repeatLong(count){out.seek(targetRelative++);val v=out.read();require(v>=0);out.seek(position++);out.write(v)}}
                }
            }
            require(c.p==c.end);out.setLength(size)
        }}
    }
    private inline fun repeatLong(n:Long,f:()->Unit){var i=0L;while(i++<n)f()}
    private fun ups(source:File,b:ByteArray,target:File) {
        require(b.size>=18 && String(b,0,4,Charsets.US_ASCII)=="UPS1");checksums(source,b)
        val c=Cursor(b,b.size-12);c.p=4;require(c.varint()==source.length());val size=c.varint();require(size in 1..LIMIT)
        source.copyTo(target);RandomAccessFile(target,"rw").use {out->out.setLength(size);RandomAccessFile(source,"r").use {input->
            var offset=0L
            while(c.p<c.end){offset=Math.addExact(offset,c.varint());while(true){val value=c.byte();if(value==0){offset++;break};require(offset<maxOf(source.length(),size));val old=if(offset<source.length()){input.seek(offset);input.read()}else 0;if(offset<size){out.seek(offset);out.write(old xor value)}else require((old xor value)==0);offset++}}
        }}
    }
}
