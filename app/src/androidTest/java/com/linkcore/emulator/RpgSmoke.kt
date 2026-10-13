package com.linkcore.emulator

import android.app.ActivityManager
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import java.io.File

object RpgSmoke {
    fun run(test: Instrumentation) {
        val game = File(test.targetContext.filesDir, "rpg-games/anil")
        check(File(game, "Game.ini").isFile) { "Pokemon Anil Game.ini missing" }
        check(File(game, "Data/Scripts.rxdata").isFile) { "Pokemon Anil scripts missing" }

        val activity = test.startActivitySync(
            Intent(test.targetContext, com.hatkid.mkxpz.RpgLibraryActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        // Regression: an existing import must gain Zlib without changing its save path or preloads.
        val fixture=File(test.targetContext.cacheDir,"rpg-zlib-regression").apply {mkdirs()}
        File(fixture,"Data").mkdirs()
        File(fixture,"Game.ini").writeText("[Game]\nTitle=Fixture")
        File(fixture,"Data/Scripts.rxdata").writeBytes(byteArrayOf())
        File(fixture,"mkxp.json.before-bruma").delete()
        val original="{ // preserved settings\n \"dataPathApp\":\"existing-save\",\"preloadScript\":[\"custom.rb\"],}"
        File(fixture,"mkxp.json").writeText(original)
        val prepare=activity.javaClass.getDeclaredMethod("prepare",File::class.java).apply {isAccessible=true}
        prepare.invoke(activity,fixture)
        prepare.invoke(activity,fixture)
        val config=org.json.JSONObject(File(fixture,"mkxp.json").readText())
        check(config.getString("dataPathApp")=="existing-save")
        check(!config.getBoolean("syncToRefreshrate") && config.getInt("fixedFramerate")==0)
        val preloads=config.getJSONArray("preloadScript")
        check(preloads.length()==2 && preloads.getString(0)=="bruma-compat.rb" && preloads.getString(1)=="custom.rb")
        check(File(fixture,"bruma-compat.rb").readText().startsWith(test.targetContext.assets.open("rpg/bruma-compat.rb").bufferedReader().use {it.readText()}))
        check(File(fixture,"mkxp.json.before-bruma").readText()==original)
        RpgInputChecks.run(test)
        test.targetContext.getSharedPreferences("rpg",0).edit().putBoolean("fps60:anil",true).commit()
        test.runOnMainSync {activity.finish()}
        val unified=test.startActivitySync(Intent(test.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Thread.sleep(1800)
        val entry=LibraryStore(test.targetContext).allGames().firstOrNull {it.system=="RPG Maker XP" && it.uri.endsWith("/anil")}
        check(entry!=null) {"RPG game missing from unified library"}
        test.runOnMainSync {
            MainActivity::class.java.getDeclaredMethod("launchLibraryGame",LibraryGame::class.java).apply {isAccessible=true}.invoke(unified,entry)
        }
        val manager = test.targetContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        repeat(30) {
            Thread.sleep(500)
            if (manager.runningAppProcesses?.any { it.processName == "com.linkcore.emulator:rpg" } == true) { Thread.sleep(45000); check(manager.runningAppProcesses?.any { it.processName == "com.linkcore.emulator:rpg" } == true) { "RPG process exited during startup" }; return }
        }
        error("RPG engine process did not start")
    }
}



