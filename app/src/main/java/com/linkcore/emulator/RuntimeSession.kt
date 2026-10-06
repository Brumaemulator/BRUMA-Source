package com.linkcore.emulator

import android.app.Activity
import android.content.*
import android.os.Build
import java.util.UUID
import java.util.function.BooleanSupplier
import java.util.function.Supplier

data class RuntimeSession(val token:String,val component:String,val system:String,val path:String,val pausedAt:Long,val pid:Int=0)

/** Live, same-UID messages: a recent library entry is never treated as a running session. */
class RuntimeSessionHost(private val activity:Activity,private val system:String,
    private val path:Supplier<String>,private val ready:BooleanSupplier,private val closeGame:Runnable) {
    companion object {
        const val QUERY="com.linkcore.emulator.SESSION_QUERY"
        const val STATE="com.linkcore.emulator.SESSION_STATE"
        const val CLOSE="com.linkcore.emulator.SESSION_CLOSE"
        @JvmStatic fun query(context:Context){context.sendBroadcast(Intent(QUERY).setPackage(context.packageName))}
        @JvmStatic fun closeOthers(context:Context,keepComponent:String){context.sendBroadcast(Intent(CLOSE).setPackage(context.packageName).putExtra("keep",keepComponent))}
        @JvmStatic fun close(context:Context,token:String){context.sendBroadcast(Intent(CLOSE).setPackage(context.packageName).putExtra("token",token))}
        fun register(context:Context,receiver:BroadcastReceiver,filter:IntentFilter){
            if(Build.VERSION.SDK_INT>=33)context.registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED)
            else context.registerReceiver(receiver,filter)
        }
    }
    private val token=UUID.randomUUID().toString()
    private val component=activity.javaClass.name
    @Volatile private var paused=false
    @Volatile private var pausedAt=0L
    @Volatile private var closed=false
    private val receiver=object:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){
        when(intent.action){
            QUERY->publish()
            CLOSE->{if(intent.getStringExtra("keep")==component)return
                if(intent.hasExtra("token")&&intent.getStringExtra("token")!=token)return
                closed=true;publish();closeGame.run()}
        }
    }}
    private val sourceMonitor=ActiveGameSourceMonitor(activity,path,BooleanSupplier{!closed&&ready.asBoolean},Runnable {
        closed=true;publish();closeGame.run()
        activity.startActivity(Intent(activity,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
    })
    init {register(activity,receiver,IntentFilter().apply{addAction(QUERY);addAction(CLOSE)});sourceMonitor.start()}

    fun pause(value:Boolean){if(value&&!paused)pausedAt=System.currentTimeMillis();paused=value;publish()}
    fun publish(){activity.sendBroadcast(Intent(STATE).setPackage(activity.packageName)
        .putExtra("token",token).putExtra("component",component).putExtra("system",system)
        .putExtra("path",path.get()).putExtra("pausedAt",pausedAt)
        .putExtra("pid",android.os.Process.myPid())
        .putExtra("ready",!closed&&ready.asBoolean).putExtra("paused",paused))}
    fun dispose(){sourceMonitor.dispose();closed=true;publish();activity.unregisterReceiver(receiver)}
}

class RuntimeSessionClient(private val activity:Activity,private val changed:()->Unit){
    val sessions=linkedMapOf<String,RuntimeSession>()
    private val handler=android.os.Handler(android.os.Looper.getMainLooper())
    private val prune=object:Runnable{override fun run(){
        val processes=(activity.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).runningAppProcesses
        if(processes!=null){val live=processes.map{it.pid}.toSet();if(sessions.entries.removeAll{it.value.pid !in live})changed()}
        handler.postDelayed(this,2000)
    }}
    private val receiver=object:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){
        val component=intent.getStringExtra("component")?:return
        if(component !in setOf(NdsActivity::class.java.name,"com.hatkid.mkxpz.MainActivity","com.hatkid.mkxpz.ClassicActivity"))return
        val token=intent.getStringExtra("token")?:return
        if(intent.getBooleanExtra("ready",false)&&intent.getBooleanExtra("paused",false)){
            sessions[component]=RuntimeSession(token,component,intent.getStringExtra("system").orEmpty(),intent.getStringExtra("path").orEmpty(),intent.getLongExtra("pausedAt",0),intent.getIntExtra("pid",0))
        }else if(sessions[component]?.token==token)sessions.remove(component)
        changed()
    }}
    init{RuntimeSessionHost.register(activity,receiver,IntentFilter(RuntimeSessionHost.STATE));handler.postDelayed(prune,2000)}
    // onPause can publish before the library resumes. Keep that live state while
    // querying: Android may defer broadcasts to a cached runtime process.
    fun refresh(){changed();RuntimeSessionHost.query(activity)}
    fun close(session:RuntimeSession){sessions.remove(session.component);changed();RuntimeSessionHost.close(activity,session.token)}
    fun dispose(){handler.removeCallbacks(prune);activity.unregisterReceiver(receiver)}
}
