package com.linkcore.emulator

import android.app.Activity
import android.graphics.*
import android.net.Uri
import android.util.LruCache
import android.view.*
import android.widget.*
import java.io.File
import java.util.concurrent.Executors

object CoverImages {
    fun load(activity:android.content.Context,uri:Uri):Bitmap? {
        val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
        activity.contentResolver.openInputStream(uri)?.use {BitmapFactory.decodeStream(it,null,bounds)}
        if(bounds.outWidth !in 1..20000 || bounds.outHeight !in 1..20000) return null
        var sample=1
        while(maxOf(bounds.outWidth,bounds.outHeight)/sample>512) sample*=2
        val options=BitmapFactory.Options().apply {inSampleSize=sample;inPreferredConfig=Bitmap.Config.RGB_565}
        return activity.contentResolver.openInputStream(uri)?.use {BitmapFactory.decodeStream(it,null,options)}
    }
}

class LibraryAdapter(private val activity:Activity,private val choose:(LibraryGame)->Unit,private val options:(LibraryGame)->Unit):BaseAdapter() {
    private var games=emptyList<LibraryGame>()
    private val worker=Executors.newFixedThreadPool(2)
    private val cache=object:LruCache<String,Bitmap>(12*1024*1024) {override fun sizeOf(key:String,value:Bitmap)=value.allocationByteCount}
    private var closed=false
    fun submit(items:List<LibraryGame>) {games=items;notifyDataSetChanged()}
    fun close() {closed=true;worker.shutdownNow();cache.evictAll()}
    override fun getCount()=games.size
    override fun getItem(position:Int)=games[position]
    override fun getItemId(position:Int)=games[position].key.hashCode().toLong()
    private class Tile(val box:LinearLayout,val art:CartridgeArt,val image:ImageView,val name:TextView,val caption:TextView,val more:Button)
    override fun getView(position:Int,convertView:View?,parent:ViewGroup):View {
        val d=activity.resources.displayMetrics.density
        val tile=if(convertView!=null) convertView.tag as Tile else {
            val box=LinearLayout(activity).apply {
                orientation=LinearLayout.VERTICAL;isFocusable=true;background=Look.box(activity,0xff141927.toInt(),21,true);clipToOutline=true
                elevation=(3*d);stateListAnimator=null
            }
            val cover=FrameLayout(activity)
            val art=CartridgeArt(activity)
            val image=ImageView(activity).apply {scaleType=ImageView.ScaleType.CENTER_CROP;setBackgroundColor(Look.bg)}
            cover.addView(art,FrameLayout.LayoutParams(-1,-1));cover.addView(image,FrameLayout.LayoutParams(-1,-1))
            box.addView(cover,LinearLayout.LayoutParams(-1,((if(activity.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT) 172 else 132)*d).toInt()))
            val name=Look.text(activity,"",15f,Color.WHITE,true).apply {maxLines=2;minLines=2;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding((14*d).toInt(),(12*d).toInt(),(8*d).toInt(),0)}
            val caption=Look.text(activity,activity.getString(R.string.library_play),10f,Look.mint,true).apply {
                setPadding((9*d).toInt(),(4*d).toInt(),(9*d).toInt(),(4*d).toInt())
            }
            val footer=LinearLayout(activity).apply {gravity=Gravity.CENTER_VERTICAL}
            footer.setPadding((12*d).toInt(),(4*d).toInt(),(4*d).toInt(),(6*d).toInt())
            footer.addView(caption,LinearLayout.LayoutParams(-2,-2))
            footer.addView(Space(activity),LinearLayout.LayoutParams(0,1,1f))
            val more=Look.button(activity,"⋯") {}.apply {textSize=20f;setPadding(0,0,0,0);background=android.graphics.drawable.ColorDrawable(Color.TRANSPARENT);minHeight=(38*d).toInt()}
            footer.addView(more,LinearLayout.LayoutParams((42*d).toInt(),(38*d).toInt()))
            box.addView(name);box.addView(footer)
            Tile(box,art,image,name,caption,more).also {box.tag=it}
        }
        val g=getItem(position)
        tile.name.text=g.title;tile.art.title=g.title;tile.art.system=g.system;tile.art.invalidate()
        val displaySystem=if(g.system=="RPG Maker XP") "RPG Maker" else g.system
        tile.caption.text=displaySystem
        val accent=when(g.system) {"GBA"->Look.mint;"GBC"->Look.purple;"GB"->0xffe5bd72.toInt();"NDS"->0xff81baff.toInt();else->Look.mint}
        tile.caption.setTextColor(accent)
        tile.caption.background=android.graphics.drawable.GradientDrawable().apply {shape=android.graphics.drawable.GradientDrawable.RECTANGLE;cornerRadius=100f*d;setColor(0x22151a29);setStroke((1*d).toInt(),(accent and 0x00ffffff) or 0x66000000)}
        tile.box.contentDescription=activity.getString(R.string.library_play)+": "+g.title
        tile.box.setOnClickListener {choose(g)}
        tile.box.setOnLongClickListener {options(g);true}
        tile.more.contentDescription=activity.getString(R.string.library_options)+": "+g.title
        tile.more.setOnClickListener {options(g)}
        val cover=if(g.hasCover) g.cover else ""
        val key=g.key+cover+if(cover.startsWith("file:")) File(Uri.parse(cover).path!!).lastModified().toString() else ""
        tile.image.tag=key;tile.image.setImageDrawable(null);tile.image.visibility=View.GONE
        if(cover.isNotEmpty()) {
            val bitmap=cache.get(key)
            if(bitmap!=null) {tile.image.setImageBitmap(bitmap);tile.image.visibility=View.VISIBLE}
            else if(!closed) worker.execute {
                val loaded=runCatching {CoverImages.load(activity,Uri.parse(cover))}.getOrNull()
                if(loaded!=null) cache.put(key,loaded)
                activity.runOnUiThread {if(!closed && !activity.isDestroyed && tile.image.tag==key) {tile.image.setImageBitmap(loaded);tile.image.visibility=if(loaded==null) View.GONE else View.VISIBLE}}
            }
        }
        return tile.box
    }
}

/** Original fallback cover, rather than a missing-image placeholder. */
class CartridgeArt(context:android.content.Context):View(context) {
    var title="GBA"
    var system="GBA"
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c:Canvas) {
        val w=width.toFloat();val h=height.toFloat();val d=resources.displayMetrics.density
        val accents=intArrayOf(0xff9580ef.toInt(),0xff39ad99.toInt(),0xffdc9970.toInt(),0xff699cda.toInt())
        val accent=accents[(title.hashCode() and Int.MAX_VALUE)%accents.size]
        p.shader=LinearGradient(0f,0f,w,h,accent,0xff182133.toInt(),Shader.TileMode.CLAMP);c.drawRect(0f,0f,w,h,p);p.shader=null
        p.color=0x20ffffff;c.drawCircle(w*.92f,h*.08f,w*.65f,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=2*d;p.color=0x35ffffff
        for(n in 0..3) c.drawCircle(w*.75f,h*.58f,(28+n*14)*d,p)
        p.style=Paint.Style.FILL;p.color=0xbb101622.toInt()
        c.save();c.rotate(-12f,w*.48f,h*.60f)
        c.drawRoundRect(w*.15f,h*.29f,w*.80f,h*.86f,12*d,12*d,p)
        p.color=accent;c.drawRoundRect(w*.22f,h*.37f,w*.73f,h*.67f,6*d,6*d,p)
        p.color=0xccffffff.toInt();p.textSize=30*d;p.typeface=Typeface.create("sans-serif-black",Typeface.BOLD);p.textAlign=Paint.Align.CENTER
        c.drawText(title.firstOrNull()?.uppercase() ?: "B",w*.475f,h*.59f,p)
        p.color=0x55ffffff;p.strokeWidth=3*d
        c.drawLine(w*.28f,h*.76f,w*.64f,h*.76f,p);c.restore()
        p.textAlign=Paint.Align.LEFT;p.textSize=10*d;p.typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);p.color=Color.WHITE
        c.drawText("$system  /  BRUMA",14*d,24*d,p)
    }
}

