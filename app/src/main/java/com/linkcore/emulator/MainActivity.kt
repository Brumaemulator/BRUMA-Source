package com.linkcore.emulator

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.OpenableColumns
import android.text.TextUtils
import android.view.*
import android.widget.*
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.locks.LockSupport

class MainActivity : Activity(), SurfaceHolder.Callback {
    override fun attachBaseContext(newBase:Context) { super.attachBaseContext(BrumaLocale.attach(newBase)) }
    private lateinit var root: FrameLayout
    private lateinit var home: LinearLayout
    private lateinit var game: FrameLayout
    private lateinit var surface: GameSurface
    private lateinit var controls: ControlsView
    private lateinit var status: TextView
    private lateinit var continueCard:ContinueGameCard
    private var sessionClient:RuntimeSessionClient?=null
    private var continueSession:RuntimeSession?=null
    private var continueItem:LibraryGame?=null
    private lateinit var open: Button
    private lateinit var progress: ProgressBar
    private var menu: Dialog? = null
    private val libraryStore by lazy {LibraryStore(this)}
    private val activeSourceMonitor by lazy {ActiveGameSourceMonitor(this,
        java.util.function.Supplier{rom?.path.orEmpty()},java.util.function.BooleanSupplier{loaded&&!busy},Runnable {
            userPaused=true;stopGame();NativeCore.close();loaded=false
            rom=null;romName="";prefs.edit().remove("lastId").remove("lastName").apply()
            goHome(true)
        })}

    private var libraryGames=emptyList<LibraryGame>()
    private var libraryAdapter:LibraryAdapter?=null
    private lateinit var libraryCount:TextView
    private lateinit var libraryEmpty:TextView
    private lateinit var folderNote:TextView
    private lateinit var libraryScanSpinner:ProgressBar
    private lateinit var folderButton:Button
    private var searchText=""
    private var systemFilter="GBA"
    private fun tr(es:String,en:String)=if(resources.configuration.locales[0].language=="es") es else en
    private var coverTarget:String?=null
    private val scraper by lazy {CoverDownloadUi(this,libraryStore) {refreshLibrary(false)}}
    private val gamepad by lazy {GamepadInput(this,{action->
        if(inGame && !userPaused) {if(action==GamepadInput.PAUSE) pauseGame() else fastButton.performClick()}
    },{if(inGame && !userPaused) pauseGame()},{connected->
        if(!connected && ::controls.isInitialized) controls.visibility=View.VISIBLE
    },{
        if(::controls.isInitialized) controls.visibility=View.GONE
    })}
    private val gamepadSettings by lazy {GamepadSettings(this,gamepad)}
    private var scanning=false

    @Volatile private var fastForward=false
    @Volatile private var fastMultiplier=2
    @Volatile private var fastSound=false
    private lateinit var fastButton:Button
    private lateinit var movableUtilities:MovableButtons
    private lateinit var editBar:LinearLayout
    private fun finishEditing() { controls.edit(false); editBar.visibility=View.GONE; userPaused=true; pauseGame() }
    private fun editControls() {
        if(resources.configuration.orientation!=android.content.res.Configuration.ORIENTATION_LANDSCAPE) return
        menu?.dismiss();menu=null;controls.edit(true);editBar.visibility=View.VISIBLE
    }

    private val imports = Executors.newSingleThreadExecutor()
    private val libraryScanner = Executors.newSingleThreadExecutor()
    @Volatile private var running = false
    private var worker: Thread? = null
    private var resumed = false
    private var surfaceReady = false
    private var userPaused = true
    private var loaded = false
    private var busy = false
    private var inGame = false
    private var rom: File? = null
    private var romName = ""
    private val prefs by lazy { getSharedPreferences("library", MODE_PRIVATE) }
    private fun dp(n: Int) = Look.dp(this, n)
    private fun label(t: String, size: Float = 15f, color: Int = Color.WHITE, bold: Boolean = false) = Look.text(this,t,size,color,bold)
    private fun gap(parent: LinearLayout, height: Int) = parent.addView(Space(this), LinearLayout.LayoutParams(1,dp(height)))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coverTarget=savedInstanceState?.getString("coverTarget")
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        root = FrameLayout(this).apply { setBackgroundColor(Look.bg) }
        if(!prefs.getBoolean("originalDefaultV5",false)) {
            prefs.edit().putBoolean("fillScreen",false).putBoolean("originalDefaultV5",true).apply()
        }
        fastSound=prefs.getBoolean("fastSound",false)
        @Suppress("DEPRECATION")
        val currentDisplay=windowManager.defaultDisplay
        val mode=currentDisplay.supportedModes.filter {
            it.physicalWidth==currentDisplay.mode.physicalWidth && it.physicalHeight==currentDisplay.mode.physicalHeight &&
                (kotlin.math.abs(it.refreshRate-120f)<2f || kotlin.math.abs(it.refreshRate-60f)<2f)
        }.maxByOrNull {it.refreshRate}
        window.attributes=window.attributes.apply {
            preferredRefreshRate=mode?.refreshRate ?: 60f
            if(mode!=null) preferredDisplayModeId=mode.modeId
        }
        createGame()
        createHome()
        sessionClient=RuntimeSessionClient(this){updateContinueCard()}
        setContentView(root)
        val id = prefs.getString("lastId", null)
        if (id != null && id.matches(Regex("[a-f0-9]{64}"))) {
            rom = RomFiles.cached(this,id)
            romName = prefs.getString("lastName", getString(R.string.ui_0)) ?: getString(R.string.ui_0)
        }
        if (rom == null) {
            rom = File(filesDir,"roms").listFiles()?.filter { it.name.matches(Regex("[a-f0-9]{64}\\.(gba|gbc|gb)")) }?.maxByOrNull { it.lastModified() }
            rom?.let { file ->
                romName = runCatching { file.inputStream().use { stream ->
                    stream.skip(if(file.extension=="gba")160 else 308); val name = ByteArray(12); stream.read(name)
                    String(name, Charsets.US_ASCII).trim { it <= ' ' }
                } }.getOrDefault(getString(R.string.ui_0)).ifBlank { getString(R.string.ui_0) }
            }
        }
        gamepad
        updateStatus()
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) { handleBack() }
        }
    }

    private fun createHome() {
        libraryAdapter?.close()
        if(::continueCard.isInitialized)continueCard.close()
        val portrait=resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT
        home=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(12));setBackgroundColor(Look.bg)}
        val header=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL;isBaselineAligned=true}
        val logoB=label("B",35f,Look.purple,true).apply {
            paint.shader=android.graphics.LinearGradient(0f,0f,0f,dp(38).toFloat(),Look.purple,Look.mint,android.graphics.Shader.TileMode.CLAMP)
            includeFontPadding=false
        }
        header.addView(logoB,LinearLayout.LayoutParams(logoB.paint.measureText("B").toInt(),dp(46)))
        header.addView(label("ruma",29f,Color.WHITE,true),LinearLayout.LayoutParams(0,dp(46),1f))
        folderButton=Look.button(this,"+",true) {showAddGames()}.apply {textSize=27f;setPadding(0,0,0,dp(3));elevation=dp(5).toFloat();contentDescription=tr("Añadir juegos","Add games")}
        header.addView(folderButton,LinearLayout.LayoutParams(dp(48),dp(44)).apply {rightMargin=dp(8)})
        val menuButton=Look.button(this,"☰") {showHomeOptions()}.apply {textSize=20f;setPadding(0,0,0,0);background=Look.box(this@MainActivity,Look.panel,16,true);contentDescription=tr("Menú","Menu")}
        header.addView(menuButton,LinearLayout.LayoutParams(dp(48),dp(44)))
        home.addView(header,LinearLayout.LayoutParams(-1,dp(48)));gap(home,12)
        continueCard=ContinueGameCard(this,{resumeCurrentGame()},{showContinueOptions()})
        home.addView(continueCard,LinearLayout.LayoutParams(-1,dp(if(portrait)176 else 98)).apply{bottomMargin=dp(12)})
        val body=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
        val side=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
        // Keep resume state for gameplay; the home screen is the unified collection.
        open=Look.button(this,getString(R.string.ui_7)) {pickDocument(41)}
        folderNote=label(getString(R.string.library_scanning),10f,Look.mint).apply {
            visibility=View.GONE;setSingleLine(true);setPadding(dp(8),0,0,0)
            accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val collection=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
        val search=EditText(this).apply {
            hint=getString(R.string.library_search);setTextColor(Color.WHITE);setHintTextColor(Look.muted);textSize=14f;setSingleLine(true)
            background=Look.box(this@MainActivity,0xff151a29.toInt(),24,true);setPadding(dp(15),0,dp(15),0);setText(searchText)
            setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search,0,0,0)
            compoundDrawablePadding=dp(10)
            compoundDrawablesRelative.getOrNull(0)?.setTint(Look.muted)
            minHeight=dp(50)
            inputType=android.text.InputType.TYPE_CLASS_TEXT
            addTextChangedListener(object:android.text.TextWatcher {
                override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int) {}
                override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) {searchText=s.toString();renderLibrary()}
                override fun afterTextChanged(s:android.text.Editable?) {}
            })
        }
        if(portrait){collection.addView(search,LinearLayout.LayoutParams(-1,dp(50)));gap(collection,8)}
        val systems=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL}
        val systemsScroll=HorizontalScrollView(this).apply {isHorizontalScrollBarEnabled=false;clipToPadding=false;setPadding(0,0,0,0);addView(systems,ViewGroup.LayoutParams(-2,-1))}
        listOf("GBA" to "▣","GBC" to "▤","GB" to "▥","NDS" to "▣","RPG Maker XP" to "✦").forEach {(id,icon)->
            val title=if(id=="RPG Maker XP") "RPG Maker" else id
            val tab=Look.button(this,"$icon  $title",systemFilter==id) {systemFilter=id;root.removeView(home);createHome();updateStatus()}.apply {
                textSize=12f;maxLines=1;isSingleLine=true;ellipsize=TextUtils.TruncateAt.END
                setPadding(dp(12),0,dp(12),0);minHeight=dp(40);minimumHeight=dp(40)
                background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x338888bb),Look.box(this@MainActivity,if(systemFilter==id) 0xff30274d.toInt() else Look.panel,22,true).apply {if(systemFilter==id)setStroke(dp(1),Look.purple)},null)
                setTextColor(if(systemFilter==id) Look.mint else Color.WHITE)
            }
            systems.addView(tab,LinearLayout.LayoutParams(-2,dp(40)).apply {setMargins(0,dp(2),dp(7),dp(2))})
        }
        if(portrait)collection.addView(systemsScroll,LinearLayout.LayoutParams(-1,dp(48)))
        else{
            val filters=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL}
            filters.addView(search,LinearLayout.LayoutParams(0,dp(50),.42f).apply{rightMargin=dp(12)})
            filters.addView(systemsScroll,LinearLayout.LayoutParams(0,dp(48),.58f))
            collection.addView(filters,LinearLayout.LayoutParams(-1,dp(50)));gap(collection,8)
        }
        val heading=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL}
        libraryCount=label("",19f,Color.WHITE,true)
        heading.addView(libraryCount,LinearLayout.LayoutParams(0,-2,1f))
        libraryScanSpinner=ProgressBar(this,null,android.R.attr.progressBarStyleSmall).apply {
            isIndeterminate=true;visibility=View.GONE
            indeterminateTintList=android.content.res.ColorStateList.valueOf(Look.mint)
            importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        heading.addView(libraryScanSpinner,LinearLayout.LayoutParams(dp(14),dp(14)))
        heading.addView(folderNote,LinearLayout.LayoutParams(-2,-2))

        collection.addView(heading,LinearLayout.LayoutParams(-1,dp(if(portrait)42 else 34)))
        val content=FrameLayout(this)
        val grid=GridView(this).apply {
            numColumns=if(portrait) 2 else maxOf(2,((resources.displayMetrics.widthPixels/resources.displayMetrics.density-60)/160).toInt())
            horizontalSpacing=dp(12);verticalSpacing=dp(12);stretchMode=GridView.STRETCH_COLUMN_WIDTH
            setSelector(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));clipToPadding=false;setPadding(0,0,0,dp(12))
        }
        libraryAdapter=LibraryAdapter(this,{launchLibraryGame(it)},{showGameOptions(it)})
        grid.adapter=libraryAdapter;content.addView(grid,FrameLayout.LayoutParams(-1,-1))
        libraryEmpty=label(getString(R.string.library_empty),16f,Look.muted).apply {gravity=Gravity.CENTER;setPadding(dp(20),dp(16),dp(20),dp(16))}
        content.addView(libraryEmpty,FrameLayout.LayoutParams(-1,-1))
        collection.addView(content,LinearLayout.LayoutParams(-1,0,1f))
        body.addView(collection,LinearLayout.LayoutParams(-1,0,1f))
        home.addView(body,LinearLayout.LayoutParams(-1,0,1f))
        progress=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply {isIndeterminate=true;visibility=View.GONE}
        home.addView(progress,LinearLayout.LayoutParams(-1,dp(3)))
        root.addView(home,FrameLayout.LayoutParams(-1,-1));renderLibrary()
        updateContinueCard()
    }

    private fun renderLibrary() {
        if(!::libraryCount.isInitialized) return
        val visible=libraryGames.filter {it.title.contains(searchText,true) && (systemFilter.isEmpty() || it.system==systemFilter)}
        libraryAdapter?.submit(visible)
        libraryCount.text="${tr("Tu colección","Your collection")}  ·  ${visible.size}"
        libraryEmpty.visibility=if(visible.isEmpty()) View.VISIBLE else View.GONE
        libraryEmpty.text=getString(if(libraryGames.isEmpty()) R.string.library_empty else R.string.library_no_results)
        folderNote.visibility=if(scanning) View.VISIBLE else View.GONE
        libraryScanSpinner.visibility=folderNote.visibility
        folderButton.isEnabled=!scanning
        updateContinueCard()
    }
    private fun pickFolder() {
        if(scanning || busy) return
        @Suppress("DEPRECATION")
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),44)
    }
    private fun refreshLibrary(scan:Boolean) {
        if(scanning) return
        scanning=true;renderLibrary()
        val libraryStarted=android.os.SystemClock.elapsedRealtime()
        libraryScanner.execute {
            try {
                val immediate=libraryStore.cachedGames()
                runOnUiThread {if(!isDestroyed) {libraryGames=immediate;renderLibrary();if(BuildConfig.DEBUG) android.util.Log.d("BrumaLibrary","Cached ${immediate.size} games in ${android.os.SystemClock.elapsedRealtime()-libraryStarted} ms")}}
                if(scan) {
                    val folders=prefs.getStringSet("treeUris",emptySet())!!.toMutableSet()
                    prefs.getString("treeUri",null)?.let {folders.add(it)}
                    if(folders.isNotEmpty()) {
                        val discovered=ArrayList<LibraryGame>()
                        val pending=java.util.concurrent.atomic.AtomicReference<List<LibraryGame>>()
                        val posted=java.util.concurrent.atomic.AtomicBoolean(false)
                        val handler=android.os.Handler(mainLooper)
                        val publish=Runnable {
                            posted.set(false)
                            val batch=pending.getAndSet(null)
                            if(batch!=null && !isDestroyed) {
                                val preview=libraryStore.preview(immediate,batch)
                                libraryGames=preview;renderLibrary()
                                if(BuildConfig.DEBUG)android.util.Log.d("BrumaLibrary","Progress ${preview.size} games in ${android.os.SystemClock.elapsedRealtime()-libraryStarted} ms")
                            }
                        }
                        val found=try {folders.flatMap {folder->
                            GameFolderMedia.ensureHidden(this,Uri.parse(folder))
                            libraryStore.scan(Uri.parse(folder)) {game->
                            discovered+=game;pending.set(discovered.toList())
                            // Flush even if the scanner next blocks on a large ZIP.
                            if(posted.compareAndSet(false,true))handler.postDelayed(publish,100)
                        }}} finally {handler.removeCallbacks(publish)}
                        libraryStore.replaceFolder(found,folders.map{Uri.parse(it)},setOfNotNull(rom?.takeIf{loaded}?.nameWithoutExtension))
                        val ready=libraryStore.cachedGames()
                        runOnUiThread {if(!isDestroyed) {libraryGames=ready;renderLibrary()}}
                    }
                }
                if(scan) RpgCacheCleanup.clean(this)
                libraryStore.mergeImported()
                val items=libraryStore.allGames()
                runOnUiThread {if(!isDestroyed) {if(scan)closeDeletedLibrarySessions(immediate,items);libraryGames=items;scanning=false;renderLibrary();if(BuildConfig.DEBUG) android.util.Log.d("BrumaLibrary","Refreshed ${items.size} games in ${android.os.SystemClock.elapsedRealtime()-libraryStarted} ms")}}
            } catch(e:Exception) {android.util.Log.e("BrumaLibrary","Scan failed",e);val cached=runCatching {libraryStore.cachedGames()}.getOrDefault(emptyList());runOnUiThread {if(!isDestroyed) {libraryGames=cached;scanning=false;renderLibrary();showError(getString(R.string.library_unavailable))}}}
        }
    }
    // A paused session must not keep a deleted title alive in the continue card.
    private fun closeDeletedLibrarySessions(previous:List<LibraryGame>,current:List<LibraryGame>) {
        val removed=previous.filter {old->current.none {g->g.key==old.key || (old.cached.isNotBlank() && g.cached==old.cached) || (old.identity.isNotBlank() && g.identity==old.identity)}}
        var closed=false
        if(loaded && removed.any{it.cached.isNotBlank() && it.cached==rom?.nameWithoutExtension}) {
            userPaused=true;stopGame();NativeCore.close();loaded=false;closed=true
        }
        for(session in sessionClient?.sessions?.values?.toList().orEmpty()) {
            val file=File(session.path)
            if(removed.any{g->if(g.system=="RPG Maker XP")g.cached==file.name || Uri.parse(g.uri).path==session.path else g.cached.isNotBlank() && g.cached==file.nameWithoutExtension}) {
                sessionClient?.close(session);closed=true
            }
        }
        if(closed) {
            // Closing an RPG/DS process is asynchronous. Retry guarded cleanup;
            // if it is still shutting down, the next library refresh retries too.
            window.decorView.postDelayed({if(!isDestroyed) {
                val protected=setOfNotNull(rom?.takeIf{loaded}?.nameWithoutExtension)
                libraryScanner.execute {
                    runCatching {
                        RomCacheCleanup.clean(this,emptyList(),libraryStore.read(),protected)
                        RpgCacheCleanup.clean(this)
                    }.onFailure{android.util.Log.w("BrumaCleanup","Deferred cleanup",it)}
                }
            }},1500)
        }
    }
    private fun launchLibraryGame(item:LibraryGame) {
        if(busy) return
        if(item.system=="RPG Maker XP") {
            if(busy) return
            userPaused=true;stopGame()
            if(loaded){NativeCore.close();loaded=false;updateStatus()}
            startActivity(Intent(this,com.hatkid.mkxpz.RpgLibraryActivity::class.java).putExtra("launchUri",item.uri))
            return
        }
        // Preserve an RPG Maker runtime when returning to another RPG title.
        // Close it only when the user actually starts a non-RPG game.
        RuntimeSessionHost.closeOthers(this,if(item.system=="NDS")NdsActivity::class.java.name else javaClass.name)
        if(busy) return
        if(loaded && item.cached.isNotEmpty() && rom?.nameWithoutExtension==item.cached) {enterGame();return}
        userPaused=true;stopGame();busy=true;updateStatus()
        imports.execute {
            try {
                val cached=RomFiles.cached(this,item.cached)
                val file=if(item.cached.isNotEmpty() && cached!=null) cached else importRom(Uri.parse(item.uri)).first
                libraryStore.remember(item.uri,item.name,file.nameWithoutExtension,RomFiles.system(file.name))
                runOnUiThread {if(!isDestroyed) {NativeCore.close();loaded=false;busy=false;if(item.system=="NDS"){updateStatus();startActivity(Intent(this,NdsActivity::class.java).putExtra("romPath",file.path).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))}else{rom=file;romName=item.name;updateStatus();enterGame()}}}
            } catch(e:Exception) {runOnUiThread {if(!isDestroyed) {busy=false;updateStatus();showError(localizedError(e) ?: getString(R.string.library_unavailable))}}}
        }
    }
    private fun showHomeOptions() {
        val options=arrayOf(getString(R.string.library_refresh),getString(R.string.art_download),getString(R.string.pad_title),getString(R.string.ui_1),getString(R.string.app_language),tr("Ajustes","Settings"))
        AlertDialog.Builder(this).setTitle(tr("Opciones","Options")).setItems(options) {_,which->when(which) {
            0->refreshLibrary(true)
            1->scraper.show(libraryGames.filter {it.system in setOf("GB","GBC","GBA","NDS")})
            2->gamepadSettings.show()
            3->showHelp()
            4->showLanguagePicker()
            5->showLegalSettings()
        }}.show()
    }
    private fun showLegalSettings() {
        AlertDialog.Builder(this).setTitle(tr("Ajustes","Settings"))
            .setItems(arrayOf(tr("Acerca de","About"))) {_,_->
                AlertDialog.Builder(this).setTitle("Bruma 1.0.0")
                    .setPositiveButton(tr("Licencias de código abierto","Open-source licenses")) {_,_->showLicenses()}
                    .setNegativeButton(android.R.string.cancel,null).show()
            }.show()
    }
    private fun showLanguagePicker() {
        val tags=arrayOf("","es","en","pt-BR","fr","de","it","ja","ko","zh-CN")
        val labels=arrayOf(getString(R.string.system_default),"Español","English","Português (Brasil)","Français","Deutsch","Italiano","日本語","한국어","简体中文")
        val selected=tags.indexOf(BrumaLocale.selectedTag(this)).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle(R.string.app_language).setSingleChoiceItems(labels,selected) {dialog,index->
            BrumaLocale.setLanguage(this,tags[index])
            dialog.dismiss()
            if(Build.VERSION.SDK_INT<33)recreate()
        }.setNegativeButton(android.R.string.cancel,null).show()
    }    private fun showAddGames() {
        AlertDialog.Builder(this).setTitle(tr("Añadir juegos","Add games"))
            .setItems(arrayOf(tr("Elegir carpeta · detectar sistema","Choose folder · detect system"),tr("Abrir archivo · GB, GBC, GBA, NDS o ZIP","Open file · GB, GBC, GBA, NDS or ZIP"))) {_,which->if(which==0) pickFolder() else pickDocument(41)}.show()
    }
    private fun showGameOptions(item:LibraryGame) {
        if(item.system=="RPG Maker XP") {
            val dir=Uri.parse(item.uri).path?.let {File(it)}
            val settings=getSharedPreferences("rpg",0)
            AlertDialog.Builder(this).setTitle(item.title).setItems(arrayOf(getString(R.string.library_play),getString(R.string.library_cover),tr("Textos y menús rápidos: ","Fast text and menus: ")+(if(dir!=null && settings.getBoolean("fastUi:"+dir.name,true)) "ON" else "OFF"),tr("Motor de RPG Maker","RPG Maker engine"))) {_,which->
                when(which) {
                    0->launchLibraryGame(item)
                    1->{coverTarget=item.key;startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {addCategory(Intent.CATEGORY_OPENABLE);type="image/*"},45)}
                    3->{if(dir!=null && dir.isDirectory){
                        val choices=arrayOf(tr("Automático","Automatic"),tr("Moderno","Modern"),tr("Clásico · pruebas","Classic · testing"))
                        AlertDialog.Builder(this).setTitle(tr("Motor de RPG Maker","RPG Maker engine")).setItems(if(RpgEngine.available(this))choices else choices.take(2).toTypedArray()){_,n->settings.edit().putString("engine:"+dir.name,arrayOf("auto","modern","classic")[n]).apply()}.show()
                    }else Toast.makeText(this,tr("Abre el juego una vez para importarlo","Open the game once to import it"),Toast.LENGTH_LONG).show()}
                    2->{if(dir!=null) settings.edit().putBoolean("fastUi:"+dir.name,!settings.getBoolean("fastUi:"+dir.name,true)).apply()}
                }
            }.show();return
        }
        AlertDialog.Builder(this).setTitle(item.title).setItems(arrayOf(getString(R.string.library_play),getString(R.string.library_cover),getString(R.string.art_find))) {_,which->
            if(which==0) launchLibraryGame(item) else if(which==2) scraper.show(listOf(item),true) else {
                coverTarget=item.key
                @Suppress("DEPRECATION")
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {addCategory(Intent.CATEGORY_OPENABLE);type="image/*"},45)
            }
        }.show()
    }

    private fun createGame() {
        game=FrameLayout(this).apply { visibility=View.GONE }
        surface=GameSurface(this); surface.holder.addCallback(this)
        surface.setOnTouchListener { _,event -> if(event.actionMasked==MotionEvent.ACTION_DOWN) controls.visibility=View.VISIBLE; false }
        game.addView(surface,FrameLayout.LayoutParams(-1,-1))
        controls=ControlsView(this)
        game.addView(controls,FrameLayout.LayoutParams(-1,-1))
        surface.fillScreen=prefs.getBoolean("fillScreen",false)
        status=label("Bruma GBA")
        val pauseButton=Look.button(this,getString(R.string.ui_14)) { pauseGame() }.apply {
            alpha=.72f
            background=Look.box(this@MainActivity,0x99101523.toInt(),22,true)
        }
        movableUtilities=MovableButtons(this,game,"gba-utility-positions") {controls.editing}
        game.addView(pauseButton,FrameLayout.LayoutParams(dp(100),dp(40)))
        movableUtilities.bind(pauseButton,"pause",-1,false)
        fastButton=Look.button(this,"≫  1×") {
            if(!fastForward) { fastMultiplier=2;fastForward=true }
            else if(fastMultiplier==2) fastMultiplier=4
            else {fastForward=false;fastMultiplier=2}
            fastButton.alpha=if(fastForward) 1f else .65f
            fastButton.text=if(fastForward) "≫  ${fastMultiplier}×" else "≫  1×"
            fastButton.contentDescription=getString(R.string.speed_cycle)
        }
        fastButton.contentDescription=getString(R.string.speed_cycle)
        game.addView(fastButton,FrameLayout.LayoutParams(dp(100),dp(40)))
        movableUtilities.bind(fastButton,"fast",1,false)
        editBar=LinearLayout(this).apply { visibility=View.GONE;setBackgroundColor(Look.panel);gravity=Gravity.CENTER }
        editBar.addView(label(getString(R.string.ui_16),13f))
        editBar.addView(Look.button(this,getString(R.string.ui_17)) { controls.resetPositions();movableUtilities.reset() })
        editBar.addView(Look.button(this,getString(R.string.ui_18),true) { finishEditing() })
        game.addView(editBar,FrameLayout.LayoutParams(dp(390),dp(48),Gravity.CENTER))
        root.addView(game,FrameLayout.LayoutParams(-1,-1))
    }

    private fun rememberGame() {
        val file=rom ?: return
        prefs.edit().putString("lastId",file.nameWithoutExtension).putString("lastName",romName).apply()
        updateStatus()
    }

    private fun updateStatus() {
        status.text=romName.ifBlank { "Bruma GBA" }
        open.isEnabled=!busy
        progress.visibility=if(busy) View.VISIBLE else View.GONE
        updateContinueCard()
    }

    private fun updateContinueCard(){
        if(!::continueCard.isInitialized)return
        val remote=sessionClient?.sessions?.values?.maxByOrNull{it.pausedAt}
        val local=rom?.takeIf{loaded&&!inGame&&userPaused}?.let{RuntimeSession("gb",javaClass.name,RomFiles.system(it.name),it.path,0)}
        val session=remote?:local
        continueSession=session
        if(session==null||inGame||busy){continueCard.visibility=View.GONE;continueItem=null;return}
        val file=File(session.path)
        val game=libraryGames.firstOrNull{g->
            if(session.system=="RPG Maker XP")g.system==session.system&&(Uri.parse(g.uri).path==session.path||g.cached==file.name)
            else g.system==session.system&&g.cached==file.nameWithoutExtension
        }?:LibraryGame(session.token,if(session.component==javaClass.name)romName else file.name,Uri.fromFile(file).toString(),system=session.system)
        continueItem=game;continueCard.bind(game)
    }
    private fun resumeCurrentGame(){
        if(busy)return
        val session=continueSession?:return
        if(session.component==javaClass.name){enterGame();return}
        val target=when(session.component){NdsActivity::class.java.name->NdsActivity::class.java;"com.hatkid.mkxpz.MainActivity"->com.hatkid.mkxpz.MainActivity::class.java;"com.hatkid.mkxpz.ClassicActivity"->com.hatkid.mkxpz.ClassicActivity::class.java;else->return}
        startActivity(Intent(this,target).putExtra(if(session.system=="NDS")"romPath" else "gamePath",session.path).putExtra("brumaResume",true).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
    }
    private fun showContinueOptions(){
        val session=continueSession?:return
        AlertDialog.Builder(this).setTitle(continueItem?.title).setItems(arrayOf(getString(R.string.resume_action),getString(R.string.resume_close))){_,which->
            if(which==0)resumeCurrentGame() else if(session.component==javaClass.name){userPaused=true;stopGame();NativeCore.close();loaded=false;updateStatus()}
            else sessionClient?.close(session)
        }.show()
    }

    private fun enterGame() {
        if (busy || rom==null) return
        menu?.dismiss(); menu=null
        inGame=true; userPaused=false
        home.visibility=View.GONE; game.visibility=View.VISIBLE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        updateStatus(); startGame()
    }

    private fun pauseGame() {
        controls.edit(false);editBar.visibility=View.GONE
        userPaused=true; stopGame(); updateStatus()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (!resumed || !inGame || busy || menu?.isShowing==true) return
        val content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(26),dp(20),dp(26),dp(20)) }
        content.addView(label(getString(R.string.ui_24),10f,Look.mint,true)); gap(content,8)
        content.addView(label(getString(R.string.ui_25),28f,Color.WHITE,true)); gap(content,6)
        content.addView(label(romName,12f,Look.muted).apply { maxLines=1; ellipsize=TextUtils.TruncateAt.END }); gap(content,16)
        content.addView(Look.button(this,getString(R.string.ui_26),true) { enterGame() },LinearLayout.LayoutParams(-1,dp(48))); gap(content,10)
        val row=LinearLayout(this)
        row.addView(Look.button(this,getString(R.string.ui_27)) { exportSave() },LinearLayout.LayoutParams(0,dp(48),1f).apply { rightMargin=dp(6) })
        row.addView(Look.button(this,getString(R.string.ui_28)) { confirmImport() },LinearLayout.LayoutParams(0,dp(48),1f).apply { leftMargin=dp(6) })
        content.addView(row); gap(content,10)
        val display=Look.button(this,if(surface.fillScreen) getString(R.string.ui_29) else getString(R.string.ui_30)) { }
        display.setOnClickListener {
            surface.fillScreen=!surface.fillScreen
            prefs.edit().putBoolean("fillScreen",surface.fillScreen).apply()
            display.text=if(surface.fillScreen) getString(R.string.ui_29) else getString(R.string.ui_30)
        }
        content.addView(display,LinearLayout.LayoutParams(-1,dp(40))); gap(content,8)
        val sound=Look.button(this,getString(if(fastSound) R.string.fast_sound_on else R.string.fast_sound_off)) {}
        sound.setOnClickListener {
            fastSound=!fastSound;prefs.edit().putBoolean("fastSound",fastSound).apply()
            sound.text=getString(if(fastSound) R.string.fast_sound_on else R.string.fast_sound_off)
        }
        content.addView(sound,LinearLayout.LayoutParams(-1,dp(40)));gap(content,8)
        content.addView(Look.button(this,getString(R.string.pad_title)) {gamepadSettings.show()},LinearLayout.LayoutParams(-1,dp(40)))
        if(resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            content.addView(Look.button(this,getString(R.string.ui_31)) { editControls() },LinearLayout.LayoutParams(-1,dp(40)))
        }
        content.addView(Look.button(this,getString(R.string.ui_32)) { goHome() },LinearLayout.LayoutParams(-1,dp(44)))
        val dialog=Dialog(this)
        dialog.setContentView(ScrollView(this).apply { addView(content) })
        dialog.window?.setBackgroundDrawable(Look.box(this,Look.panel,26,true))
        dialog.window?.setLayout(minOf(dp(480),resources.displayMetrics.widthPixels-dp(40)),-2)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnCancelListener { enterGame() }
        var resumePressed=false
        dialog.setOnKeyListener {_,code,event->
            if(GamepadInput.controller(event) && code==KeyEvent.KEYCODE_BUTTON_THUMBR) {
                if(event.action==KeyEvent.ACTION_DOWN && event.repeatCount==0) resumePressed=true
                if(event.action==KeyEvent.ACTION_UP && resumePressed) enterGame()
                true
            } else false
        }
        menu=dialog; dialog.show()
    }

    private fun goHome(scan:Boolean=false) {
        userPaused=true; stopGame(); menu?.dismiss(); menu=null; inGame=false
        game.visibility=View.GONE; home.visibility=View.VISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); updateStatus();refreshLibrary(scan)
    }

    private fun pickDocument(code: Int) {
        if(busy) return
        userPaused=true; stopGame(); menu?.dismiss(); menu=null
        @Suppress("DEPRECATION")
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="*/*" },code)
    }

    private fun saveFile(): File = File(filesDir,"saves/${rom!!.nameWithoutExtension}.sav")

    private fun exportSave() {
        if (!NativeCore.save()) { showError(getString(R.string.ui_33)); return }
        if (!saveFile().exists()) { showError(getString(R.string.ui_34)); return }
        menu?.dismiss(); menu=null
        @Suppress("DEPRECATION")
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type="application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, romName.removeSuffix(".gba").replace(Regex("[^\\p{L}\\p{N} ._-]"),"_")+".lcsave")
        },42)
    }

    private fun confirmImport() {
        AlertDialog.Builder(this).setTitle(getString(R.string.ui_35))
            .setMessage(getString(R.string.ui_36))
            .setNegativeButton(getString(R.string.ui_37),null).setPositiveButton(getString(R.string.ui_38)) { _,_->pickDocument(43) }.show()
    }

    @Deprecated("Platform document picker callback")
    override fun onActivityResult(requestCode: Int,resultCode: Int,data: Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK) return
        if(requestCode==44) {
            val tree=data?.data ?: return
            try {
                val granted=data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                contentResolver.takePersistableUriPermission(tree,granted)
                val folders=prefs.getStringSet("treeUris",emptySet())!!.toMutableSet()
                prefs.getString("treeUri",null)?.let {folders.add(it)};folders.add(tree.toString())
                prefs.edit().putStringSet("treeUris",folders).putString("treeUri",tree.toString()).apply();refreshLibrary(true)
            } catch(e:Exception) {showError(getString(R.string.library_unavailable))}
            return
        }
        if(requestCode==45) {
            val selected=data?.data ?: return
            val key=coverTarget ?: return
            imports.execute {
                try {
                    val image=CoverImages.load(this,selected) ?: error(getString(R.string.library_bad_cover))
                    val folder=File(filesDir,"covers").apply {mkdirs()}
                    val file=File(folder,"custom-$key-${System.currentTimeMillis()}.png")
                    file.outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
                    libraryStore.setCover(key,Uri.fromFile(file).toString())
                    runOnUiThread {if(!isDestroyed) refreshLibrary(false)}
                } catch(e:Exception) {runOnUiThread {if(!isDestroyed) showError(getString(R.string.library_bad_cover))}}
            }
            return
        }
        if(requestCode !in 41..43) return
        val uri=data?.data ?: return
        userPaused=true; stopGame(); menu?.dismiss(); menu=null; busy=true; updateStatus()
        val current=rom
        imports.execute {
            try {
                when(requestCode) {
                    41 -> {
                        if(RomFiles.name(this,uri).endsWith(".zip",true)) {
                            val imported=GameArchives.import(this,uri)
                            libraryStore.add(imported)
                            runOnUiThread {if(!isDestroyed){busy=false;updateStatus();refreshLibrary(false);if(imported.size==1)launchLibraryGame(imported.first())}}
                            return@execute
                        }
                        val (file,name)=importRom(uri)
                        libraryStore.remember(uri.toString(),name,file.nameWithoutExtension,RomFiles.system(file.name))
                        runOnUiThread {
                            if(isDestroyed) return@runOnUiThread
                            NativeCore.close(); loaded=false; busy=false
                            if(RomFiles.system(file.name)=="NDS"){updateStatus();refreshLibrary(false);startActivity(Intent(this,NdsActivity::class.java).putExtra("romPath",file.path).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))}
                            else {rom=file;romName=name;updateStatus();enterGame()}
                        }
                    }
                    42 -> {
                        val file=current ?: error(getString(R.string.ui_39))
                        val save=File(filesDir,"saves/${file.nameWithoutExtension}.sav")
                        val bytes=SaveArchive.encode(file.nameWithoutExtension,romName,save.readBytes())
                        contentResolver.openOutputStream(uri,"wt")?.use { it.write(bytes) } ?: error(getString(R.string.ui_40))
                        done(getString(R.string.ui_41))
                    }
                    43 -> {
                        val file=current ?: error(getString(R.string.ui_39))
                        val bytes=contentResolver.openInputStream(uri)?.use { SaveArchive.decodeImport(it,file.nameWithoutExtension,RomFiles.name(this,uri)) } ?: error(getString(R.string.ui_42))
                        // The core is stopped before replacing the save; it must never write its old memory over the import.
                        runOnUiThread {
                            if(isDestroyed) return@runOnUiThread
                            try {
                                NativeCore.close(); loaded=false
                                SaveArchive.replace(File(filesDir,"saves/${file.nameWithoutExtension}.sav"),bytes)
                                done(getString(R.string.ui_43))
                            } catch(error: Exception) {
                                busy=false; updateStatus(); pauseGame()
                                showError(localizedError(error) ?: getString(R.string.ui_44))
                            }
                        }
                    }
                }
            } catch(error: Exception) {
                runOnUiThread { if(!isDestroyed) { busy=false; updateStatus(); pauseGame(); showError(localizedError(error) ?: getString(R.string.ui_45)) } }
            }
        }
    }

    private fun done(message: String) { runOnUiThread { if(!isDestroyed) { busy=false; updateStatus(); pauseGame(); Toast.makeText(this,message,Toast.LENGTH_LONG).show() } } }

    private fun showHelp() {
        AlertDialog.Builder(this).setTitle(getString(R.string.ui_46))
            .setMessage(getString(R.string.ui_47))
            .setNeutralButton(getString(R.string.ui_48)) { _,_->showLicenses() }.setPositiveButton(getString(R.string.ui_49),null).show()
    }

    private fun showLicenses() {
        startActivity(android.content.Intent(this, OpenSourceLicensesActivity::class.java))
    }

    private fun importRom(uri:Uri):Pair<File,String> = RomFiles.import(this,uri)

    private fun startGame() {
        val file = rom ?: return
        if (!resumed || !surfaceReady || userPaused || running || busy) return
        running = true
        worker = Thread({
            var audio: AudioTrack? = null
            try {
                if (!loaded) {
                    val saves = File(filesDir, "saves").apply { mkdirs() }
                    val error = NativeCore.load(file.path, File(saves, file.nameWithoutExtension + ".sav").path)
                    check(error == null) { error ?: getString(R.string.ui_57) }
                    loaded = true
                    runOnUiThread { if (!isDestroyed) rememberGame() }
                }
                val format = AudioFormat.Builder().setSampleRate(48000)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
                audio = AudioTrack.Builder().setAudioFormat(format)
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setBufferSizeInBytes(maxOf(16384, AudioTrack.getMinBufferSize(48000,
                        AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)))
                    .setTransferMode(AudioTrack.MODE_STREAM).build()
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
                val videoSize=NativeCore.videoSize()
                val bitmap = Bitmap.createBitmap(videoSize ushr 16, videoSize and 65535, Bitmap.Config.ARGB_8888)
                val isGameBoy = bitmap.width == 160 && bitmap.height == 144
                val samples = ShortArray(4096)
                var audioStarted = false
                var previousSpeed=0
                var previousSound=false
                var deadline=System.nanoTime()
                val accelerated=ShortArray(4096)
                val resampler=SpeedAudio()
                val performanceStart = System.nanoTime()
                var nextSave=System.nanoTime()+5_000_000_000L
                var frames = 0
                while (running) {
                    val count = NativeCore.frame(controls.keys or gamepad.keys, bitmap, samples)
                    val generatedAt=System.nanoTime()
                    check(count >= 0) { getString(R.string.ui_58) }
                    val speed=if(fastForward) fastMultiplier else 1
                    val audible=speed==1 || fastSound
                    if(speed!=previousSpeed || audible!=previousSound) {
                        // Only stop and discard queued audio when sound is actually muted.
                        // Changing 2x <-> 4x keeps the live track running; flushing it on every
                        // tap can leave AudioTrack rejecting the next blocking write.
                        if(audible!=previousSound) {
                            if(audible) {
                                audio.play();audioStarted=true
                            } else {
                                audio.pause();audio.flush();audioStarted=false
                            }
                        }
                        deadline=System.nanoTime()
                        resampler.reset();previousSpeed=speed;previousSound=audible
                    }
                    // AudioTrack must be playing before a blocking stream write starts.
                    if(audible && !audioStarted) {audio.play();audioStarted=true}
                    val audioCount=if(speed==1) count else resampler.convert(samples,count,speed,accelerated)
                    val pcm=if(speed==1) samples else accelerated
                    var offset=0
                    while(running && audible && offset<audioCount) {
                        val written=audio.write(pcm,offset,audioCount-offset,AudioTrack.WRITE_BLOCKING)
                        if(written<=0) android.util.Log.e("BrumaAudio","AudioTrack.write returned $written; state=${audio.state}, speed=${speed}x, samples=${audioCount-offset}")
                        check(written>0) { getString(R.string.ui_59) };offset+=written
                    }
                    // GB/GBC frames are ready now; publish before waiting for the next tick.
                    if(isGameBoy && frames%speed==0) surface.drawFrame(bitmap,generatedAt)
                    // Pace every emulated frame, including the initial audio-buffer fill.
                    deadline+=16_742_706L/speed
                    val wait=deadline-System.nanoTime()
                    if(wait>0) LockSupport.parkNanos(wait)
                    else if(wait < -50_000_000L) deadline=System.nanoTime()
                    if(!isGameBoy && frames%speed==0) surface.drawFrame(bitmap,generatedAt)
                    if (frames > 0 && frames % 300 == 0) android.util.Log.i("BrumaPerf", "fps=" + (frames*1e9/(System.nanoTime()-performanceStart)) + " underruns=" + audio.underrunCount + " fill=" + surface.fillScreen + " speed=" + speed + " sound=" + audible)
                    frames++
                    val now=System.nanoTime()
                    if(now>=nextSave) {
                        if(!NativeCore.save()) error(getString(R.string.ui_33))
                        nextSave=System.nanoTime()+5_000_000_000L
                    }
                }
                bitmap.recycle()
            } catch (error: Exception) {
                android.util.Log.e("BrumaPerf", "Operation failed", error)
                runOnUiThread {
                    if (!isDestroyed) {
                        userPaused = true
                        updateStatus()
                        showError(localizedError(error) ?: getString(R.string.ui_60))
                    }
                }
            } finally {
                audio?.pause()
                audio?.flush()
                audio?.release()
                NativeCore.save()
                running = false
            }
        }, "LinkCore-GBA").also { it.start() }
    }

    private fun stopGame() {
        running = false
        worker?.let { LockSupport.unpark(it); it.join() }
        worker = null
        surface.clearPending()
        controls.clearKeys();gamepad.clear()
    }

    private fun localizedError(error:Exception):String? = if(error is SaveArchive.Failure) getString(error.resourceId) else error.message

    private fun showError(message: String) = AlertDialog.Builder(this).setTitle("Bruma GBA").setMessage(message).setPositiveButton(getString(R.string.ui_61),null).show()
    private fun handleBack() { if(inGame) { if(menu?.isShowing==true) enterGame() else pauseGame() } else finish() }
    // API 33+ uses onBackInvokedDispatcher above; this override only serves older devices.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Deprecated("Legacy back navigation")
    override fun onBackPressed() { handleBack() }
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        controls.edit(false);editBar.visibility=View.GONE
        controls.clearKeys();gamepad.clear()
        menu?.dismiss(); menu=null
        root.removeView(home);createHome()
        if(inGame) home.visibility=View.GONE
        updateStatus()
        if(inGame && userPaused) pauseGame()
    }
    override fun onSaveInstanceState(outState:Bundle) {outState.putString("coverTarget",coverTarget);super.onSaveInstanceState(outState)}
    override fun dispatchKeyEvent(event:KeyEvent):Boolean {
        if(inGame && !userPaused && !controls.editing && gamepad.key(event)) return true
        return super.dispatchKeyEvent(event)
    }
    override fun dispatchGenericMotionEvent(event:MotionEvent):Boolean {
        if(inGame && !userPaused && !controls.editing && gamepad.motion(event)) return true
        return super.dispatchGenericMotionEvent(event)
    }
    override fun onWindowFocusChanged(hasFocus:Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(!hasFocus) gamepad.clear()
    }
    override fun onResume() {
        super.onResume(); resumed=true
        sessionClient?.refresh()
        val pending=File(filesDir,"rpg-pending-launch.txt")
        if(pending.isFile) {
            val path=runCatching {pending.readText().trim()}.getOrDefault("")
            pending.delete()
            val root=File(filesDir,"rpg-games").canonicalFile
            val game=runCatching {File(path).canonicalFile}.getOrNull()
            if(game!=null && game.path.startsWith(root.path+File.separator) && File(game,"Game.ini").isFile) {
                startActivity(Intent(this,com.hatkid.mkxpz.RpgLibraryActivity::class.java).putExtra("launchUri",Uri.fromFile(game).toString()))
                return
            }
        }
        activeSourceMonitor.start()
        if(!inGame) refreshLibrary(true)
        if(inGame && userPaused) pauseGame() else startGame()
    }
    override fun onPause() { activeSourceMonitor.stop(); resumed=false; userPaused=true; stopGame(); super.onPause() }
    override fun onDestroy() { activeSourceMonitor.dispose(); stopGame(); menu?.dismiss(); NativeCore.close(); sessionClient?.dispose();if(::continueCard.isInitialized)continueCard.close();libraryAdapter?.close(); scraper.close(); gamepadSettings.close(); gamepad.close(); imports.shutdown(); libraryScanner.shutdown(); super.onDestroy() }
    override fun surfaceCreated(holder: SurfaceHolder) { surfaceReady=true; surface.setGameFrameRate(); startGame() }
    override fun surfaceChanged(holder: SurfaceHolder,format: Int,width: Int,height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) { surfaceReady=false; stopGame() }
}











