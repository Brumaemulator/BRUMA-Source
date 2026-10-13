package com.linkcore.emulator
import android.app.Instrumentation
import java.io.File
object RpgEngineChecks {
 fun run(test:Instrumentation,fixturesOnly:Boolean=false){
  fun checkCode(expected:Boolean,vararg scripts:String){check(RpgEngine.classify(scripts.toList())==expected){"Classification mismatch: ${scripts.toList()}"}}
  checkCode(true,"case x\nwhen 0: foo\nend", "System.uptime; Essentials::VERSION")
  checkCode(true,"case x\n when -1, 2 : foo\nend")
  checkCode(false,"# when 0: example\n=begin\nwhen 1: example\n=end\nSystem.uptime")
  checkCode(false,"text = \"\nwhen 0: not code\n\"\nEssentials::VERSION")
  checkCode(false,"text = <<~TEXT\nwhen 0: example\nTEXT\nSystem.uptime")
  checkCode(false,"case x\nwhen 0 then foo\nend")
  checkCode(false,"defined?(ESSENTIALS_VERSION); Essentials::VERSION")
  checkCode(true,"class Win32API\n@@GetCurrentThreadId = api\ndef Win32API.pbFindRgssWindow\nend\nend", "ESSENTIALS_VERSION = \"BES3\"")
  checkCode(false,"class Win32API\n@@GetCurrentThreadId = api\ndef Win32API.pbFindRgssWindow\nend\nend")
  checkCode(false,"text = %q{\nwhen 0: sample\n}")
  checkCode(false,"text = %Q{nested { braces }\nwhen 0: sample\n}")
  val cacheDir=File(test.targetContext.cacheDir,"engine-cache-check-${System.nanoTime()}")
  try{
   val pack=File(cacheDir,"Data/Scripts.rxdata");pack.parentFile!!.mkdirs()
   fun packed(code:String):ByteArray {
    val compressed=java.io.ByteArrayOutputStream().also{out->java.util.zip.DeflaterOutputStream(out).use{it.write(code.toByteArray())}}.toByteArray()
    val out=java.io.ByteArrayOutputStream()
    fun number(n:Int){if(n==0)out.write(0) else if(n<123)out.write(n+5) else {out.write(4);repeat(4){out.write(n ushr (it*8))}}}
    fun string(bytes:ByteArray){out.write(34);number(bytes.size);out.write(bytes)}
    out.write(byteArrayOf(4,8,91));number(1);out.write(91);number(3);out.write(105);number(0);string("test".toByteArray());string(compressed)
    return out.toByteArray().copyOf(1024)
   }
   pack.writeBytes(packed("case x\nwhen 0: foo\nend"));check(pack.setLastModified(1000000))
   check(RpgEngine.classic(test.targetContext,cacheDir))
   pack.writeBytes(packed("case x\nwhen 0 then foo\nend"));check(pack.setLastModified(1000000))
   check(!RpgEngine.classic(test.targetContext,cacheDir)){"Stale engine decision after same-size, same-time replacement"}
  }finally{cacheDir.deleteRecursively()}
  if(fixturesOnly)return
  val root=File(test.targetContext.filesDir,"rpg-games")
  val expected=mapOf("anil" to false,"game-1790472043453" to false,"game-1790472909140" to false,"game-1790517795333" to false,"zip-ae23d6c62f144f186249843af66a4e36c99f07d1" to true,"zip-f799b2d0ad92eeab2c4f431c4c3030167297ea64" to true)
  val result=StringBuilder()
  expected.forEach{(name,classic)->val file=File(root,"$name/Data/Scripts.rxdata");check(file.isFile){"Missing $name"};val actual=RpgEngine.detect(file);check(actual==classic){"$name expected classic=$classic actual=$actual"};result.append("$name classic=$actual\n")}
  File(test.targetContext.filesDir,"rpg-engine-check.txt").writeText(result.toString())
 }
}
