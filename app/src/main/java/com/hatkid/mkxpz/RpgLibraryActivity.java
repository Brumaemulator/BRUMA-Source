package com.hatkid.mkxpz;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.database.Cursor;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import org.json.*;

public class RpgLibraryActivity extends Activity {
    @Override protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(com.linkcore.emulator.BrumaLocale.attach(newBase));
    }
    private final java.util.concurrent.ExecutorService worker=Executors.newSingleThreadExecutor();
    private boolean importing=false;
    private File gamesRoot(){File root=new File(getFilesDir(),"rpg-games" );if(!root.exists())root.mkdirs();return root;}
    private boolean es(){return getResources().getConfiguration().getLocales().get(0).getLanguage().equals("es");}
    private String tr(String spanish,String english){return es()?spanish:english;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        String uri=getIntent().getStringExtra("launchUri");
        if(uri==null){show();return;}
        worker.execute(()->{
            try {
                Uri selected=Uri.parse(uri);
                if("file".equals(selected.getScheme())) {
                    File dir=new File(selected.getPath()).getCanonicalFile();
                    if(!dir.getPath().startsWith(gamesRoot().getCanonicalPath()+File.separator))throw new IOException();
                    runOnUiThread(()->launch(dir));
                } else if("content".equals(selected.getScheme())) {
                    File[] existing=gamesRoot().listFiles();
                    if(existing!=null)for(File dir:existing) {
                        if(new File(dir,"bruma-origin.txt").isFile()&&read(new File(dir,"bruma-origin.txt")).trim().equals(uri)){
                            runOnUiThread(()->launch(dir));return;
                        }
                    }
                    runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())importGame(selected,DocumentsContract.getDocumentId(selected),uri);});
                } else runOnUiThread(this::failLaunch);
            } catch(Exception e){runOnUiThread(this::failLaunch);}
        });
    }
    private void failLaunch(){if(isFinishing()||isDestroyed())return;new AlertDialog.Builder(this).setMessage(tr("No se pudo abrir el juego.","Could not open game.")).setPositiveButton(android.R.string.ok,(d,w)->finish()).setOnCancelListener(d->finish()).show();}
    private boolean launching;
    private void launch(File dir) {
        if(launching||isFinishing()||isDestroyed())return;
        launching=true;
        worker.execute(()->{
            try {
                long started=android.os.SystemClock.elapsedRealtime();
                com.linkcore.emulator.RpgCacheCleanup.restore(this,dir);
                boolean classic=com.linkcore.emulator.RpgEngine.classic(this,dir);
                if(classic)prepareClassic(dir);else prepare(dir);
                android.util.Log.i("BRUMA-RPG","Prepared runtime in "+(android.os.SystemClock.elapsedRealtime()-started)+" ms; classic="+classic);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    com.linkcore.emulator.RuntimeSessionHost.closeOthers(this,(classic?ClassicActivity.class:MainActivity.class).getName());
                    getSharedPreferences("rpg",0).edit().putString("path",dir.getAbsolutePath()).apply();
                    startActivity(new Intent(this,classic?ClassicActivity.class:MainActivity.class).putExtra("gamePath",dir.getAbsolutePath()));finish();
                });
            }catch(Exception error){android.util.Log.e("BRUMA-RPG","Runtime preparation failed",error);runOnUiThread(()->{launching=false;if(!isFinishing()&&!isDestroyed())failLaunch();});}
        });
    }
    private void prepareClassic(File dir)throws Exception {
        new File(dir,"UserData/Temp").mkdirs();
        // Public Ruby 1.9.3-p551, BSD-2-Clause option; owned RGSS1 adapter.
        JSONObject config=new JSONObject();
        config.put("printFPS",com.linkcore.emulator.BuildConfig.DEBUG);
        config.put("gameFolder",dir.getAbsolutePath());config.put("rtps",new JSONArray().put(dir.getAbsolutePath()));
        config.put("rgssVersion",1);config.put("vsync",true);config.put("syncToRefreshrate",false);config.put("frameSkip",false);config.put("fixedFramerate",0);config.put("fullscreen",true);config.put("fontScale",1.0);config.put("execName","Game");config.put("pathCache",true);config.put("prebuiltPathCache",false);config.put("fastPathEnum",false);
        File rubyCompatibility=new File(getFilesDir(),"bruma-ruby193-compat.rb");
        try(InputStream input=getAssets().open("rpg/bruma-ruby193-compat.rb");ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1)bytes.write(buffer,0,count);
            atomicWrite(rubyCompatibility,bytes.toString("UTF-8"));
        }
        config.put("preloadScripts",new JSONArray().put(rubyCompatibility.getAbsolutePath()));
        File external=getExternalFilesDir(null);if(external==null)throw new IOException("External app storage unavailable");
        atomicWrite(new File(external,"mkxp.json"),ConfigJson.nativeText(config));
    }
    private String title(File dir){try {for(String line:read(new File(dir,"Game.ini")).split("\n")) if(line.trim().startsWith("Title=")) return line.trim().substring(6);}catch(Exception ignored){} return dir.getName();}
    private String read(File file)throws IOException {try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>1048576)throw new IOException();out.write(b,0,n);}return out.toString("UTF-8");}}
    private void show(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(24),dp(28),dp(24),dp(20));box.setBackgroundColor(0xff0b0e18);
        TextView heading=new TextView(this);heading.setText("BRUMA RPG");heading.setTextColor(0xff79ecd0);heading.setTextSize(28);box.addView(heading);
        TextView note=new TextView(this);note.setText(tr("Experimental · RPG Maker XP\nBruma no incluye juegos. Importa los tuyos; la compatibilidad depende de cada juego.\n","Experimental · RPG Maker XP\nBruma includes no games. Import your own; compatibility varies by game.\n"));note.setTextColor(0xffd4d8e4);note.setTextSize(15);box.addView(note);
        File[] dirs=gamesRoot().listFiles();if(dirs!=null) for(File dir:dirs) if(new File(dir,"Game.ini").isFile()&&!dir.getName().endsWith(".pending")) {
            Button game=new Button(this);game.setAllCaps(false);game.setText("▶  "+title(dir));game.setOnClickListener(v->{try{launch(dir);}catch(Exception e){new AlertDialog.Builder(this).setMessage(tr("No se pudo preparar el juego. Comprueba el espacio disponible.","Could not prepare the game. Check available storage.")).setPositiveButton(android.R.string.ok,null).show();}});box.addView(game);
        }
        Button add=new Button(this);add.setAllCaps(false);add.setText(tr("Importar carpeta de juego","Import game folder"));add.setEnabled(!importing);add.setOnClickListener(v->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),10));box.addView(add);
        TextView help=new TextView(this);help.setText(tr("Selecciona la carpeta que contiene Game.ini, Data, Graphics y Audio. Se copiará al almacenamiento de Bruma RPG; el original no se modifica.\n\nMotor mkxp-z para Android · GPL. Imágenes y juegos pertenecen a sus autores.","Select the folder containing Game.ini, Data, Graphics and Audio. It will be copied into Bruma RPG storage; the original stays unchanged.\n\nmkxp-z Android engine · GPL. Game artwork and games belong to their authors."));help.setTextColor(0xffabb2c6);help.setPadding(0,dp(16),0,0);box.addView(help);
        ScrollView scroll=new ScrollView(this);scroll.addView(box);setContentView(scroll);
    }
    private void removePending(File file) {File[] children=file.listFiles();if(children!=null)for(File child:children)removePending(child);file.delete();}
    private void prepare(File dir)throws Exception {
        if(!new File(dir,"Game.ini").isFile() || !new File(dir,"Data/Scripts.rxdata").isFile())throw new IOException("Not RPG Maker XP");
        File configFile=new File(dir,"mkxp.json");JSONObject config=configFile.exists()?ConfigJson.parse(read(configFile)):new JSONObject();
        JSONArray scripts=new JSONArray();scripts.put("bruma-compat.rb");JSONArray old=config.optJSONArray("preloadScript");if(old!=null)for(int n=0;n<old.length();n++)if(!old.getString(n).equals("bruma-compat.rb"))scripts.put(old.getString(n));
        config.put("syncToRefreshrate",false);config.put("fixedFramerate",0);config.put("vsync",true);config.put("preloadScript",scripts);config.put("fullscreen",true);if(dir.getName().endsWith(".pending")&&!config.has("dataPathApp"))config.put("dataPathApp",dir.getName().replace(".pending",""));
        File backup=new File(dir,"mkxp.json.before-bruma");
        if(configFile.exists()&&!backup.exists())java.nio.file.Files.copy(configFile.toPath(),backup.toPath());
                String compatibility;
        try(InputStream asset=getAssets().open("rpg/bruma-compat.rb");ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[4096];int n;while((n=asset.read(buffer))!=-1)out.write(buffer,0,n);
            compatibility=out.toString("UTF-8");
        }
        if(getSharedPreferences("rpg",0).getBoolean("fastUi:"+dir.getName(),true)) {
            try(InputStream asset=getAssets().open("rpg/bruma-ui.rb");ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] buffer=new byte[4096];int n;while((n=asset.read(buffer))!=-1)out.write(buffer,0,n);
                compatibility+="\n"+out.toString("UTF-8");
            }
        }
        atomicWrite(new File(dir,"bruma-compat.rb"),compatibility);
        atomicWrite(configFile,ConfigJson.nativeText(config));
    }
    private void atomicWrite(File file,String text)throws IOException {
        android.util.AtomicFile atomic=new android.util.AtomicFile(file);
        FileOutputStream stream=null;
        try{stream=atomic.startWrite();stream.write(text.getBytes("UTF-8"));atomic.finishWrite(stream);}
        catch(IOException e){if(stream!=null)atomic.failWrite(stream);throw e;}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=10||result!=RESULT_OK||data==null||data.getData()==null)return;
        importGame(data.getData(),DocumentsContract.getTreeDocumentId(data.getData()),data.getData().toString());
    }
    private void importGame(Uri tree,String documentId,String origin) {
        importing=true;ProgressDialog progress=new ProgressDialog(this);progress.setTitle(tr("Importando juego","Importing game"));progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);progress.setIndeterminate(true);progress.setMessage(tr("Preparando archivos… Solo es necesario la primera vez.","Preparing files… Only needed the first time."));progress.setCancelable(false);progress.show();
        worker.execute(()->{
            String id="game-"+System.currentTimeMillis();File temp=new File(gamesRoot(),id+".pending");String error=null;
            try{new RpgImporter(getContentResolver(),(done,total,copied,expected)->runOnUiThread(()->{
                if(!isDestroyed()) {progress.setIndeterminate(false);progress.setMax(Math.max(1,total));progress.setProgress(done);
                    progress.setMessage(String.format(Locale.getDefault(),tr("%d de %d archivos · %.1f de %.1f MB","%d of %d files · %.1f of %.1f MB"),done,total,copied/1048576.0,expected/1048576.0));}
            })).copy(tree,documentId,temp);atomicWrite(new File(temp,"bruma-origin.txt"),origin);prepare(temp);File ready=new File(gamesRoot(),id);if(!temp.renameTo(ready))throw new IOException();new com.linkcore.emulator.LibraryStore(this).linkRpg(origin,ready);}catch(Exception e){removePending(temp);error=tr("No se ha podido importar. Selecciona la carpeta raíz de un juego RPG Maker XP completo y comprueba el espacio disponible.","Import failed. Select the root folder of a complete RPG Maker XP game and check available storage.");}
            final String message=error;runOnUiThread(()->{importing=false;if(!isDestroyed()){progress.dismiss();if(getIntent().hasExtra("launchUri")){if(message==null){try{launch(new File(gamesRoot(),id));}catch(Exception e){failLaunch();}}else new AlertDialog.Builder(this).setMessage(message).setPositiveButton(android.R.string.ok,(d,w)->finish()).setOnCancelListener(d->finish()).show();return;}show();if(message!=null)new AlertDialog.Builder(this).setMessage(message).setPositiveButton(android.R.string.ok,null).show();}});
        });
    }
    @Override protected void onDestroy(){worker.shutdown();super.onDestroy();}
}









