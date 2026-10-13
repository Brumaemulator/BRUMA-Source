package com.linkcore.emulator;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import java.io.*;
import java.util.*;

public class LibraryFixtureProvider extends ContentProvider {
    private static final String[] TITLES={"Aurora.gba","Circuit.gba","Nebula.gba","Orbit.gba","Pixel Quest.gba","Velocity.gba"};
    private static final String[] COLUMNS={"document_id","_display_name","mime_type","_size","last_modified","flags"};
    private File directory;
    public boolean onCreate() {
        directory=new File(getContext().getCacheDir(),"library-fixture");directory.mkdirs();
        try {
            try(FileWriter out=new FileWriter(new File(directory,"Game.ini"))){out.write("[Game]\nTitle=RPG fixture\n");}
            for(String title:TITLES) try(InputStream in=getContext().getAssets().open("smoke.gba");OutputStream out=new FileOutputStream(new File(directory,title))) {
                byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1) out.write(buffer,0,n); out.write(java.util.Arrays.asList(TITLES).indexOf(title));
            }
            for(String name:new String[]{"Aurora.png","Nebula.JPG"}) {
                Bitmap image=Bitmap.createBitmap(256,300,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setShader(new LinearGradient(0,0,256,300,0xff6f59ce,0xff13394a,Shader.TileMode.CLAMP));canvas.drawPaint(p);p.setShader(null);
                p.setColor(0xff76edcd);canvas.drawCircle(128,105,54,p);p.setColor(0xff101628);
                Path path=new Path();path.moveTo(0,270);path.lineTo(100,120);path.lineTo(190,260);path.lineTo(256,155);path.lineTo(256,300);path.lineTo(0,300);path.close();canvas.drawPath(path,p);
                p.setColor(Color.WHITE);p.setTextSize(22);canvas.drawText(name.substring(0,name.indexOf('.')).toUpperCase(Locale.ROOT),20,38,p);
                try(OutputStream out=new FileOutputStream(new File(directory,name))) {image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();
            }
        } catch(IOException error) {throw new RuntimeException(error);}
        return true;
    }
    private void add(MatrixCursor c,String id) {
        boolean dir=id.equals("root") || id.equals("sub") || id.equals("rpg") || id.equals("Data");File file=new File(directory,id);
        Map<String,Object> values=new HashMap<>();values.put("document_id",id);values.put("_display_name",id);values.put("mime_type",dir?DocumentsContract.Document.MIME_TYPE_DIR:(id.endsWith("gba")?"application/octet-stream":"image/png"));values.put("_size",dir?0L:file.length());values.put("last_modified",1L);values.put("flags",0);
        Object[] row=new Object[c.getColumnCount()];for(int n=0;n<row.length;n++) row[n]=values.get(c.getColumnNames()[n]);c.addRow(row);
    }
    public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort) {
        MatrixCursor c=new MatrixCursor(projection==null?COLUMNS:projection);String id=DocumentsContract.getDocumentId(uri);
        if("children".equals(uri.getLastPathSegment())) {
            if(id.equals("root")) {for(int n=0;n<3;n++) add(c,TITLES[n]);add(c,"Aurora.png");add(c,"Nebula.JPG");add(c,"sub");}
            else if(id.equals("sub")) for(int n=3;n<6;n++) add(c,TITLES[n]);
            else if(id.equals("rpg")){add(c,"Game.ini");add(c,"Data");}
            else if(id.equals("Data")){add(c,"Scripts.rxdata");}
            else throw new IllegalArgumentException("Unknown fixture directory");
        } else add(c,id);
        return c;
    }
    public String getType(Uri uri) {return "application/octet-stream";}
    public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        String id=DocumentsContract.getDocumentId(uri);
        if(!mode.equals("r") || !(Arrays.asList(TITLES).contains(id) || id.equals("Aurora.png") || id.equals("Nebula.JPG") || id.equals("Game.ini"))) throw new FileNotFoundException();
        return ParcelFileDescriptor.open(new File(directory,id),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    public Uri insert(Uri uri,ContentValues values) {throw new UnsupportedOperationException();}
    public int delete(Uri uri,String where,String[] args) {throw new UnsupportedOperationException();}
    public int update(Uri uri,ContentValues values,String where,String[] args) {throw new UnsupportedOperationException();}
}

