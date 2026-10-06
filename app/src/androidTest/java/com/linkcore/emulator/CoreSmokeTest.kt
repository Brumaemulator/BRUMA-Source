package com.linkcore.emulator

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import java.io.File

/** On-device integration checks using an original homebrew fixture. No test libraries. */
class CoreSmokeTest : Instrumentation() {
    private var rawSave=false; private var licenseChecks=false; private var cleanup=false; private var deletedSource=false; private var crystal=false; private var engines=false; private var scale=false; private var imports=false; private var gbcUi=false
    private var ndsClose=false
    private var libraryDeletion=false
    private var archives=false
    private var utilities=false
    private var library=false
    private var pokemon=false
    private var pausedGames=false; private var rpg=false; private var ndsLanguage=false; private var ndsExpected=5; private var ndsLaunch:String?=null
    override fun onCreate(arguments: Bundle?) { ndsClose=arguments?.getString("ndsClose")=="true"; libraryDeletion=arguments?.getString("libraryDeletion")=="true"; rawSave=arguments?.getString("rawSave")=="true"; licenseChecks=arguments?.getString("licenseChecks")=="true"; pausedGames=arguments?.getString("pausedGames")=="true"; ndsExpected=arguments?.getString("ndsExpected")?.toInt()?:5; ndsLaunch=arguments?.getString("ndsLaunch"); ndsLanguage=arguments?.getString("ndsLanguage")=="true"; cleanup=arguments?.getString("cleanup")=="true"; deletedSource=arguments?.getString("deletedSource")=="true"; crystal=arguments?.getString("crystal")=="true";engines=arguments?.getString("engines")=="true";scale=arguments?.getString("scale")=="true";imports=arguments?.getString("imports")=="true";gbcUi=arguments?.getString("gbcUi")=="true";archives=arguments?.getString("archives")=="true";utilities=arguments?.getString("utilities")=="true";library=arguments?.getString("library")=="true";pokemon=arguments?.getString("pokemon")=="true";rpg=arguments?.getString("rpg")=="true"; super.onCreate(arguments); start() }
    override fun onStart() {
        if(ndsClose){NdsCloseChecks.run(this);return}
        if(libraryDeletion){LibraryDeletionTest.run(this);return}
        if(rawSave){RawSaveChecks.run(this);return}
        if(licenseChecks){LicenseRuntimeChecks.run(this);return}
        val report = Bundle()
        try {
            if(pausedGames) {PausedGameChecks.run(this);report.putString("stream","PASS: NDS back/pause, same-session resume, card portrait/landscape, close hides card; GBA, modern RPG and classic RPG pause/resume/close.\n");finish(Activity.RESULT_OK,report);return}
            if(ndsLaunch!=null) {targetContext.startActivity(android.content.Intent(targetContext,NdsActivity::class.java).putExtra("romPath",File(targetContext.filesDir,"roms/"+ndsLaunch).path).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(2000);report.putString("stream","PASS: NDS Activity launch\n");finish(Activity.RESULT_OK,report);return}
            if(ndsLanguage) {NdsLanguageChecks.run(this,ndsExpected);report.putString("stream","PASS: two DS ROMs booted with all seven firmware languages; game boot memory verified; frames/audio produced; user saves untouched.\n");finish(Activity.RESULT_OK,report);return}
            if(cleanup) {CacheCleanupCheck.run(this);report.putString("stream","PASS cache removal, save preservation and restore, path guards, unavailable source protection\n");finish(Activity.RESULT_OK,report);return}; if(deletedSource) {DeletedSourceCheck.run(this);report.putString("stream","PASS removed source excluded, existing source retained, internal copy preserved\n");finish(Activity.RESULT_OK,report);return}; if(crystal) {CrystalPacingCheck.run(this);report.putString("stream","PASS Crystal pacing and original save preserved\n");finish(Activity.RESULT_OK,report);return}; if(engines) {RpgEngineChecks.run(this);report.putString("stream","PASS engine selection\n");finish(Activity.RESULT_OK,report);return}; if(scale) {LibraryScaleChecks.run(this);report.putString("stream","PASS library scale checks\n");finish(Activity.RESULT_OK,report);return}; if(imports) {RpgImportChecks.run(this);report.putString("stream","PASS RPG import integrity and benchmark\n");finish(Activity.RESULT_OK,report);return}; if(gbcUi) {GbcUiCheck.run(this);report.putString("stream","PASS: GBC library launch and 160x144 surface\n");finish(Activity.RESULT_OK,report);return}
            if(archives) {ArchiveChecks.run(this);report.putString("stream","PASS: GBC video/audio, GB/GBC ZIPs, safe extraction, nested RPG ZIP and Pokemon Z\n");finish(Activity.RESULT_OK,report);return}
            if(utilities) {MovableButtonsChecks.run(this);report.putString("stream","PASS: utility drag, persistence, clamping, cancellation, reset and edit taps\n");finish(Activity.RESULT_OK,report);return}
            if(library) {LibrarySmoke.run(this);report.putString("stream","PASS: recursive folder scan, local covers, search, library launch, custom cover, gameplay preview, portrait/landscape\n");finish(Activity.RESULT_OK,report);return}
            if(rpg) {RpgSmoke.run(this);report.putString("stream","PASS: integrated RPG library, Pokemon Anil discovery and isolated RPG process launch\n");finish(Activity.RESULT_OK,report);return}
            if(pokemon) { PokemonSmoke.run(this); report.putString("stream","PASS: Pokemon normal/fullscreen/portrait/2x, original save preserved\n");finish(Activity.RESULT_OK,report);return }
            GamepadSmoke.run(this)
            LibretroSmoke.run(this)
            val directory = File(targetContext.cacheDir, "core-smoke").apply { mkdirs() }
            val rom = File(directory, "smoke.gba")
            context.assets.open("smoke.gba").use { input -> rom.outputStream().use { input.copyTo(it) } }
            val save = File(directory, "smoke.sav").apply { delete() }
            val invalid = File(directory, "invalid.gba").apply { writeText("invalid") }
            check(NativeCore.load(invalid.path, save.path) != null) { "Invalid ROM accepted" }
            check(NativeCore.load(rom.path, save.path) == null) { "Homebrew failed to load" }
            val bitmap = Bitmap.createBitmap(240, 160, Bitmap.Config.ARGB_8888)
            val audio = ShortArray(4096)
            var producedAudio = false
            fun frames(keys: Int, expected: Int) {
                repeat(8) {
                    val count = NativeCore.frame(keys, bitmap, audio)
                    check(count in 1..4096) { "Missing PCM output: $count" }
                    if ((0 until count).any { audio[it].toInt() != 0 }) producedAudio = true
                }
                val actual = bitmap.getPixel(120, 80)
                check(kotlin.math.abs(Color.red(actual) - Color.red(expected)) <= 8 &&
                    kotlin.math.abs(Color.green(actual) - Color.green(expected)) <= 8 &&
                    kotlin.math.abs(Color.blue(actual) - Color.blue(expected)) <= 8) {
                    "Pixel ${Integer.toHexString(bitmap.getPixel(120, 80))}, expected ${Integer.toHexString(expected)}"
                }
            }
            frames(0, Color.RED)
            frames(1, Color.GREEN)
            frames(2, Color.BLUE)
            frames(16, Color.WHITE)
            check(producedAudio) { "Only silent audio" }
            check(NativeCore.save()) { "Save failed" }
            check(save.exists() && save.readBytes()[0] == 0x42.toByte()) { "SRAM not persisted" }
            NativeCore.close()
            check(NativeCore.load(rom.path, save.path) == null)
            frames(0, Color.YELLOW)
            NativeCore.close()
            bitmap.recycle()
            val hash = "a".repeat(64)
            val savedBytes = save.readBytes()
            val archive = SaveArchive.encode(hash,"Homebrew",savedBytes)
            val restored = SaveArchive.decode(archive.inputStream(),hash)
            check(restored.contentEquals(savedBytes)) { "Portable save round-trip changed SRAM" }
            check(runCatching { SaveArchive.decode(archive.inputStream(),"b".repeat(64)) }.isFailure) { "Wrong-ROM save accepted" }
            check(runCatching { SaveArchive.decode(byteArrayOf(1,2,3).inputStream(),hash) }.isFailure) { "Invalid archive accepted" }
            check(runCatching { SaveArchive.decode(archive.copyOf(archive.size/2).inputStream(),hash) }.isFailure) { "Truncated archive accepted" }
            val destination = File(directory,"import-target.sav")
            val previous = ByteArray(32768) { 7 }
            destination.writeBytes(previous)
            SaveArchive.replace(destination,restored)
            check(destination.readBytes().contentEquals(restored)) { "Import changed save bytes" }
            check(directory.listFiles()!!.any { it.name.startsWith("import-target.sav.before-import-") && it.readBytes().contentEquals(previous) }) { "Previous save not backed up" }
            for(hz in listOf(60,120)) {
                val cadence=FrameCadence()
                val tick=1_000_000_000L/hz
                val times=ArrayList<Long>()
                repeat(hz*60) {n ->
                    val time=1_000_000_000L+n*tick
                    if(cadence.ready(time,tick)) {times.add(time);cadence.presented(time)}
                }
                check(kotlin.math.abs(times.size-60*59.7275)<2) { "Display cadence changed emulation rate" }
                val gaps=times.zipWithNext {a,b->b-a}
                check(gaps.max()<=(if(hz==120) 3 else 2)*tick) { "Unexpected presentation stall" }
                if(hz==120) check(gaps.min()>=2*tick) { "Frames presented in a burst" }
                cadence.reset();check(cadence.ready(1L,tick))
            }
            val speedAudio=SpeedAudio()
            val output=ShortArray(20)
            check(speedAudio.convert(shortArrayOf(100,-100,300,-300),4,2,output)==2)
            check(output[0].toInt()==200 && output[1].toInt()==-200)
            speedAudio.reset()
            check(speedAudio.convert(shortArrayOf(100,-100,300,-300),4,4,output)==0)
            check(speedAudio.convert(shortArrayOf(500,-500,700,-700),4,4,output)==2)
            check(output[0].toInt()==400 && output[1].toInt()==-400) { "4x stereo stream lost partial samples" }
            for(language in listOf("es","en")) {
                val config=android.content.res.Configuration(targetContext.resources.configuration)
                config.setLocale(java.util.Locale.forLanguageTag(language))
                val localized=targetContext.createConfigurationContext(config)
                check(localized.getString(R.string.ui_1)==if(language=="es") "Ayuda" else "Help")
            }
            runOnMainSync {
                val isolated=object:android.content.ContextWrapper(targetContext) {
                    override fun getSharedPreferences(name:String,mode:Int)=super.getSharedPreferences("test-"+name,mode)
                }
                isolated.getSharedPreferences("controls",0).edit().clear().commit()
                val controls = ControlsView(isolated)
                val d = targetContext.resources.displayMetrics.density
                controls.layout(0, 0, (1000 * d).toInt(), (400 * d).toInt())
                val event = MotionEvent.obtain(1, 1, MotionEvent.ACTION_DOWN, 936 * d, 236 * d, 0)
                controls.onTouchEvent(event)
                check(controls.keys == 1) { "Touch A mapping failed: ${controls.keys}" }
                event.recycle()
                val props=arrayOf(MotionEvent.PointerProperties().apply { id=0; toolType=MotionEvent.TOOL_TYPE_FINGER },MotionEvent.PointerProperties().apply { id=1; toolType=MotionEvent.TOOL_TYPE_FINGER })
                val coords=arrayOf(MotionEvent.PointerCoords().apply { x=936*d;y=236*d;pressure=1f;size=1f },MotionEvent.PointerCoords().apply { x=144*d;y=288*d;pressure=1f;size=1f })
                val combined=MotionEvent.obtain(1,2,MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,props,coords,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0)
                controls.onTouchEvent(combined)
                check(controls.keys==17) { "Simultaneous A + right failed: ${controls.keys}" }
                combined.recycle()
                val cancel=MotionEvent.obtain(1,3,MotionEvent.ACTION_CANCEL,0f,0f,0)
                controls.onTouchEvent(cancel); cancel.recycle()
                check(controls.keys==0) { "Cancelled controls stuck" }
                controls.clearKeys()
                check(controls.keys == 0) { "Touch release stuck" }
                controls.edit(true)
                fun touch(action:Int,x:Float,y:Float) {
                    val e=MotionEvent.obtain(1,5,action,x*d,y*d,0);controls.onTouchEvent(e);e.recycle()
                }
                touch(MotionEvent.ACTION_DOWN,936f,236f)
                touch(MotionEvent.ACTION_MOVE,850f,180f)
                touch(MotionEvent.ACTION_UP,850f,180f)
                check(controls.keys==0) { "Editing sent game input" }
                val restoredControls=ControlsView(isolated)
                restoredControls.layout(0,0,(1000*d).toInt(),(400*d).toInt())
                val moved=MotionEvent.obtain(1,6,MotionEvent.ACTION_DOWN,850*d,180*d,0)
                restoredControls.onTouchEvent(moved);moved.recycle()
                check(restoredControls.keys==1) { "Moved A position was not persisted" }
                controls.edit(false);controls.resetPositions()
                controls.layout(0,0,(390*d).toInt(),(844*d).toInt())
                val region=GameLayout.viewport((390*d).toInt(),(844*d).toInt(),d)
                check(kotlin.math.abs(region.width()/region.height()-1.5f)<.01f) { "Portrait distorted original aspect" }
                check(region.width()>380*d) { "Portrait viewport too narrow" }
                val panelTop=minOf(region.bottom+152*d,844*d-296*d).coerceAtLeast(region.bottom+32*d)
                val cy=panelTop+280*d*.48f
                val portraitA=MotionEvent.obtain(1,4,MotionEvent.ACTION_DOWN,335*d,cy-19*d,0)
                controls.onTouchEvent(portraitA);portraitA.recycle()
                check(controls.keys==1) { "Portrait A mapping failed: ${controls.keys}" }
                controls.clearKeys()
            }
            UiSmoke.run(this)
            report.putString("stream", "PASS: invalid-ROM rejection, GBA frames, A/B/right input, non-silent stereo PCM, SRAM save/reload, touch A/release, portable-save round-trip, wrong-ROM/corrupt/truncated rejection, import backup; UI home, ROM picker, pause/resume, background pause, remembered game.\n")
            finish(Activity.RESULT_OK, report)
        } catch (error: Throwable) {
            report.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
            finish(Activity.RESULT_CANCELED, report)
        } finally { NativeCore.close() }
    }
}




