package com.linkcore.emulator.translations

import android.content.Context
import com.linkcore.emulator.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.io.File

class TranslationClient(private val context:Context) {
    val base=if(BuildConfig.DEBUG) context.getSharedPreferences("translation-proof",Context.MODE_PRIVATE).getString("endpoint","https://brumastudio.netlify.app")!! else "https://brumastudio.netlify.app"
    private var token:String?=null
    private var expiresAt=0L
    init { File(context.noBackupFilesDir,"translation-authorization").delete() }
    private class HttpFailure(val status:Int):Exception("HTTP $status")
    @Synchronized private fun access(){
        if(token!=null && android.os.SystemClock.elapsedRealtime()<expiresAt)return
        val response=request("/api/translations/access",JSONObject().put("package","com.brumastudio.bruma").put("versionCode",BuildConfig.VERSION_CODE),false)
        val value=response.getString("token");require(value.matches(Regex("v2\\.[0-9]{1,16}\\.[a-f0-9]{48}\\.[a-f0-9]{64}")))
        val lifetime=response.getLong("expiresIn");require(lifetime in 60..3600)
        token=value;expiresAt=android.os.SystemClock.elapsedRealtime()+(lifetime-30)*1000
    }
    fun catalog()=request("/api/translations/catalog",null,false).getJSONArray("translations")
    @Synchronized fun patch(id:String,gameHash:String):ByteArray {
        repeat(2){attempt->try{
            access()
            val ticket=request("/api/translations/ticket",JSONObject().put("id",id).put("gameSha256",gameHash),true)
            return download(ticket.getString("url"),2*1024*1024,true)
        }catch(e:HttpFailure){if(e.status!=401 || attempt!=0)throw e;token=null;expiresAt=0}}
        error("Translation authorization failed")
    }
    private fun request(path:String,body:JSONObject?,auth:Boolean):JSONObject {
        val data=download(base+path,3*1024*1024,auth,body);return JSONObject(String(data,Charsets.UTF_8))
    }
    private fun download(url:String,limit:Int,auth:Boolean,body:JSONObject?=null):ByteArray {
        val expected=URI(base);val uri=URI(url);require(uri.scheme==expected.scheme && uri.host==expected.host && uri.port==expected.port && uri.userInfo==null){"Untrusted download origin"};require(BuildConfig.DEBUG || uri.scheme=="https")
        val c=uri.toURL().openConnection() as HttpURLConnection;c.instanceFollowRedirects=false;c.connectTimeout=15000;c.readTimeout=30000
        try {
            c.setRequestProperty("User-Agent","BRUMA/${BuildConfig.VERSION_CODE} Android");c.setRequestProperty("X-Bruma-Client","android")
            if(auth)c.setRequestProperty("Authorization","Bearer "+requireNotNull(token))
            if(body!=null){c.requestMethod="POST";c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write(body.toString().toByteArray())}}
            val status=c.responseCode;if(status!=200)throw HttpFailure(status)
            return c.inputStream.use{stream->val out=java.io.ByteArrayOutputStream();val b=ByteArray(65536);while(true){val n=stream.read(b);if(n<0)break;require(out.size()+n<=limit){"Response too large"};out.write(b,0,n)};out.toByteArray()}
        }finally{c.disconnect()}
    }
}
