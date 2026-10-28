package com.linkcore.emulator.translations

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.net.Uri
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import com.linkcore.emulator.*
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import org.json.JSONObject

class TranslationsActivity:Activity() {
    private val worker=Executors.newSingleThreadExecutor()
    private lateinit var cards:LinearLayout
    private lateinit var status:TextView
    private lateinit var spinner:ProgressBar
    private lateinit var refreshButton:Button
    private val actions=mutableListOf<Button>()
    private var busy=false
    private val client by lazy{TranslationClient(this)}
    private data class Match(val game:LibraryGame,val metadata:JSONObject,val installed:Boolean)
    private fun dp(n:Int)=Look.dp(this,n)
    private fun text(value:String,size:Float=15f,color:Int=Color.WHITE,bold:Boolean=false)=Look.text(this,value,size,color,bold)
    private fun space(parent:LinearLayout,n:Int){parent.addView(Space(this),LinearLayout.LayoutParams(1,dp(n)))}
    private fun vertical()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val root=FrameLayout(this).apply{setBackgroundColor(Look.bg)}
        val column=vertical().apply{setPadding(dp(20),dp(16),dp(20),dp(24))}
        val scroll=ScrollView(this).apply{isFillViewport=true;clipToPadding=false;addView(column)}
        root.addView(scroll,FrameLayout.LayoutParams(minOf(resources.displayMetrics.widthPixels,dp(720)),FrameLayout.LayoutParams.MATCH_PARENT,Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        root.setOnApplyWindowInsetsListener{view,insets->
            if(android.os.Build.VERSION.SDK_INT>=30){val bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());view.setPadding(bars.left,bars.top,bars.right,bars.bottom)}
            else {@Suppress("DEPRECATION") view.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom)}
            insets
        }
        val heading=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        heading.addView(Look.button(this,"‹"){finish()}.apply{contentDescription=getString(R.string.translation_back);textSize=28f;setPadding(0,0,0,0)},LinearLayout.LayoutParams(dp(48),dp(48)))
        heading.addView(text(getString(R.string.translations_title),24f,bold=true).apply{setPadding(dp(14),0,0,0)},LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
        column.addView(heading);space(column,22)
        val hero=vertical().apply{
            setPadding(dp(22),dp(22),dp(22),dp(22))
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(0xff292343.toInt(),0xff152c31.toInt())).apply{cornerRadius=dp(24).toFloat();setStroke(dp(1),Look.line)}
        }
        hero.addView(text("文  ↔  A",19f,Look.mint,true));space(hero,14)
        hero.addView(text(getString(R.string.translation_hero),24f,bold=true));space(hero,10)
        hero.addView(text(getString(R.string.translation_intro),15f,0xffd0d2e2.toInt()).apply{setLineSpacing(dp(3).toFloat(),1f)})
        column.addView(hero);space(column,22)
        val state=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        spinner=ProgressBar(this).apply{indeterminateTintList=android.content.res.ColorStateList.valueOf(Look.mint)}
        state.addView(spinner,LinearLayout.LayoutParams(dp(22),dp(22)).apply{marginEnd=dp(10)})
        status=text(getString(R.string.translation_searching),14f,Look.muted)
        state.addView(status,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
        refreshButton=Look.button(this,"↻"){refresh()}.apply{contentDescription=getString(R.string.translation_refresh);textSize=25f;setPadding(0,0,0,0)}
        state.addView(refreshButton,LinearLayout.LayoutParams(dp(48),dp(48)).apply{marginStart=dp(8)})
        column.addView(state);space(column,16)
        cards=vertical();column.addView(cards)
        space(column,16)
        setContentView(root);root.requestApplyInsets();refresh()
    }
    private fun runTask(after:()->Unit={},task:()->Unit){
        if(busy || isDestroyed)return
        busy=true;spinner.visibility=View.VISIBLE;refreshButton.isEnabled=false;actions.forEach{it.isEnabled=false}
        worker.execute{
            var succeeded=false
            try{task();succeeded=true}catch(e:Exception){runOnUiThread{if(!isDestroyed){status.text=getString(R.string.translation_failed)}}}
            finally{runOnUiThread{if(!isDestroyed){busy=false;spinner.visibility=View.GONE;refreshButton.isEnabled=true;actions.forEach{it.isEnabled=true};if(succeeded)after()}}}
        }
    }
    private fun sourceHash(game:LibraryGame):String = game.cached.takeIf{it.matches(Regex("[a-f0-9]{64}"))}
        ?: RomAccess.hash(this,game.source.ifBlank{game.uri})
    private fun refresh(){
        if(busy)return
        status.text=getString(R.string.translation_searching)
        runTask{
            TranslationInstaller(this).migrateLegacy()
            val catalog=client.catalog();val games=LibraryStore(this).cachedGames().filter{it.system in setOf("GBA","GBC","NDS","GB")}
            val matches=mutableListOf<Match>();val seen=mutableSetOf<String>()
            for(game in games){if((0 until catalog.length()).none{catalog.getJSONObject(it).getString("system")==game.system})continue;val hash=runCatching{sourceHash(game)}.getOrNull()?:continue
                for(i in 0 until catalog.length()){val m=catalog.getJSONObject(i)
                    val installed=TranslationInstaller.active(this,m)
                    if(m.getString("system")==game.system && (m.getString("gameSha256")==hash || (installed && m.getString("targetSha256")==hash)) && seen.add(m.getString("id")))
                        matches+=Match(game,m,installed)
                }
            }
            runOnUiThread{if(!isDestroyed){cards.removeAllViews();actions.clear();status.text=resources.getQuantityString(R.plurals.translation_count,matches.size,matches.size)
                if(matches.isEmpty()){
                    cards.addView(vertical().apply{background=Look.box(this@TranslationsActivity,border=true);setPadding(dp(22),dp(24),dp(22),dp(24));addView(text(getString(R.string.translation_empty_title),18f,bold=true));space(this,10);addView(text(getString(R.string.translation_empty),15f,Look.muted))})
                }else matches.forEach{showCard(it)}
            }}
        }
    }
    private fun showCard(match:Match){
        val m=match.metadata
        val card=vertical().apply{setPadding(dp(20),dp(20),dp(20),dp(20));background=Look.box(this@TranslationsActivity,radius=22,border=true)}
        val top=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
        val badge=text(m.getString("system"),12f,Look.mint,true).apply{setPadding(dp(10),dp(6),dp(10),dp(6));background=Look.box(this@TranslationsActivity,0xff203b3b.toInt(),10)}
        top.addView(badge);top.addView(text(getString(R.string.translation_version,m.getString("version")),12f,Look.muted).apply{gravity=Gravity.END},LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));card.addView(top);space(card,14)
        card.addView(text(m.getString("game"),20f,bold=true));space(card,10)
        val language=Locale.forLanguageTag(m.getString("language")).getDisplayName(resources.configuration.locales[0])
        card.addView(text(language.replaceFirstChar{it.titlecase(resources.configuration.locales[0])},17f,Look.mint,true));space(card,6)
        card.addView(text(getString(R.string.translation_by,m.getString("author")),14f,Look.muted));space(card,18)
        val action=Look.button(this,getString(if(match.installed)R.string.translation_view_library else R.string.translation_download),primary=true){
            if(match.installed)finish() else confirm(match)
        }
        actions+=action;card.addView(action,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(52)))
        space(card,10)
        card.addView(text(getString(if(match.installed)R.string.translation_installed else R.string.translation_ready),12f,Look.mint).apply{gravity=Gravity.CENTER})
        space(card,8)
        val details=Look.button(this,getString(R.string.translation_details)){
            AlertDialog.Builder(this).setTitle(m.getString("game")).setMessage(m.getString("notes")).setPositiveButton(android.R.string.ok,null).show()
        };actions+=details;card.addView(details,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(48)))
        cards.addView(card,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply{bottomMargin=dp(16)})
        if(match.installed){
            val restore=Look.button(this,getString(R.string.translation_restore)){
                AlertDialog.Builder(this).setTitle(R.string.translation_restore).setMessage(R.string.translation_restore_confirm)
                    .setPositiveButton(R.string.translation_restore){_,_->runTask(after={refresh()}){TranslationInstaller(this).restore(m)}}.setNegativeButton(android.R.string.cancel,null).show()
            };actions+=restore;card.addView(restore,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(52)).apply{topMargin=dp(12)})
        }
    }
    private fun confirm(match:Match){
        AlertDialog.Builder(this).setTitle(R.string.translation_apply).setMessage(getString(R.string.translation_preserve))
            .setPositiveButton(R.string.translation_download){_,_->status.text=getString(R.string.translation_applying);runTask(after={Toast.makeText(this,R.string.translation_success,Toast.LENGTH_LONG).show();refresh()}){TranslationInstaller(this).install(match.game,match.metadata)}}
            .setNegativeButton(android.R.string.cancel,null).show()
    }
    override fun onDestroy(){super.onDestroy();worker.shutdown()}
}
