package com.linkcore.emulator

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.InputType
import android.widget.*
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CoverDownloadUi(private val activity:Activity,private val store:LibraryStore,private val updated:()->Unit) {
    private val client=LibretroClient(activity)
    private val worker=Executors.newSingleThreadExecutor()
    private val cancelled=AtomicBoolean(false)
    private var dialog:AlertDialog?=null
    private var running=false
    private var closed=false
    fun show(games:List<LibraryGame>,single:Boolean=false) {
        if(running || closed) return
        dialog?.dismiss()
        val box=LinearLayout(activity).apply {orientation=LinearLayout.VERTICAL;setPadding(32,16,32,16)}
        box.addView(TextView(activity).apply {setText(R.string.art_consent)})
        val missing=CheckBox(activity).apply {setText(R.string.art_missing);isChecked=true}
        if(!single) box.addView(missing) else box.addView(TextView(activity).apply {setText(R.string.art_replace)})
        val form=AlertDialog.Builder(activity).setTitle(activity.getString(R.string.art_download)).setView(ScrollView(activity).apply {addView(box)}).setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.art_download,null).create()
        dialog=form
        form.setOnShowListener {form.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selected=if(!single && missing.isChecked) games.filter {!it.hasCover} else games
            form.dismiss();if(single) search(selected.first()) else start(selected)
        }}
        form.show()
    }
    private fun start(games:List<LibraryGame>,chosen:String?=null) {
        if(games.isEmpty()) {dialog=AlertDialog.Builder(activity).setTitle(activity.getString(R.string.art_download)).setMessage(R.string.art_nothing).setPositiveButton(android.R.string.ok,null).show();return}
        running=true;cancelled.set(false);client.begin()
        val progress=AlertDialog.Builder(activity).setTitle(activity.getString(R.string.art_download)).setMessage(activity.getString(R.string.art_progress,0,games.size)).setCancelable(false).setNegativeButton(android.R.string.cancel,null).create()
        dialog=progress
        progress.setOnShowListener {progress.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {cancelled.set(true);client.cancel();progress.dismiss();dialog=null;updated()}}
        progress.show()
        worker.execute {
            var saved=0;var skipped=0;var error:Int?=null
            val unmatched=ArrayList<LibraryGame>()
            try {
                val catalogs=if(chosen==null) games.map {it.system}.distinct().associateWith {client.catalog(it)} else emptyMap()
                for((index,game) in games.withIndex()) {
                    if(cancelled.get()) throw CoverFailure(R.string.art_cancelled)
                    activity.runOnUiThread {if(!closed && !cancelled.get()) progress.setMessage(activity.getString(R.string.art_progress,index+1,games.size)+"\n"+game.title)}
                    try {
                        val filename=chosen ?: LibretroClient.match(game.name,catalogs[game.system] ?: emptyList(),activity.resources.configuration.locales[0].language=="es") ?: throw CoverFailure(R.string.art_not_found)
                        val bytes=client.fetchCover(filename,game.system)
                        val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
                        BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
                        if(bounds.outWidth !in 1..4096 || bounds.outHeight !in 1..4096) throw CoverFailure(R.string.art_invalid)
                        val options=BitmapFactory.Options().apply {while(bounds.outWidth/inSampleSize.coerceAtLeast(1)>512 || bounds.outHeight/inSampleSize.coerceAtLeast(1)>512) inSampleSize=inSampleSize.coerceAtLeast(1)*2}
                        val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options) ?: throw CoverFailure(R.string.art_invalid)
                        val folder=File(activity.filesDir,"covers").apply {mkdirs()}
                        val file=File.createTempFile("libretro-",".png",folder)
                        try {
                            file.outputStream().use {if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) throw CoverFailure(R.string.art_invalid)}
                            if(cancelled.get()) throw CoverFailure(R.string.art_cancelled)
                            store.setCover(game.key,Uri.fromFile(file).toString());saved++
                        } catch(e:Exception) {file.delete();throw e} finally {bitmap.recycle()}
                    } catch(e:CoverFailure) {if(e.messageId==R.string.art_not_found) {skipped++;unmatched+=game} else throw e}
                }
            } catch(e:CoverFailure) {error=e.messageId} catch(e:Exception) {error=if(cancelled.get()) R.string.art_cancelled else R.string.art_network}
            activity.runOnUiThread {
                running=false
                if(!closed && !activity.isDestroyed) {
                    progress.dismiss()
                    if(cancelled.get()) {
                        dialog=null
                        updated()
                        return@runOnUiThread
                    }
                    updated()
                    val summary=activity.getString(R.string.art_result,saved,skipped)+(error?.let {"\n\n"+activity.getString(it)} ?: "")
                    val result=AlertDialog.Builder(activity).setTitle(R.string.art_download).setMessage(summary).setPositiveButton(android.R.string.ok,null)
                    if(unmatched.isNotEmpty()) result.setNeutralButton(R.string.art_resolve) {_,_->
                        dialog=AlertDialog.Builder(activity).setTitle(R.string.art_resolve).setItems(unmatched.map {it.title}.toTypedArray()) {_,position->search(unmatched[position])}.setNegativeButton(android.R.string.cancel,null).show()
                    }
                    dialog=result.show()
                }
            }
        }
    }
    private fun search(game:LibraryGame) {
        running=true;client.begin();cancelled.set(false)
        val loading=AlertDialog.Builder(activity).setTitle(R.string.art_download).setMessage(R.string.art_loading).setNegativeButton(android.R.string.cancel) {_,_->cancelled.set(true);client.cancel();dialog=null}.setCancelable(false).show()
        dialog=loading
        worker.execute {
            try {
                val catalog=client.catalog(game.system)
                activity.runOnUiThread {
                    running=false;loading.dismiss()
                    if(closed || cancelled.get()) return@runOnUiThread
                    val box=LinearLayout(activity).apply {orientation=LinearLayout.VERTICAL;setPadding(24,8,24,8)}
                    val query=EditText(activity).apply {setHint(R.string.art_search);isSingleLine=true;setText(game.title)}
                    val list=ListView(activity)
                    var matches=emptyList<String>()
                    fun filter() {
                        val key=LibretroClient.normalized(query.text.toString())
                        matches=catalog.filter {key.isBlank() || LibretroClient.normalized(it).contains(key)}.take(100)
                        list.adapter=ArrayAdapter(activity,android.R.layout.simple_list_item_1,matches.map {it.removeSuffix(".png")})
                    }
                    query.addTextChangedListener(object:android.text.TextWatcher {
                        override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int) {}
                        override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) {filter()}
                        override fun afterTextChanged(s:android.text.Editable?) {}
                    })
                    box.addView(query);box.addView(TextView(activity).apply {setText(R.string.art_search_hint)})
                    box.addView(list,LinearLayout.LayoutParams(-1,(280*activity.resources.displayMetrics.density).toInt()))
                    val picker=AlertDialog.Builder(activity).setTitle(R.string.art_find).setView(box).setNegativeButton(android.R.string.cancel,null).show()
                    dialog=picker;filter()
                    list.setOnItemClickListener {_,_,position,_->val selected=matches[position];picker.dismiss();start(listOf(game),selected)}
                }
            } catch(e:Exception) {activity.runOnUiThread {
                running=false;loading.dismiss()
                if(!closed && !cancelled.get()) dialog=AlertDialog.Builder(activity).setTitle(R.string.art_download).setMessage((e as? CoverFailure)?.messageId ?: R.string.art_network).setPositiveButton(android.R.string.ok,null).show()
            }}
        }
    }
    fun close() {closed=true;cancelled.set(true);client.cancel();worker.shutdownNow();dialog?.dismiss()}
}

