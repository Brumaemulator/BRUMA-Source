// SPDX-License-Identifier: MIT
// Copyright (c) 2026 BRUMA contributors
// Original BRUMA implementation of the existing runtime contract.
package com.hatkid.mkxpz;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.io.File;
import org.libsdl.app.SDLActivity;
import com.hatkid.mkxpz.gamepad.Gamepad;
import com.hatkid.mkxpz.gamepad.GamepadConfig;
import android.util.Log;

/** SDL host with the unchanged Java method signatures expected by public mkxp. */
public class MainActivity extends SDLActivity implements android.hardware.input.InputManager.InputDeviceListener {
    private static final String TAG="BRUMA-RPG";
    private static String GAME_PATH="";
    private final Gamepad mGamepad=new Gamepad();
    protected boolean mStarted;
    private boolean mGamepadInvisible;
    private boolean pauseOnReturn,closingToLibrary,closeReceiverRegistered,inputRegistered;
    private static final String CLOSE_PAUSED_RPG_ACTION="com.linkcore.emulator.ACTION_CLOSE_PAUSED_RPG";
    private final android.content.BroadcastReceiver closePausedReceiver=new android.content.BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent intent){pauseOnReturn=false;if(mStarted)pauseNativeThread();finish();}
    };
    private android.window.OnBackInvokedCallback brumaBackCallback;
    private com.linkcore.emulator.RuntimeSessionHost sessionHost;
    private com.linkcore.emulator.MovableButtons movableUtilities;
    private boolean controlsHidden,editingControls;
    private int logicalWidth=512,logicalHeight=384;
    private android.widget.Button optionsButton;
    private boolean es(){return getResources().getConfiguration().getLocales().get(0).getLanguage().equals("es");}
    private String tr(String es,String en){return es()?es:en;}
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(com.linkcore.emulator.BrumaLocale.attach(base));}
    @Override protected String[] getLibraries(){return new String[]{"SDL2","SDL2_image","SDL2_ttf","SDL2_sound","openal","ruby","mkxp-z"};}
    @Override protected String[] getArguments(){
        boolean debug=false;
        try {android.content.pm.ActivityInfo info=getPackageManager().getActivityInfo(getComponentName(),android.content.pm.PackageManager.GET_META_DATA);debug=info.metaData!=null&&info.metaData.getBoolean("mkxp_debug",false);}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
        return debug?new String[]{"debug"}:new String[0];
    }
    @Override protected void onCreate(Bundle state){
        String requested=getIntent().getStringExtra("gamePath");
        GAME_PATH=requested==null?getSharedPreferences("rpg",0).getString("path",""):requested;
        try {if(!new File(GAME_PATH).getCanonicalPath().startsWith(getFilesDir().getCanonicalPath()+File.separator)){super.onCreate(state);finish();return;}}catch(java.io.IOException e){super.onCreate(state);finish();return;}
        super.onCreate(state);
        if(mBrokenLibraries||mSurface==null)return;
        android.content.IntentFilter filter=new android.content.IntentFilter(CLOSE_PAUSED_RPG_ACTION);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(closePausedReceiver,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(closePausedReceiver,filter);
        closeReceiverRegistered=true;
        sessionHost=new com.linkcore.emulator.RuntimeSessionHost(this,"RPG Maker XP",()->GAME_PATH,()->mStarted&&!isFinishing()&&!mBrokenLibraries,()->{pauseOnReturn=false;if(mStarted)pauseNativeThread();finish();});
        if(Build.VERSION.SDK_INT>=33){brumaBackCallback=this::showRpgOptions;getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,brumaBackCallback);}
        try {byte[] content=java.nio.file.Files.readAllBytes(new File(GAME_PATH,"mkxp.json").toPath());if(content.length<=1048576){org.json.JSONObject cfg=ConfigJson.parse(new String(content,java.nio.charset.StandardCharsets.UTF_8));logicalWidth=cfg.optInt("defScreenW",512);logicalHeight=cfg.optInt("defScreenH",384);}}catch(Exception ignored){}
        logicalWidth=Math.max(128,Math.min(2048,logicalWidth));logicalHeight=Math.max(128,Math.min(2048,logicalHeight));
        mSurface.getHolder().setFixedSize(logicalWidth,logicalHeight);
        android.widget.RelativeLayout.LayoutParams surface=new android.widget.RelativeLayout.LayoutParams(logicalWidth,logicalHeight);surface.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);mSurface.setLayoutParams(surface);
        mGamepadInvisible=isAndroidTV()||isChromebook();mGamepad.init(new GamepadConfig(),mGamepadInvisible);
        mGamepad.setOnKeyDownListener(SDLActivity::onNativeKeyDown);mGamepad.setOnKeyUpListener(SDLActivity::onNativeKeyUp);
        if(mLayout!=null){
            mGamepad.attachTo(this,mLayout);addUtilityButtons();controlsHidden=getSharedPreferences("rpg-controls-ui",0).getBoolean("hidden",false);if(controlsHidden)mGamepad.hideView();
            mLayout.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol||b-t!=ob-ot)resizeGame();});mLayout.post(this::resizeGame);
            ((android.hardware.input.InputManager)getSystemService(INPUT_SERVICE)).registerInputDeviceListener(this,null);inputRegistered=true;
        }
    }
    @Override protected void onStart(){super.onStart();if(mBrokenLibraries)return;mStarted=true;if(mHasMultiWindow)resumeNativeThread();}
    @Override protected void onDestroy(){
        if(sessionHost!=null){sessionHost.dispose();sessionHost=null;}
        if(closeReceiverRegistered)unregisterReceiver(closePausedReceiver);
        if(Build.VERSION.SDK_INT>=33&&brumaBackCallback!=null)getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(brumaBackCallback);
        if(inputRegistered)((android.hardware.input.InputManager)getSystemService(INPUT_SERVICE)).unregisterInputDeviceListener(this);
        mGamepad.releaseAll();super.onDestroy();
        // The existing isolated RPG process must exit to avoid restarting the Ruby VM.
        System.exit(0);
    }
    @Override public void onWindowFocusChanged(boolean focus){if(!focus)mGamepad.releaseAll();super.onWindowFocusChanged(focus);if(focus)getWindow().getDecorView().postDelayed(this::hideSystemBars,200);}
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        int k=event.getKeyCode();
        // SDL consumes BACK as a native key, preventing Activity.onBackPressed.
        if(k==KeyEvent.KEYCODE_BACK){
            if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())showRpgOptions();
            return true;
        }
        if(k!=KeyEvent.KEYCODE_BACK&&k!=KeyEvent.KEYCODE_VOLUME_DOWN&&k!=KeyEvent.KEYCODE_VOLUME_UP&&k!=KeyEvent.KEYCODE_VOLUME_MUTE&&!mGamepadInvisible){mGamepad.hideView();mGamepadInvisible=true;}
        return mGamepad.processGamepadEvent(event)||super.dispatchKeyEvent(event);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event){if(mGamepadInvisible&&!controlsHidden){mGamepad.showView();mGamepadInvisible=false;}return super.dispatchTouchEvent(event);}
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event){return mGamepad.processDPadEvent(event)||super.dispatchGenericMotionEvent(event);}
    // These names and descriptors are part of the public engine JNI contract.
    @SuppressWarnings("unused") private static String getSystemLanguage(){return java.util.Locale.getDefault().toString();}
    private static android.os.Vibrator vibrator(){return (android.os.Vibrator)getContext().getSystemService(Context.VIBRATOR_SERVICE);}
    @SuppressWarnings("unused") private static boolean hasVibrator(){android.os.Vibrator v=vibrator();return v!=null&&v.hasVibrator();}
    @SuppressWarnings("unused") private static void vibrate(int duration){android.os.Vibrator v=vibrator();if(v!=null&&duration>0)v.vibrate(android.os.VibrationEffect.createOneShot(duration,android.os.VibrationEffect.EFFECT_HEAVY_CLICK));}
    @SuppressWarnings("unused") private static void vibrateStop(){android.os.Vibrator v=vibrator();if(v!=null)v.cancel();}
    @SuppressWarnings("unused") private static boolean inMultiWindow(Activity activity){return activity.isInMultiWindowMode();}
    private int dp(int value) {return Math.round(value*getResources().getDisplayMetrics().density);}
    private void addUtilityButtons() {
        movableUtilities=new com.linkcore.emulator.MovableButtons(this,mLayout,"rpg-utility-positions",()->editingControls);
        boolean spanish=getResources().getConfiguration().getLocales().get(0).getLanguage().equals("es");
        android.widget.Button keyboard=utilityButton(spanish?"Opciones":"Options");optionsButton=keyboard;
        keyboard.setContentDescription(spanish?"Abrir teclado para escribir":"Open keyboard to type");
        keyboard.setOnClickListener(v->{
            if(editingControls){editingControls=false;mGamepad.edit(false);optionsButton.setText(tr("Opciones","Options"));}else showRpgOptions();
        });
        android.widget.Button enter=utilityButton("Enter");
        enter.setContentDescription(spanish?"Confirmar con Enter":"Confirm with Enter");
        enter.setOnClickListener(v->{
            SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ENTER);
            v.postDelayed(()->SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ENTER),100);
            SDLActivity.sendMessage(3,0);
            v.postDelayed(this::hideSystemBars,150);
        });
        mLayout.addView(keyboard,new android.widget.RelativeLayout.LayoutParams(dp(100),dp(44)));
        mLayout.addView(enter,new android.widget.RelativeLayout.LayoutParams(dp(100),dp(44)));
        movableUtilities.bind(keyboard,"options",-1,true);
        movableUtilities.bind(enter,"enter",1,false);        hideSystemBars();
    }
    private void resizeGame() {
        if(mLayout==null || mSurface==null || mLayout.getWidth()==0)return;
        int width=mLayout.getWidth(),height=mLayout.getHeight();
        if(!getSharedPreferences("rpg-controls-ui",0).getBoolean("fill",false)) {
            width=Math.min(width,height*logicalWidth/logicalHeight);height=Math.min(height,width*logicalHeight/logicalWidth);
        }
        android.widget.RelativeLayout.LayoutParams p=(android.widget.RelativeLayout.LayoutParams)mSurface.getLayoutParams();
        if(p.width!=width || p.height!=height){p.width=width;p.height=height;mSurface.setLayoutParams(p);}
    }
    private android.app.AlertDialog optionsDialog;
    private void showRpgOptions() {
        if(isFinishing()||isDestroyed()||(optionsDialog!=null&&optionsDialog.isShowing()))return;
        String[] options={tr("Abrir teclado","Open keyboard"),tr("Mover botones","Move controls"),
            controlsHidden?tr("Mostrar controles","Show controls"):tr("Ocultar controles","Hide controls"),
            tr("Pantalla: ","Display: ")+(getSharedPreferences("rpg-controls-ui",0).getBoolean("fill",false)?tr("completa","full screen"):tr("proporción original","original aspect")),
            tr("Restablecer botones","Reset controls"),tr("Pausar y volver a BRUMA","Pause and return to BRUMA"),tr("Cerrar juego","Close game")};
        optionsDialog=new android.app.AlertDialog.Builder(this).setTitle(tr("Opciones del juego","Game options")).setItems(options,(dialog,which)->{
            applyRpgOption(which);
        }).create();
        optionsDialog.setOnDismissListener(d->{optionsDialog=null;hideSystemBars();});
        optionsDialog.show();
    }
    private void applyRpgOption(int which) {
            switch(which){
                case 0:SDLActivity.showTextInput(0,0,1,1);break;
                case 1:controlsHidden=false;getSharedPreferences("rpg-controls-ui",0).edit().putBoolean("hidden",false).apply();editingControls=true;mGamepad.edit(true);optionsButton.setText(tr("Guardar","Done"));android.widget.Toast.makeText(this,tr("Arrastra los botones y pulsa Guardar","Drag controls, then tap Done"),android.widget.Toast.LENGTH_LONG).show();break;
                case 2:controlsHidden=!controlsHidden;getSharedPreferences("rpg-controls-ui",0).edit().putBoolean("hidden",controlsHidden).apply();if(controlsHidden)mGamepad.hideView();else mGamepad.showView();break;
                case 3:android.content.SharedPreferences prefs=getSharedPreferences("rpg-controls-ui",0);prefs.edit().putBoolean("fill",!prefs.getBoolean("fill",false)).apply();resizeGame();break;
                case 4:mGamepad.resetPositions();movableUtilities.reset();break;
                case 5:returnToBrumaLibrary();break;
                case 6:closeRpgGame();break;
            }
            hideSystemBars();
    }    private android.widget.Button utilityButton(String title) {
        android.widget.Button button=new android.widget.Button(this);
        button.setText(title);button.setAllCaps(false);button.setTextSize(13);
        button.setTextColor(0xffeeeeff);button.setFocusable(false);
        android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();
        background.setColor(0xcc171c2b);background.setCornerRadius(dp(16));
        background.setStroke(dp(1),0xff626a83);button.setBackground(background);
        button.setPadding(dp(8),0,dp(8),0);
        return button;
    }
    private void hideSystemBars() {
        android.view.Window window=getWindow();
        android.view.View decor=window.getDecorView();
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Keep SDL's visibility-change recovery enabled alongside WindowInsets.
        SDLActivity.mFullscreenModeActive=true;
        decor.setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_FULLSCREEN |
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        if(Build.VERSION.SDK_INT>=30) {
            android.view.WindowInsetsController controller=window.getInsetsController();
            if(controller!=null) {
                controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(android.view.WindowInsets.Type.systemBars());
            }
        }
    }
    @Override protected void onPause() {if(mStarted&&!isFinishing()&&!closingToLibrary)pauseOnReturn=true;mGamepad.releaseAll();super.onPause();if(sessionHost!=null)sessionHost.pause(true);}
    private void closeRpgGame() {
        pauseOnReturn=false;
        closingToLibrary=true;
        if(mStarted)pauseNativeThread();
        Intent home=new Intent(this,com.linkcore.emulator.MainActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(home);
        finish();
    }    private void returnToBrumaLibrary() {
        if(mStarted)pauseNativeThread();
        if(sessionHost!=null)sessionHost.pause(true);
        Intent home=new Intent(this,com.linkcore.emulator.MainActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(home);
    }
    @Override public void onBackPressed() { showRpgOptions(); }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.getBooleanExtra("brumaResume",false)){setIntent(intent);pauseOnReturn=false;}
        String requested=intent.getStringExtra("gamePath");
        if(requested==null)return;
        try {
            File root=new File(getFilesDir(),"rpg-games").getCanonicalFile();
            File game=new File(requested).getCanonicalFile();
            if(!game.getPath().startsWith(root.getPath()+File.separator)||!new File(game,"Game.ini").isFile())return;
            if(game.getPath().equals(new File(GAME_PATH).getCanonicalPath())){setIntent(intent);return;}
            android.util.AtomicFile pending=new android.util.AtomicFile(new File(getFilesDir(),"rpg-pending-launch.txt"));
            java.io.FileOutputStream stream=null;
            try {stream=pending.startWrite();stream.write(game.getPath().getBytes("UTF-8"));pending.finishWrite(stream);}catch(Exception e){if(stream!=null)pending.failWrite(stream);throw e;}
            finish();
        } catch(Exception e) {Log.e(TAG,"Could not queue the next RPG game",e);}
    }
    @Override protected void onResume() {
        super.onResume();
        if(sessionHost!=null)sessionHost.pause(false);
        if(getIntent().getBooleanExtra("brumaResume",false)){getIntent().removeExtra("brumaResume");pauseOnReturn=false;if(mStarted)resumeNativeThread();}
        getWindow().getDecorView().postDelayed(this::hideSystemBars,200);
        if(pauseOnReturn&&mStarted&&!mBrokenLibraries){
            pauseOnReturn=false;
            pauseNativeThread();
            getWindow().getDecorView().post(this::showReturnPauseDialog);
        }
    }
    private void showReturnPauseDialog() {
        if(isFinishing()||isDestroyed()){resumeNativeThread();return;}
        new android.app.AlertDialog.Builder(this)
            .setTitle(tr("Juego en pausa","Game paused"))
            .setMessage(tr("El juego se ha pausado al salir de BRUMA.","The game was paused when you left BRUMA."))
            .setPositiveButton(tr("Continuar","Resume"),(dialog,which)->resumeNativeThread())
            .setNegativeButton(tr("Volver a BRUMA","Return to BRUMA"),(dialog,which)->returnToBrumaLibrary())
            .setCancelable(false)
            .show();
    }
    @Override public void onInputDeviceAdded(int id) {}
    @Override public void onInputDeviceChanged(int id) {mGamepad.releaseDevice(id);}
    @Override public void onInputDeviceRemoved(int id) {mGamepad.releaseDevice(id);}
}
