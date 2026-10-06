package com.linkcore.emulator

import android.app.Activity
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.TextUtils
import android.view.*
import android.widget.*
import java.util.concurrent.Executors

/** Responsive resume banner; artwork comes from the existing cover collection. */
class ContinueGameCard(private val activity:Activity,private val resume:()->Unit,private val options:()->Unit):FrameLayout(activity){
    private val worker=Executors.newSingleThreadExecutor()
    private var closed=false
    private var binding=""
    private val backdrop=Backdrop(activity)
    private val title=Look.text(activity,"",18f,Color.WHITE,true)
    private val badge=Look.text(activity,"",11f,Look.mint,true)
    private val art=CartridgeArt(activity)
    private val cover=ImageView(activity)
    private fun dp(n:Int)=Look.dp(activity,n)
    init{
        visibility=GONE;clipToOutline=true;background=Look.box(activity,Look.panel,22,true);elevation=dp(3).toFloat()
        addView(backdrop,LayoutParams(-1,-1))
        val compact=resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val row=LinearLayout(activity).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(15),dp(12),dp(15),dp(12))}
        val left=LinearLayout(activity).apply{orientation=LinearLayout.VERTICAL}
        val heading=Look.text(activity,activity.getString(R.string.resume_heading),13f,Look.mint,true)
        left.addView(heading);left.addView(Space(activity),LinearLayout.LayoutParams(1,dp(7)))
        title.apply{maxLines=if(compact)1 else 2;ellipsize=TextUtils.TruncateAt.END}
        left.addView(title,LinearLayout.LayoutParams(-1,0,1f))
        val info=LinearLayout(activity).apply{gravity=Gravity.CENTER_VERTICAL}
        badge.apply{setPadding(dp(8),dp(3),dp(8),dp(3));background=Look.box(activity,0x333a8175,12,true)}
        info.addView(badge);info.addView(Look.text(activity,activity.getString(R.string.resume_paused),11f,Look.muted),LinearLayout.LayoutParams(-2,-2).apply{leftMargin=dp(9)})
        left.addView(info);left.addView(Space(activity),LinearLayout.LayoutParams(1,dp(8)))
        val actions=LinearLayout(activity).apply{gravity=Gravity.CENTER_VERTICAL}
        val continueButton=Look.button(activity,activity.getString(R.string.resume_action),true,resume).apply{textSize=14f;setPadding(dp(10),0,dp(10),0);minHeight=dp(38);maxLines=1}
        actions.addView(continueButton,LinearLayout.LayoutParams(0,dp(38),1f))
        val more=Look.button(activity,"⋯",false,options).apply{textSize=21f;setPadding(0,0,0,0);minHeight=dp(38);contentDescription=activity.getString(R.string.library_options)}
        actions.addView(more,LinearLayout.LayoutParams(dp(40),dp(38)).apply{leftMargin=dp(8)})
        if(!compact)left.addView(actions)
        row.addView(left,LinearLayout.LayoutParams(0,-1,1f))
        if(compact)row.addView(actions,LinearLayout.LayoutParams(dp(174),dp(38)).apply{leftMargin=dp(15)})
        val frame=FrameLayout(activity).apply{background=Look.box(activity,Look.panel,15,true);clipToOutline=true}
        frame.addView(art,LayoutParams(-1,-1));cover.scaleType=ImageView.ScaleType.CENTER_CROP;frame.addView(cover,LayoutParams(-1,-1))
        row.addView(frame,LinearLayout.LayoutParams(dp(if(compact)58 else 94),-1).apply{leftMargin=dp(15)})
        addView(row,LayoutParams(-1,-1));setOnClickListener{resume()}
    }
    fun bind(game:LibraryGame){
        visibility=VISIBLE;title.text=game.title;badge.text=if(game.system=="RPG Maker XP")"RPG Maker" else game.system
        contentDescription=activity.getString(R.string.resume_heading)+": "+game.title
        val key=game.key+game.cover
        if(binding==key)return
        binding=key;art.title=game.title;art.system=game.system;art.invalidate();cover.setImageDrawable(null);cover.visibility=GONE;backdrop.image=null;backdrop.invalidate()
        if(game.cover.isNotBlank()&&!closed)worker.execute{
            val bitmap=runCatching{CoverImages.load(activity,Uri.parse(game.cover))}.getOrNull()
            activity.runOnUiThread{if(!closed&&binding==key&&!activity.isDestroyed){cover.setImageBitmap(bitmap);cover.visibility=if(bitmap==null)GONE else VISIBLE;backdrop.image=bitmap;backdrop.invalidate()}}
        }
    }
    fun close(){closed=true;worker.shutdownNow()}
    private class Backdrop(context:android.content.Context):View(context){
        var image:Bitmap?=null
        private val p=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        override fun onDraw(c:Canvas){
            val w=width.toFloat();val h=height.toFloat()
            p.shader=LinearGradient(0f,0f,w,h,intArrayOf(0xff101624.toInt(),0xff13202b.toInt(),0xff19433c.toInt()),null,Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,p);p.shader=null
            image?.let{b->val s=maxOf(w/b.width,h/b.height);val bw=b.width*s;val bh=b.height*s;p.alpha=65;c.drawBitmap(b,null,RectF(w-bw,(h-bh)/2,w,(h+bh)/2),p);p.alpha=255}
            p.shader=LinearGradient(0f,0f,w,0f,intArrayOf(0xff0d1423.toInt(),0xdc0d1423.toInt(),0x330d1423),floatArrayOf(0f,.48f,1f),Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,p);p.shader=null
        }
    }
}
