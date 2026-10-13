package com.linkcore.emulator.translations

import android.content.Context
import com.linkcore.emulator.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.io.File

class TranslationClient(private val context:Context) {
    val base=if(BuildConfig.DEBUG) context.getSharedPreferences("translation-proof",Context.MODE_PRIVATE).getString("endpoint","https://brumastudio.netlify.app")!! else "https://brumastudio.netlify.app"
    private val tokenFile=File(context.noBackupFilesDir,"translation-authorization")
    fun authorized()=tokenFile.isFile
    fun enroll(code:String){val response=request("/api/translations/enroll",JSONObject().put("code",code),false);val token=response.getString("token");require(token.matches(Regex("[a-f0-9]{64}")));tokenFile.writeText(token)}
    fun catalog()=request("/api/translations/catalog",null,false).getJSONArray("translations")
    fun patch(id:String,gameHash:String):ByteArray {
        val ticket=request("/api/translations/ticket",JSONObject().put("id",id).put("gameSha256",gameHash),true)
        return download(ticket.getString("url"),2*1024*1024,true)
    }
    private fun request(path:String,body:JSONObject?,auth:Boolean):JSONObject {
        val data=download(base+path,3*1024*1024,auth,body);return JSONObject(String(data,Charsets.UTF_8))
    }
    private fun download(url:String,limit:Int,auth:Boolean,body:JSONObject?=null):ByteArray {
        val expected=URI(base);val uri=URI(url);require(uri.scheme==expected.scheme && uri.host==expected.host && uri.port==expected.port){"Untrusted download origin"}
        val c=uri.toURL().openConnection() as HttpURLConnection;c.instanceFollowRedirects=false;c.connectTimeout=15000;c.readTimeout=30000
        try {
            if(auth){require(tokenFile.isFile){context.getString(com.linkcore.emulator.R.string.translation_authorize_first)};c.setRequestProperty("Authorization","Bearer "+tokenFile.readText())}
            if(body!=null){c.requestMethod="POST";c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(body.toString().toByteArray())}}
            val status=c.responseCode;require(status==200){"HTTP $status"}
            return c.inputStream.use{stream->val out=java.io.ByteArrayOutputStream();val b=ByteArray(65536);while(true){val n=stream.read(b);if(n<0)break;require(out.size()+n<=limit){"Response too large"};out.write(b,0,n)};out.toByteArray()}
        }finally{c.disconnect()}
    }
}
